import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    // Основные плагины Spring Boot
    id("org.springframework.boot") version "3.4.5" // Используем последнюю версию
    id("io.spring.dependency-management") version "1.1.6" // Управление версиями зависимостей

    // Стандартный Java плагин (нужен для компиляции)
    java
    // Плагин для отчетов покрытия кода
    jacoco
}

group = "org.example" // Рекомендуется задать группу
version = "1.0.0-SNAPSHOT" // Стандартный формат для разрабатываемых версий

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Зависимости
    implementation("org.springframework.boot:spring-boot-starter-web") // Веб (включает MVC, Tomcat, Jackson)
    implementation("org.springframework.boot:spring-boot-starter-actuator") // Эндпоинты управления (health, info, shutdown)
    implementation("org.springframework.boot:spring-boot-starter-validation") // Для валидации конфигурации и DTO

    // Зависимости для тестирования
    testImplementation("org.springframework.boot:spring-boot-starter-test") // Основной стартер тестов Spring Boot
    // Оставляем явные версии JUnit/Mockito/AssertJ для надежности, хотя starter-test их тоже включает
    testImplementation(platform("org.junit:junit-bom:5.12.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.mockito:mockito-core:5.16.1")
    testImplementation("org.mockito:mockito-junit-jupiter:5.16.1")

    // System Lambda для перехвата вывода в тестах
    testImplementation("com.github.stefanbirkner:system-lambda:1.2.1")

    testImplementation("org.awaitility:awaitility:4.2.2")

    // --- УДАЛЕНЫ ---
    // implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3") // Управляется Spring Boot
    // implementation("org.apache.logging.log4j:log4j-api:2.24.3") // Используем SLF4j+Logback из starter-logging
    // implementation("org.apache.logging.log4j:log4j-core:2.24.3") // Используем SLF4j+Logback из starter-logging
    // implementation("com.sparkjava:spark-core:2.9.4") // Заменяется на Spring Web MVC
}

// Настройка Java
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// Настройка тестирования
tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport) // Генерировать отчет после тестов

    // Аргументы JVM для SystemLambda (оставляем как было)
    jvmArgs(
        "--add-opens", "java.base/java.util=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang=ALL-UNNAMED"
    )
}

// Настройка Jacoco
tasks.withType<JacocoReport> {
    dependsOn(tasks.test) // Отчет зависит от выполнения тестов
    reports {
        xml.required = true  // Для интеграции с CI/CD
        html.required = true // Для просмотра человеком
    }
}

jacoco {
    toolVersion = "0.8.12" // Версия инструмента Jacoco
}

// Настройка сборки исполняемого JAR (Spring Boot плагин делает это автоматически)
// Конфигурация task bootJar предоставляется плагином
tasks.bootJar {
    archiveFileName.set("${project.name}-${project.version}.jar") // Имя конечного JAR файла
    mainClass.set("org.example.pizzeria.PizzeriaApplication") // Указываем основной класс Spring Boot
}

// Задача build теперь автоматически будет зависеть от bootJar