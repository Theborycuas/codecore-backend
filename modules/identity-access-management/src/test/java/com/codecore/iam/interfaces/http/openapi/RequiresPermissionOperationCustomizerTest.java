package com.codecore.iam.interfaces.http.openapi;

import com.codecore.iam.interfaces.http.admin.IamUserAdminController;
import com.codecore.iam.interfaces.http.security.RequiresPermission;
import io.swagger.v3.oas.models.Operation;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RequiresPermissionOperationCustomizerTest {

    @Test
    void shouldAddPermissionExtensionAndDescription() throws Exception {
        RequiresPermissionOperationCustomizer customizer = new RequiresPermissionOperationCustomizer();
        Method method = IamUserAdminController.class.getDeclaredMethod(
                "listUsers",
                int.class,
                int.class,
                String.class
        );
        HandlerMethod handlerMethod = new HandlerMethod(mock(IamUserAdminController.class), method);

        Operation operation = customizer.customize(new Operation(), handlerMethod);

        assertThat(operation.getExtensions())
                .containsEntry(RequiresPermissionOperationCustomizer.PERMISSION_EXTENSION, "user:read");
        assertThat(operation.getDescription()).contains("user:read");
        assertThat(method.getAnnotation(RequiresPermission.class).value()).isEqualTo("user:read");
    }
}
