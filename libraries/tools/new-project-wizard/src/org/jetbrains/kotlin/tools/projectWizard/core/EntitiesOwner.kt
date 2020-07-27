package org.jetbrains.kotlin.tools.projectWizard.core

interface EntitiesOwnerDescriptor {
    @get:NonNls
    val id: String
}

interface EntitiesOwner<D : EntitiesOwnerDescriptor> {
    val descriptor: D
}