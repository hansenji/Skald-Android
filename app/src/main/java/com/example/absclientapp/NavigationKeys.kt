package com.example.absclientapp

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object Login : NavKey

@Serializable
data object Library : NavKey

@Serializable
data class Detail(val bookId: String) : NavKey

@Serializable
data object Player : NavKey
