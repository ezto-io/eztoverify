package org.keycloak.marketplace;

public class BlockMap {

    private String name;
    private boolean isDefault;
    private boolean isPlaceHolder;
    private String parent;
    private String[] children;

    public BlockMap(String name, boolean isDefault, boolean isPlaceHolder, String parent) {
        this.name = name;
        this.isDefault = isDefault;
        this.isPlaceHolder = isPlaceHolder;
        this.parent = parent;
        this.children = null;
    }

    public String getName() {
        return name;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isPlaceHolder() {
        return isPlaceHolder;
    }

    public String getParent() {
        return parent;
    }

    public String[] getChildren() {
        return children;
    }
}
