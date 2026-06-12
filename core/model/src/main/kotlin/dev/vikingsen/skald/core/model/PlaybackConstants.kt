package dev.vikingsen.skald.core.model

object PlaybackConstants {
    val PLAYBACK_SPEEDS = listOf(
        0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.6f, 1.7f, 1.8f, 1.9f, 2.0f
    )
    val ANDROID_AUTO_SPEEDS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    val SPEED_RANGE = 0.5f..2.0f
    const val SPEED_SLIDER_STEPS = 14
}
