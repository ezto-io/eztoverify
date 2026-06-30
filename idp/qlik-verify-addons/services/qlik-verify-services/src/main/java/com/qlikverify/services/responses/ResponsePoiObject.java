package com.qlikverify.services.responses;

public class ResponsePoiObject {
    private String birthdate;
    private String nationality;
    private String gender;
    private String reference;
    private String context;
    private String birthplace;
    private PoiNObject n;
    private PoiAdrObject adr;
    private PoiDocObject doc;

    public ResponsePoiObject() {
        // NOP
    }

    public String getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public PoiNObject getN() {
        return n;
    }

    public void setN(PoiNObject n) {
        this.n = n;
    }

    public PoiAdrObject getAdr() {
        return adr;
    }

    public void setAdr(PoiAdrObject adr) {
        this.adr = adr;
    }

    public PoiDocObject getDoc() {
        return doc;
    }

    public void setDoc(PoiDocObject doc) {
        this.doc = doc;
    }

    public String getBirthplace() {
        return birthplace;
    }

    public void setBirthplace(String birthplace) {
        this.birthplace = birthplace;
    }
}
