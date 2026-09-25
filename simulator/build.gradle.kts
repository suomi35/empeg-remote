plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

// The simulator is a plain JVM program: no Android, no external dependencies
// (JDK built-in HTTP server + datagram sockets only).
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

application {
    mainClass.set("empegsim.EmpegSimulatorKt")
}

tasks.named<JavaExec>("run") {
    // Point at the captured fixtures by default; override with:
    // ./gradlew :simulator:run --args="--fixtures=/path/to/fixtures"
    args(listOf("--fixtures=${rootProject.file("fixtures/ghostwheel").absolutePath}"))
}
