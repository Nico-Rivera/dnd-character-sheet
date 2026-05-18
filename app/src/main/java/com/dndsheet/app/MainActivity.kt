package com.dndsheet.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dndsheet.app.nav.Routes
import com.dndsheet.app.ui.character.CharacterListScreen
import com.dndsheet.app.ui.character.CharacterOverviewScreen
import com.dndsheet.app.ui.theme.DnDTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DnDTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNav()
                }
            }
        }
    }
}

@Composable
private fun AppNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.LIST) {
        composable(Routes.LIST) { entry ->
            CharacterListScreen(
                onOpen = { id -> if (entry.canNavigate()) nav.navigate(Routes.overview(id)) }
            )
        }
        composable(
            route = Routes.OVERVIEW_PATTERN,
            arguments = listOf(navArgument(Routes.OVERVIEW_ARG_ID) { type = NavType.StringType })
        ) { entry ->
            val id = entry.arguments?.getString(Routes.OVERVIEW_ARG_ID).orEmpty()
            CharacterOverviewScreen(
                characterId = id,
                onBack = { if (entry.canNavigate()) nav.popBackStack() }
            )
        }
    }
}

/**
 * Tap-spam guard. A NavBackStackEntry is RESUMED only while its screen is
 * the active top of the stack. As soon as a navigation starts the entry
 * drops to STARTED, so a second rapid tap on a back/open button is a
 * no-op until the destination settles. This is the recommended Compose
 * Nav pattern for single-press navigation.
 */
private fun NavBackStackEntry.canNavigate(): Boolean =
    lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
