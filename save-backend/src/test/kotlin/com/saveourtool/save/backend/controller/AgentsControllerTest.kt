package com.saveourtool.save.backend.controller

import com.saveourtool.save.agent.AgentState
import com.saveourtool.save.agent.AgentVersion
import com.saveourtool.save.backend.SaveApplication
import com.saveourtool.save.backend.controllers.ProjectController
import com.saveourtool.save.backend.repository.AgentRepository
import com.saveourtool.save.backend.repository.AgentStatusRepository
import com.saveourtool.save.backend.scheduling.StandardSuitesUpdateScheduler
import com.saveourtool.save.backend.security.ProjectPermissionEvaluator
import com.saveourtool.save.backend.utils.MySqlExtension
import com.saveourtool.save.entities.AgentStatus
import com.saveourtool.save.entities.AgentStatusDto
import com.saveourtool.save.entities.AgentStatusesForExecution
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.reactive.function.BodyInserters
import java.time.LocalDateTime
import java.time.Month
import javax.persistence.EntityManager

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(MySqlExtension::class)
@MockBeans(
    MockBean(StandardSuitesUpdateScheduler::class),
    MockBean(ProjectController::class),
    MockBean(ProjectPermissionEvaluator::class),
)
class AgentsControllerTest {
    @Autowired
    lateinit var webTestClient: WebTestClient

    @Autowired
    lateinit var agentStatusRepository: AgentStatusRepository

    @Autowired
    lateinit var agentRepository: AgentRepository

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @Autowired
    private lateinit var entityManager: EntityManager
    private lateinit var transactionTemplate: TransactionTemplate

    @BeforeEach
    fun setUp() {
        transactionTemplate = TransactionTemplate(transactionManager)
    }

    @Test
    fun `should save agent statuses`() {
        webTestClient
            .method(HttpMethod.POST)
            .uri("/internal/updateAgentStatusesWithDto")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(
                listOf(
                    AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "container-1")
                )
            ))
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    @Suppress("TOO_LONG_FUNCTION")
    fun `check that agent statuses are updated`() {
        updateAgentStatuses(
            listOf(
                AgentStatusDto(LocalDateTime.now(), AgentState.IDLE, "container-2")
            )
        )

        val firstAgentIdle = getLastIdleForSecondContainer()

        webTestClient
            .method(HttpMethod.POST)
            .uri("/internal/updateAgentStatusesWithDto")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(
                listOf(
                    AgentStatusDto(LocalDateTime.of(2020, Month.MAY, 10, 16, 30, 20), AgentState.IDLE, "container-2")
                )
            ))
            .exchange()
            .expectStatus()
            .isOk

        updateAgentStatuses(
            listOf(
                AgentStatusDto(LocalDateTime.now(), AgentState.BUSY, "container-2")
            )
        )

        assertTrue(
            transactionTemplate.execute {
                agentStatusRepository
                    .findAll()
                    .filter { it.state == AgentState.IDLE && it.agent.containerId == "container-2" }
                    .size == 1
            }!!
        )

        val lastUpdatedIdle = getLastIdleForSecondContainer()

        assertTrue(
            lastUpdatedIdle.startTime.withNano(0) == firstAgentIdle.startTime.withNano(0) &&
                    lastUpdatedIdle.endTime.withNano(0) != firstAgentIdle.endTime.withNano(0)
        )
    }

    @Test
    @Suppress("UnsafeCallOnNullableType")
    fun `should return latest status by container id`() {
        webTestClient
            .method(HttpMethod.GET)
            .uri("/internal/getAgentsStatusesForSameExecution?agentId=container-1")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectBody<AgentStatusesForExecution>()
            .consumeWith {
                val statuses = requireNotNull(it.responseBody).agentStatuses
                Assertions.assertEquals(2, statuses.size)
                Assertions.assertEquals(AgentState.IDLE, statuses.first().state)
                Assertions.assertEquals(AgentState.BUSY, statuses[1].state)
            }
    }

    @Test
    fun `check save agent version`() {
        val agentVersion = AgentVersion("container-1", "0.0.1")
        webTestClient
            .method(HttpMethod.POST)
            .uri("/internal/saveAgentVersion")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(agentVersion))
            .exchange()
            .expectStatus()
            .isOk
        Assertions.assertEquals(agentRepository.findByContainerId(agentVersion.containerId)?.version, agentVersion.version)
    }

    private fun updateAgentStatuses(body: List<AgentStatusDto>) {
        webTestClient
            .method(HttpMethod.POST)
            .uri("/internal/updateAgentStatusesWithDto")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(
                body
            ))
            .exchange()
            .expectStatus()
            .isOk
    }

    private fun getLastIdleForSecondContainer() =
            transactionTemplate.execute {
                entityManager.createNativeQuery("select * from agent_status", AgentStatus::class.java)
                    .resultList
                    .first {
                        (it as AgentStatus).state == AgentState.IDLE && it.agent.containerId == "container-2"
                    }
                    as AgentStatus
            }!!
}
