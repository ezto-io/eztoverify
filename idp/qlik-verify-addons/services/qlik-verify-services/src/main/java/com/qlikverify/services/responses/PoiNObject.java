package com.qlikverify.services.responses;

import java.util.ArrayList;

public class PoiNObject {
    private ArrayList<String> title;
    private ArrayList<String> givenName;
    private ArrayList<String> surname;
    private String nameSuffix;

    public PoiNObject() {
        // NOP
    }

    public ArrayList<String> getTitle() {
        return title;
    }

    public void setTitle(ArrayList<String> title) {
        this.title = title;
    }

    public ArrayList<String> getGivenName() {
        return givenName;
    }

    public void setGivenName(ArrayList<String> givenName) {
        this.givenName = givenName;
    }

    public ArrayList<String> getSurname() {
        return surname;
    }

    public void setSurname(ArrayList<String> surname) {
        this.surname = surname;
    }

    public String getNameSuffix() {
        return nameSuffix;
    }

    public void setNameSuffix(String nameSuffix) {
        this.nameSuffix = nameSuffix;
    }
}
