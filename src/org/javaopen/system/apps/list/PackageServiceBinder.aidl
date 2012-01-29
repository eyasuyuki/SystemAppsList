package org.javaopen.system.apps.list;

import org.javaopen.system.apps.list.PackageCallback;

interface PackageServiceBinder {
    void setCallback(PackageCallback callback);
    void removeCallback(PackageCallback callback);
}
