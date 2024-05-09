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
    static boolean[] currentRecList;

    public static final int GET_BASENAME = 0;
    public static final int GET_VIDEOPATH = 1;
    public static final int GET_SBTPATH = 2;
    public static final int GET_RECPATH = 3;
    public static final int GET_RECEXIST = 4;
    public static final int GET_RECSUM = 5;
    public static final int GET_SBTSUM = 6;

    public static final int SET_RECEXIST = 20 + 1;


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

                        unsortflist.add(new FilesInfo(videoDir, videoSubdir, baseName, videoName, sbtName));
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

    private static boolean[] getVideoRecInfo(String sbtPath, String baseName) {
        int size = Subtitle.getSize(sbtPath);
        boolean[] recinfo = new boolean[size];
        for (int i = 0; i < size; i++) {
            if (fileExist(getRecPath(baseName, i)))
                recinfo[i] = true;
            else
                recinfo[i] = false;
        }
        return recinfo;
    }

    private static String getRecPath(String baseName, int id) {
        return recordDir + baseName + "_" + (id+1) + ".aac";
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

    public static int existInList(String baseName) {
        int size = fileslist.size();
        for (int i = 0; i < size; i++) {
            if (baseName.equals(fileslist.get(i).baseName)) {
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

    public static String getCurrInfo(int type) {
        if (type == GET_VIDEOPATH) {
            return currentLoad.videoDir + currentLoad.videoName;
        } else if (type == GET_SBTPATH) {
            return currentLoad.videoDir + currentLoad.sbtName;
        } else if (type == GET_BASENAME) {
            return currentLoad.baseName;
        } else {
            return null;
        }
    }

    public static int getCurrInfo(int type, String nothing) {
         if (type == GET_RECSUM) {
            int count = 0;
            for (int i = 0; i < currentRecList.length; i++) {
                if (currentRecList[i]) {
                    count++;
                }
            }
            return count;
        } else if (type == GET_SBTSUM) {
            return currentRecList.length;
        } else {
            return 0;
        }
    }

    public static String getCurrInfo(int type, int id) {
        if (type == GET_RECPATH) {
            return getRecPath(currentLoad.baseName, id);
        } else if (type == GET_RECEXIST) {
            if (currentRecList[id]) {
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
            currentRecList[id] = true;
        }
    }

    public static int setCurrentVideo(String baseName) {
        if (baseName == null)
            return -1;

        int idx = existInList(baseName);
        if (idx == -1) {
            return -2;
        }
        currentLoad = fileslist.get(idx);
        currentRecList = getVideoRecInfo(fileslist.get(idx).videoDir + fileslist.get(idx).sbtName, fileslist.get(idx).baseName);
        Preferences.set(currentLoad.baseName, getCurrInfo(GET_RECSUM, ""), getCurrInfo(GET_SBTSUM, ""));

        return 0;
    }

    static public class FilesInfo {
        String videoDir;
        String videoSubDir;
        String baseName;
        String videoName;
        String sbtName;


        FilesInfo(String vd, String vsd, String b, String v, String s) {
            videoDir = vd;
            videoSubDir = vsd;
            baseName = b;
            videoName = v;
            sbtName = s;
        }

        String getVideoDir() {
            return videoDir;
        }

        String getBaseName() {
            return baseName;
        }
    }
}
