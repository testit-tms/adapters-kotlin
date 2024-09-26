package ru.testit.writers

import ru.testit.kotlin.client.models.*
import ru.testit.kotlin.client.models.LinkType
import ru.testit.models.*
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.stream.Collectors


class Converter {

    companion object {

        fun testResultToAutoTestPostModel(result: TestResult): AutoTestPostModel {
            val model = AutoTestPostModel(
                result.externalId!!,
                UUID.fromString(result.uuid),
                result.name!!
            )

            model.description = result.description
            model.classname = result.className
            model.namespace = result.spaceName
            model.title = result.title
            model.links = convertPostLinks(result.linkItems)
            model.steps = convertSteps(result.getSteps())
            model.labels = labelsPostConvert(result.labels)
            model.shouldCreateWorkItem = result.automaticCreationTestCases

            return model
        }

        fun testResultToAutoTestPutModel(result: TestResult): AutoTestPutModel {
            val model = AutoTestPutModel(
                externalId = result.externalId!!,
                projectId = UUID.fromString(result.uuid),
                description = result.description,
                name = result.name!!,
                classname = result.className,
                namespace = result.spaceName,
                title = result.title,
                links = convertPutLinks(result.linkItems),
                steps = convertSteps(result.getSteps()),
                labels = labelsPostConvert(result.labels),
                setup = ArrayList(),
                teardown = ArrayList()
            )
            return model
        }


        fun testResultToTestResultUpdateModel(result: TestResultModel): TestResultUpdateModel {
            val model = TestResultUpdateModel(
                duration = result.durationInMs,
                outcome = result.outcome,
                links = result.links,
                stepResults = result.stepResults,
                failureClassIds = result.failureClassIds,
                comment = result.comment,
                attachments = if (result.attachments != null)
                    convertAttachmentsFromModel(result.attachments!!) else null
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
            val model = AutoTestPutModel(
                id = autoTestModel.id,
                externalId = autoTestModel.externalId,
                links = autoTestModel.links,
                projectId = autoTestModel.projectId,
                name = autoTestModel.name,
                namespace = autoTestModel.namespace,
                classname = autoTestModel.classname,
                steps = autoTestModel.steps,
                setup = autoTestModel.setup,
                teardown = autoTestModel.teardown,
                title = autoTestModel.title,
                description = autoTestModel.description,
                labels = labelsConvert(autoTestModel.labels!!),
            )
            return model
        }

        fun testResultToAutoTestResultsForTestRunModel(result: TestResult): AutoTestResultsForTestRunModel {
            val model = AutoTestResultsForTestRunModel(
                configurationId = UUID.fromString(result.uuid),
                autoTestExternalId = result.externalId!!,
                outcome = AvailableTestResultOutcome.valueOf(result.itemStatus?.value!!),
                links = convertPostLinks(result.resultLinks),
                startedOn = dateToOffsetDateTime(result.start!!),
                completedOn = dateToOffsetDateTime(result.stop!!),
                duration = result.stop!! - result.start!!,
                stepResults = convertResultStep(result.getSteps()),
                attachments = convertAttachments(result.getAttachments()),
                message = result.message,
                parameters = result.parameters
            )
//        TODO: with throwable
//        val throwable = result.throwable
//        if (throwable != null) {
//            model.message = throwable.message
//            model.traces = ExceptionUtils.getStackTrace(throwable)
//        }
            return model
        }

        fun convertPostLinks(links: List<LinkItem>): List<LinkPostModel> =
            links.map {
                val model = LinkPostModel(
                    it.url!!,
                    // TODO: check about hasInfo
                    true
                )

                model.title = it.title
                model.description = it.description
                model.type = LinkType.valueOf(it.type!!.value)
                model
            }

        fun convertPutLinks(links: List<LinkItem>): List<LinkPutModel> =
            links.map {
                val model = LinkPutModel(
                    it.url!!,
                    true
                )

                model.title = it.title
                model.description = it.description
                model.type = LinkType.valueOf(it.type!!.value)

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
            labels.map { LabelPostModel(name = it.getName()!!) }

        private fun dateToOffsetDateTime(time: Long): OffsetDateTime {
            val date = Date(time)
            return date.toInstant().atOffset(ZoneOffset.UTC)
        }

        fun convertAttachments(uuids: List<String>): List<AttachmentPutModel> =
            uuids.map { AttachmentPutModel(id = UUID.fromString(it)) }

        fun convertAttachmentsFromModel(models: List<AttachmentModel>): List<AttachmentPutModel> =
            models.map { AttachmentPutModel(id = it.id) }


    }
}