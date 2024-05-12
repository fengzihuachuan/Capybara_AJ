package fengzihuachuan.capybara_aj;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

public class FilesListAdapter extends ArrayAdapter<Files.Info> {
    private int resourceId;
    private static MainActivity context;

    public FilesListAdapter(Context ctx, int resourceId1, List<Files.Info> items) {
        super(ctx, resourceId1, items);
        resourceId = resourceId1;
        context = (MainActivity)ctx;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        ViewHolder viewHolder;

        Files.Info item = getItem(position);

        if (convertView == null) { //当用户为第一次访问的时候
            view = LayoutInflater.from(getContext()).inflate(resourceId, null);

            viewHolder = new ViewHolder();

            viewHolder.filelyt = view.findViewById(R.id.filelyt);
            viewHolder.recstatus = view.findViewById(R.id.recstatus);
            viewHolder.subdir = view.findViewById(R.id.subdir);
            viewHolder.filename = view.findViewById(R.id.filename);

            view.setTag(viewHolder); //设置将数据进行缓存
        } else { //第二次访问直接读取第一次访问使存取的数据
            view = convertView;
            viewHolder = (ViewHolder)view.getTag();
        }

        String [] st = item.recInfo.split("/");
        int rec = Integer.parseInt(st[0]);
        int sum = Integer.parseInt(st[1]);

        viewHolder.recstatus.setText(item.recInfo);
        if (sum == 0) {
            viewHolder.recstatus.setBackgroundColor(context.getColor(R.color.backgroudgrey));
            viewHolder.recstatus.setTextColor(context.getColor(R.color.white));
        } else {
            if (rec == sum) {
                viewHolder.recstatus.setBackgroundColor(context.getColor(R.color.darkblue));
                viewHolder.recstatus.setTextColor(context.getColor(R.color.white));
            } else if (rec == 0) {
                viewHolder.recstatus.setBackgroundColor(context.getColor(R.color.backgroudgrey));
                viewHolder.recstatus.setTextColor(context.getColor(R.color.black));
            } else {
                viewHolder.recstatus.setBackgroundColor(context.getColor(R.color.green));
                viewHolder.recstatus.setTextColor(context.getColor(R.color.white));
            }
        }

        viewHolder.subdir.setText(item.videoSubDir);
        viewHolder.filename.setText(item.baseName);

        return view;
    }

    private static class ViewHolder {
        private LinearLayout filelyt;
        private TextView recstatus, subdir, filename;
    }
}
