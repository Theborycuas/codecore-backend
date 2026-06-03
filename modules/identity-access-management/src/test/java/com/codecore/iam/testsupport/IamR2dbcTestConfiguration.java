package com.codecore.iam.testsupport;

import com.codecore.platform.r2dbc.PlatformR2dbcAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Enables {@link PlatformR2dbcAutoConfiguration} for IAM integration tests using {@code @DataR2dbcTest}.
 */
@Import(PlatformR2dbcAutoConfiguration.class)
public final class IamR2dbcTestConfiguration {
}
