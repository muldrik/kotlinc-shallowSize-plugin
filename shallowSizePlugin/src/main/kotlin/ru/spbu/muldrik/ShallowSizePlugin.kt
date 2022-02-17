package ru.spbu.muldrik

import arrow.meta.CliPlugin
import arrow.meta.Meta
import arrow.meta.phases.CompilerContext
import kotlin.contracts.ExperimentalContracts

/**
 * Standard ArrowMeta way of registering a plugin
 */
class ShallowSizePlugin : Meta {
    @ExperimentalContracts
    override fun intercept(ctx: CompilerContext): List<CliPlugin> =
        listOf(
            GenerateShallowSize
        )
}