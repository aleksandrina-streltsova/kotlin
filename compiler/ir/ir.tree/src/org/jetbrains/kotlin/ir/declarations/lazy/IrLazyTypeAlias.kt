/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrTypeAlias
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.withInitialIr
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.name.Name

@OptIn(ObsoleteDescriptorBasedAPI::class)
class IrLazyTypeAlias(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrTypeAliasSymbol,
    override val descriptor: TypeAliasDescriptor,
    override val name: Name,
    override var visibility: Visibility,
    override val isActual: Boolean,
    stubGenerator: DeclarationStubGenerator,
    typeTranslator: TypeTranslator
) :
    IrLazyDeclarationBase(startOffset, endOffset, origin, stubGenerator, typeTranslator),
    IrTypeAlias {

    init {
        symbol.bind(this)
    }

    override var typeParameters: List<IrTypeParameter> by lazyVar {
        descriptor.declaredTypeParameters.mapTo(arrayListOf()) {
            stubGenerator.generateOrGetTypeParameterStub(it)
        }
    }

    override val expandedType: IrType by lazy {
        withInitialIr {
            typeTranslator.buildWithScope(this) {
                descriptor.expandedType.toIrType()
            }
        }
    }
}
