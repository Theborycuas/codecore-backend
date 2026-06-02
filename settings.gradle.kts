rootProject.name = "codecore"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        mavenCentral()
    }
}

include(
    ":apps:codecore-api"
)

include(
    ":shared:shared-events",
    ":shared:shared-kernel",
    ":shared:shared-observability",
    ":shared:shared-security",
    ":shared:shared-tenancy",
    ":shared:shared-test",
    ":shared:shared-web"
)

include(
    ":platform:platform-postgres",
    ":platform:platform-webflux",
    ":platform:platform-r2dbc",
    ":platform:platform-kafka",
    ":platform:platform-redis",
    ":platform:platform-security",
    ":platform:platform-telemetry"
)

include(":modules:identity-access-management")


include(
    ":modules:tenant-management:tenant-domain",
    ":modules:tenant-management:tenant-application",
    ":modules:tenant-management:tenant-infrastructure",
    ":modules:tenant-management:tenant-contract"
)

include(
    ":modules:user-management:user-domain",
    ":modules:user-management:user-application",
    ":modules:user-management:user-infrastructure",
    ":modules:user-management:user-contract"
)

include(
    ":modules:authorization-management:authorization-domain",
    ":modules:authorization-management:authorization-application",
    ":modules:authorization-management:authorization-infrastructure",
    ":modules:authorization-management:authorization-contract"
)