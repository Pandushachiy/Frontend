package com.health.companion.presentation.screens.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.companion.data.canvas.CanvasApi
import com.health.companion.data.canvas.CanvasCluster
import com.health.companion.data.canvas.CanvasEdge
import com.health.companion.data.canvas.CanvasGraph
import com.health.companion.data.canvas.CanvasInsight
import com.health.companion.data.canvas.CanvasNode
import com.health.companion.data.canvas.GraphCacheManager
import com.health.companion.data.canvas.NodePositionRequest
import com.health.companion.data.canvas.RebuildGraphResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// ──────────────────────────────────────────────────────────────────────────────
// Render data — everything the Canvas needs to draw one node
// ──────────────────────────────────────────────────────────────────────────────

data class NodeRenderInfo(
    val id: String,
    val x: Float,
    val y: Float,
    val radius: Float,
    val colorInt: Int,
    val emoji: String,
    val label: String,
    val cluster: String,
    val isHot: Boolean,
    val annotationCount: Int,
    val importance: Float,
    val highlightAlpha: Float = 0f,
    val glowRadius: Float = 0f,
    val pulseScale: Float = 1f,
    val isSelected: Boolean = false,
    val isFocused: Boolean = true,
)

// Tap-pulse ring animation: one entry per in-progress pulse
data class TapPulse(
    val id: String,
    val x: Float,
    val y: Float,
    val baseRadius: Float,
    val colorInt: Int,
    val progress: Float = 0f,   // 0 → 1.4 over ~1200 ms
)

// PathAnimation still needed for SSE draw_path events
data class PathAnimation(
    val nodeNames: List<String>,
    val label: String,
    val color: Color,
    var progress: Float = 0f,
    var done: Boolean = false,
)

// ──────────────────────────────────────────────────────────────────────────────
// Internal physics node — mutable, accessed only from physics coroutine
// ──────────────────────────────────────────────────────────────────────────────
private class PhysicsNode(
    val id: String,
    var nodeRef: CanvasNode,
    var colorInt: Int,
    var baseRadius: Float,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
) {
    var ax: Float = 0f
    var ay: Float = 0f
    val isPinned get() = nodeRef.isPinned
}

