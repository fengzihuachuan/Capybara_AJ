package fengzihuachuan.capybara_aj;

import android.content.Context;
import android.content.SharedPreferences;
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


public class ListViewAdapter extends ArrayAdapter<ListItem> {
    static String TAG = "ListViewAdapter";

    private int resourceId;

    private static int selectIdx = -1;
    private static MainActivity context;
    private static ListViewAdapter listViewAdapter;
    private static ListView listView;
    private static VideoPlayer videoPlayer;

    private static ViewHolder currentView;

    private static boolean isRecording = false;
    private static boolean disableCurrSub = false;

    public static int MSGTYPE_SELECT = 0;
    public static int MSGTYPE_VIDEOSTART = 1;
    public static int MSGTYPE_VIDEOSTOP = 2;
    public static int MSGTYPE_REPLAYSTART = 3;
    public static int MSGTYPE_REPLAYSTOP = 4;
    public static int MSGTYPE_RECSTART = 5;
    public static int MSGTYPE_RECSTOP = 6;
    public static int MSGTYPE_PROGRESS = 7;

    public static int RECBTN_DISABLE = 0;
    public static int RECBTN_HAVEREC = 1;
    public static int RECBTN_NOREC = 2;
    public static int RECBTN_RECORDING = 3;
    public static int RECBTN_REPLAYING = 4;
    public static int RECBTN_COMPAREING = 5;

    public ListViewAdapter(Context ctx, int resourceId1, List<ListItem> listItems) {
        super(context, resourceId1, listItems);
        resourceId = resourceId1;
    }

