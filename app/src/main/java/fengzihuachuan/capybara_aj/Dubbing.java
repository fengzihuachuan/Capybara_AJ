package fengzihuachuan.capybara_aj;

import android.os.Message;
import android.util.Log;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback;
import com.arthenica.ffmpegkit.LogCallback;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.SessionState;
import com.arthenica.ffmpegkit.Statistics;
import com.arthenica.ffmpegkit.StatisticsCallback;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import fengzihuachuan.capybara_aj.subtitle.Time;

public class Dubbing {
    static String TAG = "Dubbing";

    static MainActivity main = null;

    static String rootdir;
    static String tmpdir;
    static String outmux;
    static String accomp;
    static String vocals;

    static ArrayList<String> spiltcmd = new ArrayList<String>();

    public static String getout() {
        return rootdir + "mux.mp4";
    }

    public static void go(MainActivity m) {
        main = m;

        rootdir = Files.dubDir + Files.getCurrInfo(Files.GET_BASENAME) + "/";
        tmpdir = rootdir + "tmp/";
        outmux = rootdir + "mux.mp4";
        accomp = rootdir + "accompaniment.mp3";
        vocals = rootdir + "vocals.mp3";

        if (!check()) {
            Message msg = new Message();
            msg.what = MainActivity.PROGRESS_DISMISS;
            main.mainHandler.sendMessage(msg);

            msg = new Message();
            msg.what = MainActivity.CANNOT_DUB;
            main.mainHandler.sendMessage(msg);
            return;
        }

        if (!need()) {
            Message msg = new Message();
            msg.what = MainActivity.PROGRESS_DISMISS;
            main.mainHandler.sendMessage(msg);
            return;
        }

        createDir(tmpdir);

        gen();

        //Log.d(TAG, "exec: " + spiltcmd.get(0));
        FFmpegKit.executeAsync(spiltcmd.get(0), ffmpegSessionCompleteCallback, logCallback, statisticsCallback);
    }

    private static boolean check() {
        File f;

        f = new File(rootdir);
        if (!f.exists())
            return false;

        f = new File(accomp);
        if (!f.exists())
            return false;

        f = new File(vocals);
        if (!f.exists())
            return false;

        return true;
    }

    private static boolean need() {
        File m = new File(outmux);
        for (int i = 0; i < Subtitle.size(); i++) {
            if (Subtitle.get(i).recExist) {
                File f = new File(Files.getCurrInfo(Files.GET_RECPATH, i));
                //Log.d(TAG, "m.lastModified() " + m.lastModified());
                //Log.d(TAG, "f.lastModified() " + f.lastModified());
                if (m.lastModified() < f.lastModified()) {
                    Log.d(TAG, "need: true ");
                    return true;
                }
            }
        }
        return false;
    }

    private static void gen() {
        String mixcmd = "";
        String mergecmd = "";

        String common = " -y -hide_banner ";
        String mixout = tmpdir + "mix.aac";

        String ss1 = common + " -i " + accomp;
        String ss2 = "";
        String ss3 = "[0:a]";
        for (int i = 0; i < Subtitle.size(); i++) {
            int id = i + 1;

            if (Subtitle.get(i).recExist) {
                ss1 = ss1 + " -i " + Files.getCurrInfo(Files.GET_RECPATH, i);
            } else {
                String tmppath = tmpdir + id + ".mp3";

                Time d = new Time("", "");
                d.setMseconds(Subtitle.get(i).subend.getMseconds() - Subtitle.get(i).substart.getMseconds());
                String duration = d.toString();

                String ss = common + " -i " + vocals + " -vn -acodec copy " + " -ss " + Subtitle.get(i).substart.toString() + " -t " + duration + " " + tmppath;
                spiltcmd.add(ss);

                ss1 = ss1 + " -i " + tmppath;
            }
            ss2 = ss2 + "[" + id + ":a]" + "volume=1," + "adelay=" + Subtitle.get(i).substart.getMseconds() + ":all=true" + "[a" + id + "];";
            ss3 = ss3 + "[a" + id + "]";
        }

        mixcmd = ss1 + " -filter_complex \"" + ss2 + " " + ss3 + " amix=inputs=" + (Subtitle.size()+1) + ":duration=first:dropout_transition=0:normalize=0\" -c:a aac -b:a 128k " + mixout;

        mergecmd = common + " -i " + Files.getCurrInfo(Files.GET_VIDEOPATH) + " -i " + mixout  + " -c:v copy -map 0:v:0 -map 1:a:0 " + outmux;

        spiltcmd.add(mixcmd);
        spiltcmd.add(mergecmd);
    }

    static FFmpegSessionCompleteCallback ffmpegSessionCompleteCallback = new FFmpegSessionCompleteCallback() {
        @Override
        public void apply(FFmpegSession session) {
            SessionState state = session.getState();
            ReturnCode returnCode = session.getReturnCode();

            //Log.d(TAG, "state " + state + " returnCode " + returnCode);

            spiltcmd.remove(0);
            if (spiltcmd.size() == 0) {
                Log.d(TAG, "all done");
                deleteDir(tmpdir);

                Message msg = new Message();
                msg.what = MainActivity.PROGRESS_DISMISS;
                main.mainHandler.sendMessage(msg);
            } else {
                //Log.d(TAG, "exec: " + spiltcmd.get(0));
                FFmpegKit.executeAsync(spiltcmd.get(0), ffmpegSessionCompleteCallback, logCallback, statisticsCallback);
            }
        }
    };

    static LogCallback logCallback = new LogCallback() {
        @Override
        public void apply(com.arthenica.ffmpegkit.Log log) {
            //Log.d(TAG, "log: " + log.getMessage());
        }
    };

    static StatisticsCallback statisticsCallback = new StatisticsCallback() {
        @Override
        public void apply(Statistics statistics) {
        }
    };

    private static void logfile(String content) {
        try {
            File file = new File(rootdir + "log.txt");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.append(content);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createDir(String path) {
        File f = new File(path);
        f.mkdirs();
    }

    public static void deleteDir(final String path) {
        File dir = new File(path);
        deleteDirWithFile(dir);
    }

    public static void deleteDirWithFile(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory())
            return;
        for (File file : dir.listFiles()) {
            if (file.isFile())
                file.delete();
            else if (file.isDirectory())
                deleteDirWithFile(file);
        }
        dir.delete();
    }
}
