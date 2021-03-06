package org.javaopen.system.apps.list;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AppAdapter extends ArrayAdapter<App> {

    Context context = null;
    int res;
    List<App> items = null;
    LayoutInflater inflater = null;
    
    ImageView icon = null;
    ImageView status = null;
    TextView appView = null;
    TextView packageView = null;
    TextView uidView = null;
    TextView pathView = null;

    public AppAdapter(Context context, int res, List<App> items) {
        super(context, res, items);
        this.context = context;
        this.res = res;
        this.items = items;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
   }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(res, null);
        }
        App item = items.get(position);
        
        icon = (ImageView)view.findViewById(R.id.icon);
        status = (ImageView)view.findViewById(R.id.status_icon);
        appView = (TextView)view.findViewById(R.id.app_name);
        packageView = (TextView)view.findViewById(R.id.package_name);
        uidView = (TextView)view.findViewById(R.id.uid_view);
        pathView = (TextView)view.findViewById(R.id.path_name);
        
        icon.setImageDrawable(item.getIcon());
        if (item.isEnabled) {
            status.setImageResource(android.R.drawable.presence_online);
        } else {
            status.setImageResource(android.R.drawable.presence_busy);
        }
        appView.setText(item.getAppName());
        packageView.setText(item.getPackageName());
        uidView.setText(Integer.toString(item.getUid()));
        pathView.setText(item.getPath());
        
        return view;
    }

}
