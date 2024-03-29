package ru.anton2319.demhack8.data.singleton;

import android.content.Intent;
import android.os.ParcelFileDescriptor;

public class SocksPersistent {
    private static volatile SocksPersistent instance = null;
    private static final Object lock = new Object();

    private Intent vpnIntent;

    private Thread vpnThread;

    ParcelFileDescriptor vpnInterface;

    private SocksPersistent() {}

    public static SocksPersistent getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new SocksPersistent();
                }
            }
        }
        return instance;
    }

    public void setVpnThread(Thread vpnThread) {
        this.vpnThread = vpnThread;
    }

    public Thread getVpnThread() {
        return vpnThread;
    }

    public ParcelFileDescriptor getVpnInterface() {
        return vpnInterface;
    }

    public Intent getVpnIntent() {
        return vpnIntent;
    }

    public void setVpnIntent(Intent vpnIntent) {
        this.vpnIntent = vpnIntent;
    }

    public void setVpnInterface(ParcelFileDescriptor vpnInterface) {
        this.vpnInterface = vpnInterface;
    }
}
