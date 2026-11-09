package dev.vikingsen.skald.feature.assistant.engine

import dev.vikingsen.skald.feature.assistant.mcp.McpToolResult
import dev.vikingsen.skald.feature.assistant.mcp.SkaldMcpToolRegistry
import dev.vikingsen.skald.feature.assistant.mcp.ToolExecutionPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface AssistantResponse {
    data class ImmediateExecution(
        val toolName: String,
        val arguments: Map<String, Any?>,
        val result: McpToolResult
    ) : AssistantResponse

    data class PendingConfirmation(
        val toolName: String,
        val arguments: Map<String, Any?>,
        val promptTitle: String,
        val promptDetails: String,
        val targetId: String
    ) : AssistantResponse

    data class Conversational(
        val replyText: String
    ) : AssistantResponse
}

class LocalAgentCoordinator(
    private val toolRegistry: SkaldMcpToolRegistry
) {

    suspend fun processQuery(query: String): AssistantResponse = withContext(Dispatchers.Default) {
        val matched = FastSemanticMatcher.match(query)
        if (matched != null) {
            val plan = toolRegistry.planExecution(matched.toolName, matched.arguments)
            return@withContext when (plan) {
                is ToolExecutionPlan.Immediate -> {
                    val result = toolRegistry.executeTool(plan.toolName, plan.arguments)
                    AssistantResponse.ImmediateExecution(
                        toolName = plan.toolName,
                        arguments = plan.arguments,
                        result = result
                    )
                }
                is ToolExecutionPlan.RequiresConfirmation -> {
                    AssistantResponse.PendingConfirmation(
                        toolName = plan.toolName,
                        arguments = plan.arguments,
                        promptTitle = plan.promptTitle,
                        promptDetails = plan.promptDetails,
                        targetId = plan.targetId
                    )
                }
            }
        }

        // Fuzzy fallback for search and play
        if (query.trim().length > 2) {
            val plan = toolRegistry.planExecution(
                SkaldMcpToolRegistry.TOOL_SEARCH_AND_PLAY,
                mapOf("query" to query.trim())
            )
            val result = toolRegistry.executeTool(
                SkaldMcpToolRegistry.TOOL_SEARCH_AND_PLAY,
                mapOf("query" to query.trim())
            )
            if (result is McpToolResult.Success) {
                return@withContext AssistantResponse.ImmediateExecution(
                    toolName = SkaldMcpToolRegistry.TOOL_SEARCH_AND_PLAY,
                    arguments = mapOf("query" to query.trim()),
                    result = result
                )
            }
        }

        AssistantResponse.Conversational(
            "I'm not sure how to help with that. You can ask me to play an audiobook, pause, set a sleep timer, adjust speed, or download books."
        )
    }

    suspend fun executeConfirmedTool(
        toolName: String,
        arguments: Map<String, Any?>
    ): McpToolResult {
        return toolRegistry.executeTool(toolName, arguments)
    }
}
