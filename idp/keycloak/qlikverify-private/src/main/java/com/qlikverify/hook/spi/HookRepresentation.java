package com.qlikverify.hook.spi;



public class HookRepresentation {

    private String id;
    private String key;
    private String chatCode;
    private String realmId;
    private String hookResult;

    public HookRepresentation() {
    }
    
    public String getId() {
		return id;
	}

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getChatCode() {
        return chatCode;
    }

    public void setChatCode(String chatCode) {
        this.chatCode = chatCode;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getHookResult() {
        return hookResult;
    }

    public void setHookResult(String hookResult) {
        this.hookResult = hookResult;
    }
}
