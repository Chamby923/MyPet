package com.example.mypet.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mypet.data.model.Pet
import com.example.mypet.ui.viewmodel.PetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPetScreen(
    viewModel: PetViewModel,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var vaccines by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var observations by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Mascota") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = breed, onValueChange = { breed = it }, label = { Text("Raza") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = sex, onValueChange = { sex = it }, label = { Text("Sexo") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = birthDate, onValueChange = { birthDate = it }, label = { Text("Fecha de nacimiento / Edad") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = color, onValueChange = { color = it }, label = { Text("Color / Características") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Nombre del dueño") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono de contacto") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = vaccines, onValueChange = { vaccines = it }, label = { Text("Vacunas") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = allergies, onValueChange = { allergies = it }, label = { Text("Alergias") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = observations, onValueChange = { observations = it }, label = { Text("Observaciones") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val newPet = Pet(
                        name = name, breed = breed, sex = sex, birthDate = birthDate,
                        color = color, ownerName = ownerName, phone = phone,
                        vaccines = vaccines, allergies = allergies, observations = observations
                    )
                    viewModel.addPet(newPet, onSuccess)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isLoading && name.isNotBlank()
            ) {
                if (viewModel.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                else Text("Guardar Mascota")
            }
        }
    }
}
