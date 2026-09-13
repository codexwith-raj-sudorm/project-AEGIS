package com.jarvis.aegis.session

import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ConsentHasherTest {
    private val policy = SessionPolicy(
        mode = SessionMode.EXTREME,
        duration = Duration.ofMinutes(45),
        targetPackages = setOf("social.example"),
        essentialPackages = setOf("phone.example"),
    )

    @Test fun hashIsDeterministic() {
        assertEquals(ConsentHasher.hash(policy), ConsentHasher.hash(policy))
    }

    @Test fun policyChangeChangesHash() {
        assertNotEquals(ConsentHasher.hash(policy), ConsentHasher.hash(policy.copy(amnestyEnabled = false)))
    }
}
