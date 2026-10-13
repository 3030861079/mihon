pluginManagement {
    includeBuild("gradle/build-logic")
    repositories {
        // ===== 本机构建适配：仓库顺序（勿随意调整）=====
        // 关键约束：Gradle 一旦在某个仓库命中模块元数据（pom/module），构件（jar/aar）
        // 就只从该仓库取，不会回退到其他仓库。所以必须把「构件最全」的源放最前。
        // 还原为上游原始配置：git checkout settings.gradle.kts
        //
        // 1) 本地补件仓库：镜像始终拿不到的构件已手工补齐（见 D:\Codex\robust_dl.py）
        maven(url = "file:///D:/Codex/m2-local")
        // 2) 腾讯 nexus 全量代理：实测覆盖面最广、速度 3.5 MB/s
        maven(url = "https://mirrors.cloud.tencent.com/nexus/repository/maven-public")
        // 3) 阿里云插件门户镜像：补插件门户专属构件
        maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
        // 4) Google Maven 官方：AndroidX / AGP 最齐全，2.1 MB/s
        google()
        // 5) 阿里云备用
        maven(url = "https://maven.aliyun.com/repository/public")
        maven(url = "https://maven.aliyun.com/repository/google")
        // ===== 以下是上游原始配置，保留作为兜底 =====
        mavenCentral()
        gradlePluginPortal()
        // 注意：上游写的是 https://www.jitpack.io，该域名在本机不可达（会挂死构建）。
        // jitpack.io（不带 www）正常，故此处改用不带 www 的地址。
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
        maven(url = "https://mirrors.cloud.tencent.com/nexus/repository/maven-public")
        google()
        maven(url = "https://maven.aliyun.com/repository/public")
        maven(url = "https://maven.aliyun.com/repository/google")
        // ===== 以下是上游原始配置，保留作为兜底 =====
        mavenCentral()
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
