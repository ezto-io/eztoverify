package com.qlikverify.services.models.JsonInputModel;

import java.util.ArrayList;

public class JsonFormInputRep {
    private String inputId;
    private String type;
    private String pattern;
    private String label;
    private String labelTxt;
    private String placeholderTxt;

    private ArrayList<String> options;

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

    public String getLabelTxt() {
        return labelTxt;
    }

    public void setLabelTxt(String labelTxt) {
        this.labelTxt = labelTxt;
    }

    public String getPlaceholderTxt() {
        return placeholderTxt;
    }

    public void setPlaceholderTxt(String placeholderTxt) {
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

    public ArrayList<String> getOptions() {
        return options;
    }

    public void setOptions(ArrayList<String> options) {
        this.options = options;
    }
}
