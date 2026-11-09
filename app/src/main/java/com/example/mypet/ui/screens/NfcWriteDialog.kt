package com.example.mypet.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mypet.ui.viewmodel.NfcWritePhase
import com.example.mypet.ui.viewmodel.NfcWriteViewModel

@Composable
fun NfcWriteDialog(viewModel: NfcWriteViewModel) {
    val state = viewModel.state
    if (state.url == null) return
    val context = LocalContext.current
    var settingsError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { viewModel.dismiss() },
        title = { Text("Programar collar NFC") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(state.message)
                Text(state.url, style = MaterialTheme.typography.bodySmall)
                if (state.reading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Puedes cancelar en cualquier momento. Después de una interrupción, vuelve a intentar para comprobar el contenido.")
                }
                if (settingsError) Text("Abre los ajustes de conexiones del teléfono y busca NFC.")
            }
        },
        confirmButton = {
            when (state.phase) {
                NfcWritePhase.DISABLED -> TextButton(onClick = {
                    val opened = runCatching {
                        context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                    }.isSuccess
                    settingsError = !opened && runCatching {
                        context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                    }.isFailure
                }) { Text("Abrir ajustes NFC") }
                NfcWritePhase.CONFIRM -> TextButton(onClick = { viewModel.confirmOverwrite() }) {
                    Text("Reemplazar contenido")
                }
                NfcWritePhase.ERROR -> TextButton(onClick = { viewModel.retry() }) {
                    Text("Volver a intentar")
                }
                NfcWritePhase.SUCCESS, NfcWritePhase.UNSUPPORTED -> TextButton(onClick = { viewModel.dismiss() }) {
                    Text("Cerrar")
                }
                else -> Unit
            }
        },
        dismissButton = {
            if (state.phase != NfcWritePhase.SUCCESS && state.phase != NfcWritePhase.UNSUPPORTED) {
                TextButton(onClick = { viewModel.dismiss() }) { Text("Cancelar") }
            }
        }
    )
}
