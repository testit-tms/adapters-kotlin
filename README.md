
# adapters-kotlin

### how to test locally with api-kotlin-client?

1. you should assemble testit-api-kotlin-client:

``
java --version // make sure that you're pointed on jdk 11
``

 ``
 .\gradlew assemble
 ``

2. copy `build\libs\testit-api-kotlin-client-1.0.0.jar` to `vendor` folder of `adapters-kotlin`

3. make sure that path is correct in `build.gradle.kts`

