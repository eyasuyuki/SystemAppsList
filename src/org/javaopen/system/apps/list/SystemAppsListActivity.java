package org.javaopen.system.apps.list;

import android.app.ListActivity;
import android.os.Bundle;

public class SystemAppsListActivity extends ListActivity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        AsyncApps async = new AsyncApps(this, getListView());
        async.execute();
    }
}