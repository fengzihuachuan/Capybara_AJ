package fengzihuachuan.capybara_aj;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;

public class FileUtils {
    static String TAG = "FileUtils";

    static String RootDirName = "CPBRs";

    static String resRootDir;
    static String recordDir;

    private static ArrayList<FilesInfo> fileslist = new ArrayList<>();
    static FilesInfo currentLoad;

    //public static final int GET_VIDOENAME = 0;
    public static final int GET_VIDEOPATH = 1;
    //public static final int GET_SBTNAME = 2;
    public static final int GET_SBTPATH = 3;
    //public static final int GET_SUBDIR = 4;
    //public static final int GET_RECDIR = 5;
    public static final int GET_RECPATH = 6;
    public static final int GET_RECEXIST = 7;
    public static final int SET_RECEXIST = 8;

    public static void init() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.e(TAG, "getExternalStorageState - ");
            return;
        }
        try {
            File sdCard = Environment.getExternalStorageDirectory();
            resRootDir = sdCard.getCanonicalPath() + "/" + RootDirName +  "/";
            File f = new File(resRootDir);
            f.mkdirs();
            recordDir = resRootDir + "recordings/";
            f = new File(recordDir);
            f.mkdirs();

            getVideoFiles();
        } catch (Exception e) {
            Log.e(TAG, "load dir - error");
        }
    }

    private static void getVideoFiles() {
        ArrayList<FilesInfo> unsortflist = new ArrayList<>();
        try {
            Files.walkFileTree(Paths.get(resRootDir), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String videoDir = file.toString().substring(0, file.toString().lastIndexOf('/') + 1);
                    String videoSubdir = videoDir.substring(videoDir.lastIndexOf(RootDirName) + RootDirName.length() + 1, videoDir.length());
                    String videoName = file.toString().substring(file.toString().lastIndexOf('/') + 1, file.toString().length());
                    if (videoName.endsWith(".mp4") || videoName.endsWith(".mkv") ||
                            videoName.endsWith(".avi") || videoName.endsWith(".webm")) {

                        String baseName = videoName.substring(0, videoName.lastIndexOf('.'));

                        String sbtName = null;
                        String t;
                        File sf = null;

                        t = baseName + ".srt";
                        sf = new File(videoDir + t);
                        if (sf.exists()) {
                            sbtName = t;
                        }

                        t = baseName + ".ass";
                        sf = new File(videoDir + t);
                        if (sf.exists()) {
                            sbtName = t;
                        }

                        unsortflist.add(new FilesInfo(videoDir, videoSubdir, baseName, videoName, sbtName, getVideoRecInfo(videoDir + sbtName, videoName)));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            fileslist.clear();
            fileslist = sortList(unsortflist);
            return;
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    private static boolean[] getVideoRecInfo(String sbtPath, String videoName) {
        int size = Subtitle.getSize(sbtPath);
        boolean[] recinfo = new boolean[size];
        for (int i = 0; i < size; i++) {
            if (fileExist(getRecPath(videoName, i)))
                recinfo[i] = true;
            else
                recinfo[i] = false;
        }
        return recinfo;
    }

    private static String getRecPath(String videoName, int id) {
        return recordDir + videoName + "_" + (id+1) + ".aac";
    }

    public static int currRecSum() {
        int count = 0;
        for (int i = 0; i < Subtitle.size(); i++) {
            if (fileExist(getRecPath(currentLoad.videoName, i))) {
                count += 1;
            }
        }
        return count;
    }

    public static boolean fileExist(String path) {
        if (path == null)
            return false;

        File f;
        try {
            f = new File(path);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        if (f == null || (!f.exists())) {
            return false;
        }
        return true;
    }

    private static ArrayList<FilesInfo> sortList(ArrayList<FilesInfo> unsortlist) {
        unsortlist.sort(Comparator.comparing(FilesInfo::getVideoDir).thenComparing(FilesInfo::getBaseName));
        return unsortlist;
    }

    public static int existInList(String videoName) {
        int size = fileslist.size();
        for (int i = 0; i < size; i++) {
            if (videoName.equals(fileslist.get(i).videoName)) {
                return i;
            }
        }

        return -1;
    }

    public static FilesInfo get(int id) {
        return fileslist.get(id);
    }

    public static int size() {
        return fileslist.size();
    }

    public static int getRecSum(int id) {
        int count = 0;
        for (int i = 0; i < fileslist.get(id).recList.length; i++) {
            if (fileslist.get(id).recList[i]) {
                count++;
            }
        }
        return count;
    }

    public static String getCurrInfo(int type) {
        if (type == GET_VIDEOPATH) {
            return currentLoad.videoDir + currentLoad.videoName;
        } else if (type == GET_SBTPATH) {
            return currentLoad.videoDir + currentLoad.sbtName;
        } else {
            return null;
        }
    }

    public static String getCurrInfo(int type, int id) {
        if (type == GET_RECPATH) {
            return getRecPath(currentLoad.videoName, id);
        } else if (type == GET_RECEXIST) {
            if (currentLoad.recList[id]) {
                return "";
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static void setCurrInfo(int type, int id) {
        if (type == SET_RECEXIST) {
            currentLoad.recList[id] = true;
            return;
        } else {
            return;
        }
    }

    public static int setCurrentVideo(String videoName) {
        if (videoName == null)
            return -1;

        int idx = existInList(videoName);
        if (idx == -1) {
            return -2;
        }
        currentLoad = fileslist.get(idx);

        return 0;
    }

    static public class FilesInfo {
        String videoDir;
        String videoSubDir;
        String baseName;
        String videoName;
        String sbtName;
        boolean[] recList;

        FilesInfo(String vd, String vsd, String b, String v, String s, boolean[] rl) {
            videoDir = vd;
            videoSubDir = vsd;
            baseName = b;
            videoName = v;
            sbtName = s;
            recList = rl;
        }

        String getVideoDir() {
            return videoDir;
        }

        String getBaseName() {
            return baseName;
        }
    }
}
