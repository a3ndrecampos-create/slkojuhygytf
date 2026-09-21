package com.listainteligente.service

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.listainteligente.model.ScannedLabel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.Executors

/** Resultado de um ciclo de análise de frame da câmera. */
sealed class ScanEvent {
    data class Detected(val label: ScannedLabel) : ScanEvent()
    /** OCR rodou mas não achou nada parecido com etiqueta de preço no frame atual. */
    object NoLabel : ScanEvent()
    /** O reconhecedor de texto falhou (ex: modelo de OCR ainda baixando na 1ª vez). */
    data class Error(val message: String) : ScanEvent()
}

class CameraService(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val executor   = Executors.newSingleThreadExecutor()

    /**
     * Inicia a câmera e emite um ScanEvent a cada frame analisado:
     * etiqueta detectada, nada encontrado ainda, ou erro do OCR.
     */
    fun startScanning(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner
    ): Flow<ScanEvent> = callbackFlow {

        val providerFuture = ProcessCameraProvider.getInstance(context)
        var boundProvider: ProcessCameraProvider? = null

        providerFuture.addListener({
            val provider = providerFuture.get()
            boundProvider = provider

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(executor) { imageProxy ->
                processFrame(imageProxy) { event ->
                    trySend(event)
                }
            }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (e: Exception) {
                trySend(ScanEvent.Error(e.message ?: "Falha ao iniciar a câmera"))
            }
        }, ContextCompat.getMainExecutor(context))

        awaitClose {
            // Desvincula a câmera ao sair da tela — sem isso ela continuaria
            // presa ao ciclo de vida da Activity mesmo após fechar o scanner.
            ContextCompat.getMainExecutor(context).execute {
                boundProvider?.unbindAll()
            }
            recognizer.close()
            executor.shutdown()
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun processFrame(
        imageProxy: ImageProxy,
        onResult: (ScanEvent) -> Unit
    ) {
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val raw = visionText.text
                if (isPriceLabel(raw)) {
                    val label = LabelParser.parse(raw)
                    if (label.priceOptions.isNotEmpty()) {
                        onResult(ScanEvent.Detected(label))
                    } else {
                        onResult(ScanEvent.NoLabel)
                    }
                } else {
                    onResult(ScanEvent.NoLabel)
                }
            }
            .addOnFailureListener { e ->
                // Antes esse erro era engolido em silêncio (sem addOnFailureListener),
                // por isso a câmera parecia "não ler nada" — na maioria das vezes é o
                // modelo de OCR ainda sendo baixado pelo Play Services na 1ª execução.
                onResult(ScanEvent.Error(e.message ?: "Erro ao processar imagem"))
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    /**
     * Heurística: procura por preço em R$ + algum contexto de etiqueta.
     * Tolera OCR ruidoso (símbolo de R$ mal lido, espaçamento estranho).
     */
    private fun isPriceLabel(text: String): Boolean {
        val upper = text.uppercase()

        val hasCurrencyPrice = Regex("""R\s*\$?\s*\d{1,4}[,.]\d{2}""").containsMatchIn(upper)
        val hasBarePrice     = Regex("""\b\d{1,4}[,.]\d{2}\b""").containsMatchIn(text)
        val hasKeyword       = upper.contains("VAREJO") || upper.contains("ATACADO") ||
                                upper.contains("UNIDADE") || upper.contains("UNITARIO") ||
                                upper.contains("UNITÁRIO") || upper.contains("KG") ||
                                upper.contains("CREDIFFATO") || upper.contains("PREÇO") ||
                                upper.contains("PRECO")

        return hasCurrencyPrice || (hasBarePrice && hasKeyword)
    }
}