// ──────────────────────────────────────────────────────────────────────────────
// ViewModel
// ──────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class CanvasViewModel @Inject constructor(
    private val canvasApi: CanvasApi,
    private val graphCacheManager: GraphCacheManager,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // ── Public state ─────────────────────────────────────────────────────────
    private val _graph = MutableStateFlow<CanvasGraph?>(null)
    val graph = _graph.asStateFlow()

    private val _renderInfos = MutableStateFlow<List<NodeRenderInfo>>(emptyList())
    val renderInfos = _renderInfos.asStateFlow()

    private val _edges = MutableStateFlow<List<CanvasEdge>>(emptyList())
    val edges = _edges.asStateFlow()

    private val _clusters = MutableStateFlow<List<CanvasCluster>>(emptyList())
    val clusters = _clusters.asStateFlow()

    private val _activePaths = MutableStateFlow<List<PathAnimation>>(emptyList())
    val activePaths = _activePaths.asStateFlow()

    private val _insights = MutableStateFlow<List<CanvasInsight>>(emptyList())
    val insights = _insights.asStateFlow()

    private val _focusedCluster = MutableStateFlow<String?>(null)
    val focusedCluster = _focusedCluster.asStateFlow()

    private val _selectedNodeId = MutableStateFlow<String?>(null)
    val selectedNodeId = _selectedNodeId.asStateFlow()

    private val _selectedNode = MutableStateFlow<CanvasNode?>(null)
    val selectedNode = _selectedNode.asStateFlow()

    private val _tapPulses = MutableStateFlow<List<TapPulse>>(emptyList())
    val tapPulses = _tapPulses.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _isSimulating = MutableStateFlow(false)
    val isSimulating = _isSimulating.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _annotationToast = MutableStateFlow<Triple<String, String, String>?>(null)
    val annotationToast = _annotationToast.asStateFlow()

    // ── Rebuild state ─────────────────────────────────────────────────────────
    private val _isRebuilding = MutableStateFlow(false)
    val isRebuilding = _isRebuilding.asStateFlow()

    // Rebuild result: non-null while the success toast is visible
    private val _rebuildResult: MutableStateFlow<RebuildGraphResponse?> = MutableStateFlow(null)
    val rebuildResult = _rebuildResult.asStateFlow()

    // ── Viewport state — persists across navigation, minimise, and process death ─
    // v6 keys — bump to discard all stale viewports from older builds
    private val KEY_VP_S  = "vp_s6"
    private val KEY_VP_OX = "vp_ox6"
    private val KEY_VP_OY = "vp_oy6"

    val viewportScale  = MutableStateFlow(
        savedStateHandle.get<Float>(KEY_VP_S)  ?: DEFAULT_SCALE
    )
    val viewportOffset = MutableStateFlow(Offset(
        savedStateHandle.get<Float>(KEY_VP_OX) ?: (390f / 2f - CANVAS_CX * DEFAULT_SCALE),
        savedStateHandle.get<Float>(KEY_VP_OY) ?: (680f / 2f - CANVAS_CY * DEFAULT_SCALE),
    ))

    /** Viewport mutations from the system (auto-fit, silent refresh). Does NOT mark user interaction. */
    fun updateViewport(scale: Float, offset: Offset) {
        viewportScale.value  = scale
        viewportOffset.value = offset
        savedStateHandle[KEY_VP_S]  = scale
        savedStateHandle[KEY_VP_OX] = offset.x
        savedStateHandle[KEY_VP_OY] = offset.y
    }

    /**
     * Called from the gesture handler when the user actively pans/zooms.
     * Marks that the user has a custom viewport — prevents auto-fit from overriding it
     * for the remainder of this session.
     */
    fun onUserViewportChange(scale: Float, offset: Offset) {
        updateViewport(scale, offset)
        if (!userHasManualViewport) userHasManualViewport = true
    }

    // Memory-only flag: true once the user pans/zooms THIS session.
    // Resets on cold start so auto-fit always centers on first load.
    private var userHasManualViewport = false

    // Screen dimensions supplied once from the composable so ViewModel can auto-fit
    private var screenW = 390f
    private var screenH = 700f

    // Memory-only: auto-fit runs once per process start (unless user already has a custom viewport)
    private var hasAutoFitted = false

    fun setScreenSize(w: Float, h: Float) {
        screenW = w
        screenH = h
        // Always reset the initial display to a sensible centered position.
        // updateViewport is NOT called here — that would save it to SavedStateHandle and prevent
        // auto-fit from running after physics. We just set the MutableStateFlow directly.
        val s = DEFAULT_SCALE
        viewportScale.value  = s
        viewportOffset.value = Offset(w / 2f - CANVAS_CX * s, h * 0.45f - CANVAS_CY * s)
    }

    // Particle time 0→1 cycling — drives animated dots on edges
    private val _particleTime = MutableStateFlow(0f)
    val particleTime = _particleTime.asStateFlow()

    // ── Internal physics state ────────────────────────────────────────────────
    private val physicsNodes = ArrayList<PhysicsNode>(350)
    private val nodeById = HashMap<String, PhysicsNode>(350)
    @Volatile private var simulationAlpha = 1.0f

    private var physicsJob: Job? = null
    private var particleJob: Job? = null
    private var pollJob: Job?    = null
    private var orbitJob: Job?   = null

    // Highlight/glow state — accessed only from main thread
    private val highlightAlphas = ConcurrentHashMap<String, Float>()
    private val glowRadii = ConcurrentHashMap<String, Float>()
    private val pulseScales = ConcurrentHashMap<String, Float>()

    // Physics + viewport constants
    companion object {
        internal const val DEFAULT_SCALE = 0.35f
        // Repulsion keeps nodes apart — must be strong enough vs gravity
        private const val REPULSION = 14000f
        private const val MAX_REPULSION_DIST = 550f
        // Springs pull connected nodes to REST_LENGTH apart
        private const val SPRING_K = 0.025f
        private const val REST_LENGTH = 160f
        // Center-of-mass gravity — extremely weak, just keeps graph visible
        private const val CENTER_STRENGTH = 0.012f
        // Velocity damping
        private const val DAMPING = 0.88f
        private const val MAX_VEL = 18f
        private const val CANVAS_CX = 500f
        private const val CANVAS_CY = 500f
        private const val MIN_ALPHA      = 0.002f
        private const val PARTICLE_SPEED  = 0.009f
        // ~1 full orbit per 6 minutes at 60 fps
        private const val ORBIT_SPEED     = 0.00028f
    }

    // ── API calls ─────────────────────────────────────────────────────────────

    fun loadGraph() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Phase 1: try to show cached graph instantly (no loading spinner)
            val cached = graphCacheManager.loadGraph()
            if (cached != null && cached.nodes.isNotEmpty()) {
                Timber.d("Canvas: showing ${cached.totalNodes} cached nodes instantly")
                applyGraph(cached)
                _isLoading.value = false
                // Also restore cached insights
                val cachedInsights = graphCacheManager.loadInsights()
                if (cachedInsights.isNotEmpty()) _insights.value = cachedInsights
            }

            // Phase 2: fetch fresh data from API
            try {
                val graph = canvasApi.getGraph()
                Timber.d("Canvas API: ${graph.totalNodes} nodes, ${graph.totalEdges} edges")
                if (cached == null || cached.nodes.isEmpty()) {
                    applyGraph(graph)
                } else {
                    // Silently update with fresh data without resetting physics
                    _graph.value = graph
                    _edges.value = graph.edges
                    _clusters.value = graph.clusters
                }
                // Persist to cache for next cold start
                graphCacheManager.saveGraph(graph)

                launch {
                    try {
                        val insights = canvasApi.getInsights()
                        _insights.value = insights
                        graphCacheManager.saveInsights(insights)
                    } catch (e: Exception) { Timber.w(e, "insights failed") }
                }
                startPolling()
            } catch (e: Exception) {
                Timber.e(e, "canvas graph API failed: ${e.message}")
                if (cached == null || cached.nodes.isEmpty()) {
                    _error.value = e.message ?: e.javaClass.simpleName
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun applyGraph(graph: CanvasGraph) {
        _graph.value = graph
        _edges.value = graph.edges
        _clusters.value = graph.clusters
        initPhysics(graph)
        startPhysicsLoop()
        startParticleLoop()
    }

    // ── Rebuild graph (full LLM recalculation) ───────────────────────────────

    fun rebuildGraph() {
        if (_isRebuilding.value) return
        viewModelScope.launch {
            _isRebuilding.value = true
            try {
                val result = canvasApi.rebuildGraph()
                _rebuildResult.value = result
                // Reload graph with updated data
                silentRefreshGraph()
                // Show success toast for 4 seconds
                delay(4_000)
                _rebuildResult.value = null
            } catch (e: Exception) {
                Timber.e(e, "rebuildGraph failed")
                _rebuildResult.value = RebuildGraphResponse(status = "error")
                delay(3_000)
                _rebuildResult.value = null
            } finally {
                _isRebuilding.value = false
            }
        }
    }

    // ── Physics initialization ────────────────────────────────────────────────

    private fun initPhysics(graph: CanvasGraph) {
        val existingPositions = HashMap<String, Pair<Float, Float>>()
        physicsNodes.forEach { existingPositions[it.id] = Pair(it.x, it.y) }

        physicsNodes.clear()
        nodeById.clear()

        val rng = java.util.Random(42)
        val totalNodes = graph.nodes.size
        val angleStep = (2 * Math.PI / maxOf(totalNodes, 1)).toFloat()

        graph.nodes.forEachIndexed { i, node ->
            val colorInt = try {
                android.graphics.Color.parseColor(node.color)
            } catch (e: Exception) {
                android.graphics.Color.parseColor("#4A9EFF")
            }
            // importance (0–1) drives base radius per backend spec: r = 12 + imp * 24
            // size (1/2/3) acts as a tier multiplier on top of that
            val importanceR = 12f + node.importance.coerceIn(0f, 1f) * 24f
            val baseRadius  = importanceR * when (node.size) {
                3 -> 1.20f; 2 -> 1.00f; else -> 0.82f
            }

            // Keep existing position if node was already in graph
            val existing = existingPositions[node.id]
            val savedX = node.positionX
            val savedY = node.positionY

            // Initial positions: wider spiral so nodes don't start on top of each other
            val x = when {
                existing != null -> existing.first
                savedX != null -> savedX
                else -> {
                    val angle = angleStep * i + rng.nextFloat() * 0.5f
                    val radius = 180f + i * 1.8f + rng.nextFloat() * 80f
                    CANVAS_CX + radius * cos(angle).toFloat()
                }
            }
            val y = when {
                existing != null -> existing.second
                savedY != null -> savedY
                else -> {
                    val angle = angleStep * i + rng.nextFloat() * 0.5f
                    val radius = 180f + i * 1.8f + rng.nextFloat() * 80f
                    CANVAS_CY + radius * sin(angle).toFloat()
                }
            }

            val pn = PhysicsNode(
                id = node.id,
                nodeRef = node,
                colorInt = colorInt,
                baseRadius = baseRadius,
                x = x,
                y = y,
                vx = (rng.nextFloat() - 0.5f) * 2f,
                vy = (rng.nextFloat() - 0.5f) * 2f,
            )
            physicsNodes.add(pn)
            nodeById[node.id] = pn
        }

        // Центрирование: при первой загрузке (нет сохранённых позиций) смещаем все ноды
        // так, чтобы центр масс был в CANVAS_CX, CANVAS_CY — ноды по центру экрана
        if (existingPositions.isEmpty() && physicsNodes.isNotEmpty()) {
            val sumX = physicsNodes.sumOf { it.x.toDouble() }.toFloat()
            val sumY = physicsNodes.sumOf { it.y.toDouble() }.toFloat()
            val n = physicsNodes.size.toFloat()
            val shiftX = CANVAS_CX - sumX / n
            val shiftY = CANVAS_CY - sumY / n
            physicsNodes.forEach { it.x += shiftX; it.y += shiftY }
        }

        simulationAlpha = if (existingPositions.isEmpty()) 1.0f else 0.3f
        publishRenderData()
    }

    // ── Physics loop ──────────────────────────────────────────────────────────

    private fun startPhysicsLoop() {
        orbitJob?.cancel()   // pause orbit while physics is running
        physicsJob?.cancel()
        physicsJob = viewModelScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) { _isSimulating.value = true }
            val edges = _edges.value

            while (isActive && simulationAlpha > MIN_ALPHA) {
                try {
                    physicsStep(edges)
                } catch (e: Exception) {
                    Timber.w(e, "Physics step error, skipping frame")
                }
                simulationAlpha *= 0.992f

                val snapshot = buildSnapshot()
                withContext(Dispatchers.Main) { _renderInfos.value = snapshot }
                // 20ms gives ~50fps cap and leaves headroom for Main thread at 60Hz
                delay(20)
            }

            // Final snapshot, then hand off to orbit loop
            val finalSnapshot = buildSnapshot()
            withContext(Dispatchers.Main) {
                _renderInfos.value = finalSnapshot
                _isSimulating.value = false
                autoFitViewportIfNeeded(screenW, screenH)
            }
            startOrbitLoop()
        }
    }

    private fun physicsStep(edges: List<CanvasEdge>) {
        val alpha = simulationAlpha
        val size = physicsNodes.size

        // Reset accelerations
        for (i in 0 until size) { physicsNodes[i].ax = 0f; physicsNodes[i].ay = 0f }

        // ── Repulsion O(n²) with distance cutoff ──────────────────────────────
        val maxDist2 = MAX_REPULSION_DIST * MAX_REPULSION_DIST
        for (i in 0 until size) {
            val a = physicsNodes[i]
            for (j in i + 1 until size) {
                val b = physicsNodes[j]
                val dx = b.x - a.x
                val dy = b.y - a.y
                val dist2 = dx * dx + dy * dy
                if (dist2 > maxDist2 || dist2 < 0.01f) continue
                val dist = sqrt(dist2)
                // Soft max at very close distances to prevent explosions
                val force = REPULSION / dist2.coerceAtLeast(25f) * alpha
                val fx = force * dx / dist
                val fy = force * dy / dist
                if (!a.isPinned) { a.ax -= fx; a.ay -= fy }
                if (!b.isPinned) { b.ax += fx; b.ay += fy }
            }
        }

        // ── Spring attraction along edges (Hooke's law) ───────────────────────
        edges.forEach { edge ->
            val a = nodeById[edge.sourceId] ?: return@forEach
            val b = nodeById[edge.targetId] ?: return@forEach
            val dx = b.x - a.x
            val dy = b.y - a.y
            val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
            val displacement = dist - REST_LENGTH
            val force = SPRING_K * displacement * edge.strength * alpha
            val fx = force * dx / dist
            val fy = force * dy / dist
            if (!a.isPinned) { a.ax += fx; a.ay += fy }
            if (!b.isPinned) { b.ax -= fx; b.ay -= fy }
        }

        // ── Euler integration ─────────────────────────────────────────────────
        physicsNodes.forEach { n ->
            if (!n.isPinned) {
                n.vx = (n.vx + n.ax) * DAMPING
                n.vy = (n.vy + n.ay) * DAMPING
                n.vx = n.vx.coerceIn(-MAX_VEL, MAX_VEL)
                n.vy = n.vy.coerceIn(-MAX_VEL, MAX_VEL)
                n.x += n.vx
                n.y += n.vy
            }
        }

        // ── Center-of-mass correction (D3-style forceCenter) ──────────────────
        // Translates the WHOLE graph as a unit — no individual node gravity.
        // This keeps the graph visible without collapsing it.
        var sumX = 0f; var sumY = 0f
        for (n in physicsNodes) { sumX += n.x; sumY += n.y }
        val comDx = (CANVAS_CX - sumX / size) * CENTER_STRENGTH
        val comDy = (CANVAS_CY - sumY / size) * CENTER_STRENGTH
        for (n in physicsNodes) {
            if (!n.isPinned) { n.x += comDx; n.y += comDy }
        }
    }

    private fun buildSnapshot(): List<NodeRenderInfo> {
        val focusedCluster = _focusedCluster.value
        val selectedId = _selectedNodeId.value
        return physicsNodes.map { pn ->
            NodeRenderInfo(
                id = pn.id,
                x = pn.x,
                y = pn.y,
                radius = pn.baseRadius * (pulseScales[pn.id] ?: 1f),
                colorInt = pn.colorInt,
                emoji = pn.nodeRef.emoji,
                label = pn.nodeRef.name,
                cluster = pn.nodeRef.cluster,
                isHot = pn.nodeRef.isHot,
                annotationCount = pn.nodeRef.annotations.size,
                importance = pn.nodeRef.importance,
                highlightAlpha = highlightAlphas[pn.id] ?: 0f,
                glowRadius = glowRadii[pn.id] ?: 0f,
                pulseScale = pulseScales[pn.id] ?: 1f,
                isSelected = pn.id == selectedId,
                isFocused = focusedCluster == null || focusedCluster == pn.nodeRef.cluster,
            )
        }
    }

    private fun publishRenderData() {
        _renderInfos.value = buildSnapshot()
    }

    // Called once after the very first simulation ends.
    // Fits the FULL bounding box of all nodes so every bubble is visible with minimal empty space.
    fun autoFitViewportIfNeeded(screenW: Float, screenH: Float) {
        if (hasAutoFitted) return
        // Respect any manual pan/zoom the user has done THIS session
        if (userHasManualViewport) { hasAutoFitted = true; return }

        val nodes = _renderInfos.value
        if (nodes.isEmpty()) return

        val xs = nodes.map { it.x }
        val ys = nodes.map { it.y }

        // Bounding box with generous padding
        val minX = xs.min() - 60f; val maxX = xs.max() + 60f
        val minY = ys.min() - 60f; val maxY = ys.max() + 60f
        val gw = (maxX - minX).coerceAtLeast(1f)
        val gh = (maxY - minY).coerceAtLeast(1f)

        // Centre of bounding box (not mean) — always visually centred
        val boxCX = (minX + maxX) / 2f
        val boxCY = (minY + maxY) / 2f

        // Usable screen area: leave horizontal margins + bottom chrome (tab + filter ≈ 110dp)
        val usableW = screenW * 0.90f
        val usableH = (screenH - 110f).coerceAtLeast(200f)
        val fit = minOf(usableW / gw, usableH / gh).coerceIn(0.12f, 1.2f)

        // Visual centre: horizontally centred, shifted 5% above screen centre to
        // compensate for the filter bar at the bottom
        val targetScreenX = screenW / 2f
        val targetScreenY = screenH * 0.45f
        updateViewport(
            scale  = fit,
            offset = Offset(targetScreenX - boxCX * fit, targetScreenY - boxCY * fit),
        )
        hasAutoFitted = true
    }

    // Smooth camera: compute bounding box of cluster (or all nodes) and set viewport
    // LivingMapScreen's spring animation takes care of smooth interpolation
    private fun focusViewportOnCluster(cluster: String?) {
        val nodes = if (cluster != null) {
            physicsNodes.filter { it.nodeRef.cluster == cluster }
        } else {
            physicsNodes.toList()
        }
        if (nodes.isEmpty()) return

        val minX = nodes.minOf { it.x } - 50f
        val maxX = nodes.maxOf { it.x } + 50f
        val minY = nodes.minOf { it.y } - 50f
        val maxY = nodes.maxOf { it.y } + 50f
        val gw = (maxX - minX).coerceAtLeast(1f)
        val gh = (maxY - minY).coerceAtLeast(1f)
        val boxCX = (minX + maxX) / 2f
        val boxCY = (minY + maxY) / 2f

        val usableW = screenW * 0.88f
        val usableH = (screenH - 120f).coerceAtLeast(200f)
        val fit = minOf(usableW / gw, usableH / gh).coerceIn(0.12f, 1.5f)

        val targetScreenX = screenW / 2f
        val targetScreenY = screenH * 0.42f
        updateViewport(
            scale  = fit,
            offset = Offset(targetScreenX - boxCX * fit, targetScreenY - boxCY * fit),
        )
    }

    // Reheat simulation (e.g. after drag ends or structural graph changes)
    private fun reheat(alpha: Float = 0.4f) {
        orbitJob?.cancel()   // orbit stops, physics takes over
        simulationAlpha = alpha
        if (physicsJob?.isActive != true) startPhysicsLoop()
    }

    // ── Orbital idle animation ────────────────────────────────────────────────
    /**
     * Slowly rotates the entire graph as a rigid body around its centroid.
     * Started once physics settles; paused whenever physics/reheat resumes.
     */
    private fun startOrbitLoop() {
        orbitJob?.cancel()
        orbitJob = viewModelScope.launch(Dispatchers.Default) {
            if (physicsNodes.isEmpty()) return@launch
            // Fixed rotation centre computed once at start
            val orbitCx = physicsNodes.sumOf { it.x.toDouble() }.toFloat() / physicsNodes.size
            val orbitCy = physicsNodes.sumOf { it.y.toDouble() }.toFloat() / physicsNodes.size
            val cosA = cos(ORBIT_SPEED.toDouble()).toFloat()
            val sinA = sin(ORBIT_SPEED.toDouble()).toFloat()

            while (isActive) {
                physicsNodes.forEach { node ->
                    if (node.isPinned) return@forEach
                    val dx = node.x - orbitCx
                    val dy = node.y - orbitCy
                    node.x = orbitCx + dx * cosA - dy * sinA
                    node.y = orbitCy + dx * sinA + dy * cosA
                }
                val snapshot = buildSnapshot()
                withContext(Dispatchers.Main) { _renderInfos.value = snapshot }
                // 33ms = ~30fps for orbit — imperceptible at 6-min/revolution speed,
                // halves Main-thread update pressure vs 16ms at 60Hz displays
                delay(33)
            }
        }
    }

    // ── Particle animation ────────────────────────────────────────────────────

    private fun startParticleLoop() {
        particleJob?.cancel()
        particleJob = viewModelScope.launch {
            while (isActive) {
                var t = _particleTime.value + PARTICLE_SPEED
                if (t > 100_000f) t -= 100_000f
                _particleTime.value = t

                val phase = t * 2f * Math.PI.toFloat()
                physicsNodes.forEach { pn ->
                    if (pn.nodeRef.isHot) {
                        val nodePhase = phase + (pn.id.hashCode() and 0xFF) * 0.025f
                        pulseScales[pn.id] = 1f + kotlin.math.sin(nodePhase) * 0.13f
                    }
                }

                delay(32)
            }
        }
    }

    // ── Silent live refresh (no viewport/physics reset) ───────────────────────

    /**
     * Fetch latest graph from the API and update node data **in-place**:
     * – existing nodes keep their positions (physics untouched)
     * – new nodes are added near the centroid with a small reheat
     * – removed nodes are dropped
     * Viewport is NEVER touched.
     */
    private suspend fun silentRefreshGraph() {
        try {
            val newGraph = canvasApi.getGraph()
            val existingIds = nodeById.keys.toSet()
            val newIds      = newGraph.nodes.map { it.id }.toSet()

            // Update edges & clusters immediately
            _edges.value    = newGraph.edges
            _clusters.value = newGraph.clusters
            _graph.value    = newGraph

            val hasNewNodes    = newIds.any { it !in existingIds }
            val hasRemovedNodes = existingIds.any { it !in newIds }

            if (!hasNewNodes && !hasRemovedNodes) {
                // Data-only: patch nodeRef/color/radius in-place — orbit keeps running
                val fresh = newGraph.nodes.associateBy { it.id }
                physicsNodes.forEach { pn ->
                    val node = fresh[pn.id] ?: return@forEach
                    pn.nodeRef = node
                    pn.colorInt = runCatching { android.graphics.Color.parseColor(node.color) }
                        .getOrElse { android.graphics.Color.parseColor("#4A9EFF") }
                    val importanceR = 12f + node.importance.coerceIn(0f, 1f) * 24f
                    pn.baseRadius = importanceR * when (node.size) { 3 -> 1.20f; 2 -> 1.00f; else -> 0.82f }
                }
                withContext(Dispatchers.Main) { publishRenderData() }
            } else {
                orbitJob?.cancel()
                // Structural change: add/remove nodes then do a gentle reheat
                if (hasRemovedNodes) {
                    physicsNodes.removeAll { it.id !in newIds }
                    (existingIds - newIds).forEach { nodeById.remove(it) }
                }
                if (hasNewNodes) {
                    val cxCom = if (physicsNodes.isNotEmpty()) physicsNodes.map { it.x }.average().toFloat() else CANVAS_CX
                    val cyCom = if (physicsNodes.isNotEmpty()) physicsNodes.map { it.y }.average().toFloat() else CANVAS_CY
                    val rng   = java.util.Random()
                    val spawnIds = mutableListOf<String>()
                    newGraph.nodes.filter { it.id !in existingIds }.forEach { node ->
                        val colorInt = runCatching { android.graphics.Color.parseColor(node.color) }
                            .getOrElse { android.graphics.Color.parseColor("#4A9EFF") }
                        val importanceR = 12f + node.importance.coerceIn(0f, 1f) * 24f
                        val baseR = importanceR * when (node.size) { 3 -> 1.20f; 2 -> 1.00f; else -> 0.82f }
                        PhysicsNode(node.id, node, colorInt, baseR,
                            x  = node.positionX ?: (cxCom + (rng.nextFloat() - 0.5f) * 200f),
                            y  = node.positionY ?: (cyCom + (rng.nextFloat() - 0.5f) * 200f),
                            vx = (rng.nextFloat() - 0.5f) * 2f,
                            vy = (rng.nextFloat() - 0.5f) * 2f,
                        ).also { physicsNodes.add(it); nodeById[node.id] = it; spawnIds.add(node.id) }
                    }
                    // Animate new nodes: grow from scale 0 → 1
                    if (spawnIds.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            spawnIds.forEach { id -> pulseScales[id] = 0f }
                            viewModelScope.launch {
                                for (step in 0..20) {
                                    val s = step / 20f
                                    spawnIds.forEach { id -> pulseScales[id] = s }
                                    publishRenderData()
                                    delay(18)
                                }
                                spawnIds.forEach { id -> pulseScales.remove(id) }
                            }
                        }
                    }
                }
                reheat(0.14f)
            }
            // Update cache after successful silent refresh
            graphCacheManager.saveGraph(newGraph)
        } catch (e: Exception) {
            Timber.w(e, "silent graph refresh failed")
        } finally {
            if (physicsJob?.isActive != true) startOrbitLoop()  // resume orbit
        }
    }

    /** Background polling — refreshes canvas data every 12 s.
     *  First poll after 5 s so new facts from the chat appear almost immediately. */
    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            delay(5_000)
            while (isActive) {
                silentRefreshGraph()
                delay(12_000)
            }
        }
    }

    // ── Canvas update events from SSE ─────────────────────────────────────────

    fun handleCanvasUpdate(action: String, payloadJson: String) {
        viewModelScope.launch {
            try {
                val payload = JSONObject(payloadJson)
                when (action) {
                    "highlight" -> {
                        val names = payload.optJSONArray("node_names")?.toStringList()
                            ?: return@launch
                        animateHighlight(names)
                    }
                    "focus_cluster" -> {
                        val cluster = payload.optString("cluster").takeIf { it.isNotBlank() }
                        _focusedCluster.value = cluster
                        publishRenderData()
                        focusViewportOnCluster(cluster)
                    }
                    "draw_path" -> {
                        val names = payload.optJSONArray("node_names")?.toStringList()
                            ?: return@launch
                        val label = payload.optString("label", "")
                        val colorHex = payload.optString("color", "#EF4444")
                        val color = parseColor(colorHex)
                        animatePath(names, label, color)
                    }
                    "annotate" -> {
                        val nodeName =
                            payload.optString("node_name").takeIf { it.isNotBlank() }
                                ?: return@launch
                        val text =
                            payload.optString("text").takeIf { it.isNotBlank() }
                                ?: return@launch
                        val colorHex = payload.optString("color", "#FFD166")
                        _annotationToast.value = Triple(nodeName, text, colorHex)
                        // Show toast, pulse the node, then silently refresh graph data in-place
                        animatePulse(listOf(nodeName))
                        delay(800)
                        silentRefreshGraph()
                        delay(2200)
                        _annotationToast.value = null
                    }
                    "zoom_out" -> {
                        _focusedCluster.value = null
                        publishRenderData()
                        focusViewportOnCluster(null)
                    }
                    "pulse" -> {
                        val names = payload.optJSONArray("node_names")?.toStringList()
                            ?: return@launch
                        animatePulse(names)
                    }
                    "discover" -> {
                        val connections = payload.optJSONArray("connections")
                        if (connections != null) {
                            val names = mutableListOf<String>()
                            for (i in 0 until connections.length()) {
                                val c = connections.optJSONObject(i) ?: continue
                                c.optString("source").takeIf { it.isNotBlank() }
                                    ?.let { names.add(it) }
                                c.optString("target").takeIf { it.isNotBlank() }
                                    ?.let { names.add(it) }
                            }
                            if (names.isNotEmpty()) animateHighlight(names.distinct())
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "canvas_update parse error")
            }
        }
    }

    // ── Animation helpers ─────────────────────────────────────────────────────

    private fun animateHighlight(names: List<String>) {
        val ids = findNodeIds(names)
        ids.forEach { id ->
            viewModelScope.launch {
                for (step in 0..12) {
                    highlightAlphas[id] = step / 12f
                    glowRadii[id] = step * 2.2f
                    publishRenderData()
                    delay(25)
                }
                delay(2200)
                for (step in 12 downTo 0) {
                    highlightAlphas[id] = step / 12f
                    glowRadii[id] = step * 2.2f
                    publishRenderData()
                    delay(70)
                }
                highlightAlphas.remove(id)
                glowRadii.remove(id)
                publishRenderData()
            }
        }
    }

    private fun animatePulse(names: List<String>) {
        val ids = findNodeIds(names)
        ids.forEach { id ->
            viewModelScope.launch {
                for (step in 0..8) {
                    pulseScales[id] = 1f + step * 0.04f
                    publishRenderData()
                    delay(20)
                }
                for (step in 8 downTo 0) {
                    pulseScales[id] = 1f + step * 0.04f
                    publishRenderData()
                    delay(30)
                }
                pulseScales.remove(id)
                publishRenderData()
            }
        }
    }

    private fun animatePath(names: List<String>, label: String, color: Color) {
        val path = PathAnimation(nodeNames = names, label = label, color = color)
        viewModelScope.launch {
            _activePaths.value = _activePaths.value + path
            val steps = names.size * 30
            for (step in 0..steps) {
                path.progress = step.toFloat() / steps
                _activePaths.value = _activePaths.value.toList() // trigger emit
                delay(20)
            }
            delay(2500)
            path.done = true
            _activePaths.value = _activePaths.value.filter { !it.done }
        }
    }

    // ── Node interaction ──────────────────────────────────────────────────────

    fun onNodeClick(nodeId: String) {
        val newId = if (_selectedNodeId.value == nodeId) null else nodeId
        _selectedNodeId.value = newId
        _selectedNode.value = if (newId != null) nodeById[newId]?.nodeRef else null
        publishRenderData()

        // Animate tap-pulse rings from the tapped node
        val nri = _renderInfos.value.find { it.id == nodeId } ?: return
        viewModelScope.launch {
            val pulseId = "$nodeId:${System.currentTimeMillis()}"
            val startMs = System.currentTimeMillis()
            val durationMs = 1200L
            while (isActive) {
                val t = (System.currentTimeMillis() - startMs).toFloat() / durationMs * 1.4f
                _tapPulses.value = _tapPulses.value.filter { it.id != pulseId } +
                        TapPulse(pulseId, nri.x, nri.y, nri.radius, nri.colorInt, t)
                if (t >= 1.4f) break
                delay(16)
            }
            _tapPulses.value = _tapPulses.value.filter { it.id != pulseId }
        }
    }

    fun dismissSelectedNode() {
        _selectedNodeId.value = null
        _selectedNode.value = null
        publishRenderData()
    }

    fun onNodeDragEnd(nodeId: String, x: Float, y: Float) {
        nodeById[nodeId]?.let { pn ->
            pn.x = x; pn.y = y; pn.vx = 0f; pn.vy = 0f
        }
        reheat(0.25f)
        publishRenderData()
        viewModelScope.launch {
            try {
                val node = nodeById[nodeId]?.nodeRef ?: return@launch
                canvasApi.saveNodePosition(
                    NodePositionRequest(
                        entityId = nodeId.removePrefix("kg_"),
                        entitySource = node.source,
                        x = x, y = y, isPinned = true
                    )
                )
            } catch (e: Exception) {
                Timber.w(e, "saveNodePosition failed")
            }
        }
    }

    // ── Utilities ──────────────────────────────────────────────────────────────

    private fun findNodeIds(names: List<String>): List<String> =
        physicsNodes
            .filter { pn -> names.any { it.equals(pn.nodeRef.name, ignoreCase = true) } }
            .map { it.id }

    private fun parseColor(hex: String): Color = try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFFEF4444)
    }

    private fun JSONArray.toStringList(): List<String> =
        (0 until length()).mapNotNull { optString(it).takeIf { s -> s.isNotBlank() } }

    override fun onCleared() {
        super.onCleared()
        physicsJob?.cancel()
        particleJob?.cancel()
    }
}
