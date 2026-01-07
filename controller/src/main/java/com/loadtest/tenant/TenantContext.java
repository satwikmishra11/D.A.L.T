package com.loadtest.tenant;

public class TenantContext {

    private static final ThreadLocal<String> ORG = new ThreadLocal<>();

    public static void setOrgId(String orgId) {
        ORG.set(orgId);
    }

    public static String getOrgId() {
        return ORG.get();
    }

    public static void clear() {
        ORG.remove();
    }
}
