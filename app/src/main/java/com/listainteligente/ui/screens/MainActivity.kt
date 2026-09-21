package com.listainteligente.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.compose.*
import com.listainteligente.ui.theme.ListaInteligenteTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ListaInteligenteTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {

        // Tela inicial: listas
        composable("home") {
            HomeScreen(
                onOpenList = { listId -> navController.navigate("detail/$listId") }
            )
        }

        // Tela de detalhes da lista
        composable("detail/{listId}") { backStack ->
            val listId = backStack.arguments?.getString("listId")?.toLongOrNull() ?: return@composable
            ListDetailScreen(
                listId = listId,
                onScan = { navController.navigate("scanner/$listId") },
                onBack = { navController.popBackStack() }
            )
        }

        // Tela de escaneamento de etiqueta
        composable("scanner/{listId}") { backStack ->
            val listId = backStack.arguments?.getString("listId")?.toLongOrNull() ?: return@composable
            val vm: ShoppingViewModel = androidx.hilt.navigation.compose.hiltViewModel(
                navController.getBackStackEntry("detail/$listId")
            )
            ScannerScreen(
                listId = listId,
                onItemAdded = { name, qty, price, label ->
                    vm.addItem(listId, name, qty, price, label)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
