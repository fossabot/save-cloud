package com.saveourtool.save.agent

import com.saveourtool.save.agent.utils.readProperties

import generated.SAVE_CORE_VERSION
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.json.JsonPlugin
import io.ktor.client.plugins.kotlinx.serializer.KotlinxSerializer
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import platform.posix.system

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap

class SaveAgentTest {
    @OptIn(ExperimentalSerializationApi::class)
    private val configuration: AgentConfiguration = Properties.decodeFromStringMap<AgentConfiguration>(readProperties("src/linuxX64Main/resources/agent.properties")).let {
        if (Platform.osFamily == OsFamily.WINDOWS) it.copy(cliCommand = "save-$SAVE_CORE_VERSION-linuxX64.bat") else it
    }
    private val saveAgentForTest = SaveAgent(configuration, httpClient = HttpClient(MockEngine) {
        install(JsonPlugin) {
            serializer = KotlinxSerializer(json)
        }
        engine {
            addHandler { request ->
                when (request.url.encodedPath) {
                    "/heartbeat" -> respond(
                        json.encodeToString(PolymorphicSerializer(HeartbeatResponse::class), ContinueResponse),
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                    "/executionData" -> respond("", status = HttpStatusCode.OK)
                    "/executionLogs" -> respond("", status = HttpStatusCode.OK)
                    else -> error("Unhandled ${request.url}")
                }
            }
        }
    })

    @BeforeTest
    fun setUp() {
        if (Platform.osFamily != OsFamily.WINDOWS) {
            platform.posix.system("echo echo 0 > save-$SAVE_CORE_VERSION-linuxX64.kexe")
            platform.posix.system("chmod +x save-$SAVE_CORE_VERSION-linuxX64.kexe")
        } else {
            platform.posix.system("echo echo 0 > save-$SAVE_CORE_VERSION-linuxX64.bat")
        }
    }

    @AfterTest
    fun tearDown() {
        platform.posix.system("rm -rf save-$SAVE_CORE_VERSION-linuxX64.kexe")
    }

    @Test
    fun `agent should send heartbeats`() {
        runBlocking {
            saveAgentForTest.sendHeartbeat(ExecutionProgress(0))
        }
    }

    @Test
    fun `should change state to FINISHED after SAVE CLI returns`() = runBlocking {
        assertEquals(AgentState.STARTING, saveAgentForTest.state.value)
        runBlocking { saveAgentForTest.run { startSaveProcess("") } }
        assertEquals(AgentState.FINISHED, saveAgentForTest.state.value)
    }
}
