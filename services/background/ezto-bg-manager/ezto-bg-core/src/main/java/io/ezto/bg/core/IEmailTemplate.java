package io.ezto.bg.core;


import java.util.Locale;
import java.util.Map;

public abstract class IEmailTemplate implements Cloneable{

    private Locale locale;

    public abstract String getTemplatePath();
    public abstract String getSubject();
    public abstract void setSubject(String subject);
    public abstract String getId();
    public abstract String getToEmail();
    public abstract IEmailTemplate setToEmail(String toEmail);
    public abstract IEmailTemplate setPlaceHolders(Map<String,String> params);
    public abstract Map<String,String> getPlaceHolders();


    @Override
    public IEmailTemplate clone() {
        try {
            return (IEmailTemplate) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Locale getLocale() {
        return locale;
    }
}
