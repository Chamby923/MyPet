package com.example.mypet.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth

    var currentUser by mutableStateOf<FirebaseUser?>(auth.currentUser)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun register(email: String, pass: String, onSuccess: () -> Unit) {
        isLoading = true
        errorMessage = null
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    currentUser = auth.currentUser
                    onSuccess()
                } else {
                    errorMessage = task.exception?.localizedMessage
                }
            }
    }

    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        isLoading = true
        errorMessage = null
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    currentUser = auth.currentUser
                    onSuccess()
                } else {
                    errorMessage = task.exception?.localizedMessage
                }
            }
    }

    fun logout() {
        auth.signOut()
        currentUser = null
    }
}
