package com.codecore.iam.interfaces.http.openapi;

import com.codecore.iam.interfaces.http.security.RequiresPermission;
import io.swagger.v3.oas.models.Operation;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springframework.web.method.HandlerMethod;

/**
 * Maps {@link RequiresPermission} to OpenAPI operation metadata (FASE 15.8).
 */
public class RequiresPermissionOperationCustomizer implements GlobalOperationCustomizer {

    public static final String PERMISSION_EXTENSION = "x-permission";

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        RequiresPermission requiresPermission = handlerMethod.getMethodAnnotation(RequiresPermission.class);
        if (requiresPermission == null) {
            requiresPermission = handlerMethod.getBeanType().getAnnotation(RequiresPermission.class);
        }
        if (requiresPermission == null) {
            return operation;
        }

        String permission = requiresPermission.value();
        operation.addExtension(PERMISSION_EXTENSION, permission);

        String descriptionPrefix = "Required permission: `" + permission + "`.";
        if (operation.getDescription() == null || operation.getDescription().isBlank()) {
            operation.setDescription(descriptionPrefix);
        } else if (!operation.getDescription().contains(permission)) {
            operation.setDescription(descriptionPrefix + " " + operation.getDescription());
        }
        return operation;
    }
}
