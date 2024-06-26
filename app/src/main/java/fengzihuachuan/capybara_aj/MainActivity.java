package fengzihuachuan.capybara_aj;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import fengzihuachuan.capybara_aj.databinding.ActivityMainBinding;

import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    static String TAG = "MainActivity";

    static final public int WORKMODE_REPEAT = Menu.FIRST + 0;
    static final public int WORKMODE_PLAY = Menu.FIRST + 1;
    static final public int WORKMODE_DUBBING = Menu.FIRST + 2;
    public int currentWorkMode = Menu.FIRST + 0;

    private ActivityMainBinding binding;

    AlertDialog alertDialogProgress = null;
    AlertDialog alertDialogFileSelect = null;
    public VideoPlayer videoPlayer = null;

    public static int MAINMSG_SCREEN = 0;
    public static int MAINMSG_PLAYER = 1;
    public static int PROGRESS_DISMISS = 2;
    public static int CANNOT_DUB = 3;

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
        Files.init();
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
                if (alertDialogFileSelect != null) {
                    alertDialogFileSelect.dismiss();
                }
                Files.init();
                resumeLast();
            }
        }, 10);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (videoPlayer != null) {
            videoPlayer.reset();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, WORKMODE_REPEAT, 0, "1").setIcon(R.drawable.ic_launcher_background);
        menu.add(Menu.NONE, WORKMODE_PLAY, 0, "1").setIcon(R.drawable.ic_launcher_background);
        menu.add(Menu.NONE, WORKMODE_DUBBING, 0, "1").setIcon(R.drawable.ic_launcher_background);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        String[] m = {"复读模式", "播放模式", "配音模式"};
        for (int i = 0; i < menu.size(); i++) {
            if ((currentWorkMode-Menu.FIRST) == i) {
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

        if (id != currentWorkMode) {
            currentWorkMode = id;
            if (currentWorkMode == WORKMODE_REPEAT) {
                Toast.makeText(getApplicationContext(), "进入 复读模式", Toast.LENGTH_LONG).show();
            } else if (currentWorkMode == WORKMODE_PLAY) {
                Toast.makeText(getApplicationContext(), "进入 播放模式", Toast.LENGTH_LONG).show();
            } else if (currentWorkMode == WORKMODE_DUBBING) {
                Toast.makeText(getApplicationContext(), "进入 配音模式", Toast.LENGTH_LONG).show();

                showProgress();
                Dubbing.go(this);
            }

            SubtitleListAdapter.workModeChange();
        }

        return super.onOptionsItemSelected(item);
    }

    private void showProgress() {
        if (alertDialogProgress == null) {
            alertDialogProgress = new AlertDialog.Builder(this).create();
        }
        View loadView = LayoutInflater.from(this).inflate(R.layout.alert_progress, null);
        alertDialogProgress.setView(loadView, 0, 0, 0, 0);
        alertDialogProgress.setCanceledOnTouchOutside(false);
        alertDialogProgress.show();
    }

    private void dismissProgress() {
        alertDialogProgress.dismiss();
    }

    private View.OnClickListener openOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            View filelist = getLayoutInflater().inflate(R.layout.file_selector, null);
            ListView listView = filelist.findViewById(R.id.fileslistview);
            FilesListAdapter adapter = new FilesListAdapter(MainActivity.this, R.layout.file_item, Files.list());
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    int ret = loadCurrent(null, position, 0);
                    if (ret != 0) {
                        Toast.makeText(getApplicationContext(), "Files.VideoFiles NULL", Toast.LENGTH_LONG).show();
                    }
                    alertDialogFileSelect.dismiss();
                }
            });
            adapter.notifyDataSetChanged();
            builder.setView(filelist);
            alertDialogFileSelect = builder.create();
            alertDialogFileSelect.show();
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
            baseName = Files.get(id).baseName;
        } else if (id == -1 && baseName != null) {
            id = Files.existInList(baseName);
        } else {
            return -1;
        }

        if (Files.setCurrent(baseName) != 0)
            return -1;

        ((TextView)findViewById(R.id.dirname)).setText(Files.get(id).videoSubDir);
        ((TextView)findViewById(R.id.baseName)).setText(Files.get(id).baseName);

        AudioRecorder.init(MainActivity.this);
        AudioPlayer.init(MainActivity.this);
        videoPlayer.init(MainActivity.this, findViewById(R.id.videosfc));
        SubtitleListAdapter.initSubtitle(MainActivity.this, findViewById(R.id.subtitlelist), videoPlayer, pos);

        currentWorkMode = WORKMODE_REPEAT;

        Preferences.set(baseName);
        Preferences.set(baseName, Subtitle.getRecSum(), Subtitle.size());
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
            } else if (msg.what == PROGRESS_DISMISS) {
                dismissProgress();
            } else if (msg.what == CANNOT_DUB) {
                Toast.makeText(getApplicationContext(), "配音模式不可用", Toast.LENGTH_LONG).show();
                currentWorkMode = Menu.FIRST + 0;
            }
        }
    };
}