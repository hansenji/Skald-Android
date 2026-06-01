package dev.vikingsen.skald.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Library(
    val id: String,
    val name: String,
    val type: String?
)
