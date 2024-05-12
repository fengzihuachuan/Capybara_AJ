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

    public void init(MainActivity m, SurfaceView sfv) {
        main = m;
        videopath = Files.getCurrInfo(Files.GET_VIDEOPATH);
        videoSuf = sfv;

        videoSuf.setOnTouchListener(this);

        surfaceHolder = videoSuf.getHolder();
        surfaceHolder.addCallback(this);

        try {
            if (mPlayer != null) {
                mPlayer.stop();
                mPlayer.reset();
                mPlayer = null;
            }

            mPlayer = new MediaPlayer();
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
        if (mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.pause();
        }
    }

    public void reset() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.reset();
        }
    }

    public int getDuration() {
        return mPlayer.getDuration();
    }

    public void play(int start, int end) {
        startPos = start;
        endPos = end;

        try {
            if (mPlayer.isPlaying()) {
                mPlayer.pause();
            }
            mPlayer.reset();

            Uri playUri;
            if (main.currentWorkMode == MainActivity.WORKMODE_DUBBING) {
                playUri = Uri.parse(Dubbing.getout());
            } else {
                playUri = Uri.parse(videopath);
            }
            mPlayer.setDisplay(surfaceHolder);
            mPlayer.setDataSource(main, playUri);
            mPlayer.prepare();
            mPlayer.seekTo(start, MediaPlayer.SEEK_CLOSEST);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Log.d(TAG, "play - start: " + TimeFmt.strFromMs(start) + " end: " + TimeFmt.strFromMs(end));
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
        //Log.d(TAG, "onPrepared: ");
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        //Log.d(TAG, "onCompletion: ");
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
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
                        if (main.currentWorkMode == main.WORKMODE_REPEAT) {
                            if (currentpos > endPos) {
                                mPlayer.pause();
                                break;
                            }
                            msg = new Message();
                            msg.what = MainActivity.MAINMSG_PLAYER;
                            msg.arg1 = currentpos;
                            main.mainHandler.sendMessage(msg);
                        } else {
                            SubtitleListAdapter.playUpdate(currentpos);
                        }
                        sleep(100);
                    }

                    msg = new Message();
                    msg.what = SubtitleListAdapter.MSGTYPE_PROCSTOP;
                    SubtitleListAdapter.listHandler.sendMessage(msg);

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

