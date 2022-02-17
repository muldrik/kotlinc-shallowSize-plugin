package ru.spbu.muldrik

import arrow.meta.CliPlugin
import arrow.meta.Meta
import arrow.meta.invoke
import arrow.meta.quotes.Transform
import arrow.meta.quotes.classDeclaration
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*


/** System call to determine JVM bitness and therefore pointer size */
val pointerSize = with(System.getProperty("sun.arch.data.model")) {
    if (this == "64") return@with 8
    else return@with 4
}

/**
 * Calculate the type's byte size. All nullable and non-basic types are considered to be stored as a pointer(reference)
 * https://kotlinlang.org/docs/basic-types.html is used as a reference for basic types
 */
fun IrType.byteSize(): Int {
    return when {
        isNullable() -> pointerSize
        isChar() -> Char.SIZE_BYTES
        isByte() -> Byte.SIZE_BYTES
        isShort() -> Short.SIZE_BYTES
        isInt() -> Int.SIZE_BYTES
        isLong() -> Long.SIZE_BYTES
        isUByte() -> UByte.SIZE_BYTES
        isUShort() -> UShort.SIZE_BYTES
        isULong() -> ULong.SIZE_BYTES
        isFloat() -> Float.SIZE_BYTES
        isDouble() -> Double.SIZE_BYTES
        // I couldn't find specification of Boolean and Unit byte size. 1 and 0 make sense
        isBoolean() -> Byte.SIZE_BYTES
        isUnit() -> 0
        else -> pointerSize
    }
}

/**
 * Plugin for adding a shallowSize method to every compiled data class
 * shallowSize() returns the total byte size of all properties containing a backing field
 * First it injects a function signature to the syntax tree
 * Then it replaces the return value to an actual result computed from Kotlin Intermediate Representation
 * Note that implicitly inherited superclass properties only contain a getter to super, therefore not accounted for
 * Overriden properties containing a backing field do contribute to the result
 */
val Meta.GenerateShallowSize: CliPlugin
    get() = "Generate shallowSize method" {
        meta(
            classDeclaration(this, { element.isData() }) { classDecl ->
                Transform.replace(
                    classDecl.element,
                    """
                        $`@annotations` 
                        $visibility $modality $kind $name $`(typeParameters)` $`(params)` $superTypes {
                         $body
                            fun shallowSize(): Int = throw IllegalStateException("Placeholder for shallowSize function created but implementation not substituted in data class $name")
                         }
                    """.trimIndent().`class`
                )
            },
            irClass { `class` ->
                if (`class`.isData) {
                    val shallowSize = `class`.properties.sumOf { it.backingField?.type?.byteSize() ?: 0 }

                    `class`.functions.first { it.name.toString() == "shallowSize" && it.valueParameters.isEmpty() }
                        .also { function ->
                            function.body = DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody {
                                +irReturn(irInt(shallowSize))
                            }
                        }
                }
                `class`
            }
        )
    }