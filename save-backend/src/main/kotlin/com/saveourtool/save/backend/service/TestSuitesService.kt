package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.ProjectRepository
import com.saveourtool.save.backend.repository.TestExecutionRepository
import com.saveourtool.save.backend.repository.TestRepository
import com.saveourtool.save.backend.repository.TestSuiteRepository
import com.saveourtool.save.entities.Project
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.testsuite.TestSuiteType
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Example
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Service for test suites
 */
@Service
class TestSuitesService(
    private val testSuiteRepository: TestSuiteRepository,
    private val testRepository: TestRepository,
    private val testExecutionRepository: TestExecutionRepository,
    private val projectRepository: ProjectRepository,
) {
    /**
     * Save new test suites to DB
     *
     * @param testSuitesDto test suites, that should be checked and possibly saved
     * @return list of *all* TestSuites
     */
    @Suppress("TOO_MANY_LINES_IN_LAMBDA", "UnsafeCallOnNullableType")
    fun saveTestSuite(testSuitesDto: List<TestSuiteDto>): List<TestSuite> {
        val testSuites = testSuitesDto
            .distinctBy {
                // Same suites may be declared in different directories, we unify them here.
                // We allow description of existing test suites to be changed.
                it.copy(description = null)
            }
            .map {
                TestSuite(
                    type = it.type,
                    name = it.name,
                    description = it.description,
                    project = it.project,
                    dateAdded = null,
                    testRootPath = it.testRootPath,
                    testSuiteRepoUrl = it.testSuiteRepoUrl,
                    language = it.language
                )
            }
            .map { testSuite ->
                // try to find TestSuite in the DB based on all non-null properties of `testSuite`
                // NB: that's why `dateAdded` is null in the mapping above
                val description = testSuite.description
                testSuiteRepository
                    .findOne(
                        Example.of(testSuite.apply { this.description = null })
                    )
                    .orElseGet {
                        // if testSuite is not present in the DB, we will save it with current timestamp
                        testSuite.apply {
                            dateAdded = LocalDateTime.now()
                            this.description = description
                        }
                    }
            }
        testSuiteRepository.saveAll(testSuites)
        return testSuites.toList()
    }

    /**
     * @return all standard test suites
     */
    fun getStandardTestSuites() =
            testSuiteRepository.findAllByTypeIs(TestSuiteType.STANDARD).map { it.toDto() }

    /**
     * @param name name of the test suite
     * @return all standard test suites with specific name
     */
    fun findStandardTestSuitesByName(name: String) =
            testSuiteRepository.findAllByNameAndType(name, TestSuiteType.STANDARD)

    /**
     * @param project a project associated with test suites
     * @return a list of test suites
     */
    @Transactional
    fun findTestSuitesByProject(project: Project) =
            testSuiteRepository.findByProjectId(
                requireNotNull(project.id) { "Cannot find test suites for project with missing id (name=${project.name}, organization=${project.organization.name})" }
            )

    /**
     * @param id
     * @return test suite with [id]
     */
    fun findTestSuiteById(id: Long) = testSuiteRepository.findById(id)

    /**
     * Mark provided testSuites as obsolete
     *
     * @param testSuiteDtos
     */
    @Suppress("UnsafeCallOnNullableType")
    fun markObsoleteTestSuites(testSuiteDtos: List<TestSuiteDto>) {
        testSuiteDtos.forEach { testSuiteDto ->
            val testSuite = testSuiteRepository.findByNameAndTypeAndTestRootPathAndTestSuiteRepoUrl(
                testSuiteDto.name,
                testSuiteDto.type!!,
                testSuiteDto.testRootPath,
                testSuiteDto.testSuiteRepoUrl,
            )
            log.info("Mark test suite ${testSuite.name} with id ${testSuite.id} as obsolete")
            testSuite.type = TestSuiteType.OBSOLETE_STANDARD
            testSuiteRepository.save(testSuite)
        }
    }

    /**
     * Delete testSuites and related tests & test executions from DB
     *
     * @param testSuiteDtos suites, which need to be deleted
     */
    @Suppress("UnsafeCallOnNullableType")
    fun deleteTestSuiteDto(testSuiteDtos: List<TestSuiteDto>) {
        testSuiteDtos.forEach { testSuiteDto ->
            // Get test suite id by testSuiteDto
            val testSuiteId = testSuiteRepository.findByNameAndTypeAndTestRootPathAndTestSuiteRepoUrl(
                testSuiteDto.name,
                testSuiteDto.type!!,
                testSuiteDto.testRootPath,
                testSuiteDto.testSuiteRepoUrl,
            ).id!!

            // Get test ids related to the current testSuiteId
            val testIds = testRepository.findAllByTestSuiteId(testSuiteId).map { it.id }
            testIds.forEach { testId ->
                // Executions could be absent
                testExecutionRepository.findByTestId(testId!!).ifPresent { testExecution ->
                    // Delete test executions
                    log.debug("Delete test execution with id ${testExecution.id}")
                    testExecutionRepository.deleteById(testExecution.id!!)
                }
                // Delete tests
                log.debug("Delete test with id $testId")
                testRepository.deleteById(testId)
            }
            log.info("Delete test suite ${testSuiteDto.name} with id $testSuiteId")
            testSuiteRepository.deleteById(testSuiteId)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(TestSuitesService::class.java)
    }
}
