package org.cqfn.save.agent.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.cqfn.save.agent.AgentConfiguration
import org.cqfn.save.agent.ContinueResponse
import org.cqfn.save.agent.HeartbeatResponse
import org.cqfn.save.agent.SaveAgent

/**
 * Create a [SaveAgent] with underlying [MockEngine] for http.
 *
 * @param handler a handler for HTTP requests in mock engine
 */
fun createAgentForTest(handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) = SaveAgent(AgentConfiguration(), httpClient = HttpClient(MockEngine) {
    install(JsonFeature) {
        serializer = KotlinxSerializer(Json {
            serializersModule = SerializersModule {
                // for some reason for K/N it's needed explicitly, at least for ktor 1.5.1, kotlin 1.4.21
                contextual(HeartbeatResponse::class, HeartbeatResponse.serializer())
            }
        })
    }
    engine {
        addHandler(handler)
    }
})