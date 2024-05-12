package fengzihuachuan.capybara_aj;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;

public class Files {
    static String TAG = "FileUtils";

    static String RootDirName = "CPBRs";
    static String RecDir = "recordings";
    static String DubDir = "dubbing";

    static String resRootDir;
    static String recordDir;
    static String dubDir;

    private static ArrayList<Info> fileslist = new ArrayList<>();
    private static Info currentLoad = null;

    public static final int GET_BASENAME = 0;
    public static final int GET_VIDEOPATH = 1;
    public static final int GET_SBTPATH = 2;
    public static final int GET_RECPATH = 3;


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

            recordDir = resRootDir + RecDir + "/";
            f = new File(recordDir);
            f.mkdirs();

            dubDir = resRootDir + DubDir + "/";
            f = new File(dubDir);
            f.mkdirs();

            getVideoFiles();
        } catch (Exception e) {
            Log.e(TAG, "load dir - error");
        }
    }

    private static void getVideoFiles() {
        ArrayList<Info> unsortflist = new ArrayList<>();
        try {
            java.nio.file.Files.walkFileTree(Paths.get(resRootDir), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String videoDir = file.toString().substring(0, file.toString().lastIndexOf('/') + 1);
                    String videoSubdir = videoDir.substring(videoDir.lastIndexOf(RootDirName) + RootDirName.length() + 1, videoDir.length());
                    String videoName = file.toString().substring(file.toString().lastIndexOf('/') + 1, file.toString().length());

                    if (videoSubdir.equals(RecDir) || videoSubdir.equals(DubDir)) {
                        return FileVisitResult.CONTINUE;
                    }

                    if (videoName.endsWith(".mp4") || videoName.endsWith(".mkv") ||
                            videoName.endsWith(".avi") || videoName.endsWith(".webm")) {

                        String baseName = videoName.substring(0, videoName.lastIndexOf('.'));

                        String sbtName = null;
                        String t1 = baseName + ".srt";
                        String t2 = baseName + ".ass";
                        File tf1 = new File(videoDir + t1);
                        File tf2 = new File(videoDir + t2);
                        if (tf1.exists() || tf2.exists()) {
                            if (tf2.exists())
                                sbtName = t2;
                            if (tf1.exists())
                                sbtName = t1;

                            unsortflist.add(new Info(videoDir, videoSubdir, baseName, videoName, sbtName, Preferences.get(baseName, 0, 0)));
                        }
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

    public static boolean getRecStatus(int id) {
        return fileExist(getCurrInfo(GET_RECPATH, id));
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

    private static ArrayList<Info> sortList(ArrayList<Info> unsortlist) {
        unsortlist.sort(Comparator.comparing(Info::getVideoDir).thenComparing(Info::getBaseName));
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

    public static Info get(int id) {
        return fileslist.get(id);
    }

    public static ArrayList<Info> list() {
        return fileslist;
    }

    public static int size() {
        return fileslist.size();
    }

    public static String getCurrInfo(int type) {
        if (currentLoad == null)
            return null;

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

    public static String getCurrInfo(int type, int id) {
        if (currentLoad == null)
            return null;

        if (type == GET_RECPATH) {
            return recordDir + currentLoad.baseName + "_" + (id+1) + ".aac";
        } else {
            return null;
        }
    }

    public static int setCurrent(String baseName) {
        if (baseName == null)
            return -1;

        int idx = existInList(baseName);
        if (idx == -1) {
            return -2;
        }
        currentLoad = fileslist.get(idx);

        return 0;
    }

    static public class Info {
        String videoDir;
        String videoSubDir;
        String baseName;
        String videoName;
        String sbtName;
        String recInfo;

        Info(String vd, String vsd, String b, String v, String s, String rec) {
            videoDir = vd;
            videoSubDir = vsd;
            baseName = b;
            videoName = v;
            sbtName = s;
            recInfo = rec;
        }

        String getVideoDir() {
            return videoDir;
        }

        String getBaseName() {
            return baseName;
        }
    }
}
