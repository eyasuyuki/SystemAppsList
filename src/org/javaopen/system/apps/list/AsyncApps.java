package org.javaopen.system.apps.list;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.XmlResourceParser;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class AsyncApps extends AsyncTask<Void, String, Void> {
    private static final String TAG = AsyncApps.class.getName();
    public static final String USES_PERMISSION = "uses-permission";
	public static final String RECEIVER = "receiver";
	public static final String INTENT_FILTER = "intent-filter";
	public static final String ACTION = "action";
	public static final String NAMESPACE = "http://schemas.android.com/apk/res/android";
	public static final String NAME = "name";
    
    Context context = null;
    ListView enabledList = null;
    ListView disabledList = null;
    ProgressDialog dialog = null;
    List<App> enabledApps = new ArrayList<App>();
    List<App> disabledApps = new ArrayList<App>();
    
    public AsyncApps(Context context
            ,ListView enabledList
            ,ListView disabledList) {
        this.context = context;
        this.enabledList = enabledList;
        this.disabledList = disabledList;
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
            pm.getInstalledApplications(PackageManager.GET_META_DATA
            		| PackageManager.GET_UNINSTALLED_PACKAGES);
        for (ApplicationInfo a: alist) {
        	parseManifest(context, a.packageName);
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
            app.setUid(a.uid);
            app.setPath(a.sourceDir);
            app.setEnabled(a.enabled);
            copy(a.sourceDir);
            if (app.isEnabled) {
                enabledApps.add(app);
            } else {
                disabledApps.add(app);
            }
        }
        // sort
        Comparator<App> comp = new Comparator<App>(){
            @Override
            public int compare(App lhs, App rhs) {
                //return lhs.getUid() - rhs.getUid();
                return rhs.getUid() - lhs.getUid();
            }};
        Collections.sort(enabledApps, comp);
        Collections.sort(disabledApps, comp);
        
        return null;
    }
    
    String getOdexName(File src) {
        StringBuffer result = new StringBuffer();
        result.append(FilenameUtils.getPath(src.getAbsolutePath()));
        result.append(FilenameUtils.getBaseName(src.getName()));
        result.append(".odex");
        Log.d(TAG, "getOdexName: buf="+result.toString());
        return result.toString();
    }
    
    void copy(String filename) {
        File src = new File(filename);
        File dst = new File(context.getExternalFilesDir(null), src.getName());
        File odex = new File(getOdexName(src));
        try {
            if (!dst.exists()) FileUtils.copyFile(src, dst);
            if (odex.exists() && !dst.exists()) {
                File odst = new File(context.getExternalFilesDir(null), odex.getName());
                FileUtils.copyFile(odex, odst);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        dialog.setMessage(values[0]);
    }
    
    void processClick(List<App> appList, int position) {
        final App item = appList.get(position);
        
        context.startActivity(
                new Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:"+item.getPackageName())));
    }
    
    AdapterView.OnItemClickListener clickEnabled = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        	processClick(enabledApps, position);
        }
    };

    AdapterView.OnItemClickListener clickDisabled = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        	processClick(disabledApps, position);
        }
    };
    
    void processLongClick(List<App> appList, int position) {
        final App item = appList.get(position);
        Log.d(TAG, "processLongClick: item.packageName="+item.packageName+", item.isEnabled"+item.isEnabled);
        
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
    
    AdapterView.OnItemLongClickListener longClickEnabled = new AdapterView.OnItemLongClickListener() {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        	processLongClick(enabledApps, position);
            return true;
        }
    };

    AdapterView.OnItemLongClickListener longClickDisabled = new AdapterView.OnItemLongClickListener() {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        	processLongClick(disabledApps, position);
            return true;
        }
    };

    @Override
    protected void onPostExecute(Void result) {
        AppAdapter enabledAdapter = new AppAdapter(context, R.layout.app_row, enabledApps);
        enabledList.setAdapter(enabledAdapter);
        enabledList.setOnItemClickListener(clickEnabled);
        enabledList.setOnItemLongClickListener(longClickEnabled);

        AppAdapter disabledAdapter = new AppAdapter(context, R.layout.app_row, disabledApps);
        disabledList.setAdapter(disabledAdapter);
        disabledList.setOnItemClickListener(clickDisabled);
        disabledList.setOnItemLongClickListener(longClickDisabled);
        
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
	static void parseManifest(Context context, String packageName) {
		List<String> requestedPermissions = new ArrayList<String>();
		List<String> receivers = new ArrayList<String>();
		PackageManager pm = context.getPackageManager();
		ApplicationInfo info = null;
		try {
			info = pm.getApplicationInfo(
					packageName, PackageManager.GET_META_DATA);
		} catch (NameNotFoundException e1) {
			e1.printStackTrace();
		}
    	if (info != null) {
    		try {
    			// TODO Log.d(TAG, "parseManifest: publicSourceDir="+info.publicSourceDir);
    			Class clazz = Class.forName("android.content.res.AssetManager");
    			Object am = clazz.newInstance();
    			Method addAssetPath =
    				am.getClass().getMethod(
    						"addAssetPath",
    						new Class[]{ String.class });
    			Object c = addAssetPath.invoke(am, info.publicSourceDir);
    			// TODO Log.d(TAG, "parseManifest: c="+c);
    			if (c != null) {
    				int cookie = Integer.parseInt(c.toString());
    				Method openXmlResourceParser =
    					am.getClass().getMethod(
    							"openXmlResourceParser",
    							new Class[]{ int.class, String.class });
    				Object p =
    					openXmlResourceParser.invoke(am, cookie, "AndroidManifest.xml");
    				// TODO Log.d(TAG, "parseManifest: p="+p);
    				if (p instanceof XmlResourceParser) {
    					XmlResourceParser parser = (XmlResourceParser)p;
    					int type;
    					boolean inUsesPermission = false;
    					boolean inReceiver = false;
    					boolean inIntentFilter = false;
    					boolean inAction = false;
    					String tagName = null;
    					while ((type = parser.next()) != XmlResourceParser.END_DOCUMENT) {
    						// TODO Log.d(TAG, "1:parseManifest: type="+type+", name="+parser.getName());
    						switch (type) {
    						case XmlResourceParser.START_TAG:
    							tagName = parser.getName();
    							if (tagName != null) {
    								if (tagName.equals(USES_PERMISSION)) inUsesPermission = true;
    								if (tagName.equals(RECEIVER))        inReceiver     = true;
    								if (tagName.equals(INTENT_FILTER))   inIntentFilter = true;
    								if (tagName.equals(ACTION))          inAction       = true;
    							}
    							break;
    						case XmlResourceParser.END_TAG:
    							tagName = parser.getName();
    							if (tagName != null) {
    								if (tagName.equals(USES_PERMISSION)) inUsesPermission = false;
    								if (tagName.equals(RECEIVER))      inReceiver         = false;
    								if (tagName.equals(INTENT_FILTER)) inIntentFilter     = false;
    								if (tagName.equals(ACTION))        inAction           = false;
    							}
    							break;
    						}
    						// TODO Log.d(TAG, "inReceiver="+inReceiver+", inIntentFilter="+inIntentFilter+", inAction="+inAction);
    						if (inUsesPermission) {
    							String value = parser.getAttributeValue(NAMESPACE, NAME);
    							Log.d(TAG, "parseManifest: packageName="+packageName+", uses-permission="+value);
    							requestedPermissions.add(value);
    						}
    						if (inReceiver && inIntentFilter && inAction) {
    							String value = parser.getAttributeValue(NAMESPACE, NAME);
    							Log.d(TAG, "parseManifest: packageName="+packageName+", receiver="+value);
    							if (value != null) {
    								receivers.add(value);
    							}
    						}
    					}
    				}
    			}
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	}
	}
}
