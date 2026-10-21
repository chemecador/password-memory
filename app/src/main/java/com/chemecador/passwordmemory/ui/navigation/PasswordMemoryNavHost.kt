package com.chemecador.passwordmemory.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chemecador.passwordmemory.ui.autofill.AutofillSettingsScreen
import com.chemecador.passwordmemory.ui.backup.BackupScreen
import com.chemecador.passwordmemory.ui.detail.EntryDetailScreen
import com.chemecador.passwordmemory.ui.edit.EditEntryScreen
import com.chemecador.passwordmemory.ui.list.EntryListScreen
import com.chemecador.passwordmemory.ui.list.ServiceEntriesScreen
import com.chemecador.passwordmemory.ui.privacy.PrivacyPolicyScreen
import com.chemecador.passwordmemory.ui.settings.SettingsScreen

@Composable
fun PasswordMemoryNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destinations.LIST,
        modifier = modifier
    ) {
        composable(Destinations.LIST) {
            EntryListScreen(
                onEntryClick = { navController.navigate(Destinations.detail(it)) },
                onServiceClick = { navController.navigate(Destinations.service(it)) },
                onCreateClick = { navController.navigate(Destinations.edit()) },
                onBackupClick = { navController.navigate(Destinations.BACKUP) },
                onAutofillClick = { navController.navigate(Destinations.AUTOFILL) },
                onSettingsClick = { navController.navigate(Destinations.SETTINGS) },
                onPrivacyClick = { navController.navigate(Destinations.PRIVACY) }
            )
        }
        composable(
            route = Destinations.SERVICE,
            arguments = listOf(navArgument(Destinations.ARG_SERVICE_KEY) {
                type = NavType.StringType
            })
        ) {
            ServiceEntriesScreen(
                onBack = navController::popBackStack,
                onEntryClick = { navController.navigate(Destinations.detail(it)) },
                onAddAccountClick = { navController.navigate(Destinations.createForService(it)) }
            )
        }
        composable(
            route = Destinations.DETAIL,
            arguments = listOf(navArgument(Destinations.ARG_ENTRY_ID) { type = NavType.LongType })
        ) {
            EntryDetailScreen(
                onBack = navController::popBackStack,
                onEditClick = { navController.navigate(Destinations.edit(it)) },
                onAddAccountClick = { navController.navigate(Destinations.createForService(it)) }
            )
        }
        composable(
            route = Destinations.EDIT,
            arguments = listOf(
                navArgument(Destinations.ARG_ENTRY_ID) { type = NavType.LongType },
                // Only set when the form was opened from a service, to fill its name in.
                navArgument(Destinations.ARG_SERVICE_NAME) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            EditEntryScreen(onDone = navController::popBackStack)
        }
        composable(Destinations.BACKUP) {
            BackupScreen(onBack = navController::popBackStack)
        }
        composable(Destinations.AUTOFILL) {
            AutofillSettingsScreen(onBack = navController::popBackStack)
        }
        composable(Destinations.SETTINGS) {
            SettingsScreen(onBack = navController::popBackStack)
        }
        composable(Destinations.PRIVACY) {
            PrivacyPolicyScreen(onBack = navController::popBackStack)
        }
    }
}
