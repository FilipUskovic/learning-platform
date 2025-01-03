plugins {
    java
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.6"
    id("org.hibernate.orm") version "6.6.2.Final"
    /* kasije cu korsiti za sada mi ne treba
    id("org.graalvm.buildtools.native") version "0.10.3"

     */
}

group = "com.micro"
version = "0.0.1-SNAPSHOT"

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

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.flywaydb:flyway-core")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.apache.commons:commons-collections4:4.5.0-M2")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // https://mvnrepository.com/artifact/org.hibernate.orm/hibernate-jcache
    implementation ("org.hibernate.orm:hibernate-jcache:6.6.2.Final")
    implementation ("org.ehcache:ehcache:3.10.8")
    implementation ("org.springframework.boot:spring-boot-starter-cache")
    implementation ("com.github.ben-manes.caffeine:caffeine")
    // Ovisnost za Redis
    implementation ("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-hateoas")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    implementation ("com.google.guava:guava:32.1.2-jre")
    implementation("org.springframework.kafka:spring-kafka")

   // implementation("io.micrometer:micrometer-registry-kafka:1.11.3")

    implementation ("org.zalando:problem-spring-web:0.27.0")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-authorization-server")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation ("io.jsonwebtoken:jjwt-api:0.12.6")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.6")
    implementation("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // za ucitavanje env fileova za gradle
    implementation ("me.paulschwarz:spring-dotenv:3.0.0")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")


    // implementation("org.apache.kafka:kafka-streams")
    testImplementation("org.springframework.kafka:spring-kafka-test")

    // swager 3 tj opeapi
    implementation ("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.0.3")
    testImplementation("org.springframework.security:spring-security-test")


    /* kasnije cu korsiti
    implementation("org.springframework.boot:spring-boot-starter-webflux")

     */
    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    runtimeOnly("org.postgresql:postgresql")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // rate limiter i bucket4j
    implementation ("com.bucket4j:bucket4j-core:8.10.1")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    /* kasnije cu korsiti

    testImplementation("io.projectreactor:reactor-test")

     */
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

hibernate {
    enhancement {
        enableAssociationManagement = true
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
