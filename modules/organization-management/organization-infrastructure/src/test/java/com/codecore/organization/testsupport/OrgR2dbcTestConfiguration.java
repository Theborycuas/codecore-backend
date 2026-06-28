package com.codecore.organization.testsupport;

import com.codecore.platform.r2dbc.PlatformR2dbcAutoConfiguration;
import org.springframework.context.annotation.Import;

@Import(PlatformR2dbcAutoConfiguration.class)
public final class OrgR2dbcTestConfiguration {
}
