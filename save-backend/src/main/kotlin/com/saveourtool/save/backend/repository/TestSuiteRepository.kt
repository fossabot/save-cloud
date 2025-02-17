package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.testsuite.TestSuiteType
import org.springframework.data.repository.query.QueryByExampleExecutor
import org.springframework.stereotype.Repository

/**
 * JPA repositories for TestSuite
 */
@Repository
interface TestSuiteRepository : BaseEntityRepository<TestSuite>, QueryByExampleExecutor<TestSuite> {
    /**
     * @param testSuiteType
     * @return list of test suites by type
     */
    fun findAllByTypeIs(testSuiteType: TestSuiteType): List<TestSuite>

    /**
     * @param testSuiteName name of the test suite
     * @param testSuiteType type of the test suite
     * @return list of test suites by name and type
     */
    fun findAllByNameAndType(testSuiteName: String, testSuiteType: TestSuiteType): List<TestSuite>

    /**
     * @param projectId id of the project associated with test suites
     * @return a list of test suites
     */
    fun findByProjectId(projectId: Long): List<TestSuite>

    /**
     * @param name name of the test suite
     * @param type type of the test suite
     * @param testRootPath properties relative path of the test suite
     * @param testSuiteRepoUrl test suite repo url of the test suite
     * @return matched test suite
     */
    fun findByNameAndTypeAndTestRootPathAndTestSuiteRepoUrl(
        name: String,
        type: TestSuiteType,
        testRootPath: String,
        testSuiteRepoUrl: String?,
    ): TestSuite
}
