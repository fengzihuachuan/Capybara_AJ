package fengzihuachuan.capybara_aj;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.widget.LinearLayoutCompat;

import java.util.List;


public class SubtitleListAdapter extends ArrayAdapter<Subtitle.Item> {
    static String TAG = "SubtitleListAdapter";

    private int resourceId;
    private static MainActivity context;

    private static int selectIdx = -1;
    private static SubtitleListAdapter subtitleListAdapter;
    private static ListView listView;
    private static VideoPlayer videoPlayer;

    private static ViewHolder currentView;

    public static final int MSGTYPE_SELECTED = 1;
    public static final int MSGTYPE_PROCSTOP = 2;
    public static final int MSGTYPE_RECSTOP = 3;
    public static final int MSGTYPE_PROGRESS = 4;

    private static final int STATUS_NOTHING = 0;
    private static final int STATUS_PLAYING = 1;
    private static final int STATUS_RECORDING = 2;
    private static final int STATUS_REPLAYING = 3;
    private static final int STATUS_COMPAREING = 4;
    private static int currentStatus;

    public SubtitleListAdapter(Context ctx, int resourceId1, List<Subtitle.Item> items) {
        super(ctx, resourceId1, items);
        resourceId = resourceId1;
        context = (MainActivity)ctx;
    }

