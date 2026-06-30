package com.qlikverify.services.models.JsonInputModel;
import java.util.Map;


public class JSONButton {
    private String btnId;
    private String btnBgClr;
    private Map<String, Object> btnText;
    private String btnTextClr;

    public JSONButton() {
    }

    public JSONButton(String btnId, Map<String, Object> btnText, String btnTextClr, String btnBgClr){
        this.btnId = btnId;
        this.btnBgClr = btnBgClr;
        this.btnText = btnText;
        this.btnTextClr = btnTextClr;
    }

    public String getBtnId() {
        return btnId;
    }

    public void setBtnId(String btnId) {
        this.btnId = btnId;
    }

    public String getBtnBgClr() {
        return btnBgClr;
    }

    public void setBtnBgClr(String btnBgClr) {
        this.btnBgClr = btnBgClr;
    }

    public Map<String, Object> getBtnText() {
        return btnText;
    }

    public void setBtnText(Map<String, Object> btnText) {
        this.btnText = btnText;
    }

    public String getBtnTextClr() {
        return btnTextClr;
    }

    public void setBtnTextClr(String btnTextClr) {
        this.btnTextClr = btnTextClr;
    }
}
