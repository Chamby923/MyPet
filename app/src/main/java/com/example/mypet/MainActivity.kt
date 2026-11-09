package com.example.mypet

import android.os.Bundle
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mypet.ui.screens.*
import com.example.mypet.ui.theme.MyPetTheme
import com.example.mypet.ui.viewmodel.AuthViewModel
import com.example.mypet.ui.viewmodel.PetViewModel
import com.example.mypet.ui.viewmodel.NfcAvailability
import com.example.mypet.ui.viewmodel.NfcWriteViewModel

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val petViewModel: PetViewModel by viewModels()
    private val nfcViewModel: NfcWriteViewModel by viewModels()
    private var nfcAdapter: NfcAdapter? = null
    private var nfcResumed by mutableStateOf(false)
    private var readerEnabled = false
    private val nfcStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshNfcAvailability()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = (getSystemService(Context.NFC_SERVICE) as? NfcManager)?.defaultAdapter
        setContent {
            val state = nfcViewModel.state
            LaunchedEffect(state.reading, state.attempt, nfcResumed) {
                setNfcReading(nfcResumed && state.reading, state.attempt)
            }
            MyPetTheme {
                MyPetApp(authViewModel, petViewModel, nfcViewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this, nfcStateReceiver, IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED),
            // El cambio de estado puede proceder del servicio NFC con un UID propio.
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    override fun onResume() {
        super.onResume()
        refreshNfcAvailability()
        nfcResumed = true
    }

    override fun onPause() {
        nfcResumed = false
        setNfcReading(false, nfcViewModel.state.attempt)
        nfcViewModel.pause()
        super.onPause()
    }

    override fun onStop() {
        unregisterReceiver(nfcStateReceiver)
        super.onStop()
    }

    private fun refreshNfcAvailability() {
        nfcViewModel.updateAvailability(when {
            nfcAdapter == null -> NfcAvailability.UNSUPPORTED
            nfcAdapter?.isEnabled == true -> NfcAvailability.AVAILABLE
            else -> NfcAvailability.DISABLED
        })
    }

    private fun setNfcReading(enabled: Boolean, attempt: Long) {
        val adapter = nfcAdapter ?: return
        if (!enabled) {
            if (readerEnabled) runCatching { adapter.disableReaderMode(this) }
            readerEnabled = false
            return
        }
        try {
            adapter.enableReaderMode(
                this,
                { tag -> runOnUiThread { nfcViewModel.onTag(tag, attempt) } },
                NfcAdapter.FLAG_READER_NFC_A, null
            )
            readerEnabled = true
        } catch (_: SecurityException) {
            nfcViewModel.readerFailed()
        } catch (_: UnsupportedOperationException) {
            nfcViewModel.readerFailed()
        } catch (_: IllegalStateException) {
            nfcViewModel.readerFailed()
        }
    }
}

@Composable
fun MyPetApp(authViewModel: AuthViewModel, petViewModel: PetViewModel, nfcViewModel: NfcWriteViewModel) {
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
                nfcViewModel = nfcViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
