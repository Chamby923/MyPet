package com.example.mypet.data.model

import com.google.firebase.firestore.DocumentId

data class Pet(
    @set:DocumentId
    var id: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val breed: String = "",
    val sex: String = "",
    val birthDate: String = "",
    val color: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val ownerEmail: String = "",
    val vaccines: String = "",
    val allergies: String = "",
    val observations: String = "",
    val status: String = "En casa", // "En casa", "Perdida", "Encontrada"
    val ownerId: String = "", // Relación con Firebase Auth UID
    val publicId: String = "",
    val publicBaseUrl: String = ""
)
