package dev.vikingsen.skald.feature.library

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml

fun String.parseHtml(): AnnotatedString {
    return AnnotatedString.fromHtml(this)
}
