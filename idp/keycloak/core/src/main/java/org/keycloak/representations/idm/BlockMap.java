package org.keycloak.representations.idm;

import java.util.List;

public class BlockMap {

    private String id;
    private String name;
    private String authConfig;
    private String requirement;
    private List<InputMap> inputs;
    private List<OutputMap> outputs;

    public BlockMap(String id, String name, String authConfig,String requirement, List<InputMap> ins, List<OutputMap> outs) {
        this.id = id;
        this.name = name;
        this.authConfig = authConfig;
        this.requirement = requirement;
        this.inputs = ins;
        this.outputs = outs;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAuthConfig() {
        return authConfig;
    }

    public void setAuthConfig(String authConfig) {
        this.authConfig = authConfig;
    }


    public String getRequirement() {
        return requirement;
    }

    public void setRequirement(String requirement) {
        this.requirement = requirement;
    }

    public List<InputMap> getInputs() {
        return inputs;
    }

    public List<OutputMap> getOutputs() {
        return outputs;
    }
}
