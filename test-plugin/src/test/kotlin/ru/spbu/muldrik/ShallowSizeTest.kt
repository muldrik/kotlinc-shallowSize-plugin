package ru.spbu.muldrik

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.reflect.full.*


val pointerSize = with(System.getProperty("sun.arch.data.model")) {
    if (this == "64") return@with 8
    else return@with 4
}

class ShallowSizeTests {
    @Test
    fun `method exists`() {
        val a = OneProperty(10)
        a.shallowSize()
    }

    @Test
    fun `single property in primary constructor`() {
        val a = OneProperty(10)
        assertEquals(Int.SIZE_BYTES, a.shallowSize())
    }

    @Test
    fun `Multiple properties in primary constructor`() {
        val a = MultipleProperties(5, 'z')
        assertEquals(Int.SIZE_BYTES + Char.SIZE_BYTES, a.shallowSize())
    }

    @Test
    fun `Property has no backing field`() {
        val a = ContainsPropertyWithNoBackingField(5)
        assertEquals(Int.SIZE_BYTES, a.shallowSize())
    }

    @Test
    fun `General edge cases`() {
        val a = MixedCases(1, "haha")
        assertEquals(Int.SIZE_BYTES + pointerSize + pointerSize + Long.SIZE_BYTES, a.shallowSize())
    }

    @Test
    fun inheritance() {
        val a = NoOverriding(10)
        val b = OverridesWithNoField(10)
        val c = OveridesWithNoField2(10)
        val d = SimpleInherited(10)
        val e = OverridesWithField(10)
        assertEquals(Long.SIZE_BYTES, a.shallowSize())
        assertEquals(Long.SIZE_BYTES, b.shallowSize())
        assertEquals(Long.SIZE_BYTES, c.shallowSize())
        assertEquals(Long.SIZE_BYTES, d.shallowSize())
        assertEquals(Long.SIZE_BYTES + Int.SIZE_BYTES, e.shallowSize())
    }
}