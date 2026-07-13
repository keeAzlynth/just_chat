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

// ── Single-pass parser: block structure → inline markup ───

object MarkdownParser {

    /** Top-level parse: split into lines, group into blocks, then inline-parse each block content. */
    fun parse(text: String): List<MdToken> {
        if (text.isBlank()) return listOf(MdToken.Text(text))

        val lines = text.split("\n")
        val tokens = mutableListOf<MdToken>()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            when {
                // ── Code block (fenced) ─────────────────────────────────
                trimmed.startsWith("```") -> {
                    val lang = trimmed.removePrefix("```").trim()
                    val sb = StringBuilder()
                    i++
                    while (i < lines.size && !lines[i].trim().startsWith("```")) {
                        if (sb.isNotEmpty()) sb.append("\n")
                        sb.append(lines[i])
                        i++
                    }
                    i++ // skip closing ```
                    tokens.add(MdToken.CodeBlock(sb.toString(), lang))
                }

                // ── Horizontal rule ─────────────────────────────────────
                trimmed.matches(HR_REGEX) -> {
                    tokens.add(MdToken.HorizontalRule)
                    i++
                }

                // ── Blockquote ──────────────────────────────────────────
                trimmed.startsWith(">") || (trimmed.startsWith("&gt;")) -> {
                    val quoteLines = mutableListOf<String>()
                    while (i < lines.size) {
                        val tl = lines[i].trimStart()
                        if (tl.startsWith(">") || tl.startsWith("&gt;")) {
                            quoteLines.add(
                                tl.removePrefix("&gt;").removePrefix(">").trimStart()
                            )
                            i++
                        } else if (lines[i].isBlank()) {
                            quoteLines.add("")
                            i++
                        } else {
                            break
                        }
                    }
                    val body = quoteLines.joinToString("\n").trim()
                    tokens.add(MdToken.Blockquote(parseInlineOnly(body)))
                }

                // ── Unordered list ──────────────────────────────────────
                isUnorderedListItem(line) -> {
                    val items = mutableListOf<List<MdToken>>()
                    while (i < lines.size && isUnorderedListItem(lines[i])) {
                        val itemText = lines[i].trimStart().removePrefix("-").removePrefix("*").trimStart()
                        items.add(parseInlineOnly(itemText))
                        i++
                    }
                    tokens.add(MdToken.UnorderedList(items))
                }

                // ── Ordered list ────────────────────────────────────────
                trimmed.matches(ORDERED_REGEX) -> {
                    val items = mutableListOf<List<MdToken>>()
                    while (i < lines.size && lines[i].trimStart().matches(ORDERED_REGEX)) {
                        val itemText = lines[i].trimStart().replace(ORDERED_EXTRACT, "")
                        items.add(parseInlineOnly(itemText))
                        i++
                    }
                    tokens.add(MdToken.OrderedList(items))
                }

                // ── Header ──────────────────────────────────────────────
                trimmed.matches(HEADER_REGEX) -> {
                    val level = line.trimStart().takeWhile { it == '#' }.length.coerceIn(1, 6)
                    val content = trimmed.drop(level).trimStart()
                    tokens.add(MdToken.Header(level, content))
                    i++
                }

                // ── Regular paragraph (inline only) ─────────────────────
                else -> {
                    val paraLines = mutableListOf<String>()
                    while (i < lines.size && !isBlockBoundary(lines[i])) {
                        paraLines.add(lines[i])
                        i++
                    }
                    val body = paraLines.joinToString("\n")
                    tokens.addAll(parseInlineOnly(body))
                }
            }
        }
        return tokens
    }

    // ── Inline parsing ─────────────────────────────────────

    private fun parseInlineOnly(text: String): List<MdToken> {
        val tokens = mutableListOf<MdToken>()

        // Pattern order matters — longer/more specific first
        val pattern = Regex(
            """!\[([^\]]+)]\(([^)]+)\)""" +          // Image
            """|\[([^\]]+)]\(([^)]+)\)""" +           // Link
            """|@(\w[\w.]*)""" +                      // Mention
            """|\*\*(.+?)\*\*""" +                     // Bold
            """|(?<![*\w])\*(?!\s)(.+?)(?<!\s)\*(?![*\w])""" + // Italic (single *, not bold)
            """|(?<![~])~~(.+?)~~(?![~])""" +          // Strikethrough
            """|`([^`]+)`""" +                        // Inline code
            """|(https?://\S+)"""                      // Raw URL
        )

        val matches = pattern.findAll(text).toList()
        var lastEnd = 0

        for (m in matches) {
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

    // ── Helpers ────────────────────────────────────────────

    private fun isBlockBoundary(line: String): Boolean {
        val t = line.trimStart()
        if (t.isEmpty()) return false  // blank lines consumed within paragraph
        return t.startsWith("```") || t.startsWith("#") ||
               t.startsWith(">") || t.startsWith("&gt;") ||
               isUnorderedListItem(line) ||
               t.matches(ORDERED_REGEX) ||
               t.matches(HR_REGEX)
    }

    private fun isUnorderedListItem(line: String): Boolean {
        val t = line.trimStart()
        return (t.startsWith("- ") || t.startsWith("* ")) && !t.startsWith("**")
    }

    private val HEADER_REGEX = Regex("^#{1,6}\\s")
    private val ORDERED_REGEX = Regex("^\\d+\\.\\s.*")
    private val ORDERED_EXTRACT = Regex("^\\d+\\.\\s")
    private val HR_REGEX = Regex("^(-{3,}|\\*{3,}|_{3,})\\s*$")
}
