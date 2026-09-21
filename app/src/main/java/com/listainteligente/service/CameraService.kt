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

class CameraService(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val executor   = Executors.newSingleThreadExecutor()

    /**
     * Inicia a câmera e emite ScannedLabel sempre que detectar
     * uma etiqueta de preço válida no frame.
     */
    fun startScanning(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner
    ): Flow<ScannedLabel> = callbackFlow {

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
                processFrame(imageProxy) { label ->
                    trySend(label)
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
                e.printStackTrace()
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
        onResult: (ScannedLabel) -> Unit
    ) {
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val raw = visionText.text
                // Só processa se parecer uma etiqueta de preço
                if (isPriceLabel(raw)) {
                    val label = LabelParser.parse(raw)
                    if (label.priceOptions.isNotEmpty()) {
                        onResult(label)
                    }
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    /**
     * Heurística: texto tem "R$" e pelo menos um número com vírgula/ponto
     */
    private fun isPriceLabel(text: String): Boolean {
        val upper = text.uppercase()
        return upper.contains("R$") &&
               (upper.contains("VAREJO") || upper.contains("ATACADO") ||
                upper.contains("UNIDADE") || upper.contains("KG") ||
                Regex("""\d+[,\.]\d{2}""").containsMatchIn(text))
    }
}
