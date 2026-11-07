package com.example.mypet.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mypet.data.model.Pet
import com.example.mypet.ui.viewmodel.AuthViewModel
import com.example.mypet.ui.viewmodel.PetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    petViewModel: PetViewModel,
    authViewModel: AuthViewModel,
    onNavigateToAddPet: () -> Unit,
    onNavigateToPetDetail: (String) -> Unit,
    onLogout: () -> Unit
) {
    LaunchedEffect(Unit) {
        petViewModel.fetchPets()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Mascotas") },
                actions = {
                    IconButton(onClick = {
                        authViewModel.logout()
                        onLogout()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddPet) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Mascota")
            }
        }
    ) { padding ->
        if (petViewModel.pets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No tienes mascotas registradas aún.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(petViewModel.pets) { pet ->
                    PetItem(pet, onClick = { onNavigateToPetDetail(pet.id) })
                }
            }
        }
    }
}

@Composable
fun PetItem(pet: Pet, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = pet.name, style = MaterialTheme.typography.titleLarge)
            Text(text = "${pet.breed} - ${pet.sex}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = if (pet.status == "En casa") MaterialTheme.colorScheme.primaryContainer 
                        else MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = pet.status,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
