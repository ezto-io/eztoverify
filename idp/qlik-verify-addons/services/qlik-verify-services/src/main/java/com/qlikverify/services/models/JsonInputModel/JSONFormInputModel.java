package com.qlikverify.services.models.JsonInputModel;

import java.util.ArrayList;
import java.util.Map;

public class JSONFormInputModel {

    private String inputId;
    private String type;
    private String pattern;
    private String label;
    private Map<String, Object> options;
    private Map<String, Object> labelTxt;
    private Map<String, Object> placeholderTxt;


    public String getInputId() {
        return inputId;
    }

    public void setInputId(String inputId) {
        this.inputId = inputId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getLabelTxt() {
        return labelTxt;
    }

    public void setLabelTxt(Map<String, Object> labelTxt) {
        this.labelTxt = labelTxt;
    }

    public Map<String, Object> getPlaceholderTxt() {
        return placeholderTxt;
    }

    public void setPlaceholderTxt(Map<String, Object> placeholderTxt) {
        this.placeholderTxt = placeholderTxt;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }

    @Override
    public String toString() {
        return "JSONFormInputModel{" +
                "inputId='" + inputId + '\'' +
                ", type='" + type + '\'' +
                ", pattern='" + pattern + '\'' +
                ", label='" + label + '\'' +
                ", options=" + options +
                ", labelTxt=" + labelTxt +
                ", placeholderTxt=" + placeholderTxt +
                '}';
    }
}
