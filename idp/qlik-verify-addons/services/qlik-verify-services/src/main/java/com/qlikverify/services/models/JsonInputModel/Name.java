package com.qlikverify.services.models.JsonInputModel;

public class Name {
    private String en;
    private String tr;
    private String de;

    public String getEn() {
        return en;
    }

    public void setEn(String en) {
        this.en = en;
    }

    public String getTr() {
        return tr;
    }

    public void setTr(String tr) {
        this.tr = tr;
    }

    public String getDe() {
        return de;
    }

    public void setDe(String de) {
        this.de = de;
    }

    @Override
    public String toString() {
        return "Name{" +
                "en='" + en + '\'' +
                ", tr='" + tr + '\'' +
                ", de='" + de + '\'' +
                '}';
    }
}
