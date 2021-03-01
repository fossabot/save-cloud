@file:Suppress("PACKAGE_NAME_INCORRECT_PATH")

package org.cqfn.save.agent

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlin.test.AfterClass
import kotlin.test.AfterTest
import kotlin.test.BeforeClass


@BeforeClass
fun setUp() {
//        val tmpDir =
    platform.posix.system("export PATH=.:\$PATH")
    platform.posix.system("echo 'sleep 5' > save && chmod +x save")
}

@AfterClass
fun tearDown() {
    platform.posix.system("rm save")
}

@Suppress("INLINE_CLASS_CAN_BE_USED")
class SaveAgentTest {
    // todo: create temp dir with save executable, run tests with env PATH=$PATH+temp dir, change `sleep 5` to `./save` and mock it with `sleep` only in tests
    private val saveAgentForTest = SaveAgent(AgentConfiguration(), httpClient = HttpClient(MockEngine) {
        install(JsonFeature) {
            serializer = KotlinxSerializer(Json {
                serializersModule = SerializersModule {
                    // for some reason for K/N it's needed explicitly, at least for ktor 1.5.1, kotlin 1.4.21
                    contextual(HeartbeatResponse::class, HeartbeatResponse.serializer())
                }
            })
        }
        engine {
            addHandler { request ->
                when (request.url.encodedPath) {
                    "/heartbeat" -> respond(
                        Json.encodeToString(HeartbeatResponse.serializer(), ContinueResponse),
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                    "/executionData" -> respond("", status = HttpStatusCode.OK)
                    else -> error("Unhandled ${request.url}")
                }
            }
        }
    })

    @Test
    fun `agent should send heartbeats`() {
        runBlocking {
            saveAgentForTest.sendHeartbeat(ExecutionProgress(0))
        }
    }

    @Test
    fun `should change state to FINISHED after SAVE CLI returns`() = runBlocking {
        assertEquals(AgentState.IDLE, saveAgentForTest.state.value)
        runBlocking { saveAgentForTest.startSaveProcess() }
        println("Waiting for 5 sec in test")
        delay(5_000)  // todo: proper criterion of SAVE CLI termination
        assertEquals(AgentState.FINISHED, saveAgentForTest.state.value)
    }
}
