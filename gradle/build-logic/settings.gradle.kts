dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        // ===== 本机构建适配：仓库顺序（勿随意调整）=====
        // Gradle 命中元数据的仓库即锁定构件来源，不会回退，故把最全的源放最前。
        // 还原：git checkout gradle/build-logic/settings.gradle.kts
        maven(url = "file:///D:/Codex/m2-local")
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://mirrors.cloud.tencent.com/nexus/repository/maven-public")
        maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
        maven(url = "https://maven.aliyun.com/repository/public")
        maven(url = "https://maven.aliyun.com/repository/google")
    }
    versionCatalogs {
        create("libs") {
            from(files("../libs.versions.toml"))
        }
        create("mihonx") {
            from(files("../mihon.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
