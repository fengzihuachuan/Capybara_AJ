package fengzihuachuan.capybara_aj;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import fengzihuachuan.capybara_aj.databinding.ActivityMainBinding;

import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    static String TAG = "MainActivity";

    /* 注意workmode和OptionsMenu中的序列相关联 */
    static final public int WORKMODE_REPEAT = 0;
    static final public int WORKMODE_PLAY = 1;
    public int currentWorkMode = 0;

    private ActivityMainBinding binding;

    public VideoPlayer videoPlayer = null;

    public static int MAINMSG_SCREEN = 0;
    public static int MAINMSG_PLAYER = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        binding.open.setOnClickListener(openOnClickListener);

        Permissions.check(MainActivity.this);
    }

    public void init() {
        Preferences.init(this);
        videoPlayer = new VideoPlayer();
        FileUtils.init();
        resumeLast();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Permissions.onResult(requestCode, grantResults);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (videoPlayer != null) {
            videoPlayer.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        new Handler().postDelayed(new Runnable() {
            public void run() {
                FileUtils.init();
                resumeLast();
            }
        }, 10);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (videoPlayer != null) {
            videoPlayer.release();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, Menu.FIRST + 0, 0, "1").setIcon(R.drawable.ic_launcher_background);
        menu.add(Menu.NONE, Menu.FIRST + 1, 0, "1").setIcon(R.drawable.ic_launcher_background);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        String[] m = {"复读模式", "播放模式"};
        for (int i = 0; i < menu.size(); i++) {
            if (currentWorkMode == i) {
                menu.getItem(i).setTitle("> " + m[i]);
            } else {
                menu.getItem(i).setTitle(m[i]);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == Menu.FIRST + 0) {
            currentWorkMode = WORKMODE_REPEAT;
            Toast.makeText(getApplicationContext(), "进入 复读模式", Toast.LENGTH_LONG).show();
        } else if (id == Menu.FIRST + 1) {
            currentWorkMode = WORKMODE_PLAY;
            Toast.makeText(getApplicationContext(), "进入 播放模式", Toast.LENGTH_LONG).show();
        }

        return super.onOptionsItemSelected(item);
    }

    private View.OnClickListener openOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("请选择文件");

            final String[] filesShow =  new String[FileUtils.size()];
            for (int i = 0; i < FileUtils.size(); i++) {
                String t = Preferences.get(FileUtils.get(i).baseName, 0, 0) + "  -  " + FileUtils.get(i).baseName + "\n" + FileUtils.get(i).videoSubDir;
                filesShow[i] = t.substring(0, Math.min(t.length(), 64)) + "...";
            }

            builder.setSingleChoiceItems(filesShow, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    int ret = loadCurrent(null, which, 0);
                    if (ret != 0) {
                        Toast.makeText(getApplicationContext(), "Files.VideoFiles NULL", Toast.LENGTH_LONG).show();
                    }
                    dialog.dismiss();
                }
            });
            builder.show();
        }
    };

    private void resumeLast() {
        int ret = loadCurrent(Preferences.get(null), -1, Preferences.get(0));
        if (ret != 0) {
            Log.i(TAG, "resumeLast NULL ");
        }
    }

    private int loadCurrent(String baseName, int id, int pos) {
        if (baseName == null && id != -1) {
            baseName = FileUtils.get(id).baseName;
        } else if (id == -1 && baseName != null) {
            id = FileUtils.existInList(baseName);
        } else {
            return -1;
        }

        if (FileUtils.setCurrentVideo(baseName) != 0)
            return -1;

        ((TextView)findViewById(R.id.dirname)).setText(FileUtils.get(id).videoSubDir);
        ((TextView)findViewById(R.id.baseName)).setText(FileUtils.get(id).baseName);

        AudioRecorder.init(MainActivity.this);
        AudioPlayer.init(MainActivity.this);
        videoPlayer.init(MainActivity.this, findViewById(R.id.videosfc));
        ListViewAdapter.initSubtitle(MainActivity.this, findViewById(R.id.subtitlelist), videoPlayer);

        Message msg = new Message();
        msg.what = ListViewAdapter.MSGTYPE_SELECT;
        msg.arg1 = pos;
        ListViewAdapter.listHandler.sendMessage(msg);

        Preferences.set(baseName);
        return 0;
    }

    public Handler mainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MAINMSG_SCREEN) {
                if (msg.arg1 == 0) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            } else if (msg.what == MAINMSG_PLAYER) {
                ProgressBar progressbar = findViewById(R.id.Progressbar);
                int currentpos = msg.arg1;
                progressbar.setProgress(currentpos * 100 / videoPlayer.getDuration());
            }
        }
    };
}