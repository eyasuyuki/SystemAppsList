package org.javaopen.system.apps.list;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ListView;

public class AsyncApps extends AsyncTask<Void, Void, Void> {
    
    Context context = null;
    ListView listView = null;
    ProgressDialog dialog = null;
    List<App> apps = new ArrayList<App>();
    
    static Method copyFile = null;
    
    static {
        try {
            Class clazz = Class.forName("android.os.FileUtils");
            copyFile = clazz.getMethod("copyFile", File.class, File.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public AsyncApps(Context context, ListView listView) {
        this.context = context;
        this.listView = listView;
    }

    @Override
    protected void onPreExecute() {
        dialog = new ProgressDialog(context);
        dialog.setTitle(R.string.progress_title);
        try { dialog.show(); } catch (Exception e) {}
    }

    @Override
    protected Void doInBackground(Void... params) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> alist =
            pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo a: alist) {
            if((a.flags & ApplicationInfo.FLAG_SYSTEM) != ApplicationInfo.FLAG_SYSTEM) continue;
            Drawable icon = null;
            String appName = null;
            try {
                icon = pm.getApplicationIcon(a.packageName);
                appName = pm.getApplicationLabel(a).toString();
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
            App app = new App();
            app.setIcon(icon);
            app.setAppName(appName);
            app.setPackageName(a.packageName);
            app.setPath(a.sourceDir);
            apps.add(app);
            copy(a.sourceDir);
        }
        return null;
    }
    
    void copy(String filename) {
        File src = new File(filename);
        File dst = new File(context.getExternalFilesDir(null), src.getName());
        if (copyFile != null && !dst.exists()) {
            try {
                copyFile.invoke(null, src, dst);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPostExecute(Void result) {
        AppAdapter adapter = new AppAdapter(context, R.layout.app_row, apps);
        listView.setAdapter(adapter);
        
        try { dialog.dismiss(); } catch (Exception e) {}
    }

}
