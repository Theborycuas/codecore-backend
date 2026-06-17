package com.codecore.iam.testsupport;

import com.codecore.iam.configuration.IamOpenApiConfiguration;
import com.codecore.iam.interfaces.http.admin.IamAdministrationController;
import com.codecore.iam.interfaces.http.admin.IamMembershipAdminController;
import com.codecore.iam.interfaces.http.admin.IamMembershipRoleAdminController;
import com.codecore.iam.interfaces.http.admin.IamPermissionAdminController;
import com.codecore.iam.interfaces.http.admin.IamRoleAdminController;
import com.codecore.iam.interfaces.http.admin.IamRolePermissionAdminController;
import com.codecore.iam.interfaces.http.admin.IamTenantAdminController;
import com.codecore.iam.interfaces.http.admin.IamUserAdminController;
import com.codecore.iam.interfaces.http.openapi.RequiresPermissionOperationCustomizer;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAutoConfiguration
@Import({
        IamOpenApiConfiguration.class,
        IamAdministrationController.class,
        IamUserAdminController.class,
        IamMembershipAdminController.class,
        IamMembershipRoleAdminController.class,
        IamRoleAdminController.class,
        IamPermissionAdminController.class,
        IamRolePermissionAdminController.class,
        IamTenantAdminController.class
})
public class IamOpenApiTestConfiguration {
}
