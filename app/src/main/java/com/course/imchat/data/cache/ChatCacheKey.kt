package com.course.imchat.data.cache

/**
 * Single source of truth for all cache keys.
 * Prevents key inconsistency across AppCache, PersistentCache, ViewModel, and delegates.
 *
 * Usage:
 *   AppCache.cacheMessages(ChatCacheKey.public, msgs)
 *   PersistentCache.loadDraft(ChatCacheKey.privateChat(userId))
 *   viewModel.currentChatKey() → ChatCacheKey.forChat(state)
 */
object ChatCacheKey {
    const val PUBLIC = "public"

    fun privateChat(userId: String) = "private_$userId"
    fun groupChat(groupId: String) = "group_$groupId"

    /** Extract the cache key from the current chat context */
    fun forChat(
        selectedGroupId: String?,
        selectedPrivateUserId: String?,
    ): String = when {
        !selectedGroupId.isNullOrEmpty() -> groupChat(selectedGroupId)
        !selectedPrivateUserId.isNullOrEmpty() -> privateChat(selectedPrivateUserId)
        else -> PUBLIC
    }
}
