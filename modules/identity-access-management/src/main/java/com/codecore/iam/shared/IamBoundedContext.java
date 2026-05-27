package com.codecore.iam.shared;

/**
 * IAM bounded context metadata and official package root.
 *
 * <p>Layering reference for CodeCore modules:
 * <ul>
 *   <li>{@code domain} — aggregates, entities, domain value objects, domain exceptions (no Spring, no Reactor)</li>
 *   <li>{@code application.port.in} — use cases (inbound)</li>
 *   <li>{@code application.port.out} — repositories and external integrations (outbound, reactive)</li>
 *   <li>{@code application.command} / {@code application.dto} — application data shapes</li>
 *   <li>{@code infrastructure} — adapters (persistence, hashing, messaging)</li>
 *   <li>{@code interfaces.http} — WebFlux controllers and request/response models</li>
 *   <li>{@code configuration} — Spring wiring</li>
 * </ul>
 */
public final class IamBoundedContext {

    public static final String NAME = "identity-access-management";
    public static final String BASE_PACKAGE = "com.codecore.iam";

    private IamBoundedContext() {
    }
}
