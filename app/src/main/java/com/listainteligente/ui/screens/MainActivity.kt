package com.listainteligente.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
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
                listId     = listId,
                onScan     = { navController.navigate("scanner/$listId") },
                onScanItem = { itemId -> navController.navigate("scanner/$listId?itemId=$itemId") },
                onBack     = { navController.popBackStack() }
            )
        }

        // Tela de escaneamento de etiqueta.
        // itemId ausente (-1) → escaneou pra adicionar um item novo.
        // itemId presente     → escaneou pra precificar um item já existente na lista.
        composable(
            route = "scanner/{listId}?itemId={itemId}",
            arguments = listOf(
                navArgument("itemId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStack ->
            val listId = backStack.arguments?.getString("listId")?.toLongOrNull() ?: return@composable
            val itemId = backStack.arguments?.getLong("itemId") ?: -1L
            val vm: ShoppingViewModel = androidx.hilt.navigation.compose.hiltViewModel(
                navController.getBackStackEntry("detail/$listId")
            )
            ScannerScreen(
                listId = listId,
                onItemAdded = { name, qty, price, label ->
                    val existing = if (itemId != -1L) vm.findItem(itemId) else null
                    if (existing != null) {
                        vm.setScannedPriceAndCheck(existing, qty, price, label)
                    } else {
                        vm.addItem(listId, name, qty, price, label)
                    }
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
