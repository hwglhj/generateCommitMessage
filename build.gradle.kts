plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.8.21"
    id("org.jetbrains.intellij") version "1.13.3"
}

group = "com.fs"
version = "1.0.1"

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
}
