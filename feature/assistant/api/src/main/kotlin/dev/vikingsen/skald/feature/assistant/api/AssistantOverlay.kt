package dev.vikingsen.skald.feature.assistant.api

/**
 * Public contract for opening the Assistant sheet or overlay.
 */
interface AssistantOverlayController {
    fun showAssistant()
    fun hideAssistant()
}
