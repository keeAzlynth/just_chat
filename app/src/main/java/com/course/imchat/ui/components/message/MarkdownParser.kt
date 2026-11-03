package com.course.imchat.ui.components.message

// ── Rich Markdown token model ─────────────────────────────

sealed class MdToken {
    // Inline
    data class Text(val content: String) : MdToken()
    data class Bold(val content: String) : MdToken()
    data class Italic(val content: String) : MdToken()
    data class Strikethrough(val content: String) : MdToken()
    data class InlineCode(val content: String) : MdToken()
    data class Link(val text: String, val url: String) : MdToken()
    data class Image(val alt: String, val url: String) : MdToken()
    data class Url(val url: String) : MdToken()
    data class Mention(val name: String) : MdToken()

    // Block-level
    data class Header(val level: Int, val content: String) : MdToken()
    data class CodeBlock(val content: String, val language: String = "") : MdToken()
    object HorizontalRule : MdToken()
    data class Blockquote(val children: List<MdToken>) : MdToken()
    data class UnorderedList(val items: List<List<MdToken>>) : MdToken()
    data class OrderedList(val items: List<List<MdToken>>, val start: Int = 1) : MdToken()
    object Newline : MdToken()
}

/**
 * 一次解析的结果。
 *
 * 除了 token 本身，还缓存了 [hasBlocks]：渲染时每帧都要判断"有没有块级内容"，
 * 让它跟着解析结果一起算一次即可，不必每次重新扫 token 列表。
 */
class MdDocument(
    val tokens: List<MdToken>,
    val hasBlocks: Boolean,
)

// ── Single-pass parser: block structure → inline markup ───

object MarkdownParser {

    /** 缓存条数：一次聊天的可视消息基本都在这个范围内 */
    private const val CACHE_SIZE = 256

