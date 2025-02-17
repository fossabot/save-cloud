package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.AwesomeBenchmarksRepository
import com.saveourtool.save.entities.benchmarks.AwesomeBenchmarks
import com.saveourtool.save.entities.benchmarks.BenchmarkEntity
import com.saveourtool.save.entities.benchmarks.toEntity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * @property awesomeBenchmarksRepository
 */
@Service
class AwesomeBenchmarksService(val awesomeBenchmarksRepository: AwesomeBenchmarksRepository) {
    private val log = LoggerFactory.getLogger(AwesomeBenchmarksService::class.java)

    /**
     * @param benchmarks
     */
    @Transactional
    internal fun saveBenchmarksToDb(benchmarks: List<BenchmarkEntity>) {
        log.debug("Saving Awesome Benchmarks to 'awesome_benchmarks' table in DB: $benchmarks")
        // as we plan to override everything, we can simple delete all records in the table
        awesomeBenchmarksRepository.deleteAll()
        // flush is always needed after the deletion
        awesomeBenchmarksRepository.flush()
        awesomeBenchmarksRepository.saveAll(benchmarks.map { it.toEntity() })
    }

    /**
     * @return list with al benchmarks from the DB
     */
    internal fun getAllBenchmarks(): List<AwesomeBenchmarks> = awesomeBenchmarksRepository.findAll()
}
