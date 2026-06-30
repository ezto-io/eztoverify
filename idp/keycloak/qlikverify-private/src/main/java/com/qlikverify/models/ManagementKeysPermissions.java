package com.qlikverify.models;

public enum ManagementKeysPermissions {
    LIST("list"),
    REMOVE("remove"),
    DISABLE("disable"),
    ENABLE("enable"),
    MARK_AS_LAST_USED("mark-as-last-used");

    private final String permission;
    private ManagementKeysPermissions(String permission) {
        this.permission = permission;
    }

    public String getName(){
        return permission;
    }

    public static String getEnumByString(String code){
        for(ManagementKeysPermissions e : ManagementKeysPermissions.values()){
            if(e.permission.equals(code)) return e.name();
        }
        return null;
    }
}
