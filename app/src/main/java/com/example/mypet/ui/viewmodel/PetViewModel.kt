package com.example.mypet.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.mypet.data.model.Pet
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class PetViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    var isLoading by mutableStateOf(false)
        private set

    var pets = mutableStateListOf<Pet>()
        private set

    fun addPet(pet: Pet, onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        isLoading = true
        
        val newPet = pet.copy(ownerId = userId)
        
        db.collection("pets")
            .add(newPet)
            .addOnSuccessListener {
                isLoading = false
                onSuccess()
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    fun fetchPets() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("pets")
            .whereEqualTo("ownerId", userId)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener
                if (value != null) {
                    pets.clear()
                    pets.addAll(value.toObjects(Pet::class.java))
                }
            }
    }

    fun updatePet(pet: Pet, onSuccess: () -> Unit) {
        if (pet.id.isEmpty()) return
        isLoading = true
        db.collection("pets").document(pet.id)
            .set(pet)
            .addOnSuccessListener {
                isLoading = false
                onSuccess()
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    fun updatePetStatus(petId: String, newStatus: String) {
        if (petId.isEmpty()) return
        db.collection("pets").document(petId)
            .update("status", newStatus)
    }
}
