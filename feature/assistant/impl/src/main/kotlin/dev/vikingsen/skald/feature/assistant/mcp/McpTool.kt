package dev.vikingsen.skald.feature.assistant.mcp

enum class ToolCategory {
    PLAYBACK,
    SETTINGS,
    DOWNLOAD,
    QUERY
}

data class McpParameterDefinition(
    val type: String,
    val description: String,
    val isRequired: Boolean = false,
    val default: Any? = null
)

data class McpToolDefinition(
    val name: String,
    val description: String,
    val category: ToolCategory,
    val requiresConfirmation: Boolean = (category == ToolCategory.DOWNLOAD),
    val parameters: Map<String, McpParameterDefinition> = emptyMap()
)

sealed interface ToolExecutionPlan {
    data class Immediate(
        val toolName: String,
        val arguments: Map<String, Any?> = emptyMap()
    ) : ToolExecutionPlan

    data class RequiresConfirmation(
        val toolName: String,
        val arguments: Map<String, Any?> = emptyMap(),
        val promptTitle: String,
        val promptDetails: String,
        val targetId: String
    ) : ToolExecutionPlan
}

sealed interface McpToolResult {
    data class Success(
        val message: String,
        val payload: Any? = null
    ) : McpToolResult

    data class Error(
        val errorMessage: String,
        val recoverable: Boolean = true
    ) : McpToolResult
}
