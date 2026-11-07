import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.8.21"
    id("org.jetbrains.intellij") version "1.13.3"
}

group = "com.fs"
version = "1.0.2"

repositories {
    // 优先使用国内镜像,直连 Maven Central 在部分网络环境下无法访问
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}

dependencies {
    // 测试用的 JUnit4 由 gradle-intellij-plugin 提供(IDEA 发行版自带 lib/junit4.jar),无需外部依赖
}

intellij {
    version.set("2022.2.5")
    type.set("IC")
    plugins.set(listOf(
        "com.intellij.java",
        "org.jetbrains.kotlin",
        "org.jetbrains.plugins.terminal"
    ))
}

tasks {
    test {
        val tempDirectory = layout.projectDirectory.dir("tmp/tests").asFile
        systemProperty("java.io.tmpdir", tempDirectory.absolutePath)
        doFirst { tempDirectory.mkdirs() }
    }
    initializeIntelliJPlugin {
        selfUpdateCheck.set(false)
    }
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("222")
        untilBuild.set("")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    runPluginVerifier {
        verifierVersion.set("1.411")
        ideVersions.set(listOf("IU-2026.1.5", "IU-2026.2", "IU-2026.2.4", "IU-2026.3"))
        failureLevel.set(listOf(
            org.jetbrains.intellij.tasks.RunPluginVerifierTask.FailureLevel.COMPATIBILITY_PROBLEMS,
            org.jetbrains.intellij.tasks.RunPluginVerifierTask.FailureLevel.INVALID_PLUGIN,
            org.jetbrains.intellij.tasks.RunPluginVerifierTask.FailureLevel.MISSING_DEPENDENCIES,
        ))
    }
}

// IDEA needs a repository descriptor as well as the installable ZIP to identify an update source.
tasks.register("generateUpdatePluginsXml") {
    group = "intellij"
    description = "Build the plugin and its GitHub Releases custom repository descriptor."
    dependsOn(tasks.buildPlugin)

    val descriptor = tasks.patchPluginXml.flatMap { it.destinationDir.file("plugin.xml") }
    val archive = tasks.buildPlugin.flatMap { it.archiveFile }
    val repositoryFile = layout.buildDirectory.file("distributions/updatePlugins.xml")
    inputs.file(descriptor)
    inputs.file(archive)
    outputs.file(repositoryFile)

    doLast {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val plugin = builder.parse(descriptor.get().asFile).documentElement
        val pluginId = plugin.getElementsByTagName("id").item(0).textContent.trim()
        val pluginVersion = plugin.getElementsByTagName("version").item(0).textContent.trim()
        val repository = builder.newDocument()
        val plugins = repository.createElement("plugins")
        repository.appendChild(plugins)
        val entry = repository.createElement("plugin")
        entry.setAttribute("id", pluginId)
        entry.setAttribute("version", pluginVersion)
        entry.setAttribute("url", "https://github.com/hwglhj/generateCommitMessage/releases/download/" +
            "v$pluginVersion/${archive.get().asFile.name}")
        entry.appendChild(repository.importNode(plugin.getElementsByTagName("idea-version").item(0), true))
        plugins.appendChild(entry)

        TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            setOutputProperty(OutputKeys.INDENT, "yes")
        }.transform(DOMSource(repository), StreamResult(repositoryFile.get().asFile))
    }
}
