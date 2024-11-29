package ru.testit

import ru.testit.kotlin.client.models.*
import ru.testit.models.*
import ru.testit.models.Label
import ru.testit.models.LinkType
import ru.testit.models.StepResult
import java.time.OffsetDateTime
import java.util.*

class Helper {
    companion object {
        const val EXTERNAL_ID: String = "5819479d"
        const val TITLE: String = "Test title"
        const val DESCRIPTION: String = "Test description"
        const val NAME: String = "Test name"
        const val CLASS_NAME: String = "ClassName"
        const val SPACE_NAME: String = "SpaceName"
        const val WORK_ITEM_ID: String = "6523"
        val ITEM_STATUS: ItemStatus = ItemStatus.PASSED
        const val TEST_UUID: String = "99d77db9-8d68-4835-9e17-3a6333f01251"

        const val LINK_TITLE: String = "Link title"
        const val LINK_DESCRIPTION: String = "Link description"
        val LINK_TYPE: LinkType = LinkType.ISSUE
        const val LINK_URL: String = "https://example.test/"

        const val STEP_TITLE: String = "Step title"
        const val STEP_DESCRIPTION: String = "Step description"

        const val LABEL_NAME: String = "Label name"

        const val CLASS_UUID: String = "179f193b-2519-4ae9-a364-173c3d8fa6cd"

        const val BEFORE_EACH_NAME: String = "Before Each name"
        const val BEFORE_EACH_DESCRIPTION: String = "Before Each description"

        const val AFTER_EACH_NAME: String = "After Each name"
        const val AFTER_EACH_DESCRIPTION: String = "After Each description"

        const val BEFORE_ALL_NAME: String = "Before All name"
        const val BEFORE_ALL_DESCRIPTION: String = "Before All description"

        const val AFTER_ALL_NAME: String = "After All name"
        const val AFTER_ALL_DESCRIPTION: String = "After All description"

        fun generateListUuid(): List<UUID> {
            return listOf(UUID.randomUUID())
        }

        fun generateTestResult(): TestResultCommon {
            val startDate = Date()
            val stopDate = Date(startDate.time + 1000)

            val links = mutableListOf(generateLinkItem())

            val steps = mutableListOf(generateStepResult())

            val labels_ = mutableListOf(Label(LABEL_NAME))

            return TestResultCommon().setSteps(steps)
                .apply {
                externalId = EXTERNAL_ID
                uuid = TEST_UUID
                title = TITLE
                description = DESCRIPTION
                className = CLASS_NAME
                name = NAME
                spaceName = SPACE_NAME
                start = startDate.time
                stop = stopDate.time
                workItemIds = mutableListOf(WORK_ITEM_ID)
                itemStatus = ITEM_STATUS
                linkItems = links
                labels = labels_
            }

        }

        fun generateLinkItem(): LinkItem {
            return LinkItem(
                title = LINK_TITLE,
                description = LINK_DESCRIPTION,
                type = LINK_TYPE,
                url = LINK_URL
            )
        }

        fun generateTestResultModel(): TestResultModel {
            val model = TestResultModel(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                OffsetDateTime.now(),
                UUID.randomUUID(),
                listOf(),
                durationInMs = 12345L
            )
//            model.durationInMs = 12345L
            return model
        }

        fun generateAutoTestModel(projectId: String): AutoTestModel {
            val model = AutoTestModel(
                globalId = 12345L,
                isDeleted = false,
                mustBeApproved = false,
                id = UUID.fromString(TEST_UUID),
                createdDate = OffsetDateTime.now(),
                createdById = UUID.randomUUID(),
                externalId = EXTERNAL_ID,
                projectId = UUID.fromString(projectId),
                name = NAME,
                links = generatePutLinks(),
                steps = generateSteps(),
                setup = emptyList(),
                teardown = emptyList(),
                title = TITLE,
                description = DESCRIPTION,
                labels = generateShortLabels(),
            )
            return model
        }

        fun generateAutoTestPutModel(projectId: String): AutoTestPutModel {
            val model = AutoTestPutModel(
                externalId = EXTERNAL_ID,
                projectId = UUID.fromString(projectId),
                name = NAME,
                title = TITLE,
                description = DESCRIPTION,
                classname = CLASS_NAME,
                namespace = SPACE_NAME,
                steps = generateSteps(),
                links = generatePutLinks(),
                labels = generatePostLabels(),
                setup = emptyList(),
                teardown = emptyList(),
                id = UUID.fromString(TEST_UUID),
                isFlaky = null
            )
            return model
        }

        fun generateAutoTestPostModel(projectId: String): AutoTestPostModel {
            val model = AutoTestPostModel(
                externalId = EXTERNAL_ID,
                projectId = UUID.fromString(projectId),
                name = NAME,
                title = TITLE,
                description = DESCRIPTION,
                classname = CLASS_NAME,
                namespace = SPACE_NAME,
                steps = generateSteps(),
                links = generatePostLinks(),
                labels = generatePostLabels(),
                shouldCreateWorkItem = false
            )

            return model
        }

        fun generateClassContainer(): ClassContainer {
            val container = ClassContainer().apply {
                uuid = CLASS_UUID
                children.add(TEST_UUID)
                beforeEachTest.add(generateBeforeEachFixtureResult())
                afterEachTest.add(generateAfterEachFixtureResult())
            }

            return container
        }

        fun generateMainContainer(): MainContainer {
            val container = MainContainer().apply {
                children.add(CLASS_UUID)
                beforeMethods.add(generateBeforeAllFixtureResult())
                afterMethods.add(generateAfterAllFixtureResult())
            }

            return container
        }

        fun generateStepResult(): StepResult {
            val startDate = Date()
            val stopDate = Date(startDate.time + 500)

            val result = StepResult()
                .setSteps(mutableListOf())
                .apply {
                name = STEP_TITLE
                description = STEP_DESCRIPTION
                start = startDate.time
                stop = stopDate.time
                itemStatus = ItemStatus.PASSED
            }
            return result
        }

        fun generateBeforeEachFixtureResult(): FixtureResult {
            val startDate = Date()
            val stopDate = Date(startDate.time + 100)

            val fixtureResult = FixtureResult().apply {
                name = BEFORE_EACH_NAME
                parent = TEST_UUID
                itemStatus = ItemStatus.PASSED
                description = BEFORE_EACH_DESCRIPTION
                start = startDate.time
                stop = stopDate.time
            }

            return fixtureResult
        }

        fun generateAfterEachFixtureResult(): FixtureResult {
            val startDate = Date()
            val stopDate = Date(startDate.time + 100)

            return FixtureResult().apply {
                name = AFTER_EACH_NAME
                parent = TEST_UUID
                itemStatus = ItemStatus.PASSED
                description = AFTER_EACH_DESCRIPTION
                start = startDate.time
                stop = stopDate.time
            }
        }

        fun generateBeforeEachSetup(): AutoTestStepModel {
            return AutoTestStepModel(
                title = BEFORE_EACH_NAME,
                description = BEFORE_EACH_DESCRIPTION,
                steps = ArrayList()
            )
        }

        fun generateAfterEachSetup(): AutoTestStepModel {
            return AutoTestStepModel(
                title = AFTER_EACH_NAME,
                description = AFTER_EACH_DESCRIPTION,
                steps = ArrayList()
            )
        }

        fun generateBeforeAllFixtureResult(): FixtureResult {
            val startDate = Date()
            val stopDate = Date(startDate.time + 100)

            return FixtureResult().apply {
                name = BEFORE_ALL_NAME
                parent = TEST_UUID.toString()
                itemStatus = ItemStatus.PASSED
                description = BEFORE_ALL_DESCRIPTION
                start = startDate.time
                stop = stopDate.time
            }
        }

        fun generateAfterAllFixtureResult(): FixtureResult {
            val startDate = Date()
            val stopDate = Date(startDate.time + 100)

            return FixtureResult().apply {
                name = AFTER_ALL_NAME
                parent = TEST_UUID.toString()
                itemStatus = ItemStatus.PASSED
                description = AFTER_ALL_DESCRIPTION
                start = startDate.time
                stop = stopDate.time
            }
        }

        fun generateBeforeAllSetup(): AutoTestStepModel {
            return AutoTestStepModel(
                title = BEFORE_ALL_NAME,
                description = BEFORE_ALL_DESCRIPTION,
                steps = ArrayList()
            )
        }

        fun generateAfterAllSetup(): AutoTestStepModel {
            return AutoTestStepModel(
                title = AFTER_ALL_NAME,
                description = AFTER_ALL_DESCRIPTION,
                steps = ArrayList()
            )
        }

        private fun generateShortLabels(): List<LabelShortModel> {
            val labels = mutableListOf<LabelShortModel>()
            val label = LabelShortModel(
                globalId = 12345L,
                name = LABEL_NAME
            )
            labels.add(label)

            return labels
        }

        private fun generatePostLabels(): List<LabelPostModel> {
            val labels = mutableListOf<LabelPostModel>()

            val label = LabelPostModel(
                name = LABEL_NAME
            )
            labels.add(label)

            return labels
        }

        private fun generatePutLinks(): List<LinkPutModel> {
            val links = mutableListOf<LinkPutModel>()

            val link = LinkPutModel(
                title = LINK_TITLE,
                hasInfo = true,
                description = LINK_DESCRIPTION,
                url = LINK_URL,
                type = ru.testit.kotlin.client.models.LinkType.valueOf(LINK_TYPE.value)
            )

            links.add(link)

            return links
        }

        private fun generatePostLinks(): List<LinkPostModel> {
            val links = mutableListOf<LinkPostModel>()

            val link = LinkPostModel(
                title = LINK_TITLE,
                hasInfo = true,
                description = LINK_DESCRIPTION,
                url = LINK_URL,
                type = ru.testit.kotlin.client.models.LinkType.valueOf(LINK_TYPE.value)
            )

            links.add(link)

            return links
        }

        private fun generateSteps(): List<AutoTestStepModel> {
            val steps = mutableListOf<AutoTestStepModel>()

            val step = AutoTestStepModel(
                title = STEP_TITLE,
                description = STEP_DESCRIPTION,
                steps = ArrayList()
            )

            steps.add(step)
            return steps
        }

    }
}