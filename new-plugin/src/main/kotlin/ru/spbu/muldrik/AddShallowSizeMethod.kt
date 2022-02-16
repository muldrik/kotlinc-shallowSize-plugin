package ru.spbu.muldrik

import arrow.meta.CliPlugin
import arrow.meta.Meta
import arrow.meta.invoke
import arrow.meta.quotes.Transform
import arrow.meta.quotes.classDeclaration
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.ir.isStatic
import org.jetbrains.kotlin.backend.jvm.codegen.psiElement
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.getOrCreateBody
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.backend.js.export.ExportedType
import org.jetbrains.kotlin.ir.backend.js.lower.calls.getPrimitiveType
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.addMember
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.Name

val pointerSize = with(System.getProperty("sun.arch.data.model")) {
    if (this == "64") return@with 8
    else return@with 4
}

// https://kotlinlang.org/docs/basic-types.html
fun IrType.size(): Int {
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
        isBoolean() -> Byte.SIZE_BYTES
        else -> pointerSize
    }
}



val Meta.GenerateShallowSize: CliPlugin
    get() = "Generate shallowSize method" {
        meta(
            classDeclaration(this, {element.isData()}) { classDecl ->
                Transform.replace(
                    classDecl.element,
                    """
                        $`@annotations` 
                        $visibility $modality $kind $name $`(typeParameters)` $`(params)` $superTypes {
                         $body
                            inline fun shallowSize(): Int = throw IllegalStateException("Placeholder for shallowSize function created but implementation not substituted in data class $name")
                         }
                    """.trimIndent().`class`
                )
            },
            irClass { `class`->
                if (`class`.isData) {
                    val shallowSize = `class`.properties.sumOf { it.backingField?.type?.size() ?: 0  }

                    `class`.functions.first { it.name.toString() == "shallowSize" && it.valueParameters.isEmpty() }.also { function ->
                        function.body = DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody {
                            +irReturn (irInt(shallowSize))
                        }
                    }

                }
                `class`
            }
        )
    }