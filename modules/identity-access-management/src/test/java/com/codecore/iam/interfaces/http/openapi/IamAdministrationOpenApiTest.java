package com.codecore.iam.interfaces.http.openapi;

import com.codecore.iam.application.port.in.CreateAdminMembershipUseCase;
import com.codecore.iam.application.port.in.CreateAdminRoleUseCase;
import com.codecore.iam.application.port.in.CreateAdminUserUseCase;
import com.codecore.iam.application.port.in.DeactivateAdminMembershipUseCase;
import com.codecore.iam.application.port.in.DeactivateAdminUserUseCase;
import com.codecore.iam.application.port.in.DeleteAdminRoleUseCase;
import com.codecore.iam.application.port.in.GetAdminMembershipRolesUseCase;
import com.codecore.iam.application.port.in.GetAdminMembershipUseCase;
import com.codecore.iam.application.port.in.GetAdminPermissionUseCase;
import com.codecore.iam.application.port.in.GetAdminRolePermissionsUseCase;
import com.codecore.iam.application.port.in.GetAdminRoleUseCase;
import com.codecore.iam.application.port.in.GetAdminTenantUseCase;
import com.codecore.iam.application.port.in.GetAdminUserUseCase;
import com.codecore.iam.application.port.in.ListAdminMembershipsUseCase;
import com.codecore.iam.application.port.in.ListAdminPermissionsUseCase;
import com.codecore.iam.application.port.in.ListAdminRolesUseCase;
import com.codecore.iam.application.port.in.ListAdminUsersUseCase;
import com.codecore.iam.application.port.in.ReplaceAdminMembershipRolesUseCase;
import com.codecore.iam.application.port.in.ReplaceAdminRolePermissionsUseCase;
import com.codecore.iam.application.port.in.UpdateAdminMembershipUseCase;
import com.codecore.iam.application.port.in.UpdateAdminRoleUseCase;
import com.codecore.iam.application.port.in.UpdateAdminTenantUseCase;
import com.codecore.iam.application.port.in.UpdateAdminUserUseCase;
import com.codecore.iam.configuration.IamOpenApiConfiguration;
import com.codecore.iam.interfaces.http.admin.IamAdminApiPaths;
import com.codecore.iam.testsupport.IamOpenApiTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = IamOpenApiTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.r2dbc.R2dbcRepositoriesAutoConfiguration"
        }
)
@AutoConfigureWebTestClient
class IamAdministrationOpenApiTest {

    private static final Set<String> EXPECTED_PATHS = Set.of(
          IamAdminApiPaths.ADMINISTRATION + "/status",
          IamAdminApiPaths.USERS,
          IamAdminApiPaths.USERS + "/{id}",
          IamAdminApiPaths.MEMBERSHIPS,
          IamAdminApiPaths.MEMBERSHIPS + "/{id}",
          IamAdminApiPaths.MEMBERSHIPS + "/{membershipId}/roles",
          IamAdminApiPaths.ROLES,
          IamAdminApiPaths.ROLES + "/{id}",
          IamAdminApiPaths.ROLES + "/{roleId}/permissions",
          IamAdminApiPaths.PERMISSIONS,
          IamAdminApiPaths.PERMISSIONS + "/{id}",
          IamAdminApiPaths.TENANTS + "/current"
  );

  @Autowired
  private WebTestClient webTestClient;

  @MockBean
  private ListAdminUsersUseCase listAdminUsersUseCase;
  @MockBean
  private GetAdminUserUseCase getAdminUserUseCase;
  @MockBean
  private CreateAdminUserUseCase createAdminUserUseCase;
  @MockBean
  private UpdateAdminUserUseCase updateAdminUserUseCase;
  @MockBean
  private DeactivateAdminUserUseCase deactivateAdminUserUseCase;
  @MockBean
  private ListAdminMembershipsUseCase listAdminMembershipsUseCase;
  @MockBean
  private GetAdminMembershipUseCase getAdminMembershipUseCase;
  @MockBean
  private CreateAdminMembershipUseCase createAdminMembershipUseCase;
  @MockBean
  private UpdateAdminMembershipUseCase updateAdminMembershipUseCase;
  @MockBean
  private DeactivateAdminMembershipUseCase deactivateAdminMembershipUseCase;
  @MockBean
  private ListAdminRolesUseCase listAdminRolesUseCase;
  @MockBean
  private GetAdminRoleUseCase getAdminRoleUseCase;
  @MockBean
  private CreateAdminRoleUseCase createAdminRoleUseCase;
  @MockBean
  private UpdateAdminRoleUseCase updateAdminRoleUseCase;
  @MockBean
  private DeleteAdminRoleUseCase deleteAdminRoleUseCase;
  @MockBean
  private ListAdminPermissionsUseCase listAdminPermissionsUseCase;
  @MockBean
  private GetAdminPermissionUseCase getAdminPermissionUseCase;
  @MockBean
  private GetAdminRolePermissionsUseCase getAdminRolePermissionsUseCase;
  @MockBean
  private ReplaceAdminRolePermissionsUseCase replaceAdminRolePermissionsUseCase;
  @MockBean
  private GetAdminMembershipRolesUseCase getAdminMembershipRolesUseCase;
  @MockBean
  private ReplaceAdminMembershipRolesUseCase replaceAdminMembershipRolesUseCase;
  @MockBean
  private GetAdminTenantUseCase getAdminTenantUseCase;
  @MockBean
  private UpdateAdminTenantUseCase updateAdminTenantUseCase;

  @Test
  void shouldExposeIamAdministrationOpenApiGroup() {
    webTestClient.get()
            .uri("/v3/api-docs/" + IamOpenApiConfiguration.IAM_ADMINISTRATION_GROUP)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.info.title").isEqualTo("CodeCore IAM Administration API")
            .jsonPath("$.components.securitySchemes['bearer-jwt']").exists();
  }

  @Test
  void shouldDocumentAllAdministrationPaths() {
    byte[] body = webTestClient.get()
            .uri("/v3/api-docs/" + IamOpenApiConfiguration.IAM_ADMINISTRATION_GROUP)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .returnResult()
            .getResponseBody();

    assertThat(body).isNotNull();
    String json = new String(body);

    for (String path : EXPECTED_PATHS) {
      assertThat(json)
              .as("OpenAPI document must include path %s", path)
              .contains("\"%s\"".formatted(path));
    }
  }

    @Test
    void shouldAttachPermissionMetadataOnProtectedOperations() {
        webTestClient.get()
                .uri("/v3/api-docs/" + IamOpenApiConfiguration.IAM_ADMINISTRATION_GROUP)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.paths['" + IamAdminApiPaths.USERS + "'].get.description")
                .value(description -> assertThat(description.toString()).contains("user:read"));
    }

    @Test
    void shouldExposeGroupedApiDocsEndpoint() {
        webTestClient.get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus().isOk();
    }
}
