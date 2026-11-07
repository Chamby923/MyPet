package com.example.mypet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mypet.ui.screens.*
import com.example.mypet.ui.theme.MyPetTheme
import com.example.mypet.ui.viewmodel.AuthViewModel
import com.example.mypet.ui.viewmodel.PetViewModel

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val petViewModel: PetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyPetTheme {
                MyPetApp(authViewModel, petViewModel)
            }
        }
    }
}

@Composable
fun MyPetApp(authViewModel: AuthViewModel, petViewModel: PetViewModel) {
    val navController = rememberNavController()
    
    // Si ya hay un usuario logueado, empezamos en Home
    val startDestination = if (authViewModel.currentUser != null) "home" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = { navController.navigate("register") },
                onLoginSuccess = { 
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("register") {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                petViewModel = petViewModel,
                authViewModel = authViewModel,
                onNavigateToAddPet = { navController.navigate("add_pet") },
                onNavigateToPetDetail = { petId -> navController.navigate("pet_detail/$petId") },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
        composable("add_pet") {
            AddPetScreen(
                viewModel = petViewModel,
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }
        composable("pet_detail/{petId}") { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: ""
            PetDetailScreen(
                petId = petId,
                viewModel = petViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
