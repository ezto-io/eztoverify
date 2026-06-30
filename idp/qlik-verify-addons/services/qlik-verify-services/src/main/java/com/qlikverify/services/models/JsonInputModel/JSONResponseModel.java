package com.qlikverify.services.models.JsonInputModel;

import java.util.List;
import java.util.Map;

public class JSONResponseModel {
    private String formId;
    private Map<String, Object> formDisplayTitle;
    private String formDisplayTitleClr;
    private String formDisplayTitleAlign;
    private Map<String, Object> formDescription;
    private String formDescriptionClr;
    private String formDescriptionAlign;
    private List<JSONButton> formDisplayButtons;
    private List<JSONFormInputModel> formInputs;

    public String getFormId() {
        return formId;
    }

    public void setFormId(String formId) {
        this.formId = formId;
    }

    public Map<String, Object> getFormDisplayTitle() {
        return formDisplayTitle;
    }

    public void setFormDisplayTitle(Map<String, Object> formDisplayTitle) {
        this.formDisplayTitle = formDisplayTitle;
    }

    public String getFormDisplayTitleClr() {
        return formDisplayTitleClr;
    }

    public void setFormDisplayTitleClr(String formDisplayTitleClr) {
        this.formDisplayTitleClr = formDisplayTitleClr;
    }

    public String getFormDisplayTitleAlign() {
        return formDisplayTitleAlign;
    }

    public void setFormDisplayTitleAlign(String formDisplayTitleAlign) {
        this.formDisplayTitleAlign = formDisplayTitleAlign;
    }

    public Map<String, Object> getFormDescription() {
        return formDescription;
    }

    public void setFormDescription(Map<String, Object> formDescription) {
        this.formDescription = formDescription;
    }

    public String getFormDescriptionClr() {
        return formDescriptionClr;
    }

    public void setFormDescriptionClr(String formDescriptionClr) {
        this.formDescriptionClr = formDescriptionClr;
    }

    public String getFormDescriptionAlign() {
        return formDescriptionAlign;
    }

    public void setFormDescriptionAlign(String formDescriptionAlign) {
        this.formDescriptionAlign = formDescriptionAlign;
    }

    public List<JSONButton> getFormDisplayButtons() {
        return formDisplayButtons;
    }

    public void setFormDisplayButtons(List<JSONButton> formDisplayButtons) {
        this.formDisplayButtons = formDisplayButtons;
    }

    public List<JSONFormInputModel> getFormInputs() {
        return formInputs;
    }

    public void setFormInputs(List<JSONFormInputModel> formInputs) {
        this.formInputs = formInputs;
    }

    @Override
    public String toString() {
        return "JSONResponseModel{" +
                "formId='" + formId + '\'' +
                ", formDisplayTitle=" + formDisplayTitle +
                ", formDisplayTitleClr='" + formDisplayTitleClr + '\'' +
                ", formDisplayTitleAlign='" + formDisplayTitleAlign + '\'' +
                ", formDescription=" + formDescription +
                ", formDescriptionClr='" + formDescriptionClr + '\'' +
                ", formDescriptionAlign='" + formDescriptionAlign + '\'' +
                ", formDisplayButtons=" + formDisplayButtons +
                ", formInputs=" + formInputs +
                '}';
    }
}