    public static Handler listHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSGTYPE_SELECTED) {
                subtitleSelected(msg.arg1);
            } else if (msg.what == MSGTYPE_PROCSTOP) {
                updateStatus(STATUS_NOTHING);
            } else if (msg.what == MSGTYPE_RECSTOP) {
                Subtitle.get(msg.arg1).recExist = true;
                ((TextView)context.findViewById(R.id.recsum)).setText(Subtitle.getRecSum()+"");
                currentView.subcontent.setTextColor(context.getColor(R.color.green));
                Preferences.set(Files.getCurrInfo(Files.GET_BASENAME), Subtitle.getRecSum(), Subtitle.size());
            } else if (msg.what == MSGTYPE_PROGRESS) {
                if (msg.arg1 >= 999) {
                    currentView.progressBar.setVisibility(View.INVISIBLE);
                } else {
                    currentView.progressBar.setVisibility(View.VISIBLE);
                    currentView.progressBar.setProgress(msg.arg1/10);
                }
            }
        }
    };

    static void initSubtitle(Context ctx, ListView lv, VideoPlayer vp) {
        context = (MainActivity)ctx;
        listView = lv;
        videoPlayer = vp;

        Subtitle.loadCurrent();

        refreshSubtitle();
    }

    static void refreshSubtitle() {
        subtitleListAdapter = new SubtitleListAdapter(context, R.layout.subtitle_item, Subtitle.list());
        listView.setAdapter(subtitleListAdapter);
        listView.setOnItemClickListener(itemClickListener);
    }

    public static void subtitleSelected(int pos) {
        selectIdx = pos;

        listView.setSelection(pos - 2);
        subtitleListAdapter.notifyDataSetInvalidated();

        ((TextView)context.findViewById(R.id.sbtinfo)).setText((pos + 1) + "/" + Subtitle.size());
        ((TextView)context.findViewById(R.id.recsum)).setText(Subtitle.getRecSum()+"");

        Preferences.set(pos);
    }

    public static void workmodeChange() {
        videoPlayer.pause();
        AudioPlayer.stop();

        if (context.currentWorkMode == context.WORKMODE_REPEAT) {
            currentView.recordlyt.setVisibility(View.VISIBLE);
            updateStatus(STATUS_NOTHING);
        } else {
            currentView.recordlyt.setVisibility(View.GONE);
        }
    }

    private static AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (currentStatus == STATUS_NOTHING) {
                subtitleSelected(position);

                updateStatus(STATUS_PLAYING);
                Subtitle.Item i = Subtitle.get(selectIdx);
                if (context.currentWorkMode == context.WORKMODE_REPEAT) {
                    videoPlayer.play(i.substart.getMseconds(), i.subend.getMseconds());
                } else {
                    videoPlayer.play(i.substart.getMseconds(), -1);
                }
            }
        }
    };

    static void playUpdate(int currentpos) {
        Message msg = new Message();
        msg.what = MainActivity.MAINMSG_PLAYER;
        msg.arg1 = currentpos;
        context.mainHandler.sendMessage(msg);

        if (Subtitle.get(selectIdx+1) == null) {
            return;
        } else {
            if (currentpos > Subtitle.get(selectIdx).subend.getMseconds()) {
                msg = new Message();
                msg.what = MSGTYPE_SELECTED;
                msg.arg1 = selectIdx + 1;
                listHandler.sendMessage(msg);
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        ViewHolder viewHolder;

        Subtitle.Item item = getItem(position);

        if (convertView == null) { //当用户为第一次访问的时候
            view = LayoutInflater.from(getContext()).inflate(resourceId, null);

            viewHolder = new ViewHolder();

            viewHolder.timelyt = view.findViewById(R.id.timelyt);
            viewHolder.substart = view.findViewById(R.id.substart);
            viewHolder.subend = view.findViewById(R.id.subend);

            viewHolder.contentlyt = view.findViewById(R.id.contentlyt);
            viewHolder.subcontent = view.findViewById(R.id.subcontent);

            viewHolder.recordlyt = view.findViewById(R.id.recordlyt);
            viewHolder.progressBar = view.findViewById(R.id.recProgressBar);
            viewHolder.compareBnt = view.findViewById(R.id.compareBnt);
            viewHolder.replayBnt = view.findViewById(R.id.replayBnt);
            viewHolder.recordBnt = view.findViewById(R.id.recordBnt);

            view.setTag(viewHolder); //设置将数据进行缓存
        } else { //第二次访问直接读取第一次访问使存取的数据
            view = convertView;
            viewHolder = (ViewHolder)view.getTag();
        }

        viewHolder.substart.setText(item.substart.toString());
        viewHolder.subcontent.setText(item.subcontent);
        viewHolder.subend.setText(item.subend.toString());

        if (selectIdx == position) {
            currentView = viewHolder;

            viewHolder.timelyt.setVisibility(View.VISIBLE);
            viewHolder.subcontent.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            viewHolder.subcontent.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

            viewHolder.recordlyt.setOnClickListener(noneOnClickListener);
            viewHolder.compareBnt.setOnClickListener(compareBntOnClickListener);
            viewHolder.replayBnt.setOnClickListener(replayBntOnClickListener);
            viewHolder.recordBnt.setOnClickListener(recordBntOnClickListener);

            if (context.currentWorkMode == context.WORKMODE_REPEAT) {
                viewHolder.recordlyt.setVisibility(View.VISIBLE);
            }
        } else {
            viewHolder.timelyt.setVisibility(View.GONE);
            viewHolder.recordlyt.setVisibility(View.GONE);
            viewHolder.subcontent.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
        }

        if (Subtitle.get(position).recExist) {
            viewHolder.subcontent.setTextColor(context.getColor(R.color.green));
        } else {
            viewHolder.subcontent.setTextColor(context.getColor(R.color.black));
        }

        return view;
    }

    private View.OnClickListener noneOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
        }
    };

    private View.OnClickListener compareBntOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            updateStatus(STATUS_COMPAREING);
            Subtitle.Item i = Subtitle.get(selectIdx);
            AudioPlayer.replay(selectIdx);
            videoPlayer.play(i.substart.getMseconds(), i.subend.getMseconds());
        }
    };

    private View.OnClickListener replayBntOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            updateStatus(STATUS_REPLAYING);
            AudioPlayer.replay(selectIdx);
        }
    };

    private View.OnClickListener recordBntOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (currentStatus == STATUS_NOTHING) {
                updateStatus(STATUS_RECORDING);

                Subtitle.Item i = Subtitle.get(selectIdx);
                int period = i.subend.getMseconds() - i.substart.getMseconds() + 400;
                AudioRecorder.start(selectIdx, period);
            } else {
                AudioRecorder.stop();
            }
        }
    };

    private static void updateStatus(int s) {
        currentStatus = s;

        //Log.e(TAG, "updateStatus = " + s);

        if (s == STATUS_NOTHING) {
            if (Subtitle.get(selectIdx).recExist) {
                currentView.compareBnt.setImageResource(R.drawable.compare_available);
                currentView.compareBnt.setEnabled(true);
                currentView.replayBnt.setImageResource(R.drawable.replay_available);
                currentView.replayBnt.setEnabled(true);
                currentView.recordBnt.setImageResource(R.drawable.record_available);
                currentView.recordBnt.setEnabled(true);
            } else {
                currentView.compareBnt.setImageResource(R.drawable.compare_disable);
                currentView.compareBnt.setEnabled(false);
                currentView.replayBnt.setImageResource(R.drawable.replay_disable);
                currentView.replayBnt.setEnabled(false);
                currentView.recordBnt.setImageResource(R.drawable.record_available);
                currentView.recordBnt.setEnabled(true);
            }
        } else if (s == STATUS_PLAYING) {
            //Log.e(TAG, "updateStatus = STATUS_PLAYING");
            currentView.compareBnt.setImageResource(R.drawable.compare_disable);
            currentView.compareBnt.setEnabled(false);
            currentView.replayBnt.setImageResource(R.drawable.replay_disable);
            currentView.replayBnt.setEnabled(false);
            currentView.recordBnt.setImageResource(R.drawable.record_disable);
            currentView.recordBnt.setEnabled(false);
        }  else if (s == STATUS_RECORDING) {
            currentView.compareBnt.setImageResource(R.drawable.compare_disable);
            currentView.compareBnt.setEnabled(false);
            currentView.replayBnt.setImageResource(R.drawable.replay_disable);
            currentView.replayBnt.setEnabled(false);
            currentView.recordBnt.setImageResource(R.drawable.stop_avaiable);
            currentView.recordBnt.setEnabled(true);
        } else if (s == STATUS_REPLAYING) {
            currentView.compareBnt.setImageResource(R.drawable.compare_disable);
            currentView.compareBnt.setEnabled(false);
            currentView.replayBnt.setImageResource(R.drawable.replay_active);
            currentView.replayBnt.setEnabled(false);
            currentView.recordBnt.setImageResource(R.drawable.record_disable);
            currentView.recordBnt.setEnabled(false);
        } else if (s == STATUS_COMPAREING) {
            currentView.compareBnt.setImageResource(R.drawable.compare_active);
            currentView.compareBnt.setEnabled(false);
            currentView.replayBnt.setImageResource(R.drawable.replay_disable);
            currentView.replayBnt.setEnabled(false);
            currentView.recordBnt.setImageResource(R.drawable.record_disable);
            currentView.recordBnt.setEnabled(false);
        } else {
            Log.e(TAG, "unknow status.");
        }
    }

    private static class ViewHolder {
        private LinearLayoutCompat timelyt, contentlyt, recordlyt;
        private TextView substart, subcontent, subend;
        private ImageButton compareBnt, replayBnt, recordBnt;
        private ProgressBar progressBar;
    }
}
