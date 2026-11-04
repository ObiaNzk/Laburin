package com.example.laburin.data.remote

import com.example.laburin.di.NetworkModule
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import org.junit.rules.ExternalResource
import retrofit2.Retrofit

/** A real HTTP server per test, so the Retrofit annotations (paths, headers, bodies) are what gets checked. */
class ApiTestServer : ExternalResource() {

    private val server = MockWebServer()

    lateinit var retrofit: Retrofit
        private set

    override fun before() {
        server.start()
        retrofit = NetworkModule.buildRetrofit(OkHttpClient(), NetworkModule.provideJson(), server.url("/").toString())
    }

    override fun after() = server.close()

    fun respond(code: Int = 200, body: String = "") = server.enqueue(MockResponse(code = code, body = body))

    /** Simulates a network failure: nothing is listening anymore. */
    fun goOffline() = server.close()

    fun takeRequest(): RecordedRequest = server.takeRequest()

    inline fun <reified T> create(): T = retrofit.create(T::class.java)
}

fun RecordedRequest.bodyJson(): JsonElement = Json.parseToJsonElement(body!!.utf8())

fun json(text: String): JsonElement = Json.parseToJsonElement(text)
