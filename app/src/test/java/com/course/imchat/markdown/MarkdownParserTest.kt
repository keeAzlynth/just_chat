package com.course.imchat.markdown

import com.course.imchat.ui.components.message.MarkdownParser
import com.course.imchat.ui.components.message.MdToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Markdown 解析器的回归测试。
 *
 * 重点覆盖两类问题：
 *  1. 死循环：旧实现里 `## 标题` 会让游标不前进，UI 线程直接卡死；
 *  2. 性能：每次解析都新建 Regex、无缓存，单条消息要上百微秒。
 */
class MarkdownParserTest {

    // ── 死循环回归 ─────────────────────────────────────────

    @Test(timeout = 3_000)
    fun `标题行不会死循环`() {
        val doc = MarkdownParser.parseDocument("## 今天的改动\n- 线程池隔离阻塞操作\n- 群消息定向投递")

        val header = doc.tokens.filterIsInstance<MdToken.Header>().firstOrNull()
        assertEquals(2, header?.level)
        assertEquals("今天的改动", header?.content)

        val list = doc.tokens.filterIsInstance<MdToken.UnorderedList>().firstOrNull()
        assertEquals(2, list?.items?.size)
    }

    @Test(timeout = 3_000)
    fun `各种井号开头都能终止`() {
        listOf("# 一级", "###### 六级", "#没有空格", "####### 七个井号").forEach { text ->
            val doc = MarkdownParser.parseDocument(text)
            assertTrue("解析结果不应为空: $text", doc.tokens.isNotEmpty())
        }
    }

    @Test(timeout = 3_000)
    fun `连续块级语法不会互相吞掉`() {
        val text = "# 标题\n\n> 引用\n\n- a\n- b\n\n1. one\n2. two\n\n---\n\n结尾段落"
        val doc = MarkdownParser.parseDocument(text)
        assertTrue(doc.hasBlocks)
        assertEquals(1, doc.tokens.count { it is MdToken.Header })
        assertEquals(1, doc.tokens.count { it is MdToken.Blockquote })
        assertEquals(1, doc.tokens.count { it is MdToken.UnorderedList })
        assertEquals(1, doc.tokens.count { it is MdToken.OrderedList })
        assertEquals(1, doc.tokens.count { it is MdToken.HorizontalRule })
    }

    // ── 语义 ───────────────────────────────────────────────

    @Test
    fun `行内格式`() {
        val tokens = MarkdownParser.parse("**粗** *斜* ~~删~~ `code`")

        assertTrue(tokens.any { it is MdToken.Bold && it.content == "粗" })
        assertTrue(tokens.any { it is MdToken.Italic && it.content == "斜" })
        assertTrue(tokens.any { it is MdToken.Strikethrough && it.content == "删" })
        assertTrue(tokens.any { it is MdToken.InlineCode && it.content == "code" })
    }

    @Test
    fun `链接 图片 与 裸链接`() {
        val tokens = MarkdownParser.parse("[官网](https://a.com) ![图](https://a.com/x.png) https://b.com")

        assertTrue(tokens.any { it is MdToken.Link && it.url == "https://a.com" })
        assertTrue(tokens.any { it is MdToken.Image && it.url == "https://a.com/x.png" })
        assertTrue(tokens.any { it is MdToken.Url && it.url == "https://b.com" })
    }

    @Test
    fun `代码块内的标记不被解析`() {
        val doc = MarkdownParser.parseDocument("```cpp\nint main() { return 0; }\n```")

        val block = doc.tokens.filterIsInstance<MdToken.CodeBlock>().first()
        assertEquals("cpp", block.language)
        assertTrue(block.content.contains("int main()"))
        assertTrue(doc.hasBlocks)
        // 代码块内部不应产生 Bold/Italic 之类的行内 token
        assertTrue(doc.tokens.none { it is MdToken.Bold })
    }

    @Test
    fun `纯文本走快速路径`() {
        val doc = MarkdownParser.parseDocument("在的，稍等我看一下代码")
        assertEquals(1, doc.tokens.size)
        assertTrue(doc.tokens[0] is MdToken.Text)
        assertTrue(!doc.hasBlocks)
    }

    @Test
    fun `下划线标识符不被当成斜体`() {
        val tokens = MarkdownParser.parse("调用 save_message() 和 foo_bar_baz")
        assertTrue(tokens.none { it is MdToken.Italic })
    }

    // ── 缓存 ───────────────────────────────────────────────

    @Test
    fun `相同文本命中缓存`() {
        val text = "**缓存** 测试 `code`"
        val first = MarkdownParser.parseDocument(text)
        val second = MarkdownParser.parseDocument(text)
        assertSame(first, second)
    }

    // ── 性能 ───────────────────────────────────────────────

    @Test(timeout = 10_000)
    fun `批量解析性能`() {
        val samples = listOf(
            "在的，稍等我看一下代码",
            "把 `save_message()` 的返回值改成 **消息ID**，前端就能直接编辑了",
            "文档见 https://example.com/docs ，第 3 节有说明",
            "## 改动\n- 线程池\n- 定向投递",
            "```cpp\nint main() { return 0; }\n```",
            "> 引用一行\n\n补充说明",
        )

        // 预热 JIT
        repeat(50) { samples.forEach { MarkdownParser.parseDocument(it) } }

        val rounds = 200
        val start = System.nanoTime()
        var tokenCount = 0
        repeat(rounds) {
            for (s in samples) tokenCount += MarkdownParser.parseDocument(s).tokens.size
        }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
        val perCallUs = elapsedMs * 1000 / (rounds * samples.size)

        println("解析 ${rounds * samples.size} 次，用时 %.1f ms，单条 %.2f µs".format(elapsedMs, perCallUs))

        assertTrue("token 数量异常", tokenCount > 0)
        // 阈值放得很宽：正常在 1µs 上下，这里只拦住"每次重新编译正则"那种退化
        assertTrue("单条解析耗时过高: $perCallUs µs", perCallUs < 200)
    }
}
