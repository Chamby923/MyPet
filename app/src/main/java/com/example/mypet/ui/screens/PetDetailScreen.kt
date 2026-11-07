package com.example.mypet.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mypet.data.model.Pet
import com.example.mypet.ui.viewmodel.PetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailScreen(
    petId: String,
    viewModel: PetViewModel,
    onBack: () -> Unit
) {
    val pet = viewModel.pets.find { it.id == petId } ?: return
    var isEditing by remember { mutableStateOf(false) }

    // Estados locales para la edición
    var name by remember { mutableStateOf(pet.name) }
    var breed by remember { mutableStateOf(pet.breed) }
    var sex by remember { mutableStateOf(pet.sex) }
    var birthDate by remember { mutableStateOf(pet.birthDate) }
    var color by remember { mutableStateOf(pet.color) }
    var ownerName by remember { mutableStateOf(pet.ownerName) }
    var phone by remember { mutableStateOf(pet.phone) }
    var vaccines by remember { mutableStateOf(pet.vaccines) }
    var allergies by remember { mutableStateOf(pet.allergies) }
    var observations by remember { mutableStateOf(pet.observations) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar Mascota" else pet.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (isEditing) {
                            val updatedPet = pet.copy(
                                name = name, breed = breed, sex = sex, birthDate = birthDate,
                                color = color, ownerName = ownerName, phone = phone,
                                vaccines = vaccines, allergies = allergies, observations = observations
                            )
                            viewModel.updatePet(updatedPet) { isEditing = false }
                        } else {
                            isEditing = true
                        }
                    }) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = if (isEditing) "Guardar" else "Editar"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Sección de Cambio de Estado (Siempre visible)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (pet.status == "Perdida") MaterialTheme.colorScheme.errorContainer 
                                    else MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Estado: ${pet.status}", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.updatePetStatus(pet.id, "En casa") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            enabled = pet.status != "En casa"
                        ) {
                            Text("EN CASA")
                        }
                        Button(
                            onClick = { viewModel.updatePetStatus(pet.id, "Perdida") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            enabled = pet.status != "Perdida"
                        ) {
                            Text("PERDIDA")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campos de información
            DetailField("Nombre", name, isEditing) { name = it }
            DetailField("Raza", breed, isEditing) { breed = it }
            DetailField("Sexo", sex, isEditing) { sex = it }
            DetailField("Edad / Fecha Nac.", birthDate, isEditing) { birthDate = it }
            DetailField("Color / Características", color, isEditing) { color = it }
            DetailField("Dueño", ownerName, isEditing) { ownerName = it }
            DetailField("Teléfono", phone, isEditing) { phone = it }
            DetailField("Vacunas", vaccines, isEditing) { vaccines = it }
            DetailField("Alergias", allergies, isEditing) { allergies = it }
            DetailField("Observaciones", observations, isEditing, singleLine = false) { observations = it }
        }
    }
}

@Composable
fun DetailField(
    label: String,
    value: String,
    isEditing: Boolean,
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        if (isEditing) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = singleLine
            )
        } else {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(text = if (value.isBlank()) "No especificado" else value, style = MaterialTheme.typography.bodyLarge)
            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
        }
    }
}
