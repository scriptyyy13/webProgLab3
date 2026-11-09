plugins {
    kotlin("jvm") version "1.9.22"
    war
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")
    compileOnly("jakarta.servlet.jsp:jakarta.servlet.jsp-api:3.1.1")
}

// собираем в ROOT.war
tasks.war {
    archiveFileName.set("ROOT.war")
    from("src/main/webapp")
}

kotlin {
    jvmToolchain(17)
}

// таска запуска вайлдфлая локально
tasks.register<Exec>("run") {
    group = "application"
    description = "собирает war и запускает локальный wildfly"
    dependsOn("war")

    val wildflyDir = file("${layout.buildDirectory.asFile.get()}/wildfly-27.0.1.Final")

    doFirst {
        // копируем собранный war в deployments перед стартом
        copy {
            from(tasks.war.get().archiveFile)
            into(file("$wildflyDir/standalone/deployments"))
        }
    }

    workingDir = file("$wildflyDir/bin")

    commandLine("cmd", "/c", "standalone.bat", "-b", "0.0.0.0")
}