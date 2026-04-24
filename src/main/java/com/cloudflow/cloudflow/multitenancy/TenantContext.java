package com.cloudflow.cloudflow.multitenancy;

import java.util.UUID;

public class TenantContext {

    // ThreadLocal stores a separate value per thread.
    // Each HTTP request runs on its own thread, so each
    // request gets its own tenant ID — they never interfere.
    private static final ThreadLocal<UUID> currentTenantId = new ThreadLocal<>();

    public static void setTenantId(UUID tenantId) {
        currentTenantId.set(tenantId);
    }

    public static UUID getTenantId() {
        return currentTenantId.get();
    }

    // CRITICAL: always call this at the end of the request.
    // If you don't clear it, the next request on this thread
    // inherits the previous request's tenant ID — a security leak.
    public static void clear() {
        currentTenantId.remove();
    }
}