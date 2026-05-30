package dev.vikingsen.absclientapp.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Library(
    val id: String,
    val name: String,
    val type: String?
)
