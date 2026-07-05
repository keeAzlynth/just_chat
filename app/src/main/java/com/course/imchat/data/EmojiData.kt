package com.course.imchat.data

data class EmojiCategory(
    val name: String,
    val emojis: List<String>,
)

object EmojiData {
    // ── Recent emoji tracking ──────────────────────────
    private const val MAX_RECENT = 20
    private val recentEmojis = mutableListOf<String>()

    fun recordEmoji(emoji: String) {
        recentEmojis.remove(emoji)
        recentEmojis.add(0, emoji)
        if (recentEmojis.size > MAX_RECENT) {
            recentEmojis.removeAt(recentEmojis.lastIndex)
        }
    }

    fun getRecentEmojis(): List<String> = recentEmojis.toList()

    val categories = listOf(
        EmojiCategory(
            name = "常用",
            emojis = listOf(
                "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂",
                "🙂", "🙃", "😉", "😊", "😇", "🥰", "😍", "🤩",
                "😘", "😗", "😚", "😙", "🥲", "😋", "😛", "😜",
                "🤪", "😝", "🤑", "🤗", "🤭", "🤫", "🤔", "🫡",
                "🤐", "🤨", "😐", "😑", "😶", "🫥", "😏", "😒",
                "🙄", "😬", "🤥", "😌", "😔", "😪", "🤤", "😴",
            )
        ),
        EmojiCategory(
            name = "手势",
            emojis = listOf(
                "👋", "🤚", "🖐️", "✋", "🖖", "🫱", "🫲", "🫳",
                "🫴", "👌", "🤌", "🤏", "✌️", "🤞", "🫰", "🤟",
                "🤘", "🤙", "👈", "👉", "👆", "🖕", "👇", "☝️",
                "🫵", "👍", "👎", "✊", "👊", "🤛", "🤜", "👏",
                "🙌", "🫶", "👐", "🤲", "🤝", "🙏", "✍️", "💅",
            )
        ),
        EmojiCategory(
            name = "爱心",
            emojis = listOf(
                "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍",
                "🤎", "💔", "❤️‍🔥", "❤️‍🩹", "❣️", "💕", "💞", "💓",
                "💗", "💖", "💘", "💝", "💟", "☮️", "✝️", "☪️",
                "🕉️", "☸️", "✡️", "🔯", "🕎", "☯️", "☦️", "🛐",
            )
        ),
        EmojiCategory(
            name = "动物",
            emojis = listOf(
                "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼",
                "🐻‍❄️", "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵",
                "🐔", "🐧", "🐦", "🐤", "🐣", "🐥", "🦆", "🦅",
                "🦉", "🦇", "🐺", "🐗", "🐴", "🦄", "🐝", "🪱",
            )
        ),
        EmojiCategory(
            name = "食物",
            emojis = listOf(
                "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓",
                "🫐", "🍈", "🍒", "🍑", "🥭", "🍍", "🥥", "🥝",
                "🍅", "🍆", "🥑", "🥦", "🥬", "🥒", "🌶️", "🫑",
                "🌽", "🥕", "🫒", "🧄", "🧅", "🥔", "🍠", "🥐",
            )
        ),
        EmojiCategory(
            name = "活动",
            emojis = listOf(
                "⚽", "🏀", "🏈", "⚾", "🥎", "🎾", "🏐", "🏉",
                "🥏", "🎱", "🪀", "🏓", "🏸", "🏒", "🏑", "🥍",
                "🏏", "🪃", "🥅", "⛳", "🪁", "🏹", "🎣", "🤿",
                "🥊", "🥋", "🎽", "🛹", "🛼", "🛷", "⛸️", "🥌",
            )
        ),
        EmojiCategory(
            name = "旅行",
            emojis = listOf(
                "🚗", "🚕", "🚙", "🚌", "🚎", "🏎️", "🚓", "🚑",
                "🚒", "🚐", "🛻", "🚚", "🚛", "🚜", "🏍️", "🛵",
                "🚲", "🛴", "🛺", "🚁", "✈️", "🛩️", "🚀", "🛸",
                "🚢", "⛵", "🚤", "🛥️", "🛳️", "⛴️", "🚁", "🚡",
            )
        ),
        EmojiCategory(
            name = "符号",
            emojis = listOf(
                "💯", "🔥", "✨", "🌟", "💫", "💥", "💦", "💨",
                "🕳️", "💣", "💬", "👁️‍🗨️", "🗨️", "🗯️", "💭", "💤",
                "👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤌", "🤏",
                "✌️", "🤞", "🫰", "🤟", "🤘", "🤙", "👈", "👉",
            )
        ),
    )

    val allEmojis: List<String>
        get() = categories.flatMap { it.emojis }
}
