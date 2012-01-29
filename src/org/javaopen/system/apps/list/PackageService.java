package org.javaopen.system.apps.list;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

public class PackageService extends IntentService {
    private static final String TAG = PackageService.class.getName();

    public static final String ACTION_UPDATE = "org.javaopen.system.apps.list.UPDATE";
    
    RemoteCallbackList<PackageCallback> callbacks = 
        new RemoteCallbackList<PackageCallback>();

    PackageServiceBinder.Stub binder = new PackageServiceBinder.Stub() {
        
        @Override
        public void setCallback(PackageCallback callback) throws RemoteException {
            callbacks.register(callback);
        }
        
        @Override
        public void removeCallback(PackageCallback callback) throws RemoteException {
            callbacks.unregister(callback);
        }
    };

    public PackageService() {
        super(PackageService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onHandleIntent: action="+action);
        if (ACTION_UPDATE.equals(action)) {
            update();
        }
    }
    
    void update() {
        int count = callbacks.beginBroadcast();
        Log.d(TAG, "update: count="+count);
        for (int i=0; i<count; i++) {
            PackageCallback callback = callbacks.getBroadcastItem(i);
            try {
                callback.update();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        callbacks.finishBroadcast();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

}
