package com.codecore.iam.interfaces.http.admin;

import com.codecore.iam.application.command.CreateAdminUserCommand;
import com.codecore.iam.application.command.UpdateAdminUserCommand;
import com.codecore.iam.application.port.in.CreateAdminUserUseCase;
import com.codecore.iam.application.port.in.DeactivateAdminUserUseCase;
import com.codecore.iam.application.port.in.GetAdminUserUseCase;
import com.codecore.iam.application.port.in.ListAdminUsersUseCase;
import com.codecore.iam.application.port.in.UpdateAdminUserUseCase;
import com.codecore.iam.application.query.PageQuery;
import com.codecore.iam.application.query.PageQueryParser;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.interfaces.http.admin.dto.CreateUserRequest;
import com.codecore.iam.interfaces.http.admin.dto.PagedUserResponse;
import com.codecore.iam.interfaces.http.admin.dto.UpdateUserRequest;
import com.codecore.iam.interfaces.http.admin.dto.UserResponse;
import com.codecore.iam.interfaces.http.security.RequiresPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping(IamAdminApiPaths.USERS)
@Tag(name = "Users", description = "Identity administration (`user:*` permissions)")
public class IamUserAdminController {

    private final ListAdminUsersUseCase listAdminUsersUseCase;
    private final GetAdminUserUseCase getAdminUserUseCase;
    private final CreateAdminUserUseCase createAdminUserUseCase;
    private final UpdateAdminUserUseCase updateAdminUserUseCase;
    private final DeactivateAdminUserUseCase deactivateAdminUserUseCase;

    public IamUserAdminController(
            ListAdminUsersUseCase listAdminUsersUseCase,
            GetAdminUserUseCase getAdminUserUseCase,
            CreateAdminUserUseCase createAdminUserUseCase,
            UpdateAdminUserUseCase updateAdminUserUseCase,
            DeactivateAdminUserUseCase deactivateAdminUserUseCase
    ) {
        this.listAdminUsersUseCase = listAdminUsersUseCase;
        this.getAdminUserUseCase = getAdminUserUseCase;
        this.createAdminUserUseCase = createAdminUserUseCase;
        this.updateAdminUserUseCase = updateAdminUserUseCase;
        this.deactivateAdminUserUseCase = deactivateAdminUserUseCase;
    }

    @GetMapping
    @RequiresPermission("user:read")
    public Mono<PagedUserResponse> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        PageQuery pageQuery = PageQueryParser.parseUserPageQuery(page, size, sort);
        return listAdminUsersUseCase.execute(pageQuery).map(PagedUserResponse::from);
    }

    @GetMapping("/{id}")
    @RequiresPermission("user:read")
    public Mono<UserResponse> getUser(@PathVariable UUID id) {
        return getAdminUserUseCase.execute(new IdentityId(id)).map(UserResponse::from);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission("user:create")
    public Mono<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        CreateAdminUserCommand command = new CreateAdminUserCommand(
                request.email(),
                request.password(),
                request.initialStatus()
        );
        return createAdminUserUseCase.execute(command).map(UserResponse::from);
    }

    @PutMapping("/{id}")
    @RequiresPermission("user:update")
    public Mono<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        UpdateAdminUserCommand command = new UpdateAdminUserCommand(
                new IdentityId(id),
                request.status(),
                request.email()
        );
        return updateAdminUserUseCase.execute(command).map(UserResponse::from);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("user:delete")
    public Mono<Void> deactivateUser(@PathVariable UUID id) {
        return deactivateAdminUserUseCase.deactivate(new IdentityId(id));
    }
}
