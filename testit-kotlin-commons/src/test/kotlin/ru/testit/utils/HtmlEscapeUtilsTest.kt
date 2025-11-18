package ru.testit.utils

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class HtmlEscapeUtilsTest {

    @Test
    fun `escapeHtmlTags should return null for null input`() {
        val result = HtmlEscapeUtils.escapeHtmlTags(null)
        assertNull(result)
    }

    @Test
    fun `escapeHtmlTags should return original string if no HTML tags`() {
        val input = "This is a plain text without HTML"
        val result = HtmlEscapeUtils.escapeHtmlTags(input)
        assertEquals(input, result)
    }

    @Test
    fun `escapeHtmlTags should escape simple HTML tags`() {
        val input = "Hello <script>alert('xss')</script> World"
        val expected = "Hello &lt;script&gt;alert('xss')&lt;/script&gt; World"
        val result = HtmlEscapeUtils.escapeHtmlTags(input)
        assertEquals(expected, result)
    }

    @Test
    fun `escapeHtmlTags should not double escape already escaped tags`() {
        val input = "Hello \\<script\\>alert('xss')\\</script\\> World"
        val expected = "Hello \\&lt;script\\&gt;alert('xss')\\&lt;/script\\&gt; World"
        val result = HtmlEscapeUtils.escapeHtmlTags(input)
        assertEquals(expected, result) // Should remain unchanged
    }

    @Test
    fun `escapeHtmlTags should handle multiple HTML tags`() {
        val input = "<div>Content</div><span>More content</span>"
        val expected = "&lt;div&gt;Content&lt;/div&gt;&lt;span&gt;More content&lt;/span&gt;"
        val result = HtmlEscapeUtils.escapeHtmlTags(input)
        assertEquals(expected, result)
    }

    @Test
    fun `escapeHtmlInObject should return null for null input`() {
        val result: Any? = HtmlEscapeUtils.escapeHtmlInObject(null)
        assertNull(result)
    }

    @Test
    fun `escapeHtmlInObjectList should return null for null input`() {
        val result: MutableList<Any>? = HtmlEscapeUtils.escapeHtmlInObjectList(null)
        assertNull(result)
    }

    @Test
    fun `escapeHtmlInObject should handle simple data class`() {
        data class TestData(var title: String, var description: String, val id: Int)
        
        val obj = TestData(
            title = "Test <script>alert('xss')</script>",
            description = "Description with <div>HTML</div>",
            id = 123
        )
        
        val result = HtmlEscapeUtils.escapeHtmlInObject(obj)
        
        assertNotNull(result)
        assertEquals("Test &lt;script&gt;alert('xss')&lt;/script&gt;", result!!.title)
        assertEquals("Description with &lt;div&gt;HTML&lt;/div&gt;", result.description)
        assertEquals(123, result.id) // Should remain unchanged
    }
} 