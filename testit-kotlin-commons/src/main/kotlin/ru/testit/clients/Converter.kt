package ru.testit.clients

import ru.testit.kotlin.client.models.*
import ru.testit.kotlin.client.models.LinkType
import ru.testit.models.*
import ru.testit.models.Label
import ru.testit.models.LinkItem
import ru.testit.models.StepResult
import ru.testit.models.TestResultCommon
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
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
                setup = ArrayList(),
                teardown = ArrayList(),
                isFlaky = isFlaky

            )
            return model
        }


        fun testResultToTestResultUpdateModel(result: TestResultResponse,
                                              setupResults: List<AutoTestStepResultUpdateRequest>?,
                                              teardownResults: List<AutoTestStepResultUpdateRequest>?
        ): TestResultUpdateV2Request {
            val model = TestResultUpdateV2Request(
                duration = result.durationInMs,
                statusCode = result.status!!.code,
                links = result.links,
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

        fun autoTestModelToAutoTestUpdateApiModel(autoTestModel: AutoTestModel): AutoTestUpdateApiModel {
            return autoTestModelToAutoTestUpdateApiModel(autoTestModel, null, null, null, null)
        }


        fun autoTestModelToAutoTestUpdateApiModel(autoTestModel: AutoTestModel,
                                                  setup:  List<AutoTestStepModel>?,
                                                  teardown:  List<AutoTestStepModel>?,
                                                  isFlaky: Boolean?): AutoTestUpdateApiModel {
            return autoTestModelToAutoTestUpdateApiModel(autoTestModel, null, isFlaky, setup, teardown)
        }


        fun autoTestModelToAutoTestUpdateApiModel(autoTestModel: AutoTestModel,
                                                  links: List<LinkUpdateApiModel>?,
                                                  isFlaky: Boolean?): AutoTestUpdateApiModel {
            return autoTestModelToAutoTestUpdateApiModel(autoTestModel, links, isFlaky, null, null)
        }

        fun autoTestModelToAutoTestUpdateApiModel(
            autoTestModel: AutoTestModel,
            links: List<LinkUpdateApiModel>?,
            isFlaky: Boolean?,
            setup: List<AutoTestStepModel>?,
            teardown: List<AutoTestStepModel>?,
        ): AutoTestUpdateApiModel {
            val model = AutoTestUpdateApiModel(
                id = autoTestModel.id,
                externalId = autoTestModel.externalId,
                externalKey = autoTestModel.externalKey,
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
                statusCode = result.itemStatus?.value!!,
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
                    hasInfo = false,
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
                    hasInfo = false,
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

        private fun labelsConvert(labels: List<LabelShortModel>): List<LabelApiModel> =
            labels.map { LabelApiModel(name = it.name) }

        private fun labelsPostConvert(labels: List<Label>): List<LabelApiModel> =
            labels.map { LabelApiModel(name = it.name!!) }

        private fun dateToOffsetDateTime(time: Long): OffsetDateTime {
            val date = Date(time)
            return date.toInstant().atOffset(ZoneOffset.UTC)
        }

        private fun convertAttachments(uuids: List<String>): List<AttachmentPutModel>? =
            uuids.map { AttachmentPutModel(id = UUID.fromString(it)) }

        private fun convertAttachmentsFromResult(models: List<AttachmentApiResult>): List<AttachmentUpdateRequest> =
            models.map { AttachmentUpdateRequest(id = it.id) }

        fun AutoTestApiResult?.toModel(): AutoTestModel? {
            if (this?.externalId == null) {
                return null;
            }

            val model = AutoTestModel(
                id = this.id,
                externalId = this.externalId!!,
                links = this.links.toPutModels(),
                projectId = this.projectId,
                name = this.name,
                namespace = this.namespace,
                classname = this.classname,
                steps = this.steps.toStepModels(),
                setup = this.setup.toStepModels(),
                teardown = this.teardown.toStepModels(),
                title = this.title,
                description = this.description,
                labels = this.labels.toPutModels(),
                externalKey = this.externalKey,
                globalId = this.globalId,
                isDeleted = this.isDeleted,
                mustBeApproved = this.mustBeApproved,
                createdDate = this.createdDate,
                createdById = this.createdById,
                lastTestResultStatus = if (this.lastTestResultStatus != null)
                    this.lastTestResultStatus!!.toModel() else null
            )

            return model;
        }

        private fun TestStatusApiResult.toModel(): TestStatusModel {
            return TestStatusModel(
                id = this.id,
                name = this.name,
                type = this.type.toModel(),
                isSystem = this.isSystem,
                code = this.code,
                description = this.description
            )
        }

        private fun TestStatusApiType.toModel(): TestStatusType {
            return TestStatusType.valueOf(this.value)
        }

        @JvmName("autoTestStepApiResultToModels")
        private fun List<AutoTestStepApiResult>?.toStepModels(): List<AutoTestStepModel>? {
            if (this == null) {
                return ArrayList()
            }

            return this.stream().map { step: AutoTestStepApiResult ->
                val model = AutoTestStepModel(
                    title = step.title,
                    description = step.description,
                    steps = step.steps.toStepModels(),
                )
                model
            }.collect(Collectors.toList())
        }

        @JvmName("autoTestStepModelsToStepApiModels")
        private fun List<AutoTestStepModel>?.toApiModels(): List<AutoTestStepApiModel>? {
            if (this == null) {
                return ArrayList()
            }

            return this.stream().map { step: AutoTestStepModel ->
                val model = AutoTestStepApiModel(
                    title = step.title,
                    description = step.description,
                    steps = step.steps.toApiModels(),
                )
                model
            }.collect(Collectors.toList())
        }


        @JvmName("linkApiResultToUpdateModels")
        private fun List<LinkApiResult>?.toUpdateModels(): List<LinkUpdateApiModel> {
            if (this == null) {
                return ArrayList()
            }

            return this.stream().map { link: LinkApiResult ->
                val model = LinkUpdateApiModel(
                    url = link.url,
                    hasInfo = false,
                    title = link.title,
                    description = link.description,
                    type = link.type?.let { LinkType.valueOf(it.value) }
                )
                model
            }.collect(Collectors.toList())
        }

        @JvmName("linkApiResultToPutModels")
        private fun List<LinkApiResult>?.toPutModels(): List<LinkPutModel> {
            if (this == null) {
                return ArrayList()
            }

            return this.stream().map { link: LinkApiResult ->
                val model = LinkPutModel(
                    url = link.url,
                    hasInfo = false,
                    title = link.title,
                    description = link.description,
                    type = link.type?.let { LinkType.valueOf(it.value) }
                )
                model
            }.collect(Collectors.toList())
        }

        @JvmName("linkPutModelToUpdateApiModels")
        private fun List<LinkPutModel>?.toUpdateApiModels(): List<LinkUpdateApiModel> {
            if (this == null) {
                return ArrayList()
            }

            return this.stream().map { link: LinkPutModel ->
                val model = LinkUpdateApiModel(
                    url = link.url,
                    hasInfo = false,
                    title = link.title,
                    description = link.description,
                    type = link.type?.let { LinkType.valueOf(it.value) }
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

        public fun TestRunV2ApiResult.toModel(name: String): UpdateEmptyTestRunApiModel {
            return UpdateEmptyTestRunApiModel(
                id = this.id,
                name = name,
                description = this.description,
                launchSource = this.launchSource,
                attachments = this.attachments.stream().map { attachment: AttachmentApiResult ->
                    val model = AssignAttachmentApiModel(id = attachment.id)
                    model
                }.collect(Collectors.toList()),
                links = this.links.stream().map { link: LinkApiResult ->
                    val model = UpdateLinkApiModel(
                        id = link.id,
                        url = link.url,
                        title = link.title,
                        description = link.description,
                        hasInfo = link.hasInfo,
                    )
                    model
                }.collect(Collectors.toList()),
            )
        }
    }
}