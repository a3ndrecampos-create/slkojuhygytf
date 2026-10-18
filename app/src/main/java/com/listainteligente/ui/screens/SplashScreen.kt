package com.listainteligente.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listainteligente.ui.theme.Green500
import com.listainteligente.ui.theme.Green700
import com.listainteligente.ui.theme.Green900
import kotlinx.coroutines.delay

/**
 * Tela de abertura: logo animado no centro e a assinatura "From André Campos" embaixo.
 * Começa no mesmo verde da splash do sistema, então a transição é contínua.
 */
@Composable
fun AppSplashScreen(onFinished: () -> Unit) {
    val logoScale = remember { Animatable(0.7f) }
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val footerAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, tween(450))
    }
    LaunchedEffect(Unit) {
        logoScale.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        delay(350)
        textAlpha.animateTo(1f, tween(450))
    }
    LaunchedEffect(Unit) {
        delay(700)
        footerAlpha.animateTo(1f, tween(450))
    }
    LaunchedEffect(Unit) {
        delay(2200)
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Green900, Green700, Green500)))
    ) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
                    .size(110.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Checklist, null, tint = Green900, modifier = Modifier.size(64.dp))
            }
            Spacer(Modifier.height(24.dp))
            Column(
                Modifier.alpha(textAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Lista Inteligente",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Compre melhor, gaste menos",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 36.dp)
                .alpha(footerAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("From", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                "André Campos",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
