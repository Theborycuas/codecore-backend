package com.codecore.iam.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Greenfield platform bootstrap configuration (PASO 15.9.2).
 */
@ConfigurationProperties(prefix = "codecore.platform.bootstrap")
public class PlatformBootstrapProperties {

    private boolean enabled = false;
    private String tenantName = "CodeCore";
    private String ownerEmail;
    private String ownerPassword;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getOwnerPassword() {
        return ownerPassword;
    }

    public void setOwnerPassword(String ownerPassword) {
        this.ownerPassword = ownerPassword;
    }
}
