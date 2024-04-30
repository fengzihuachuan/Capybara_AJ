package fengzihuachuan.capybara_aj;

import fengzihuachuan.capybara_aj.subtitle.Time;

public class ListItem {
    private int key;
    private Time substart;
    private Time subend;
    private String subcontent;

    public ListItem(int key, Time substart, String subcontent, Time subend) {
        this.key = key;
        this.substart = substart;
        this.subcontent = subcontent;
        this.subend = subend;
    }

    public Time getSubStart() {
        return substart;
    }

    public String getSubContent() {
        return subcontent;
    }

    public Time getSubEnd() {
        return subend;
    }
}