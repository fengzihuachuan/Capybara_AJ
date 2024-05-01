package fengzihuachuan.capybara_aj;

import static java.lang.Thread.sleep;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

public class VideoPlayer implements SurfaceHolder.Callback,
        View.OnTouchListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnVideoSizeChangedListener
{
    static String TAG = "VideoPlayer";

    static MainActivity main;
    static String videopath;
    SurfaceView videoSuf;
    static SurfaceHolder surfaceHolder;
    static MediaPlayer mPlayer = null;

    static int startPos = 0, endPos = 0;
    static int playmode = 0;

    public static int PLAYMODE_WHOLE = 0;
    public static int PLAYMODE_PART = 1;
    public static int PLAYMODE_PART_COMBINE = 2;

    void init(MainActivity m, SurfaceView sfv) {
        main = m;
        videopath = FileUtils.getCurrInfo(FileUtils.GET_VIDEOPATH);
        videoSuf = sfv;

        videoSuf.setOnTouchListener(this);

        surfaceHolder = videoSuf.getHolder();
        surfaceHolder.addCallback(this);

        try {
            if (mPlayer != null) {
                mPlayer.stop();
                mPlayer.release();
                mPlayer = null;
            }

            Uri playUri = Uri.parse(videopath);
            mPlayer = MediaPlayer.create(main, playUri);

            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnErrorListener(this);
            mPlayer.setOnInfoListener(this);
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnSeekCompleteListener(this);
            mPlayer.setOnVideoSizeChangedListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        if (mPlayer != null) {
            mPlayer.pause();
        }
    }

    public void release() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
        }
    }

    public int getDuration() {
        return mPlayer.getDuration();
    }

    public void play(int start, int end, int mode) {
        startPos = start;
        endPos = end;
        playmode = mode;
        try {
            if (mPlayer.isPlaying()) {
                mPlayer.pause();
                sleep(100);
            }

            mPlayer.setDisplay(surfaceHolder);
            mPlayer.seekTo(start, MediaPlayer.SEEK_CLOSEST);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "play - start: " + TimeFmt.strFromMs(start) + " end: " + TimeFmt.strFromMs(end));
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(v.getId() == R.id.videosfc) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (mPlayer != null && mPlayer.isPlaying()) {
                    mPlayer.pause();
                }
            }
        }
        return false;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mPlayer.setSurface(videoSuf.getHolder().getSurface());
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "onPrepared: ");
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "onCompletion: ");
        Toast.makeText(main, "视频播放完毕", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        Log.d(TAG, "onSeekComplete: ");
        Message msg = new Message();
        msg.what = ListViewAdapter.MSGTYPE_VIDEOSTART;
        msg.arg1 = playmode;
        ListViewAdapter.listHandler.sendMessage(msg);

        mPlayer.start();

        new Thread() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Thread: ");

                    // keep screen on
                    Message msg = new Message();
                    msg.what = MainActivity.MAINMSG_SCREEN;
                    msg.arg1 = 1;
                    main.mainHandler.sendMessage(msg);

                    while (mPlayer.isPlaying()) {
                        int currentpos = mPlayer.getCurrentPosition();
                        if (playmode == PLAYMODE_WHOLE) {
                            ListViewAdapter.playUpdate(currentpos);
                        } else {
                            if (currentpos > endPos) {
                                mPlayer.pause();
                                break;
                            }
                            msg = new Message();
                            msg.what = MainActivity.MAINMSG_PLAYER;
                            msg.arg1 = currentpos;
                            main.mainHandler.sendMessage(msg);
                        }
                        sleep(100);
                    }

                    msg = new Message();
                    msg.what = ListViewAdapter.MSGTYPE_VIDEOSTOP;
                    msg.arg1 = playmode;
                    ListViewAdapter.listHandler.sendMessage(msg);

                    // quit keeping screen on
                    msg = new Message();
                    msg.what = MainActivity.MAINMSG_SCREEN;
                    msg.arg1 = 0;
                    main.mainHandler.sendMessage(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {}

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }
}

