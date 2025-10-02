val mockitoAgent = configurations.create("mockitoAgent")

plugins {
    java
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "7.4.0"
}

group = "com.julianw03"
version = "0.0.2"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

val feignVersion = "11.6"

dependencies {
    implementation(project(":rcls-riot-api-client"))
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")
    implementation("org.openapitools:jackson-databind-nullable:0.2.7")

    implementation("org.msgpack:jackson-dataformat-msgpack:0.9.9")
    implementation("io.github.openfeign:feign-core:${feignVersion}")
    implementation("io.github.openfeign:feign-jackson:${feignVersion}")
    implementation("com.fasterxml.jackson.core:jackson-core:2.19.0")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-crypto")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-quartz")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.mockito:mockito-junit-jupiter")
    mockitoAgent("org.mockito:mockito-core") { isTransitive = false }
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

openApiGenerate {
    inputSpec.set("${projectDir}/src/main/resources/swagger/riot-client-openapi.json")
    outputDir.set("${projectDir}/../rcls-riot-api-client")
    generatorName.set("java")
    library.set("feign")
    modelPackage.set("com.julianw03.rcls.generated.model")
    apiPackage.set("com.julianw03.rcls.generated.api")
    invokerPackage.set("com.julianw03.rcls.generated")
    generateModelTests.set(false)
    generateApiTests.set(false)
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "test")
}

tasks.bootRun {
    systemProperty("spring.profiles.active", "dev")
}

tasks.jar {
    enabled = false;
}

tasks.bootJar {
    this.archiveFileName.set("RCLS-v${version}.${archiveExtension.get()}");
}

tasks.test {
    jvmArgs("-javaagent:${mockitoAgent.asPath}")
}