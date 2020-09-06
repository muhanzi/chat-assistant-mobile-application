package com.muhanzi.notify.activities;

public class Spam {
    private String notificationTitle,spamText,date,time,packageName;

    public Spam() {
    }

    public Spam(String notificationTitle, String spamText, String date, String time, String packageName) {
        this.notificationTitle = notificationTitle;
        this.spamText = spamText;
        this.date = date;
        this.time = time;
        this.packageName = packageName;
    }

    public String getNotificationTitle() {
        return notificationTitle;
    }

    public void setNotificationTitle(String notificationTitle) {
        this.notificationTitle = notificationTitle;
    }

    public String getSpamText() {
        return spamText;
    }

    public void setSpamText(String spamText) {
        this.spamText = spamText;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
}
