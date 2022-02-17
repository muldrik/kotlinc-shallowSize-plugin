package ru.spbu.muldrik

import arrow.meta.CliPlugin
import arrow.meta.Meta
import arrow.meta.invoke
import arrow.meta.quotes.Transform
import arrow.meta.quotes.classDeclaration
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.wasm.ir2wasm.getSuperClass
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name


// System call
val pointerSize = with(System.getProperty("sun.arch.data.model")) {
    if (this == "64") return@with 8
    else return@with 4
}

fun IrClass.calculateSizeRecursively(alreadyDefined: MutableSet<Name>, builtIns: IrBuiltIns) : Int {
    return this.properties.sumOf { it.sizeIfNotOverriden(alreadyDefined) } + (this.getSuperClass(builtIns)?.calculateSizeRecursively(alreadyDefined, builtIns) ?: 0)
}

fun IrProperty.sizeIfNotOverriden(alreadyDefined : MutableSet<Name>) : Int {
    val backingField = this.backingField
    if (backingField == null || alreadyDefined.contains(backingField.name)) return 0
    else return backingField.type.size().also { alreadyDefined.add(backingField.name) }
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
        // I couldn't find specification of Boolean and Unit byte size. 1 and 0 make sense
        isBoolean() -> Byte.SIZE_BYTES
        isUnit() -> 0
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
                val alreadyDefined = mutableSetOf<Name>() // If the field is overriden then it shouldn't be summed in superclasses
                if (`class`.isData) {
                    val shallowSize = `class`.calculateSizeRecursively(alreadyDefined, irBuiltIns)
                    //val shallowSize = `class`.properties.sumOf { it.sizeIfNotOverriden(alreadyAccounted) }

                    `class`.functions.first { it.name.toString() == "shallowSize" && it.valueParameters.isEmpty() }.also { function ->
                        function.body = DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody {
                            +irReturn (irInt(shallowSize))
                        }
                    }
                    /*
                    Used for debuggin
                    TODO remove before release

                    val funPrintln = pluginContext.referenceFunctions(FqName("kotlin.io.println"))
                        .single {
                            val parameters = it.owner.valueParameters
                            parameters.size == 1 && parameters[0].type == pluginContext.irBuiltIns.anyNType
                        }
                    val duplicate = "TODO"
                    `class`.functions.first { it.name.toString() == "helper" && it.valueParameters.isEmpty() }.also { function ->
                        function.body = DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody {
                            val callPrintln = irCall(funPrintln)
                            var log = ""
                            `class`.properties.forEach {
                                val f = it.backingField
                                if (f != null) {
                                    log+=f.annotations.joinToString() + "\n"
                                }
                            }
                            callPrintln.putValueArgument(0, irString(duplicate))
                            +callPrintln

                        }
                    }

                     */

                }
                `class`
            }
        )
    }