package fengzihuachuan.capybara_aj;

import fengzihuachuan.capybara_aj.subtitle.Time;

public class SubtitleItem {
    private int key;
    private Time substart;
    private Time subend;
    private String subcontent;
    private boolean recExist;

    public SubtitleItem(int key, Time substart, Time subend, String subcontent, boolean recExist) {
        this.key = key;
        this.substart = substart;
        this.subend = subend;
        this.subcontent = subcontent;
        this.recExist = recExist;
    }

    public Time getSubStart() {
        return substart;
    }

    public Time getSubEnd() {
        return subend;
    }

    public String getSubContent() {
        return subcontent;
    }

    public boolean getRecExist() {
        return recExist;
    }

    public void setRecExist(boolean exist) {
        recExist = exist;
    }
}