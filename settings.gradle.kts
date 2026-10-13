pluginManagement {
    includeBuild("gradle/build-logic")
    repositories {
        // ===== 本机构建适配：仓库顺序（勿随意调整）=====
        // 关键约束：Gradle 一旦在某个仓库命中模块元数据（pom/module），构件（jar/aar）
        // 就只从该仓库取，不会回退到其他仓库。所以必须把「构件最全」的源放最前。
        // 腾讯镜像常有 pom 无 jar，故官方源优先；镜像仅作加速备用。
        // 还原为上游原始配置：git checkout settings.gradle.kts
        //
        // 1) 本地补件仓库
        maven(url = "file:///D:/Codex/m2-local")
        // 2) 官方源（构件齐全，避免镜像空 jar）
        google()
        mavenCentral()
        gradlePluginPortal()
        // 3) 国内镜像备用
        maven(url = "https://mirrors.cloud.tencent.com/nexus/repository/maven-public")
        maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
        maven(url = "https://maven.aliyun.com/repository/public")
        maven(url = "https://maven.aliyun.com/repository/google")
        // 注意：上游写的是 https://www.jitpack.io，该域名在本机不可达（会挂死构建）。
        maven(url = "https://jitpack.io")
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("mihonx") {
            from(files("gradle/mihon.versions.toml"))
        }
    }

    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    @Suppress("UnstableApiUsage")
    repositories {
        // 顺序理由同上。还原为上游原始配置：git checkout settings.gradle.kts
        maven(url = "file:///D:/Codex/m2-local")
        google()
        mavenCentral()
        maven(url = "https://mirrors.cloud.tencent.com/nexus/repository/maven-public")
        maven(url = "https://maven.aliyun.com/repository/public")
        maven(url = "https://maven.aliyun.com/repository/google")
        // www.jitpack.io 在本机不可达（挂死），改用 jitpack.io
        maven(url = "https://jitpack.io")
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "Mihon"
include(":app")
include(":baseline-profile")
include(":core-metadata")
include(":core:archive")
include(":core:common")
include(":core:metro")
include(":data")
include(":domain")
include(":i18n")
include(":icons:custom-material-symbols")
include(":icons:material-symbols")
include(":icons:simple-icons")
include(":presentation-core")
include(":presentation-widget")
include(":source-api")
include(":source-local")
include(":telemetry")
