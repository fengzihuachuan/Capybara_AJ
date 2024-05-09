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
    static String dubDir;

    private static ArrayList<FilesInfo> fileslist = new ArrayList<>();
    static FilesInfo currentLoad;

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

            recordDir = resRootDir + "recordings/";
            f = new File(recordDir);
            f.mkdirs();

            dubDir = resRootDir + "dubbing/";
            f = new File(dubDir);
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

    public static String getCurrInfo(int type, int id) {
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
