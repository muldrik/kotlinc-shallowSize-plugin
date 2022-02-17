package ru.spbu.muldrik

data class OneProperty(val a: Int)

data class MultipleProperties(val a: Int, var b: Char)

data class ContainsPropertyWithNoBackingField(val a: Int) {
    val noField: Int
        get() = a * 2
}

data class MixedCases(val a: Int, var b: String) {
    val noField
        get() = "This has no backup field"
    var bodyProp = listOf(1, 2, 3)
    val looong: Long = 100000
    fun unitFunc(): Unit {}
    val zeroSize = unitFunc()
}

open class SimpleParent {
    val superClassProp = 100
}

data class SimpleInherited(val a: Long): SimpleParent()

open class ParentWithOpenValProp {
    open val overridable: Int = 10
}

data class NoOverriding(val a: Long) : ParentWithOpenValProp()

data class OverridesWithNoField(val a: Long) : ParentWithOpenValProp() {
    override val overridable
        get() = 30
}

data class OverridesWithField(val a: Long) : ParentWithOpenValProp() {
    override val overridable = 40
}


open class ParentWithOpenVarProp {
    open var overridable = 10
}

data class OveridesWithNoField2(val a: Long) : ParentWithOpenVarProp() {
    override var overridable
        get() = 20
        set(i) {}
}


