package ru.spbu.muldrik

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.reflect.full.*


val pointerSize = with(System.getProperty("sun.arch.data.model")) {
  if (this == "64") return@with 8
  else return@with 4
}

open class Par() {
  var reallyChar: Char = 'a'
  open var duplicate : Long = 10
}

fun lol(): Unit {}

data class Mda(val i: Int): Par() {
  override var duplicate : Long = 20
}

class ShallowSizeTests {
  @Test
  fun `method exists`() {
    val x = MyClass(11)
    val y = Hranilka('z')
    val z = Mda(1)
    z::class.memberProperties.forEach {
      println(it)
    }
    assertEquals(2*Int.SIZE_BYTES, x.shallowSize())
    assertEquals(pointerSize + Char.SIZE_BYTES, y.shallowSize())
    println(z.shallowSize())
  }
}