package dev.vikingsen.absclientapp.feature.library.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object Library : NavKey

@Serializable
data class Detail(val bookId: String) : NavKey
