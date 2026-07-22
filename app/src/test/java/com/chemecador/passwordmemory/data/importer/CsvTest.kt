package com.chemecador.passwordmemory.data.importer

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvTest {

    @Test
    fun `plain rows are split on commas`() {
        val rows = Csv.parse("a,b,c\n1,2,3\n")

        assertEquals(listOf(listOf("a", "b", "c"), listOf("1", "2", "3")), rows)
    }

    @Test
    fun `quoted fields keep their commas`() {
        val rows = Csv.parse("name,password\nBank,\"p4ss,word\"\n")

        assertEquals(listOf("Bank", "p4ss,word"), rows[1])
    }

    @Test
    fun `doubled quotes become a single quote`() {
        val rows = Csv.parse("password\n\"say \"\"hi\"\"\"\n")

        assertEquals(listOf("say \"hi\""), rows[1])
    }

    @Test
    fun `a quoted field can span several lines`() {
        val rows = Csv.parse("note\n\"first\nsecond\"\n")

        assertEquals(2, rows.size)
        assertEquals(listOf("first\nsecond"), rows[1])
    }

    @Test
    fun `windows line endings are handled`() {
        val rows = Csv.parse("a,b\r\n1,2\r\n")

        assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), rows)
    }

    @Test
    fun `classic mac line endings are handled`() {
        val rows = Csv.parse("a,b\r1,2")

        assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), rows)
    }

    @Test
    fun `blank lines are dropped`() {
        val rows = Csv.parse("a,b\n\n1,2\n\n")

        assertEquals(2, rows.size)
    }

    @Test
    fun `a file without a trailing newline keeps its last row`() {
        val rows = Csv.parse("a,b\n1,2")

        assertEquals(listOf("1", "2"), rows.last())
    }

    @Test
    fun `empty fields are preserved`() {
        val rows = Csv.parse("a,b,c\n1,,3")

        assertEquals(listOf("1", "", "3"), rows[1])
    }

    @Test(expected = CsvFormatException::class)
    fun `an unterminated quote is rejected`() {
        Csv.parse("a,b\n\"never closed,2")
    }
}
