package com.example.mypet.data.model

import java.net.URI

// Solo estos datos se copian a publicPets tras la revisión del dueño.
data class PublicPet(
    val name: String = "",
    val breed: String = "",
    val sex: String = "",
    val age: String = "",
    val traits: String = "",
    val medicalAlert: String = "",
    val status: String = "home",
    val photoUrl: String = "",
    val contactPhone: String = ""
) {
    fun validationError(): String? = when {
        name.isBlank() || name.length > 100 -> "Indica un nombre público de hasta 100 caracteres."
        listOf(breed, sex, age).any { it.length > 100 } -> "Raza, sexo y edad admiten hasta 100 caracteres."
        traits.length > 1000 || medicalAlert.length > 500 -> "Reduce las características o la alerta médica."
        status !in setOf("home", "lost", "found") -> "Selecciona un estado válido."
        photoUrl.length > 2048 || photoUrl.isNotEmpty() && !isHttpsUrl(photoUrl) ->
            "La foto debe tener una URL HTTPS sin credenciales y de hasta 2048 caracteres."
        contactPhone.isNotEmpty() && !contactPhone.matches(Regex("\\+[1-9][0-9]{7,14}")) ->
            "Usa un teléfono con + y entre 8 y 15 dígitos."
        else -> null
    }

    fun toDocument(): Map<String, Any> = mapOf(
        "active" to true, "name" to name.trim(), "breed" to breed.trim(),
        "sex" to sex.trim(), "age" to age.trim(), "traits" to traits.trim(),
        "medicalAlert" to medicalAlert.trim(), "status" to status,
        "photoUrl" to photoUrl.trim(), "contactPhone" to contactPhone.trim()
    )

    companion object {
        const val PUBLIC_BASE_URL = "https://mypet-pip.web.app/"

        fun isHttpsUrl(value: String): Boolean = runCatching {
            val uri = URI(value)
            uri.scheme == "https" && !uri.host.isNullOrEmpty() && uri.rawUserInfo == null
        }.getOrDefault(false)

        fun profileUrl(baseUrl: String, id: String): String? {
            if (baseUrl != PUBLIC_BASE_URL && baseUrl != PUBLIC_BASE_URL.removeSuffix("/")) return null
            if (!id.matches(Regex("[A-Za-z0-9_-]{22,64}"))) return null
            return "$PUBLIC_BASE_URL?p=$id"
        }
    }
}
