package com.course.imchat.ui.components.message

import java.util.regex.Pattern

// ── Markdown + @mention unified token model ────────────────
sealed class MdToken {
    data class Text(val content: String) : MdToken()
    data class Bold(val content: String) : MdToken()
    data class Italic(val content: String) : MdToken()
    data class Strikethrough(val content: String) : MdToken()
    data class InlineCode(val content: String) : MdToken()
    data class Header(val level: Int, val content: String) : MdToken()
    data class Link(val text: String, val url: String) : MdToken()
    data class Url(val url: String) : MdToken()
    data class CodeBlock(val content: String, val language: String = "") : MdToken()
    data class Mention(val name: String) : MdToken()
}

/** Single-pass parser: markdown + @mentions simultaneously */
object MarkdownParser {

    private val MENTION_PATTERN = Pattern.compile("@(\\S+)")

    /**
     * Parse text into tokens.
     * 1. Extract code blocks (protected from other parsing)
     * 2. Parse remaining text for inline markdown + mentions
     */
    fun parse(text: String): List<MdToken> {
        val tokens = mutableListOf<MdToken>()

        val codeBlockRegex = Pattern.compile("```(?:(\\w*)\\n)?([\\s\\S]*?)```")
        val m = codeBlockRegex.matcher(text)
        var lastEnd = 0
        while (m.find()) {
            if (m.start() > lastEnd) parseInline(text.substring(lastEnd, m.start()), tokens)
            val lang = m.group(1) ?: ""
            tokens.add(MdToken.CodeBlock(m.group(2).trim(), lang.trim()))
            lastEnd = m.end()
        }
        if (lastEnd < text.length) parseInline(text.substring(lastEnd), tokens)
        return tokens
    }

    private fun parseInline(text: String, tokens: MutableList<MdToken>) {
        val pattern = Pattern.compile(
            "@(\\w[\\w.]*)" +
            "|\\[([^]]+)]\\(([^)]+)\\)" +
            "|\\*\\*(.+?)\\*\\*" +
            "|(?<![*\\w])\\*(?!\\s)(.+?)(?<!\\s)\\*(?![*\\w])" +
            "|~~(.+?)~~" +
            "|`([^`]+)`" +
            "|(https?://\\S+)"
        )
        val m = pattern.matcher(text)
        var lastEnd = 0
        while (m.find()) {
            if (m.start() > lastEnd) tokens.add(MdToken.Text(text.substring(lastEnd, m.start())))
            when {
                m.group(1) != null -> tokens.add(MdToken.Mention(m.group(1)))
                m.group(2) != null -> tokens.add(MdToken.Link(m.group(2), m.group(3)))
                m.group(4) != null -> tokens.add(MdToken.Bold(m.group(4)))
                m.group(5) != null -> tokens.add(MdToken.Italic(m.group(5)))
                m.group(6) != null -> tokens.add(MdToken.Strikethrough(m.group(6)))
                m.group(7) != null -> tokens.add(MdToken.InlineCode(m.group(7)))
                m.group(8) != null -> tokens.add(MdToken.Url(m.group(8)))
            }
            lastEnd = m.end()
        }
        if (lastEnd < text.length) {
            val suffix = text.substring(lastEnd)
            val lines = suffix.split("\n")
            lines.forEachIndexed { i, line ->
                if (i > 0) tokens.add(MdToken.Text("\n"))
                val hm = Regex("^(#{1,6})\\s+(.*)").find(line)
                if (hm != null) tokens.add(MdToken.Header(hm.groupValues[1].length, hm.groupValues[2]))
                else tokens.add(MdToken.Text(line))
            }
        }
    }
}
