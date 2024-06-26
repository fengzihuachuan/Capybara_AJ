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

        mMediaRecorder = new MediaRecorder();

        try {
            mMediaRecorder.setAudioSource(asource);
            mMediaRecorder.setOutputFormat(aformat);
            mMediaRecorder.setAudioEncoder(aencoder);
            mMediaRecorder.setAudioChannels(achannel);
            mMediaRecorder.setAudioEncodingBitRate(abitrate);
            mMediaRecorder.setAudioSamplingRate(asamplerate);
            mMediaRecorder.setOutputFile(Files.getCurrInfo(Files.GET_RECPATH, recSbtId));
            mMediaRecorder.prepare();
            AudioPlayer.playBeep(0);
            mMediaRecorder.start();

            new Thread() {
                @Override
                public void run() {
                    try {
                        Log.d(TAG, "Thread: ");

                        isRecording = true;
                        long start = System.currentTimeMillis();
                        while (isRecording) {
                            Message msg = new Message();
                            msg.what = SubtitleListAdapter.MSGTYPE_PROGRESS;

                            if (System.currentTimeMillis() - start > period) {
                                msg.arg1 = (int) (1000);
                                SubtitleListAdapter.listHandler.sendMessage(msg);

                                isRecording = false;
                                break;
                            } else {
                                msg.arg1 = (int) ((System.currentTimeMillis() - start) * 1000 / period);
                                SubtitleListAdapter.listHandler.sendMessage(msg);

                                sleep(100);
                            }
                        }

                        AudioRecorder._stop();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
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
        msg.what = SubtitleListAdapter.MSGTYPE_RECSTOP;
        msg.arg1 = recSbtId;
        SubtitleListAdapter.listHandler.sendMessage(msg);

        msg = new Message();
        msg.what = SubtitleListAdapter.MSGTYPE_PROCSTOP;
        msg.arg1 = recSbtId;
        SubtitleListAdapter.listHandler.sendMessage(msg);
    }
}
