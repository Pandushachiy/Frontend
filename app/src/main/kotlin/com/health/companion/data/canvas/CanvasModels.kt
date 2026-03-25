package com.health.companion.data.canvas

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CanvasAnnotation(
    val id: String,
    val text: String,
    val color: String,
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CanvasNode(
    val id: String,
    val name: String,
    @SerialName("entity_type") val entityType: String,
    val cluster: String,
    val color: String,
    val size: Int,
    val emoji: String,
    val importance: Float,
    @SerialName("activity_score") val activityScore: Float,
    @SerialName("is_hot") val isHot: Boolean,
    val confidence: Float,
    val description: String? = null,
    @SerialName("position_x") val positionX: Float? = null,
    @SerialName("position_y") val positionY: Float? = null,
    @SerialName("is_pinned") val isPinned: Boolean = false,
    val annotations: List<CanvasAnnotation> = emptyList(),
    val source: String
)

@Serializable
data class CanvasEdge(
    val id: String,
    @SerialName("source_id") val sourceId: String,
    @SerialName("target_id") val targetId: String,
    @SerialName("source_name") val sourceName: String,
    @SerialName("target_name") val targetName: String,
    @SerialName("relation_type") val relationType: String,
    val strength: Float,
    val animated: Boolean
)

@Serializable
data class CanvasCluster(
    val id: String,
    val label: String,
    val color: String,
    @SerialName("node_count") val nodeCount: Int
)

@Serializable
data class CanvasGraph(
    val nodes: List<CanvasNode>,
    val edges: List<CanvasEdge>,
    val clusters: List<CanvasCluster>,
    @SerialName("total_nodes") val totalNodes: Int,
    @SerialName("total_edges") val totalEdges: Int,
    @SerialName("last_updated") val lastUpdated: String? = null
)

@Serializable
data class CanvasInsight(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    @SerialName("related_nodes") val relatedNodes: List<String> = emptyList(),
    val action: String? = null
)

@Serializable
data class CanvasState(
    val graph: CanvasGraph? = null,
    val insights: List<CanvasInsight> = emptyList()
)

@Serializable
data class NodePositionRequest(
    @SerialName("entity_id") val entityId: String,
    @SerialName("entity_source") val entitySource: String = "entity",
    val x: Float,
    val y: Float,
    @SerialName("is_pinned") val isPinned: Boolean = false
)

@Serializable
data class AnnotationRequest(
    @SerialName("node_id") val nodeId: String,
    val text: String,
    val color: String = "#FFD166"
)

@Serializable
data class CreateResponse(
    val id: String,
    val status: String = "ok"
)

@Serializable
data class RebuildGraphStats(
    val created: Int = 0,
    val updated: Int = 0,
    val removed: Int = 0,
    val total: Int = 0
)

@Serializable
data class RebuildGraphResponse(
    val status: String = "ok",
    val stats: RebuildGraphStats = RebuildGraphStats()
)
