package ru.testit.writers

import ru.testit.kotlin.client.models.*
import ru.testit.kotlin.client.models.LinkType
import ru.testit.models.*
import ru.testit.models.Label
import ru.testit.models.StepResult
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.stream.Collectors


class Converter {

    companion object {

        fun testResultToAutoTestPostModel(result: TestResultCommon, projectId: UUID?): AutoTestPostModel {
            val model = AutoTestPostModel(
                externalId = result.externalId!!,
                projectId = projectId ?: UUID.fromString(result.uuid),
                name = result.name!!,
                description = result.description,
                classname = result.className,
                namespace = result.spaceName,
                title = result.title,
                links = convertPostLinks(result.linkItems),
                steps = convertSteps(result.getSteps()),
                labels = labelsPostConvert(result.labels),
                shouldCreateWorkItem = result.automaticCreationTestCases,
            )
            return model
        }
        fun testResultToAutoTestPutModel(result: TestResultCommon): AutoTestPutModel {
            return testResultToAutoTestPutModel(result, null, null)
        }

        fun testResultToAutoTestPutModel(result: TestResultCommon,
                                         projectId: UUID?,
                                         isFlaky: Boolean?): AutoTestPutModel {
            val model = AutoTestPutModel(
                externalId = result.externalId!!,
                projectId = projectId ?: UUID.fromString(result.uuid),
                description = result.description,
                name = result.name!!,
                classname = result.className,
                namespace = result.spaceName,
                title = result.title,
                links = convertPutLinks(result.linkItems),
                steps = convertSteps(result.getSteps()),
                labels = labelsPostConvert(result.labels),
                setup = ArrayList(),
                teardown = ArrayList(),
                isFlaky = isFlaky

            )
            return model
        }


        fun testResultToTestResultUpdateModel(result: TestResultResponse,
                                              setupResults: List<AttachmentPutModelAutoTestStepResultsModel>?,
                                              teardownResults: List<AttachmentPutModelAutoTestStepResultsModel>?
        ): TestResultUpdateV2Request  {
            val model = TestResultUpdateV2Request (
                duration = result.durationInMs,
                outcome = result.outcome,
                links = result.links,
                stepResults = result.stepResults,
                failureClassIds = result.failureClassIds,
                comment = result.comment,
                attachments = if (result.attachments != null)
                    convertAttachmentsFromModel(result.attachments!!) else null,
                setupResults = setupResults,
                teardownResults = teardownResults
            )
            return model
        }

        fun convertFixture(fixtures: List<FixtureResult>, parentUuid: String?): MutableList<AutoTestStepModel> {
            return fixtures.stream()
                .filter { filterSteps(parentUuid, it) }
                .map { fixture ->
                    val model = AutoTestStepModel(
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

        fun autoTestModelToAutoTestPutModel(autoTestModel: AutoTestModel): AutoTestPutModel {
            return autoTestModelToAutoTestPutModel(autoTestModel, null, null, null, null)
        }


        fun autoTestModelToAutoTestPutModel(autoTestModel: AutoTestModel,
                                            setup:  List<AutoTestStepModel>?,
                                            teardown:  List<AutoTestStepModel>?,
                                            isFlaky: Boolean?): AutoTestPutModel {
            return autoTestModelToAutoTestPutModel(autoTestModel, null, isFlaky, setup, teardown)
        }


        fun autoTestModelToAutoTestPutModel(autoTestModel: AutoTestModel,
                                            links: List<LinkPutModel>?,
                                            isFlaky: Boolean?): AutoTestPutModel {
            return autoTestModelToAutoTestPutModel(autoTestModel, links, isFlaky, null, null)
        }

        fun autoTestModelToAutoTestPutModel(
            autoTestModel: AutoTestModel,
            links: List<LinkPutModel>?,
            isFlaky: Boolean?,
            setup: List<AutoTestStepModel>?,
            teardown: List<AutoTestStepModel>?,
        ): AutoTestPutModel {
            val model = AutoTestPutModel(
                id = autoTestModel.id,
                externalId = autoTestModel.externalId,
                links = links ?: autoTestModel.links,
                projectId = autoTestModel.projectId,
                name = autoTestModel.name,
                namespace = autoTestModel.namespace,
                classname = autoTestModel.classname,
                steps = autoTestModel.steps,
                setup = setup ?: autoTestModel.setup,
                teardown = teardown ?: autoTestModel.teardown,
                title = autoTestModel.title,
                description = autoTestModel.description,
                labels = labelsConvert(autoTestModel.labels!!),
                isFlaky = isFlaky,

            )
            return model
        }

        fun testResultToAutoTestResultsForTestRunModel(
            result: TestResultCommon, configurationId: UUID?,
        ): AutoTestResultsForTestRunModel {
            return testResultToAutoTestResultsForTestRunModel(
                result, configurationId, null, null)
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
                outcome = AvailableTestResultOutcome.valueOf(result.itemStatus?.value!!),
                links = convertPostLinks(result.resultLinks),
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

        fun convertPostLinks(links: List<LinkItem>): List<LinkPostModel> =
            links.map {
                val model = LinkPostModel(
                    url = it.url,
                    // TODO: check about hasInfo ?
                    hasInfo = true,
                    title = it.title,
                    description = it.description,
                    type = LinkType.valueOf(it.type.value)
                )
                model
            }

        fun convertPutLinks(links: List<LinkItem>): List<LinkPutModel> =
            links.map {
                val model = LinkPutModel(
                    url = it.url,
                    hasInfo = true,
                    title = it.title,
                    description = it.description,
                    type = LinkType.valueOf(it.type.value)
                )
                model
            }

        fun convertSteps(steps: List<StepResult>): List<AutoTestStepModel> =
            steps.map {
                val model = AutoTestStepModel(
                    title = it.name!!,
                    description = it.description,
                    steps = convertSteps(it.getSteps())
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

        fun labelsConvert(labels: List<LabelShortModel>): List<LabelPostModel> =
            labels.map { LabelPostModel(name = it.name) }

        fun labelsPostConvert(labels: List<Label>): List<LabelPostModel> =
            labels.map { LabelPostModel(name = it.name!!) }

        private fun dateToOffsetDateTime(time: Long): OffsetDateTime {
            val date = Date(time)
            return date.toInstant().atOffset(ZoneOffset.UTC)
        }

        fun convertAttachments(uuids: List<String>): List<AttachmentPutModel>? =
            uuids.map { AttachmentPutModel(id = UUID.fromString(it)) }

        fun convertAttachmentsFromModel(models: List<Attachment>): List<AttachmentUpdateRequest> =
            models.map { AttachmentUpdateRequest(id = it.id) }

        fun convertAutoTestApiResultToAutoTestModel(autoTestApiResult: AutoTestApiResult?): AutoTestModel? {
            if (autoTestApiResult?.externalId == null) {
                return null;
            }

            val model = AutoTestModel(
                id = autoTestApiResult.id,
                externalId = autoTestApiResult.externalId!!,
                links = convertLinkApiResultsToPutLinks(autoTestApiResult.links),
                projectId = autoTestApiResult.projectId,
                name = autoTestApiResult.name,
                namespace = autoTestApiResult.namespace,
                classname = autoTestApiResult.classname,
                steps = convertAutoTestStepApiResultsToSteps(autoTestApiResult.steps),
                setup = convertAutoTestStepApiResultsToSteps(autoTestApiResult.setup),
                teardown = convertAutoTestStepApiResultsToSteps(autoTestApiResult.teardown),
                title = autoTestApiResult.title,
                description = autoTestApiResult.description,
                labels = convertLabelApiResultsToLabelShortModels(autoTestApiResult.labels),
                externalKey = autoTestApiResult.externalKey,
                globalId = autoTestApiResult.globalId,
                isDeleted = autoTestApiResult.isDeleted,
                mustBeApproved = autoTestApiResult.mustBeApproved,
                createdDate = autoTestApiResult.createdDate,
                createdById = autoTestApiResult.createdById,
            );

            return model;
        }

        private fun convertAutoTestStepApiResultsToSteps(steps: List<AutoTestStepApiResult>?): List<AutoTestStepModel> {
            if (steps == null) {
                return ArrayList()
            }

            return steps.stream().map { step: AutoTestStepApiResult ->
                val model = AutoTestStepModel(
                    title = step.title,
                    description = step.description,
                    steps = convertAutoTestStepApiResultsToSteps(step.steps),
                )
                model
            }.collect(Collectors.toList())
        }

        private fun convertLinkApiResultsToPutLinks(links: List<LinkApiResult>?): List<LinkPutModel> {
            if (links == null) {
                return ArrayList()
            }

            return links.stream().map { link: LinkApiResult ->
                val model = LinkPutModel(
                    url = link.url,
                    hasInfo = true,
                    title = link.title,
                    description = link.description,
                    type = link.type?.let { LinkType.valueOf(it.value) }
                )
                model
            }.collect(Collectors.toList())
        }

        private fun convertLabelApiResultsToLabelShortModels(labels: List<LabelApiResult>?): List<LabelShortModel> {
            if (labels == null) {
                return ArrayList()
            }

            return labels.stream().map { label: LabelApiResult ->
                val model = LabelShortModel(
                    name = label.name,
                    globalId = label.globalId
                )
                model
            }.collect(Collectors.toList())
        }
    }
}