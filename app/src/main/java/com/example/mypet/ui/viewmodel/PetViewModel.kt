package com.example.mypet.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.mypet.data.model.Pet
import com.example.mypet.data.model.PublicPet
import com.google.firebase.firestore.ListenerRegistration
import java.util.UUID
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class PetViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private var petsListener: ListenerRegistration? = null
    private var observedUserId = auth.currentUser?.uid
    private val authListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener {
        if (observedUserId != it.currentUser?.uid) {
            observedUserId = it.currentUser?.uid
            petsListener?.remove()
            petsListener = null
            pets.clear()
        }
    }

    var isLoading by mutableStateOf(false)
        private set

    var pets = mutableStateListOf<Pet>()
        private set

    var publicMessage by mutableStateOf<String?>(null)
        private set

    var isPublishing by mutableStateOf(false)
        private set

    init {
        auth.addAuthStateListener(authListener)
    }

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
        petsListener?.remove()
        pets.clear()
        val userId = auth.currentUser?.uid ?: return
        observedUserId = userId
        petsListener = db.collection("pets")
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
            .update(mapOf(
                "name" to pet.name, "breed" to pet.breed, "sex" to pet.sex,
                "birthDate" to pet.birthDate, "color" to pet.color,
                "ownerName" to pet.ownerName, "phone" to pet.phone,
                "vaccines" to pet.vaccines, "allergies" to pet.allergies,
                "observations" to pet.observations
            ))
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

    fun publishPet(petId: String, baseUrl: String, profile: PublicPet) {
        if (isPublishing) return
        val userId = auth.currentUser?.uid ?: return
        val validation = profile.validationError()
        if (validation != null) {
            publicMessage = validation
            return
        }
        val newId = UUID.randomUUID().toString().replace("-", "")
        if (PublicPet.profileUrl(baseUrl, newId) == null) {
            publicMessage = "La ficha solo puede publicarse con la dirección autorizada de MyPet."
            return
        }
        isPublishing = true
        publicMessage = null
        val petRef = db.collection("pets").document(petId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(petRef)
            check(snapshot.getString("ownerId") == userId)
            val existingId = snapshot.getString("publicId").orEmpty()
            val id = existingId.ifEmpty { newId }
            val publicRef = db.collection("publicPets").document(id)
            val linkRef = db.collection("publicPetLinks").document(id)
            // Leer antes de escribir permite detectar una colisión sin sobrescribirla.
            if (existingId.isEmpty()) check(!transaction.get(linkRef).exists())
            transaction.set(linkRef, mapOf("ownerId" to userId, "petId" to petId))
            transaction.set(publicRef, profile.toDocument())
            transaction.update(petRef, mapOf("publicId" to id, "publicBaseUrl" to baseUrl))
        }.addOnSuccessListener {
            isPublishing = false
            publicMessage = "Ficha guardada. Abre el enlace para verificar el acceso público."
        }.addOnFailureListener {
            isPublishing = false
            publicMessage = "No se pudo publicar. Revisa la conexión y los permisos de pets, publicPetLinks y publicPets."
        }
    }

    fun revokePublicPet(petId: String) {
        if (isPublishing) return
        val userId = auth.currentUser?.uid ?: return
        isPublishing = true
        publicMessage = null
        val petRef = db.collection("pets").document(petId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(petRef)
            check(snapshot.getString("ownerId") == userId)
            val id = snapshot.getString("publicId").orEmpty()
            if (id.isNotEmpty()) {
                transaction.set(db.collection("publicPets").document(id), mapOf("active" to false))
                transaction.update(petRef, mapOf("publicId" to "", "publicBaseUrl" to ""))
            }
        }.addOnSuccessListener {
            isPublishing = false
            publicMessage = "Enlace desactivado. Una nueva publicación generará otro identificador."
        }.addOnFailureListener {
            isPublishing = false
            publicMessage = "No se pudo desactivar el enlace. Comprueba la conexión y los permisos."
        }
    }

    override fun onCleared() {
        petsListener?.remove()
        auth.removeAuthStateListener(authListener)
        super.onCleared()
    }
}
