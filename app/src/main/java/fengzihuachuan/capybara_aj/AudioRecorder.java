package fengzihuachuan.capybara_aj;

import android.media.MediaRecorder;
import android.os.Message;
import android.util.Log;

import java.io.IOException;

public class AudioRecorder {
    static String TAG = "AudioRecorder";

    static MediaRecorder mMediaRecorder;

    static int asource =  MediaRecorder.AudioSource.MIC;
    static int aformat =  MediaRecorder.OutputFormat.AAC_ADTS;
    static int aencoder = MediaRecorder.AudioEncoder.AAC;
    static int achannel = 1;
    static int abitrate = 96000;
    static int asamplerate = 44100;

    private static boolean isRecording = false;
    private static int recSbtId = -1;

    public static void init(MainActivity m) {
    }

    public static void start(int sbtidx, int period) {
        recSbtId = sbtidx;
        _start();

        new Thread() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Thread: ");

                    isRecording = true;
                    long start = System.currentTimeMillis();
                    while (isRecording) {
                        if (System.currentTimeMillis() - start > period) {
                            Message msg = new Message();
                            msg.what = ListViewAdapter.MSGTYPE_PROGRESS;
                            msg.arg1 = (int) (1000);
                            ListViewAdapter.listHandler.sendMessage(msg);
                            isRecording = false;
                            break;
                        } else {
                            Message msg = new Message();
                            msg.what = ListViewAdapter.MSGTYPE_PROGRESS;
                            msg.arg1 = (int) ((System.currentTimeMillis() - start) * 1000 / period);
                            ListViewAdapter.listHandler.sendMessage(msg);

                            sleep(20);
                        }
                    }

                    AudioRecorder._stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private static void _start() {
        mMediaRecorder = new MediaRecorder();

        try {
            mMediaRecorder.setAudioSource(asource);
            mMediaRecorder.setOutputFormat(aformat);
            mMediaRecorder.setAudioEncoder(aencoder);
            mMediaRecorder.setAudioChannels(achannel);
            mMediaRecorder.setAudioEncodingBitRate(abitrate);
            mMediaRecorder.setAudioSamplingRate(asamplerate);
            mMediaRecorder.setOutputFile(FileUtils.getCurrInfo(FileUtils.GET_RECPATH, recSbtId));
            mMediaRecorder.prepare();
            AudioPlayer.playBeep(0);
            mMediaRecorder.start();

            Message msg = new Message();
            msg.what = ListViewAdapter.MSGTYPE_RECSTART;
            ListViewAdapter.listHandler.sendMessage(msg);
        } catch (IllegalStateException e) {
            Log.d(TAG, "record failed!" + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "record failed!" + e.getMessage());
        }

        Log.d(TAG, "record start!");
    }

    public static void stop() {
        if (isRecording == true) {
            isRecording = false;
            return;
        }
    }

    private static void _stop() {
        try {
            mMediaRecorder.stop();
            AudioPlayer.playBeep(1);
            mMediaRecorder.release();
        } catch (RuntimeException e) {
            Log.d(TAG, "stopRecord failed!" + e.getMessage());
            mMediaRecorder.reset();
            mMediaRecorder.release();
        }
        mMediaRecorder = null;

        Message msg = new Message();
        msg.what = ListViewAdapter.MSGTYPE_RECSTOP;
        msg.arg1 = recSbtId;
        ListViewAdapter.listHandler.sendMessage(msg);
    }
}
