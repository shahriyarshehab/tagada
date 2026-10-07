package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TagadaRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createAndReadContact_authenticatedOwner_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val docRef = firestore.collection("users").document(uid).collection("contacts").document("test_c1")

    withTimeout(DEFAULT_TIMEOUT_MS) {
      docRef.set(
        mapOf(
          "id" to "test_c1",
          "userId" to uid,
          "name" to "Karim",
          "phone" to "01700000000",
          "contactGroup" to "General"
        )
      ).await()

      val snapshot = docRef.get().await()
      assertTrue(snapshot.exists())
      assertEquals("Karim", snapshot.getString("name"))
    }
  }

  @Test
  fun readContact_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val docRef = firestore.collection("users").document(aliceUid).collection("contacts").document("alice_c1")

    withTimeout(DEFAULT_TIMEOUT_MS) {
      docRef.set(
        mapOf(
          "id" to "alice_c1",
          "userId" to aliceUid,
          "name" to "Alice Only",
          "phone" to "01800000000",
          "contactGroup" to "General"
        )
      ).await()
    }

    // Now sign in as Bob
    signInTestUser(BOB_EMAIL)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        docRef.get().await()
      }
      fail("Expected PermissionDenied")
    } catch (exception: FirebaseFirestoreException) {
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, exception.code)
    }
  }

  @Test
  fun readContacts_unauthenticated_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val docRef = firestore.collection("users").document(aliceUid).collection("contacts").document("alice_c2")
    withTimeout(DEFAULT_TIMEOUT_MS) {
      docRef.set(
        mapOf(
          "id" to "alice_c2",
          "userId" to aliceUid,
          "name" to "Alice Contact 2",
          "phone" to "01900000000",
          "contactGroup" to "General"
        )
      ).await()
    }

    // Sign out to test unauthenticated access
    auth.signOut()

    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        docRef.get().await()
      }
      fail("Expected PermissionDenied")
    } catch (exception: FirebaseFirestoreException) {
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, exception.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice@tagada.test"
    const val BOB_EMAIL = "bob@tagada.test"
    const val DEFAULT_TIMEOUT_MS = 5000L
  }
}
