/**
 * Utility methods for creation of the module window for the selection of test resources
 */

@file:Suppress("WildcardImport", "FILE_WILDCARD_IMPORTS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.frontend.components.views.ProjectView
import com.saveourtool.save.frontend.externals.fontawesome.faQuestionCircle
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.testsuite.TestSuiteDto

import org.w3c.dom.events.Event
import react.PropsWithChildren
import react.RBuilder
import react.dom.*
import react.fc

import kotlinx.html.InputType
import kotlinx.html.classes
import kotlinx.html.js.onChangeFunction

/**
 * Types of testing (that can be selected by user)
 */
enum class TestingType {
    CONTEST_MODE,
    CUSTOM_TESTS,
    STANDARD_BENCHMARKS,
    ;
}

/**
 * Properties for test resources
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface TestResourcesProps : PropsWithChildren {
    var testingType: TestingType
    var isSubmitButtonPressed: Boolean?
    var gitDto: GitDto?

    // properties for CUSTOM_TESTS mode
    var gitUrlFromInputField: String
    var gitBranchOrCommitFromInputField: String
    var execCmd: String
    var batchSizeForAnalyzer: String
    var testRootPath: String

    // properties for STANDARD_BENCHMARKS mode
    var standardTestSuites: List<TestSuiteDto>
    var selectedStandardSuites: MutableList<String>
    var selectedLanguageForStandardTests: String?
}

@Suppress("LongParameterList", "TOO_MANY_PARAMETERS")
private fun RBuilder.setAdditionalPropertiesForStandardMode(
    value: String,
    placeholder: String,
    tooltipText: String,
    labelText: String,
    inputType: InputType,
    onChangeFunc: (Event) -> Unit
) = div("input-group mb-3") {
    if (labelText.isNotEmpty()) {
        div("input-group-prepend") {
            label("input-group-text") {
                +labelText
            }
        }
    }

    input(type = inputType, name = "itemText") {
        // workaround to have a default value for Batch field
        if (labelText.isNotEmpty()) {
            attrs.defaultValue = "1"
        }
        key = "itemText"
        attrs["min"] = 1
        attrs["class"] = "form-control"
        if (tooltipText.isNotBlank()) {
            attrs["data-toggle"] = "tooltip"
            attrs["data-placement"] = "right"
            attrs["title"] = tooltipText
        }
        attrs {
            this.value = value
            this.placeholder = placeholder
            onChangeFunction = {
                onChangeFunc(it)
            }
        }
    }
}

/**
 * @param updateGitUrlFromInputField
 * @param updateGitBranchOrCommitInputField
 * @param updateTestRootPath
 * @param setSelectedLanguageForStandardTests
 * @param setExecCmd
 * @param setBatchSize
 * @return an RComponent
 */
@Suppress(
    "LongMethod",
    "TOO_LONG_FUNCTION",
    "TOO_MANY_PARAMETERS",
    "LongParameterList",
)
fun testResourcesSelection(
    updateGitUrlFromInputField: (Event) -> Unit,
    updateGitBranchOrCommitInputField: (Event) -> Unit,
    updateTestRootPath: (Event) -> Unit,
    setExecCmd: (Event) -> Unit,
    setBatchSize: (Event) -> Unit,
    setSelectedLanguageForStandardTests: (String) -> Unit,
) =
        fc<TestResourcesProps> { props ->
            if (props.testingType == TestingType.CONTEST_MODE) {
                label(classes = "control-label col-auto justify-content-between justify-content-center font-weight-bold text-danger mb-4 pl-0") {
                    +"Stay tuned! Contests will be here soon"
                }
            } else {
                label(classes = "control-label col-auto justify-content-between font-weight-bold text-gray-800 mb-4 pl-0") {
                    +"3. Specify test-resources that will be used for testing:"
                }
            }

            div {
                attrs.classes = cardStyleByTestingType(props, TestingType.CUSTOM_TESTS)

                div("card-body ") {
                    div("input-group-sm mb-3") {
                        div("row") {
                            sup("tooltip-and-popover") {
                                fontAwesomeIcon(icon = faQuestionCircle)
                                attrs["tooltip-placement"] = "top"
                                attrs["tooltip-title"] = ""
                                attrs["popover-placement"] = "left"
                                attrs["popover-title"] =
                                        "Use the following link to read more about save format:"
                                attrs["popover-content"] =
                                        "<a href =\"https://github.com/saveourtool/save-cli/blob/main/README.md\" > SAVE core README </a>"
                                attrs["data-trigger"] = "focus"
                                attrs["tabindex"] = "0"
                            }
                            h6(classes = "d-inline ml-2") {
                                +"Git Url of your test suites (in save format):"
                            }
                        }
                        div("input-group-prepend") {
                            input(type = InputType.text) {
                                attrs["class"] =
                                        if (props.gitUrlFromInputField.isBlank() && props.isSubmitButtonPressed!!) {
                                            "form-control is-invalid"
                                        } else {
                                            "form-control"
                                        }
                                attrs {
                                    if (props.gitUrlFromInputField.isNotBlank()) {
                                        defaultValue = props.gitUrlFromInputField
                                    }

                                    placeholder = "https://github.com/my-project"
                                    onChangeFunction = {
                                        updateGitUrlFromInputField(it)
                                    }
                                }
                            }
                        }
                    }

                    div("input-group-sm") {
                        div("row") {
                            sup("tooltip-and-popover") {
                                fontAwesomeIcon(icon = faQuestionCircle)
                                attrs["tooltip-placement"] = "top"
                                attrs["tooltip-title"] = ""
                                attrs["popover-placement"] = "left"
                                attrs["popover-title"] = "Keep in mind the following rules:"
                                attrs["popover-content"] = "Provide full name of your brach with `origin` prefix: origin/your_branch." +
                                        " Or in aim to use the concrete commit just provide hash of it."
                                attrs["data-trigger"] = "focus"
                                attrs["tabindex"] = "0"
                            }
                            h6(classes = "d-inline ml-2") {
                                +"Git branch or specific commit in your repository:"
                            }
                        }
                        div("input-group-prepend") {
                            input(type = InputType.text, name = "itemText") {
                                key = "itemText"
                                attrs["class"] = "form-control"

                                attrs {
                                    if (props.gitBranchOrCommitFromInputField.isNotBlank()) {
                                        defaultValue = props.gitBranchOrCommitFromInputField
                                    }
                                    placeholder = "leave empty if you would like to use default branch with latest commit"
                                    onChangeFunction = {
                                        updateGitBranchOrCommitInputField(it)
                                    }
                                }
                            }
                        }
                    }

                    div("input-group-sm mt-3") {
                        div("row") {
                            sup("tooltip-and-popover") {
                                fontAwesomeIcon(icon = faQuestionCircle)
                                attrs["tooltip-placement"] = "top"
                                attrs["tooltip-title"] = ""
                                attrs["popover-placement"] = "left"
                                attrs["popover-title"] = "Relative path to the root directory with tests"
                                attrs["popover-content"] = ProjectView.TEST_ROOT_DIR_HINT
                                attrs["data-trigger"] = "focus"
                                attrs["tabindex"] = "0"
                            }
                            h6(classes = "d-inline ml-2") {
                                +"Relative path (to the root directory) of the test suites in the repo:"
                            }
                        }
                        div("input-group-prepend") {
                            input(type = InputType.text, name = "itemText") {
                                key = "itemText"
                                attrs["class"] = "form-control"
                                attrs {
                                    value = props.testRootPath
                                    placeholder = "leave empty if tests are in the repository root"
                                    onChangeFunction = {
                                        updateTestRootPath(it)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            div {
                attrs.classes = cardStyleByTestingType(props, TestingType.STANDARD_BENCHMARKS)
                div("card-body") {
                    child(
                        suitesTable(
                            props.standardTestSuites,
                            props.selectedLanguageForStandardTests,
                            setSelectedLanguageForStandardTests
                        )
                    ) {}

                    setAdditionalPropertiesForStandardMode(
                        props.execCmd,
                        "Execution command",
                        "Execution command that will be used to run the tool and tests",
                        "",
                        InputType.text,
                        setExecCmd
                    )
                    val toolTipTextForBatchSize = "Batch size controls how many files will be processed at the same time." +
                            " To know more about batch size, please visit: https://github.com/saveourtool/save."
                    setAdditionalPropertiesForStandardMode(
                        props.batchSizeForAnalyzer,
                        "",
                        toolTipTextForBatchSize,
                        "Batch size (default: 1):",
                        InputType.number,
                        setBatchSize
                    )

                    child(checkBoxGrid(props.standardTestSuites, props.selectedLanguageForStandardTests)) {
                        attrs.selectedStandardSuites = props.selectedStandardSuites
                        attrs.rowSize = ProjectView.TEST_SUITE_ROW
                    }
                }
            }
        }

private fun cardStyleByTestingType(props: TestResourcesProps, testingType: TestingType) =
        if (props.testingType == testingType) setOf("card", "shadow", "mb-4", "w-100") else setOf("d-none")
