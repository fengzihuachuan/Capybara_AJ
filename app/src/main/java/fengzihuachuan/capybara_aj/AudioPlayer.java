package fengzihuachuan.capybara_aj;

import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Message;

import java.io.IOException;

public class AudioPlayer {
    static String TAG = "AudioPlayer";

    private static MediaPlayer player;

    private static SoundPool spl;
    private static int[] splid = new int[4];

    static MainActivity main;

    public static void init(MainActivity m) {
        main = m;
        initBeep();
    }

    public static void replay(int sbtidx) {
        try {
            MediaPlayer player = new MediaPlayer();
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    player.release();

                    Message msg = new Message();
                    msg.what = ListViewAdapter.MSGTYPE_REPLAYSTOP;
                    ListViewAdapter.listHandler.sendMessage(msg);
                }
            });
            player.setDataSource(FileUtils.getCurrInfo(FileUtils.GET_RECPATH, sbtidx));
            player.prepare();
            player.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void initBeep() {
        spl = new SoundPool.Builder()
                .setMaxStreams(10)
                .build();
        splid[0] = spl.load(main, R.raw.start, 2);
        splid[1] = spl.load(main, R.raw.end, 2);
    }

    public static void playBeep(int id) {
        spl.play(splid[id], 1, 1, 2, 0, 1);
        try {
            Thread.sleep(210);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
