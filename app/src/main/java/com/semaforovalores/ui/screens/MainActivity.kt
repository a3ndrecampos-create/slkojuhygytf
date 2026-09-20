package com.semaforovalores.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import com.semaforovalores.ui.theme.SemaforoTheme

class MainActivity : ComponentActivity() {

    /** Incrementado em onResume() para reavaliar permissões ao voltar das configurações. */
    private var refreshTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SemaforoTheme {
                MainScreen(refreshTick)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshTick++
    }
}

@Composable
private fun MainScreen(refreshTick: Int) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Início" to "🚦", "Histórico" to "🕘", "Ajustes" to "⚙️")

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, (label, emoji) ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Text(emoji) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { inner ->
        val modifier = Modifier.padding(inner)
        when (tab) {
            0 -> HomeScreen(refreshTick, modifier)
            1 -> HistoryScreen(modifier)
            else -> SettingsScreen(modifier)
        }
    }
}
