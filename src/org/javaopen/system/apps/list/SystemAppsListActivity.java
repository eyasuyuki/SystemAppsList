package org.javaopen.system.apps.list;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class SystemAppsListActivity extends ListActivity {
    private static final String TAG = SystemAppsListActivity.class.getName();
    
    Handler handler = new Handler();
    MenuItem menuItem = null;
    
    PackageCallback callback = new PackageCallback.Stub() {
        
        @Override
        public void update() throws RemoteException {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    reload();
                }
            });
        }
    };
    
    PackageServiceBinder service = null;
    
    ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.d(TAG, "onServiceConnected: name="+name+", binder="+binder);
            service = PackageServiceBinder.Stub.asInterface(binder);
            try {
                service.setCallback(callback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected: name="+name);
            if (service != null) {
                try {
                    service.removeCallback(callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            service = null;
        }};
    
    //Map<String, App> apps = new HashMap<String, App>();
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // bind
        Intent intent = new Intent(this, PackageService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);
        
        initPreferences();
        
        reload();
    }
    
    void initPreferences() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String key = getString(R.string.preference_su_path_key);
        if (!sp.contains(key)) {
            String value = getString(R.string.preference_su_path_default);
            Editor editor = sp.edit();
            editor.putString(key, value);
            editor.commit();
        }
    }
    
    void reload() {
        AsyncApps async = new AsyncApps(this
                //,apps
                ,getListView());
        async.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_preferences:
            Intent intent = new Intent(this, Preferences.class);
            startActivity(intent);
            break;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (service != null) {
            try {
                service.removeCallback(callback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        unbindService(connection);
    }
}