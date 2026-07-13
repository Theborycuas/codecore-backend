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
    ":modules:organization-management:organization-domain",
    ":modules:organization-management:organization-application",
    ":modules:organization-management:organization-infrastructure",
    ":modules:organization-management:organization-contract"
)

include(
    ":modules:patient-management:patient-domain",
    ":modules:patient-management:patient-application",
    ":modules:patient-management:patient-infrastructure",
    ":modules:patient-management:patient-contract"
)

include(
    ":modules:appointment-management:appointment-domain",
    ":modules:appointment-management:appointment-application",
    ":modules:appointment-management:appointment-infrastructure",
    ":modules:appointment-management:appointment-contract"
)

include(
    ":modules:encounter-management:encounter-domain",
    ":modules:encounter-management:encounter-application",
    ":modules:encounter-management:encounter-infrastructure",
    ":modules:encounter-management:encounter-contract"
)

include(
    ":modules:inventory-management:inventory-domain",
    ":modules:inventory-management:inventory-application",
    ":modules:inventory-management:inventory-infrastructure",
    ":modules:inventory-management:inventory-contract"
)

include(
    ":modules:billing-management:billing-domain",
    ":modules:billing-management:billing-application",
    ":modules:billing-management:billing-infrastructure",
    ":modules:billing-management:billing-contract"
)

include(
    ":modules:payment-management:payment-domain",
    ":modules:payment-management:payment-application",
    ":modules:payment-management:payment-infrastructure",
    ":modules:payment-management:payment-contract"
)

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