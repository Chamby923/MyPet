package com.example.mypet.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.example.mypet.data.model.Pet
import com.example.mypet.data.model.PublicPet
import com.example.mypet.ui.viewmodel.PetViewModel
import com.example.mypet.ui.viewmodel.NfcWriteViewModel

@Composable
fun PublicProfileCard(pet: Pet, viewModel: PetViewModel, nfcViewModel: NfcWriteViewModel) {
    var expanded by rememberSaveable(pet.id) { mutableStateOf(false) }
    val baseUrl = PublicPet.PUBLIC_BASE_URL
    var name by rememberSaveable(pet.id) { mutableStateOf(pet.name) }
    var breed by rememberSaveable(pet.id) { mutableStateOf(pet.breed) }
    var sex by rememberSaveable(pet.id) { mutableStateOf(pet.sex) }
    var age by rememberSaveable(pet.id) { mutableStateOf("") }
    var traits by rememberSaveable(pet.id) { mutableStateOf(pet.color) }
    var photo by rememberSaveable(pet.id) { mutableStateOf(pet.photoUrl) }
    var medical by rememberSaveable(pet.id) { mutableStateOf("") }
    var phone by rememberSaveable(pet.id) { mutableStateOf(pet.phone) }
    var sharePhone by rememberSaveable(pet.id) { mutableStateOf(false) }
    var consent by rememberSaveable(pet.id) { mutableStateOf(false) }
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val url = PublicPet.profileUrl(pet.publicBaseUrl, pet.publicId)
    var linkError by remember { mutableStateOf(false) }
    DisposableEffect(pet.id, url) {
        onDispose { nfcViewModel.dismiss(pet.id) }
    }
    if (nfcViewModel.state.petId == pet.id && nfcViewModel.state.url == url) {
        NfcWriteDialog(nfcViewModel)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ficha pública", style = MaterialTheme.typography.titleLarge)
            Text("Cualquier persona con el enlace podrá ver los datos que autorices.")
            if (url != null) {
                Text(url, style = MaterialTheme.typography.bodySmall)
                Button(onClick = {
                    linkError = runCatching { uriHandler.openUri(url) }.isFailure
                }) { Text("Abrir enlace") }
                OutlinedButton(onClick = {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, url)
                    }
                    linkError = runCatching {
                        context.startActivity(Intent.createChooser(share, "Compartir ficha"))
                    }.isFailure
                }) { Text("Compartir enlace") }
                OutlinedButton(
                    onClick = { nfcViewModel.start(pet.id, url) },
                    enabled = !viewModel.isPublishing
                ) { Text("Programar collar NFC") }
                OutlinedButton(
                    onClick = { viewModel.revokePublicPet(pet.id) },
                    enabled = !viewModel.isPublishing
                ) { Text("Desactivar enlace") }
                Text("Los cambios de la ficha privada se publican al revisar y guardar aquí.")
            }
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Cerrar formulario" else "Revisar datos para publicar")
            }
            if (expanded) {
                OutlinedTextField(
                    value = baseUrl, onValueChange = {},
                    label = { Text("Dirección HTTPS de MyPet Web") },
                    readOnly = true, modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Text("Configura primero Firebase Hosting y los permisos revisados de Firestore.")
                DetailField("Nombre público", name, true) { name = it }
                DetailField("Raza pública", breed, true) { breed = it }
                DetailField("Sexo público", sex, true) { sex = it }
                DetailField("Edad aproximada (opcional)", age, true) { age = it }
                DetailField("Características públicas", traits, true, false) { traits = it }
                DetailField("URL HTTPS de foto autorizada (opcional)", photo, true) { photo = it }
                DetailField("Alerta médica autorizada (opcional)", medical, true, false) { medical = it }
                Text("Estado que se publicará: ${pet.status}")
                Row {
                    Checkbox(checked = sharePhone, onCheckedChange = { sharePhone = it })
                    Text("Autorizo mostrar un teléfono de contacto", Modifier.padding(top = 12.dp))
                }
                if (sharePhone) {
                    DetailField("Teléfono internacional con +", phone, true) { phone = it }
                }
                Row {
                    Checkbox(checked = consent, onCheckedChange = { consent = it })
                    Text("Revisé estos campos y autorizo publicarlos", Modifier.padding(top = 12.dp))
                }
                Button(
                    enabled = consent && !viewModel.isPublishing,
                    onClick = {
                        val status = when (pet.status) {
                            "Perdida" -> "lost"
                            "Encontrada" -> "found"
                            else -> "home"
                        }
                        viewModel.publishPet(
                            pet.id, baseUrl.trim(),
                            PublicPet(
                                name.trim(), breed.trim(), sex.trim(), age.trim(), traits.trim(),
                                medical.trim(), status, photo.trim(),
                                if (sharePhone) phone.trim() else ""
                            )
                        )
                        consent = false
                    }
                ) { Text(if (viewModel.isPublishing) "Guardando…" else "Publicar datos revisados") }
            }
            viewModel.publicMessage?.let { Text(it) }
            if (linkError) Text("No se pudo abrir una aplicación para este enlace.")
        }
    }
}