    public static Handler listHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSGTYPE_SELECT) {
                selectIdx = msg.arg1;

                listView.setSelection(msg.arg1 - 2);
                listViewAdapter.setSelectItem(msg.arg1);
                listViewAdapter.notifyDataSetInvalidated();

                ((TextView)context.findViewById(R.id.sbtinfo)).setText((msg.arg1 + 1) + "/" + Subtitle.size());
                ((TextView)context.findViewById(R.id.recsum)).setText(FileUtils.currRecSum()+"");

                if (MainActivity.sharedPref != null) {
                    SharedPreferences.Editor editor = MainActivity.sharedPref.edit();
                    editor.putInt("lastPos", msg.arg1);
                    editor.apply();
                }
            } else if (msg.what == MSGTYPE_VIDEOSTART) {
                if (msg.arg1 == VideoPlayer.PLAYMODE_PART_COMBINE) {
                    setRecButtons(RECBTN_COMPAREING);
                } else {
                    setRecButtons(RECBTN_DISABLE);
                }
            } else if (msg.what == MSGTYPE_VIDEOSTOP) {
                if (msg.arg1 == VideoPlayer.PLAYMODE_PART) {
                    resetRecButtons();
                } else if (msg.arg1 == VideoPlayer.PLAYMODE_PART_COMBINE) {
                    AudioPlayer.replay(selectIdx);
                }
            } else if (msg.what == MSGTYPE_REPLAYSTART) {

            } else if (msg.what == MSGTYPE_REPLAYSTOP) {
                resetRecButtons();
            } else if (msg.what == MSGTYPE_RECSTART) {
                setRecButtons(RECBTN_RECORDING);
                isRecording = true;
            } else if (msg.what == MSGTYPE_RECSTOP) {
                resetRecButtons();
                isRecording = false;
                FileUtils.setCurrInfo(FileUtils.SET_RECEXIST, msg.arg1);
                ((TextView)context.findViewById(R.id.recsum)).setText(FileUtils.currRecSum()+"");
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

    public void setSelectItem(int s) {
        this.selectIdx = s;
    }

    static void initSubtitle(Context ctx, ListView lv, VideoPlayer vp) {
        context = (MainActivity)ctx;
        listView = lv;
        videoPlayer = vp;

        Subtitle.loadCurrent();

        refreshSubtitle();
    }

    static void refreshSubtitle() {
        listViewAdapter = new ListViewAdapter(context, R.layout.listview, Subtitle.list());
        listView.setAdapter(listViewAdapter);
        listView.setOnItemClickListener(itemClickListener);
    }

    private static AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (selectIdx != position) {
                Message msg = new Message();
                msg.what = ListViewAdapter.MSGTYPE_SELECT;
                msg.arg1 = position;
                ListViewAdapter.listHandler.sendMessage(msg);
            } else {
                if (!disableCurrSub) {
                    ListItem i = Subtitle.get(selectIdx);
                    if (context.currentWorkMode == context.WORKMODE_PLAY) {
                        videoPlayer.play(i.getSubStart().getMseconds(), -1, VideoPlayer.PLAYMODE_WHOLE);
                    } else {
                        videoPlayer.play(i.getSubStart().getMseconds(), i.getSubEnd().getMseconds(), VideoPlayer.PLAYMODE_PART);
                    }
                }
            }
        }
    };

    static void playUpdate(int currentpos) {
        if (currentpos > Subtitle.get(selectIdx).getSubEnd().getMseconds()) {
            Message msg = new Message();
            msg.what = ListViewAdapter.MSGTYPE_SELECT;
            msg.arg1 = selectIdx + 1;
            ListViewAdapter.listHandler.sendMessage(msg);
            selectIdx = selectIdx + 1;
        }
        Message msg = new Message();
        msg.what = MainActivity.MAINMSG_PLAYER;
        msg.arg1 = currentpos;
        context.mainHandler.sendMessage(msg);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        ViewHolder viewHolder;

        ListItem listItem = getItem(position);

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

        viewHolder.substart.setText(listItem.getSubStart().toString());
        viewHolder.subcontent.setText(listItem.getSubContent());
        viewHolder.subend.setText(listItem.getSubEnd().toString());

        if (selectIdx == position) {
            currentView = viewHolder;

            viewHolder.timelyt.setVisibility(View.VISIBLE);
            viewHolder.subcontent.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            viewHolder.subcontent.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

            if (context.currentWorkMode == context.WORKMODE_REPEAT) {
                viewHolder.recordlyt.setVisibility(View.VISIBLE);
                viewHolder.recordlyt.setOnClickListener(noneOnClickListener);
                viewHolder.compareBnt.setOnClickListener(compareBntOnClickListener);
                viewHolder.replayBnt.setOnClickListener(replayBntOnClickListener);
                viewHolder.recordBnt.setOnClickListener(recordBntOnClickListener);

                resetRecButtons();
            }
        } else {
            viewHolder.timelyt.setVisibility(View.GONE);
            viewHolder.recordlyt.setVisibility(View.GONE);
            viewHolder.subcontent.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
        }

        if (FileUtils.getCurrInfo(FileUtils.GET_RECEXIST, position) != null) {
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
            setRecButtons(RECBTN_COMPAREING);
            ListItem i = Subtitle.get(selectIdx);
            videoPlayer.play(i.getSubStart().getMseconds(), i.getSubEnd().getMseconds(), VideoPlayer.PLAYMODE_PART_COMBINE);
        }
    };

    private View.OnClickListener replayBntOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            setRecButtons(RECBTN_REPLAYING);
            AudioPlayer.replay(selectIdx);
        }
    };

    private View.OnClickListener recordBntOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (isRecording == false) {
                setRecButtons(RECBTN_DISABLE);

                ListItem i = Subtitle.get(selectIdx);
                int period = i.getSubEnd().getMseconds() - i.getSubStart().getMseconds() + 500;
                AudioRecorder.start(selectIdx, period);
            } else {
                AudioRecorder.stop();
            }
        }
    };

    private static void resetRecButtons() {
        if (FileUtils.getCurrInfo(FileUtils.GET_RECEXIST, selectIdx) != null) {
            setRecButtons(RECBTN_HAVEREC);
        } else {
            setRecButtons(RECBTN_NOREC);
        }
    }

    private static void setRecButtons(int type) {
        if (type == RECBTN_DISABLE) {
            currentView.compareBnt.setImageResource(R.drawable.compare_disable);
            currentView.compareBnt.setEnabled(false);
            currentView.replayBnt.setImageResource(R.drawable.replay_disable);
            currentView.replayBnt.setEnabled(false);
            currentView.recordBnt.setImageResource(R.drawable.record_disable);
            currentView.recordBnt.setEnabled(false);
        } else if (type == RECBTN_HAVEREC) {
            disableCurrSub = false;
            currentView.compareBnt.setImageResource(R.drawable.compare_available);
            currentView.compareBnt.setEnabled(true);
            currentView.replayBnt.setImageResource(R.drawable.replay_available);
            currentView.replayBnt.setEnabled(true);
            currentView.recordBnt.setImageResource(R.drawable.record_available);
            currentView.recordBnt.setEnabled(true);
        } else if (type == RECBTN_NOREC) {
            disableCurrSub = false;
            currentView.compareBnt.setImageResource(R.drawable.compare_disable);
            currentView.compareBnt.setEnabled(false);
            currentView.replayBnt.setImageResource(R.drawable.replay_disable);
            currentView.replayBnt.setEnabled(false);
            currentView.recordBnt.setImageResource(R.drawable.record_available);
            currentView.recordBnt.setEnabled(true);
        } else if (type == RECBTN_RECORDING) {
            disableCurrSub = true;
            currentView.compareBnt.setImageResource(R.drawable.compare_disable);
            currentView.compareBnt.setEnabled(false);
            currentView.replayBnt.setImageResource(R.drawable.replay_disable);
            currentView.replayBnt.setEnabled(false);
            currentView.recordBnt.setImageResource(R.drawable.stop_avaiable);
            currentView.recordBnt.setEnabled(true);
        } else if (type == RECBTN_REPLAYING) {
            disableCurrSub = true;
            currentView.compareBnt.setImageResource(R.drawable.compare_disable);
            currentView.compareBnt.setEnabled(false);
            currentView.replayBnt.setImageResource(R.drawable.replay_active);
            currentView.replayBnt.setEnabled(false);
            currentView.recordBnt.setImageResource(R.drawable.record_disable);
            currentView.recordBnt.setEnabled(false);
        } else if (type == RECBTN_COMPAREING) {
            disableCurrSub = true;
            currentView.compareBnt.setImageResource(R.drawable.compare_active);
            currentView.compareBnt.setEnabled(false);
            currentView.replayBnt.setImageResource(R.drawable.replay_disable);
            currentView.replayBnt.setEnabled(false);
            currentView.recordBnt.setImageResource(R.drawable.record_disable);
            currentView.recordBnt.setEnabled(false);
        } else {
            Log.e(TAG, "unknow type.");
        }
    }

    class ViewHolder {
        private LinearLayoutCompat timelyt, contentlyt, recordlyt;
        private TextView substart, subcontent, subend;
        private ImageButton compareBnt, replayBnt, recordBnt;
        private ProgressBar progressBar;
    }
}
