package com.pkoder.finest.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncReconcileTest {

    @Test
    fun `prunes local rows missing from the server snapshot`() {
        val prune = idsToPrune(
            localSyncedIds = listOf("a", "b", "c"),
            remoteIds = listOf("a", "c")
        )

        assertEquals(listOf("b"), prune)
    }

    @Test
    fun `keeps everything when the snapshot matches`() {
        assertTrue(idsToPrune(listOf("a", "b"), listOf("b", "a")).isEmpty())
    }

    @Test
    fun `prunes nothing when there are no local rows`() {
        assertTrue(idsToPrune(emptyList(), listOf("a")).isEmpty())
    }

    @Test
    fun `unsynced rows are never candidates because they are not passed in`() {
        // The repository only ever passes synced ids; an empty snapshot must not wipe a queue
        // of local-only rows.
        val prune = idsToPrune(localSyncedIds = emptyList(), remoteIds = emptyList())

        assertTrue(prune.isEmpty())
    }

    @Test
    fun `empty snapshot prunes every synced row`() {
        assertEquals(listOf("a", "b"), idsToPrune(listOf("a", "b"), emptyList()))
    }
}