    // 访问序 LinkedHashMap 当 LRU 用；解析结果是不可变的，可以安全共享
    private val cache = object : LinkedHashMap<String, MdDocument>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, MdDocument>): Boolean =
            size > CACHE_SIZE
    }

    /** 解析（带缓存）。同一段文本在滚动、重组、预览里重复出现时只解析一次。 */
    fun parseDocument(text: String): MdDocument {
        synchronized(cache) { cache[text] }?.let { return it }
        val doc = parseUncached(text)
        synchronized(cache) { cache[text] = doc }
        return doc
    }

    /** 兼容旧调用点 */
    fun parse(text: String): List<MdToken> = parseDocument(text).tokens

    // ── 行级分类 ───────────────────────────────────────────
    //
    // 关键点：分类只做一次，主循环和段落前瞻都用同一个 [classify]。
    // 旧实现里 isBlockBoundary() 和各个分支的判定条件不一致，
    // 出现「是边界但没人处理」的行时游标不前进，直接死循环
    // （比如 `## 标题`：isBlockBoundary 认为是边界，HEADER_REGEX.matches 却不认）。

    private enum class LineKind { Blank, Fence, Hr, Quote, Unordered, Ordered, Header, Text }

    private fun classify(line: String): LineKind {
        val t = line.trim()
        return when {
            t.isEmpty() -> LineKind.Blank
            t.startsWith("```") -> LineKind.Fence
            HR_REGEX.matches(t) -> LineKind.Hr
            t.startsWith(">") || t.startsWith("&gt;") -> LineKind.Quote
            isUnorderedItem(t) -> LineKind.Unordered
            ORDERED_REGEX.matches(t) -> LineKind.Ordered
            HEADER_REGEX.containsMatchIn(t) -> LineKind.Header
            else -> LineKind.Text
        }
    }

    private fun isUnorderedItem(trimmed: String): Boolean =
        (trimmed.startsWith("- ") || trimmed.startsWith("* ")) && !trimmed.startsWith("**")

    // ── 主解析 ─────────────────────────────────────────────

    private fun parseUncached(text: String): MdDocument {
        if (text.isEmpty()) return MdDocument(listOf(MdToken.Text(text)), false)

        // 快速路径：没有任何 Markdown 特征，连正则都不用跑
        if (!NEEDS_MARKDOWN.containsMatchIn(text)) {
            return MdDocument(listOf(MdToken.Text(text)), false)
        }

        val lines = text.split("\n")
        val tokens = ArrayList<MdToken>(lines.size)
        var hasBlocks = false
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            when (classify(line)) {
                LineKind.Blank -> i++

                // ── 代码块 ─────────────────────────────────────
                LineKind.Fence -> {
                    val lang = trimmed.removePrefix("```").trim()
                    val sb = StringBuilder()
                    i++
                    while (i < lines.size && !lines[i].trim().startsWith("```")) {
                        if (sb.isNotEmpty()) sb.append('\n')
                        sb.append(lines[i])
                        i++
                    }
                    if (i < lines.size) i++ // 跳过结束围栏
                    tokens.add(MdToken.CodeBlock(sb.toString(), lang))
                    hasBlocks = true
                }

                // ── 分隔线 ─────────────────────────────────────
                LineKind.Hr -> {
                    tokens.add(MdToken.HorizontalRule)
                    hasBlocks = true
                    i++
                }

                // ── 引用 ───────────────────────────────────────
                LineKind.Quote -> {
                    val quoteLines = ArrayList<String>()
                    while (i < lines.size) {
                        val kind = classify(lines[i])
                        if (kind == LineKind.Quote) {
                            quoteLines.add(
                                lines[i].trimStart().removePrefix("&gt;").removePrefix(">").trimStart()
                            )
                            i++
                        } else if (kind == LineKind.Blank) {
                            // 引用中间的空白保留，但遇到非引用行就结束
                            quoteLines.add("")
                            i++
                        } else {
                            break
                        }
                    }
                    tokens.add(MdToken.Blockquote(parseInlineOnly(quoteLines.joinToString("\n").trim())))
                    hasBlocks = true
                }

                // ── 无序列表 ───────────────────────────────────
                LineKind.Unordered -> {
                    val items = ArrayList<List<MdToken>>()
                    while (i < lines.size && classify(lines[i]) == LineKind.Unordered) {
                        val itemText = lines[i].trimStart()
                            .removePrefix("-").removePrefix("*").trimStart()
                        items.add(parseInlineOnly(itemText))
                        i++
                    }
                    tokens.add(MdToken.UnorderedList(items))
                    hasBlocks = true
                }

                // ── 有序列表 ───────────────────────────────────
                LineKind.Ordered -> {
                    val items = ArrayList<List<MdToken>>()
                    while (i < lines.size && classify(lines[i]) == LineKind.Ordered) {
                        val itemText = lines[i].trimStart().replace(ORDERED_EXTRACT, "")
                        items.add(parseInlineOnly(itemText))
                        i++
                    }
                    tokens.add(MdToken.OrderedList(items))
                    hasBlocks = true
                }

                // ── 标题 ───────────────────────────────────────
                LineKind.Header -> {
                    val level = line.trimStart().takeWhile { it == '#' }.length.coerceIn(1, 6)
                    val content = line.trimStart().drop(level).trimStart()
                    tokens.add(MdToken.Header(level, content))
                    hasBlocks = true
                    i++
                }

                // ── 普通段落 ───────────────────────────────────
                LineKind.Text -> {
                    val paraLines = ArrayList<String>(2)
                    // classify(line) == Text 保证至少消费一行，游标一定前进
                    while (i < lines.size && classify(lines[i]) == LineKind.Text) {
                        paraLines.add(lines[i])
                        i++
                    }
                    tokens.addAll(parseInlineOnly(paraLines.joinToString("\n")))
                }
            }
        }

        if (tokens.any { it is MdToken.Image }) hasBlocks = true
        return MdDocument(tokens, hasBlocks)
    }

    // ── 行内解析 ───────────────────────────────────────────

    /** 含这些字符才需要跑行内正则；纯中文句子直接整段输出 */
    private val NEEDS_INLINE = Regex("[`*~\\[\\]!@]|https?://")

    private fun parseInlineOnly(text: String): List<MdToken> {
        if (text.isEmpty()) return emptyList()
        if (!NEEDS_INLINE.containsMatchIn(text)) {
            return listOf(MdToken.Text(text))
        }

        val tokens = ArrayList<MdToken>(8)
        var lastEnd = 0

        for (m in INLINE_REGEX.findAll(text)) {
            if (m.range.first > lastEnd) {
                tokens.add(MdToken.Text(text.substring(lastEnd, m.range.first)))
            }
            when {
                m.groups[1] != null -> tokens.add(MdToken.Image(m.groups[1]!!.value, m.groups[2]!!.value))
                m.groups[3] != null -> tokens.add(MdToken.Link(m.groups[3]!!.value, m.groups[4]!!.value))
                m.groups[5] != null -> tokens.add(MdToken.Mention(m.groups[5]!!.value))
                m.groups[6] != null -> tokens.add(MdToken.Bold(m.groups[6]!!.value))
                m.groups[7] != null -> tokens.add(MdToken.Italic(m.groups[7]!!.value))
                m.groups[8] != null -> tokens.add(MdToken.Strikethrough(m.groups[8]!!.value))
                m.groups[9] != null -> tokens.add(MdToken.InlineCode(m.groups[9]!!.value))
                m.groups[10] != null -> tokens.add(MdToken.Url(m.groups[10]!!.value))
            }
            lastEnd = m.range.last + 1
        }

        if (lastEnd < text.length) {
            tokens.add(MdToken.Text(text.substring(lastEnd)))
        }
        return tokens
    }

    // ── 预编译正则（旧实现每次解析都 new 一个，单条消息就多花上百微秒）──

    private val HEADER_REGEX = Regex("^#{1,6}\\s")
    private val ORDERED_REGEX = Regex("^\\d+\\.\\s.*")
    private val ORDERED_EXTRACT = Regex("^\\d+\\.\\s")
    private val HR_REGEX = Regex("^(-{3,}|\\*{3,}|_{3,})\\s*$")

    private val NEEDS_MARKDOWN = Regex("[`*~#>\\[\\]!@]|https?://")

    private val INLINE_REGEX = Regex(
        """!\[([^\]]+)]\(([^)]+)\)""" +                  // Image
        """|\[([^\]]+)]\(([^)]+)\)""" +                   // Link
        """|@(\w[\w.]*)""" +                              // Mention
        """|\*\*(.+?)\*\*""" +                            // Bold
        """|(?<![*\w])\*(?!\s)(.+?)(?<!\s)\*(?![*\w])""" + // Italic
        """|(?<![~])~~(.+?)~~(?![~])""" +                 // Strikethrough
        """|`([^`]+)`""" +                                // Inline code
        """|(https?://\S+)"""                             // Raw URL
    )
}
