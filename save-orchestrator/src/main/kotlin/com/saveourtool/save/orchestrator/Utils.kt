/**
 * Utilities for orchestrator
 */

package com.saveourtool.save.orchestrator

import com.saveourtool.save.orchestrator.config.AgentSettings

import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.AsyncDockerCmd
import com.github.dockerjava.api.command.SyncDockerCmd
import generated.SAVE_CORE_VERSION
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.apache.commons.io.FileUtils
import org.springframework.core.io.ClassPathResource

import java.io.File
import java.util.function.Supplier

internal const val DOCKER_METRIC_PREFIX = "save.orchestrator.docker"

internal const val SAVE_CLI_EXECUTABLE_NAME = "save-$SAVE_CORE_VERSION-linuxX64.kexe"

/**
 * Execute this async docker command while recording its execution duration.
 *
 * @param meterRegistry a registry to record data to
 * @param name name of the timer
 * @param tags additional tags for the timer (command name in form of Java class name is assigned automatically)
 * @param resultCallbackProducer a function returning result callback. Should call its argument to record duration.
 * @return async result
 */
inline fun <reified CMD_T : AsyncDockerCmd<CMD_T, A_RES_T>, RC_T : ResultCallback<A_RES_T>, A_RES_T> CMD_T.execTimed(
    meterRegistry: MeterRegistry,
    name: String,
    vararg tags: String,
    resultCallbackProducer: (() -> Unit) -> RC_T,
): RC_T {
    val timer = meterRegistry.timer(name, "cmd", "${CMD_T::class.simpleName}", *tags)
    val sample = Timer.start(meterRegistry)
    return exec(resultCallbackProducer {
        sample.stop(timer)
    })
}

/**
 * Execute this sync docker command while recording its execution duration.
 *
 * @param meterRegistry a registry to record data to
 * @param name name of the timer
 * @param tags additional tags for the timer (command name in form of Java class name is assigned automatically)
 * @return sync result
 */
inline fun <reified CMD_T : SyncDockerCmd<RES_T>, RES_T> CMD_T.execTimed(
    meterRegistry: MeterRegistry,
    name: String,
    vararg tags: String,
): RES_T? {
    val timer = meterRegistry.timer(name, "cmd", "${CMD_T::class.simpleName}", *tags)
    return timer.record(Supplier {
        exec()
    })
}

/**
 * Create synthetic toml config for standard mode in aim to execute all suites at the same time
 *
 * @param execCmd execCmd for SAVE-cli for testing in standard mode
 * @param batchSizeForAnalyzer batchSize for SAVE-cli for testing in standard mode
 * @return synthetic toml config data
 */
// FixMe: Use serialization after ktoml upgrades
fun createSyntheticTomlConfig(execCmd: String?, batchSizeForAnalyzer: String?): String {
    val exeCmdForTomlConfig = if (execCmd.isNullOrBlank()) "" else "execCmd = \"$execCmd\""
    val batchSizeForTomlConfig = if (batchSizeForAnalyzer.isNullOrBlank()) {
        ""
    } else {
        """
        |[fix]
        |    batchSize = $batchSizeForAnalyzer
        |[warn]
        |    batchSize = $batchSizeForAnalyzer
        """.trimMargin()
    }
    return """
           |[general]
           |$exeCmdForTomlConfig
           |$batchSizeForTomlConfig
           """.trimMargin()
}

/**
 * Load default agent.properties from classpath, get properties values using configuration and store into [agentPropertiesFile].
 *
 * @param agentPropertiesFile target file
 * @param agentSettings configuration of save-agent loaded from save-orchestrator
 * @param saveCliExecFlags flags for save-cli
 */
internal fun fillAgentPropertiesFromConfiguration(
    agentPropertiesFile: File,
    agentSettings: AgentSettings,
    saveCliExecFlags: String
) {
    FileUtils.copyInputStreamToFile(
        ClassPathResource("agent.properties").inputStream,
        agentPropertiesFile
    )

    val cliCommand = "./$SAVE_CLI_EXECUTABLE_NAME$saveCliExecFlags"
    agentPropertiesFile.writeText(
        agentPropertiesFile.readLines().joinToString(System.lineSeparator()) { line ->
            when {
                line.startsWith("id=") -> "id=\${${agentSettings.agentIdEnv}}"
                line.startsWith("cliCommand=") -> "cliCommand=$cliCommand"
                line.startsWith("backend.url=") && agentSettings.backendUrl != null ->
                    "backend.url=${agentSettings.backendUrl}"
                line.startsWith("orchestratorUrl=") && agentSettings.orchestratorUrl != null ->
                    "orchestratorUrl=${agentSettings.orchestratorUrl}"
                else -> line
            }
        }
    )
}
