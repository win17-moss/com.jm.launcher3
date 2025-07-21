package com.jm.launcher3;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jm.launcher3.R;

import java.util.List;

public class AppAdapter extends BaseAdapter {
    private Context context;
    private List<ApplicationInfo> appList;
    private PackageManager packageManager;

    public AppAdapter(Context context, List<ApplicationInfo> appList, PackageManager packageManager) {
        this.context = context;
        this.appList = appList;
        this.packageManager = packageManager;
    }

    @Override
    public int getCount() {
        return appList.size();
    }

    @Override
    public Object getItem(int position) {
        return appList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.app_item, parent, false);
        }

        ApplicationInfo app = appList.get(position);

        ImageView appIcon = convertView.findViewById(R.id.app_icon);
        TextView appName = convertView.findViewById(R.id.app_name);

        // 获取应用图标
        appIcon.setImageDrawable(app.loadIcon(packageManager));

        // 获取应用名称并截断
        String label = app.loadLabel(packageManager).toString();
        int maxLen = 6;
        if (label.length() > maxLen) {
            label = label.substring(0, maxLen) + "...";
        }
        appName.setText(label);

        return convertView;
    }
}