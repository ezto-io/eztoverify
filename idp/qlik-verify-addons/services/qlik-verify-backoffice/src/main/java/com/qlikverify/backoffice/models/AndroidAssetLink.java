package com.qlikverify.backoffice.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.qlikverify.backoffice.models.fidoAndroidAsset.Target;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AndroidAssetLink {
    private List<String> relation;
    private Target target;

    public List<String> getRelation() { return relation; }
    public void setRelation(List<String> value) { this.relation = value; }


    public Target getTarget() { return target; }
    public void setTarget(Target value) { this.target = value; }
}
