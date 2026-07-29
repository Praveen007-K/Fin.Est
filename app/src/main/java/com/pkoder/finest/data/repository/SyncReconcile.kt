package com.pkoder.finest.data.repository

/**
 * Local rows that should be dropped because they no longer exist in Firestore — i.e. they were
 * deleted on another device.
 *
 * Only ids of *synced* rows may be passed in: a row that has never reached the server is absent
 * from the snapshot for an entirely different reason and must be kept so the sync queue can
 * still upload it.
 */
internal fun idsToPrune(localSyncedIds: List<String>, remoteIds: Collection<String>): List<String> {
    if (localSyncedIds.isEmpty()) return emptyList()
    val remote = remoteIds.toHashSet()
    return localSyncedIds.filter { it !in remote }
}
