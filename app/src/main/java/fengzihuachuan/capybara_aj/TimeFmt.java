package fengzihuachuan.capybara_aj;

import fengzihuachuan.capybara_aj.subtitle.Time;

public class TimeFmt {
    static String strFromMs(int ms) {
        Time time = new Time("hh:mm:ss,ms", "00:00:00,000");
        time.setMseconds(ms);
        return time.toString();
    }

    static int msFromStr(String s) {
        Time time = new Time("hh:mm:ss,ms", s);
        return time.getMseconds();
    }

    static Time timeFromStr(String s) {
        Time time = new Time("hh:mm:ss,ms", s);
        return time;
    }

    static Time timeFromMs(int ms) {
        Time time = new Time("hh:mm:ss,ms", "00:00:00,000");
        time.setMseconds(ms);
        return time;
    }
}
