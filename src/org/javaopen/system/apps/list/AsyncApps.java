package org.javaopen.system.apps.list;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class AsyncApps extends AsyncTask<Void, String, Void> {
    private static final String TAG = AsyncApps.class.getName();
    
    Context context = null;
    ListView listView = null;
    ProgressDialog dialog = null;
    List<App> appList = new ArrayList<App>();
    
    static Method copyFile = null;
    
    static {
        try {
            Class clazz = Class.forName("android.os.FileUtils");
            copyFile = clazz.getMethod("copyFile", File.class, File.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public AsyncApps(Context context
            ,ListView listView) {
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
                publishProgress(appName);
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
            App app = new App();
            app.setIcon(icon);
            app.setAppName(appName);
            app.setPackageName(a.packageName);
            app.setPath(a.sourceDir);
            app.setEnabled(a.enabled);
            copy(a.sourceDir);
            appList.add(app);
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
    protected void onProgressUpdate(String... values) {
        dialog.setMessage(values[0]);
    }

    @Override
    protected void onPostExecute(Void result) {
        AppAdapter adapter = new AppAdapter(context, R.layout.app_row, appList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final App item = appList.get(position);
                
                AlertDialog.Builder builder = new AlertDialog.Builder(context);

                if (item != null && item.isEnabled) {
                    builder.setTitle(R.string.confirm_disable_title);
                    builder.setMessage(R.string.confirm_disable_caution);
                } else {
                    builder.setTitle(R.string.confirm_enable_title);
                }
                
                builder.setPositiveButton(R.string.confirm_yes, new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int code = exec(item.isEnabled(), item.getPackageName());
                        
                        if (code == 0) {
                            Intent intent = new Intent(context, PackageService.class);
                            intent.setAction(PackageService.ACTION_UPDATE);
                            context.startService(intent);
                        }
                        
                        dialog.dismiss();
                    }
                });
                
                builder.setNegativeButton(R.string.confirm_cancel, new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.create().show();
            }
        });
        
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final App item = appList.get(position);
                
                context.startActivity(
                        new Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:"+item.getPackageName())));
                return true;
            }
        });
        
        try { dialog.dismiss(); } catch (Exception e) {}
    }

    int exec(boolean enabled, String packageName) {
        String newLine = System.getProperty("line.separator");

        int commandId = R.string.command_disable;
        if (!enabled) {
            commandId = R.string.command_enable;
        }

        StringBuffer command = new StringBuffer();
        command.append(context.getString(commandId));
        command.append(" ");
        command.append(packageName);
        command.append(newLine);
        command.append("exit");
        command.append(newLine);
        Log.d(TAG, "exec: cammand="+command.toString());
        
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getString(R.string.preference_su_path_key);
        String defValue = context.getString(R.string.preference_su_path_default);
        String suPath = sp.getString(key, defValue);
        
        ProcessBuilder builder = new ProcessBuilder(suPath);
        Process proc = null;
        InputStream in = null;
        BufferedOutputStream out = null;
        InputStream err = null;
        StringBuffer result = new StringBuffer();
        int exitValue = -1;
        try {
            proc = builder.start();
            out = new BufferedOutputStream(proc.getOutputStream());
            in = proc.getInputStream();
            err = proc.getErrorStream();
            out.write(command.toString().getBytes());
            out.flush();
            proc.waitFor();
            int c = 0;
            while ((c = err.read()) != -1) {
                result.append((char)c);
            }
            exitValue = proc.exitValue();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (err != null) err.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "exec: result="+result.toString()+", exitValue="+exitValue);
        return exitValue;
    }
}
