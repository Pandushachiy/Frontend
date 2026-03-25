package com.health.companion.data.canvas

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CanvasApi {

    @GET("canvas/graph")
    suspend fun getGraph(@Query("limit") limit: Int = 150): CanvasGraph

    @GET("canvas/insights")
    suspend fun getInsights(): List<CanvasInsight>

    @GET("canvas/state")
    suspend fun getState(): CanvasState

    @POST("canvas/node-position")
    suspend fun saveNodePosition(@Body body: NodePositionRequest): retrofit2.Response<Unit>

    @POST("canvas/annotation")
    suspend fun createAnnotation(@Body body: AnnotationRequest): CreateResponse

    @DELETE("canvas/annotation/{id}")
    suspend fun deleteAnnotation(@Path("id") id: String): retrofit2.Response<Unit>

    @POST("canvas/rebuild-graph")
    suspend fun rebuildGraph(): RebuildGraphResponse
}
