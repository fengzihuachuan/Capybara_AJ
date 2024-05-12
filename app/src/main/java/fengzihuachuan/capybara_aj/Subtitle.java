package fengzihuachuan.capybara_aj;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import fengzihuachuan.capybara_aj.subtitle.FormatASS;
import fengzihuachuan.capybara_aj.subtitle.FormatSRT;
import fengzihuachuan.capybara_aj.subtitle.Time;
import fengzihuachuan.capybara_aj.subtitle.TimedTextFileFormat;
import fengzihuachuan.capybara_aj.subtitle.TimedTextObject;


public class Subtitle {
    static String TAG = "Subtitle";

    static TimedTextObject currTto;
    static String currSubtlPath;

    private static ArrayList<Item> subtitleList = null;

    private static TimedTextObject _load(String path) {
        File file = new File(path);
        try {
            TimedTextFileFormat ttff = null;
            if (path.substring(path.lastIndexOf('.')).equals(".srt")) {
                ttff = new FormatSRT();
            } else if (path.substring(path.lastIndexOf('.')).equals(".ass")) {
                ttff = new FormatASS();
            } else {
                Log.d(TAG, "ttff : Unknow format" + path);
                return null;
            }

            InputStream is = new FileInputStream(file);
            TimedTextObject tto = ttff.parseFile(path, is);
            is.close();

            return tto;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static ArrayList<Item> loadCurrent() {
        if (subtitleList != null) {
            subtitleList.clear();
        }
        subtitleList = new ArrayList<Item>();

        currSubtlPath = Files.getCurrInfo(Files.GET_SBTPATH);

        currTto = _load(currSubtlPath);
        if (currTto == null) {
            return null;
        } else {
            int id = 0;
            for (Integer key : currTto.captions.keySet()) {
                currTto.captions.get(key).content = currTto.captions.get(key).content.replaceAll("<[^>]+>", ""); //删除html标签
                Item i = new Item(id, currTto.captions.get(key).start, currTto.captions.get(key).end, currTto.captions.get(key).content, Files.getRecStatus(id));
                subtitleList.add(i);
                id++;
            }
            return subtitleList;
        }
    }

    static ArrayList<Item> list() {
        return subtitleList;
    }

    static Item get(int id) {
        if (id < subtitleList.size())
            return subtitleList.get(id);
        else
            return null;
    }

    static int size() {
        return subtitleList.size();
    }

    static int getRecSum() {
        int count = 0;
        for (int i = 0; i < subtitleList.size(); i++) {
            if (subtitleList.get(i).recExist) {
                count++;
            }
        }
        return count;
    }

    public static class Item {
        public int key;
        public Time substart;
        public Time subend;
        public String subcontent;
        public boolean recExist;

        public Item(int key, Time substart, Time subend, String subcontent, boolean recExist) {
            this.key = key;
            this.substart = substart;
            this.subend = subend;
            this.subcontent = subcontent;
            this.recExist = recExist;
        }
    }
}
