# Test IT TMS Adapter for Kotest
![Test IT](https://raw.githubusercontent.com/testit-tms/adapters-python/master/images/banner.png)

## Getting Started

### Installation

#### Maven Users

Add this dependency to your project POM:

```xml
<dependency>
    <groupId>ru.testit</groupId>
    <artifactId>testit-adapter-kotest</artifactId>
    <version>0.1.0</version>
    <scope>compile</scope>
</dependency>
```

#### Gradle Users

Add this dependency to your project build file:

```groovy
implementation "ru.testit:testit-adapter-kotest:0.1.0"
```


### Configuration

| Description                                                                                                                                                                                                                                                                                                                                                                            | File property                     | Environment variable                       | System property                      |
|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------|--------------------------------------------|--------------------------------------|
| Location of the TMS instance                                                                                                                                                                                                                                                                                                                                                           | url                               | TMS_URL                                    | tmsUrl                               |
| API secret key [How to getting API secret key?](https://github.com/testit-tms/.github/tree/main/configuration#privatetoken)                                                                                                                                                                                                                                                                                                                                   | privateToken                      | TMS_PRIVATE_TOKEN                          | tmsPrivateToken                      |
| ID of project in TMS instance [How to getting project ID?](https://github.com/testit-tms/.github/tree/main/configuration#projectid)                                                                                                                                                                                                                                                                                                                        | projectId                         | TMS_PROJECT_ID                             | tmsProjectId                         |
| ID of configuration in TMS instance [How to getting configuration ID?](https://github.com/testit-tms/.github/tree/main/configuration#configurationid)                                                                                                                                                                                                                                                                                                            | configurationId                   | TMS_CONFIGURATION_ID                       | tmsConfigurationId                   |
| ID of the created test run in TMS instance.<br/>It's necessary for **adapterMode** 0 or 1                                                                                                                                                                                                                                                                                              | testRunId                         | TMS_TEST_RUN_ID                            | tmsTestRunId                         |
| Parameter for specifying the name of test run in TMS instance (**It's optional**). If it is not provided, it is created automatically                                                                                                                                                                                                                                                  | testRunName                       | TMS_TEST_RUN_NAME                          | tmsTestRunName                       |
| Adapter mode. Default value - 0. The adapter supports following modes:<br/>0 - in this mode, the adapter filters tests by test run ID and configuration ID, and sends the results to the test run<br/>1 - in this mode, the adapter sends all results to the test run without filtering<br/>2 - in this mode, the adapter creates a new test run and sends results to the new test run | adapterMode                       | TMS_ADAPTER_MODE                           | tmsAdapterMode                       |
| It enables/disables certificate validation (**It's optional**). Default value - true                                                                                                                                                                                                                                                                                                   | certValidation                    | TMS_CERT_VALIDATION                        | tmsCertValidation                    |
| It enables/disables TMS integration (**It's optional**). Default value - true                                                                                                                                                                                                                                                                                                          | testIt                            | TMS_TEST_IT                                | tmsTestIt                            |
| Mode of automatic creation test cases (**It's optional**). Default value - false. The adapter supports following modes:<br/>true - in this mode, the adapter will create a test case linked to the created autotest (not to the updated autotest)<br/>false - in this mode, the adapter will not create a test case                                                                    | automaticCreationTestCases        | TMS_AUTOMATIC_CREATION_TEST_CASES          | tmsAutomaticCreationTestCases        |
| Mode of automatic updation links to test cases (**It's optional**). Default value - false. The adapter supports following modes:<br/>true - in this mode, the adapter will update links to test cases<br/>false - in this mode, the adapter will not update link to test cases                                                                                                         | automaticUpdationLinksToTestCases | TMS_AUTOMATIC_UPDATION_LINKS_TO_TEST_CASES | tmsAutomaticUpdationLinksToTestCases |
| Name of the configuration file If it is not provided, it is used default file name (**It's optional**)                                                                                                                                                                                                                                                                                 | -                                 | TMS_CONFIG_FILE                            | tmsConfigFile                        |

#### File

Create **testit.properties** file in the resource directory of the project:
``` 
url=URL
privateToken=USER_PRIVATE_TOKEN
projectId=PROJECT_ID
configurationId=CONFIGURATION_ID
testRunId=TEST_RUN_ID
testRunName=TEST_RUN_NAME
adapterMode=ADAPTER_MODE
automaticCreationTestCases=AUTOMATIC_CREATION_TEST_CASES
automaticUpdationLinksToTestCases=AUTOMATIC_UPDATION_LINKS_TO_TEST_CASES
certValidation=CERT_VALIDATION
testIt=TEST_IT
```


# Contributing

You can help to develop the project. Any contributions are **greatly appreciated**.

* If you have suggestions for adding or removing projects, feel free to [open an issue](https://github.com/testit-tms/adapters-kotlin/issues/new) to discuss it, or create a direct pull request after you edit the *README.md* file with necessary changes.
* Make sure to check your spelling and grammar.
* Create individual PR for each suggestion.
* Read the [Code Of Conduct](https://github.com/testit-tms/adapters-kotlin/blob/main/CODE_OF_CONDUCT.md) before posting your first idea as well.

# License

Distributed under the Apache-2.0 License. See [LICENSE](https://github.com/testit-tms/adapters-kotlin/blob/main/LICENSE.md) for more information.
