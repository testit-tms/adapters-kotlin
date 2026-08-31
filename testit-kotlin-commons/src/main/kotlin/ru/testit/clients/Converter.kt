package ru.testit.clients

import ru.testit.kotlin.adaptersapi.models.*
import ru.testit.kotlin.adaptersapi.models.LinkType
import ru.testit.models.*
import ru.testit.models.Label
import ru.testit.models.LinkItem
import ru.testit.models.StepResult
import ru.testit.models.TestResultCommon
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.Locale
import java.util.Locale.getDefault
import java.util.stream.Collectors

class Converter {

    companion object {

        fun testResultToAutoTestPostModel(result: TestResultCommon, projectId: UUID?): AutoTestCreateApiModel {
            val model = AutoTestCreateApiModel(
                externalId = result.externalId!!,
                externalKey = result.externalKey,
                projectId = projectId ?: UUID.fromString(result.uuid),
                name = result.name!!,
                description = result.description,
                classname = result.className,
                namespace = result.spaceName,
                title = result.title,
                links = convertPostLinks(result.linkItems),
                steps = convertStepsToApiModel(result.getSteps()),
                labels = labelsPostConvert(result.labels),
                tags = result.tags,
                layer = layerToApiModel(result.layer),
                shouldCreateWorkItem = result.automaticCreationTestCases,
            )
            return model
        }
        fun testResultToAutoTestPutModel(result: TestResultCommon): AutoTestUpdateApiModel {
            return testResultToAutoTestPutModel(result, null, null)
        }

        fun testResultToAutoTestPutModel(result: TestResultCommon,
                                         projectId: UUID?,
                                         isFlaky: Boolean?): AutoTestUpdateApiModel {
            val model = AutoTestUpdateApiModel(
                externalId = result.externalId!!,
                externalKey = result.externalKey,
                projectId = projectId ?: UUID.fromString(result.uuid),
                description = result.description,
                name = result.name!!,
                classname = result.className,
                namespace = result.spaceName,
                title = result.title,
                links = convertPutLinks(result.linkItems),
                steps = convertStepsToApiModel(result.getSteps()),
                labels = labelsPostConvert(result.labels),
                tags = result.tags,
                setup = ArrayList(),
                teardown = ArrayList(),
                isFlaky = isFlaky,
                resetLayer = false,
                layer = layerToApiModel(result.layer),
            )
            return model
        }


        fun testResultToTestResultUpdateModel(result: TestResultResponse,
                                              setupResults: List<AutoTestStepResultUpdateRequest>?,
                                              teardownResults: List<AutoTestStepResultUpdateRequest>?
        ): TestResultUpdateRequest {
            val model = TestResultUpdateRequest(
                duration = result.durationInMs,
                statusCode = result.status!!.code,
                links = convertLinksFromResult(result.links),
                stepResults = result.stepResults,
                failureClassIds = result.failureClassIds,
                comment = result.comment,
                attachments = if (result.attachments != null)
                    convertAttachmentsFromResult(result.attachments!!) else null,
                setupResults = setupResults,
                teardownResults = teardownResults
            )
            return model
        }

        fun testResultCommonToTestResultUpdateModel(
            testResult: TestResultCommon,
            existing: TestResultResponse,
            setupResults: List<AutoTestStepResultUpdateRequest>?,
            teardownResults: List<AutoTestStepResultUpdateRequest>?,
        ): TestResultUpdateRequest {
            val throwable = testResult.throwable
            val duration = if (testResult.start != null && testResult.stop != null) {
                testResult.stop!! - testResult.start!!
            } else {
                existing.durationInMs
            }
            return TestResultUpdateRequest(
                failureClassIds = existing.failureClassIds,
                statusCode = testResult.itemStatus?.value,
                statusType = mapStatusType(testResult.itemStatus?.value ?: ""),
                links = convertLinksFromResult(existing.links),
                stepResults = existing.stepResults,
                attachments = when {
                    testResult.attachments.isNotEmpty() ->
                        convertAttachments(testResult.attachments)!!.map { AttachmentUpdateRequest(id = it.id) }
                    existing.attachments != null -> convertAttachmentsFromResult(existing.attachments!!)
                    else -> null
                },
                durationInMs = duration,
                duration = duration,
                setupResults = setupResults,
                teardownResults = teardownResults,
                message = throwable?.message ?: testResult.message ?: existing.message,
                trace = throwable?.stackTraceToString() ?: existing.traces,
            )
        }

        fun convertFixture(fixtures: List<FixtureResult>, parentUuid: String?): MutableList<AutoTestStepApiResult> {
            return fixtures.stream()
                .filter { filterSteps(parentUuid, it) }
                .map { fixture ->
                    val model = AutoTestStepApiResult(
                        fixture.name!!,
                        fixture.description,
                        convertSteps(fixture.getSteps())
                    )
                    model
                }
                .collect(Collectors.toList())
        }

        private fun filterSteps(parentUuid: String?, f: FixtureResult): Boolean {
            if (f == null) return false

            return parentUuid != null && Objects.equals(f.parent, parentUuid)
        }

        fun autoTestModelToAutoTestUpdateApiModel(autoTestModel: AutoTestApiResult): AutoTestUpdateApiModel {
            return autoTestModelToAutoTestUpdateApiModel(autoTestModel, null, null, null, null, null, null)
        }


        fun autoTestModelToAutoTestUpdateApiModel(autoTestModel: AutoTestApiResult,
                                                  setup:  List<AutoTestStepApiResult>?,
                                                  teardown:  List<AutoTestStepApiResult>?,
                                                  isFlaky: Boolean?,
                                                  layer: String? = null): AutoTestUpdateApiModel {
            return autoTestModelToAutoTestUpdateApiModel(autoTestModel, null, isFlaky, setup, teardown, null, layer)
        }


        fun autoTestModelToAutoTestUpdateApiModel(autoTestModel: AutoTestApiResult,
                                                  links: List<LinkUpdateApiModel>?,
                                                  isFlaky: Boolean?,
                                                  externalKey: String?,
                                                  layer: String? = null): AutoTestUpdateApiModel {
            return autoTestModelToAutoTestUpdateApiModel(autoTestModel, links, isFlaky, null, null, externalKey, layer)
        }

        fun autoTestModelToAutoTestUpdateApiModel(
            autoTestModel: AutoTestApiResult,
            links: List<LinkUpdateApiModel>?,
            isFlaky: Boolean?,
            setup: List<AutoTestStepApiResult>?,
            teardown: List<AutoTestStepApiResult>?,
            externalKey: String?,
            layer: String? = null,
        ): AutoTestUpdateApiModel {
            val model = AutoTestUpdateApiModel(
                id = autoTestModel.id,
                externalId = autoTestModel.externalId!!,
                externalKey = externalKey ?: autoTestModel.externalKey,
                links = links ?: autoTestModel.links.toUpdateApiModels(),
                projectId = autoTestModel.projectId,
                name = autoTestModel.name,
                namespace = autoTestModel.namespace,
                classname = autoTestModel.classname,
                steps = autoTestModel.steps.toApiModels(),
                setup = setup.toApiModels() ?: autoTestModel.setup.toApiModels(),
                teardown = teardown.toApiModels() ?: autoTestModel.teardown.toApiModels(),
                title = autoTestModel.title,
                description = autoTestModel.description,
                labels = labelsConvert(autoTestModel.labels ?: emptyList()),
                tags = autoTestModel.tags,
                isFlaky = isFlaky,
                resetLayer = false,
                layer = layerToApiModel(layer),
            )
            return model
        }

        fun testResultToAutoTestResultsForTestRunModel(
            result: TestResultCommon, configurationId: UUID?,
        ): AutoTestResultsForTestRunModel {
            return testResultToAutoTestResultsForTestRunModel(
                result, configurationId, null, null)
        }

        // PASSED("Passed"),
        // FAILED("Failed"),
        // SKIPPED("Skipped"),
        // INPROGRESS("InProgress"),
        // BLOCKED("Blocked");
        fun mapStatusType(status: String): TestStatusType {
            when (status) {
                "Passed" -> return TestStatusType.Succeeded
                "Failed" -> return TestStatusType.Failed
                "InProgress" -> return TestStatusType.InProgress
                "Skipped" -> return TestStatusType.Incomplete
                "Blocked" -> return TestStatusType.Incomplete
            }
            return TestStatusType.Incomplete
        }

        fun testResultToAutoTestResultsForTestRunModel(result: TestResultCommon,
                                                       configurationId: UUID?,
                                                       setupResults: List<AttachmentPutModelAutoTestStepResultsModel>?,
                                                       teardownResults: List<AttachmentPutModelAutoTestStepResultsModel>?
        ): AutoTestResultsForTestRunModel {
            val throwable = result.throwable
            val model = AutoTestResultsForTestRunModel(
                configurationId = configurationId ?: UUID.fromString(result.uuid),
                autoTestExternalId = result.externalId!!,
                statusType = mapStatusType(result.itemStatus!!.value),
                links = convertPostLinksToPostModel(result.resultLinks),
                startedOn = dateToOffsetDateTime(result.start!!),
                completedOn = dateToOffsetDateTime(result.stop!!),
                duration = result.stop!! - result.start!!,
                stepResults = convertResultStep(result.getSteps()),
                attachments = convertAttachments(result.attachments),
                parameters = result.parameters,
                message = if (throwable != null) throwable.message else result.message,
                traces = throwable?.stackTraceToString(),
                setupResults = setupResults,
                teardownResults = teardownResults
            )

            return model
        }

        fun convertPostLinks(links: List<LinkItem>): List<LinkCreateApiModel> =
            links.map {
                val model = LinkCreateApiModel(
                    url = it.url,
                    title = it.title,
                    description = it.description,
                    type = LinkType.valueOf(it.type.value)
                )
                model
            }

        fun convertPostLinksToPostModel(links: List<LinkItem>): List<LinkPostModel> =
            links.map {
                val model = LinkPostModel(
                    url = it.url,
                    hasInfo = false,
                    title = it.title,
                    description = it.description,
                    type = LinkType.valueOf(it.type.value)
                )
                model
            }

        fun convertPutLinks(links: List<LinkItem>): List<LinkUpdateApiModel> =
            links.map {
                val model = LinkUpdateApiModel(
                    url = it.url,
                    title = it.title,
                    description = it.description,
                    type = LinkType.valueOf(it.type.value)
                )
                model
            }

        fun convertSteps(steps: List<StepResult>): List<AutoTestStepApiResult> =
            steps.map {
                val model = AutoTestStepApiResult(
                    title = it.name!!,
                    description = it.description,
                    steps = convertSteps(it.getSteps())
                )
                model
            }

        fun convertStepsToApiModel(steps: List<StepResult>): List<AutoTestStepApiModel> =
            steps.map {
                val model = AutoTestStepApiModel(
                    title = it.name!!,
                    description = it.description,
                    steps = convertStepsToApiModel(it.getSteps())
                )
                model
            }

        fun convertResultStep(steps: List<StepResult>): List<AttachmentPutModelAutoTestStepResultsModel> =
            steps.map {
                val model = AttachmentPutModelAutoTestStepResultsModel(
                    title = it.name,
                    description = it.description,
                    startedOn = dateToOffsetDateTime(it.start!!),
                    completedOn = dateToOffsetDateTime(it.stop!!),
                    duration = it.stop!! - it.start!!,
                    outcome = AvailableTestResultOutcome.valueOf(it.itemStatus!!.value),
                    stepResults = convertResultStep(it.getSteps()),
                    attachments = convertAttachments(it.getAttachments()),
                    parameters = it.parameters
                )
                model
            }

        fun convertResultFixture(fixtures: List<FixtureResult>, parentUuid: String?):
                List<AttachmentPutModelAutoTestStepResultsModel> {
            return fixtures.stream()
                .filter { filterSteps(parentUuid, it) }
                .map { fixture ->
                    val model = AttachmentPutModelAutoTestStepResultsModel(
                        title = fixture.name,
                        description = fixture.description,
                        startedOn = dateToOffsetDateTime(fixture.start!!),
                        completedOn = dateToOffsetDateTime(fixture.stop!!),
                        duration = fixture.stop!! - fixture.start!!,
                        outcome = AvailableTestResultOutcome.valueOf(fixture.itemStatus!!.value),
                        stepResults = convertResultStep(fixture.getSteps()),
                        attachments = convertAttachments(fixture.getAttachments()),
                        parameters = fixture.parameters
                    )
                    model
                }
                .collect(Collectors.toList())
        }

        private fun labelsConvert(labels: List<LabelApiResult>): List<LabelApiModel> =
            labels.map { LabelApiModel(name = it.name) }

        private fun labelsPostConvert(labels: List<Label>): List<LabelApiModel> =
            labels.map { LabelApiModel(name = it.name!!) }

        private fun layerToApiModel(layer: String?): LayerApiModel? {
            if (layer.isNullOrBlank()) {
                return null
            }
            return LayerApiModel(name = layer, source = LayerSource.Run)
        }

        private fun dateToOffsetDateTime(time: Long): OffsetDateTime {
            val date = Date(time)
            return date.toInstant().atOffset(ZoneOffset.UTC)
        }

        private fun convertAttachments(uuids: List<String>): List<AttachmentPutModel>? =
            uuids.map { AttachmentPutModel(id = UUID.fromString(it)) }

        private fun convertAttachmentsFromResult(models: List<AttachmentApiResult>): List<AttachmentUpdateRequest> =
            models.map { AttachmentUpdateRequest(id = it.id) }

        private fun convertLinksFromResult(links: List<LinkApiResult>?): List<CreateLinkApiModel>? {
            if (links.isNullOrEmpty()) {
                return null
            }
            return links.map { link ->
                CreateLinkApiModel(
                    url = link.url,
                    type = link.type,
                    title = link.title,
                    description = link.description,
                )
            }
        }

        @JvmName("autoTestStepApiResultToStepApiModels")
        private fun List<AutoTestStepApiResult>?.toApiModels(): List<AutoTestStepApiModel>? {
            if (this == null) {
                return ArrayList()
            }

            return this.stream().map { step: AutoTestStepApiResult ->
                val model = AutoTestStepApiModel(
                    title = step.title,
                    description = step.description,
                    steps = step.steps.toApiModels(),
                )
                model
            }.collect(Collectors.toList())
        }


        @JvmName("linkApiResultToUpdateApiModels")
        private fun List<LinkApiResult>?.toUpdateApiModels(): List<LinkUpdateApiModel> {
            if (this == null) {
                return ArrayList()
            }

            return this.stream().map { link: LinkApiResult ->
                val model = LinkUpdateApiModel(
                    url = link.url,
                    id = link.id,
                    title = link.title,
                    description = link.description,
                    type = link.type
                        ?: LinkType.valueOf(ru.testit.models.LinkType.RELATED.value)
                )
                model
            }.collect(Collectors.toList())
        }


        @JvmName("labelApiResultToModels")
        private fun List<LabelApiResult>?.toPutModels(): List<LabelShortModel> {
            if (this == null) {
                return ArrayList()
            }

            return this.stream().map { label: LabelApiResult ->
                val model = LabelShortModel(
                    name = label.name,
                    globalId = label.globalId
                )
                model
            }.collect(Collectors.toList())
        }

        public fun TestRunApiResult.toModel(name: String): UpdateEmptyTestRunApiModel {
            return UpdateEmptyTestRunApiModel(
                id = this.id,
                name = name,
                description = null,
                launchSource = null,
                attachments = this.attachments.stream().map { attachment: AttachmentApiResult ->
                    AssignAttachmentApiModel(id = attachment.id)
                }.collect(Collectors.toList()),
                links = this.links.stream().map { link: LinkApiResult ->
                    UpdateLinkApiModel(
                        id = link.id,
                        url = link.url,
                        title = link.title,
                        description = link.description,
                        type = link.type,
                    )
                }.collect(Collectors.toList()),
                tags = this.tags,
            )
        }
    }
}