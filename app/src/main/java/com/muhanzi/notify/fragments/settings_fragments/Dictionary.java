package com.muhanzi.notify.fragments.settings_fragments;

public class Dictionary {
    private String abbreviation,meaning;

    public Dictionary() {
    }

    public Dictionary(String abbreviation, String meaning) {
        this.abbreviation = abbreviation;
        this.meaning = meaning;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getMeaning() {
        return meaning;
    }

    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }
}
