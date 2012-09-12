package org.javaopen.system.apps.list;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PackageReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
            startPackageService(context, PackageService.ACTION_UPDATE);
        } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
            startPackageService(context, PackageService.ACTION_UPDATE);
        }
    }
    
    void startPackageService(Context context, String action) {
        Intent service = new Intent(context, PackageService.class);
        service.setAction(action);
        context.startService(service);
    }

}
