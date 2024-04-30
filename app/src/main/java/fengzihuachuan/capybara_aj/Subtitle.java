package fengzihuachuan.capybara_aj;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import fengzihuachuan.capybara_aj.subtitle.FormatASS;
import fengzihuachuan.capybara_aj.subtitle.FormatSRT;
import fengzihuachuan.capybara_aj.subtitle.TimedTextFileFormat;
import fengzihuachuan.capybara_aj.subtitle.TimedTextObject;


public class Subtitle {
    static String TAG = "Subtitle";

    static TimedTextObject currTto;
    static String currSubtlPath;

    private static ArrayList<ListItem> subtitleList = null;

    static int getSize(String path) {
        TimedTextObject tto = _load(path);
        return tto.captions.keySet().size();
    }

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

    static ArrayList<ListItem> loadCurrent() {
        if (subtitleList != null) {
            subtitleList.clear();
        }
        subtitleList = new ArrayList<ListItem>();

        currSubtlPath = FileUtils.getCurrInfo(FileUtils.GET_SBTPATH);

        currTto = _load(currSubtlPath);
        if (currTto == null) {
            return null;
        } else {
            for (Integer key : currTto.captions.keySet()) {
                //Log.d(TAG, "key: " + key);
                //Log.d(TAG, "value: " + tto.captions.get(key));
                currTto.captions.get(key).content = currTto.captions.get(key).content.replaceAll("<[^>]+>", ""); //删除html标签
                ListItem i = new ListItem(key, currTto.captions.get(key).start, currTto.captions.get(key).content, currTto.captions.get(key).end);
                subtitleList.add(i);
            }
            return subtitleList;
        }
    }

    static ArrayList<ListItem> list() {
        return subtitleList;
    }

    static ListItem get(int id) {
        return subtitleList.get(id);
    }

    static int size() {
        return subtitleList.size();
    }
}
