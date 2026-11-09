package com.example.mypet

import com.example.mypet.data.model.PublicPet
import org.junit.Assert.*
import org.junit.Test

class PublicPetTest {
    @Test
    fun publicDocumentContainsOnlyAuthorizedFields() {
        val document = PublicPet(name = "Nube").toDocument()
        assertEquals(setOf(
            "active", "name", "breed", "sex", "age", "traits",
            "medicalAlert", "status", "photoUrl", "contactPhone"
        ), document.keys)
        assertEquals("", document["contactPhone"])
        assertFalse(document.containsKey("ownerId"))
    }

    @Test
    fun rejectsUnsafeUrlsAndMalformedPhone() {
        assertNotNull(PublicPet(name = "Nube", contactPhone = "123").validationError())
        assertNotNull(PublicPet(name = "Nube", photoUrl = "javascript:alert(1)").validationError())
        assertNotNull(PublicPet(name = "Nube", photoUrl = "https://user:pass@example.invalid/").validationError())
        assertNull(PublicPet(name = "Nube", contactPhone = "+12025550123").validationError())
    }

    @Test
    fun profileLinksRequireHttpsAndOnePublicIdentifier() {
        val id = "a".repeat(32)
        assertEquals("https://mypet-pip.web.app/?p=$id", PublicPet.profileUrl("https://mypet-pip.web.app/", id))
        assertEquals("https://mypet-pip.web.app/?p=$id", PublicPet.profileUrl("https://mypet-pip.web.app", id))
        for (base in listOf(
            "", "http://mypet-pip.web.app/", "https://example.invalid/",
            "https://mypet-pip.web.app.example.invalid/", "https://sub.mypet-pip.web.app/",
            "https://user@mypet-pip.web.app/", "https://mypet-pip.web.app:443/",
            "https://mypet-pip.web.app/path", "https://mypet-pip.web.app//",
            "https://mypet-pip.web.app/?", "https://mypet-pip.web.app/#",
            "https://mypet-pip.web.app/?p=otro", "https://mypet-pip.web.app/#demo"
        )) {
            assertNull(base, PublicPet.profileUrl(base, id))
        }
        assertNull(PublicPet.profileUrl(PublicPet.PUBLIC_BASE_URL, "../pets"))
    }
}
