package ru.anton2319.demhack8.data.singleton;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.wireguard.android.backend.GoBackend;

import ru.anton2319.demhack8.MainActivity;
import ru.anton2319.demhack8.services.wireguard.WgController;
import ru.anton2319.demhack8.services.wireguard.WgTunnel;

public class PersistentConnectionProperties {
    private static PersistentConnectionProperties mInstance= null;

    private WgTunnel tunnel;
    private GoBackend backend;

    private Thread vpnThread;

    private Intent vpnIntent;

    private Intent sshIntent;
    private String ip;
    private String token;
    private String apiAddress;
    private String serverName = null;
    private String mainApi = null;
    private Boolean ignore_https;

    private Thread autoProtocolThread;
    private Thread wireGuardInitiationThread;

    private int CONNECTION_TIMEOUT_SECONDS = 15;

    private final MutableLiveData<String> connectionButtonText = new MutableLiveData<>();
    private final MutableLiveData<String> protocolText = new MutableLiveData<>();

    private final MutableLiveData<String> statusText = new MutableLiveData<>();

    WgController wgController;

    MainActivity mainActivity;

    public WgTunnel getTunnel() {
        try {
            tunnel.getName();
        }
        catch (NullPointerException e) {
            tunnel = new WgTunnel();
        }
        return tunnel;
    }

    public GoBackend getBackend() {
        return backend;
    }

    public void setBackend(GoBackend backend) {
        this.backend = backend;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getToken() {
        return token;
    }

    public void setVpnThread(Thread vpnThread) {
        this.vpnThread = vpnThread;
    }

    public Thread getVpnThread() {
        return vpnThread;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getCONNECTION_TIMEOUT_SECONDS() {
        return CONNECTION_TIMEOUT_SECONDS;
    }

    public void setCONNECTION_TIMEOUT_SECONDS(int CONNECTION_TIMEOUT_SECONDS) {
        this.CONNECTION_TIMEOUT_SECONDS = CONNECTION_TIMEOUT_SECONDS;
    }

    public Thread getAutoProtocolThread() {
        return autoProtocolThread;
    }

    public void setAutoProtocolThread(Thread autoProtocolThread) {
        if(autoProtocolThread != null) {
            this.autoProtocolThread = autoProtocolThread;
        }
    }

    public MutableLiveData<String> getConnectionButtonText() {
        return connectionButtonText;
    }

    public MutableLiveData<String> getProtocolText() {
        return protocolText;
    }

    public MutableLiveData<String> getStatusText() {
        return statusText;
    }

    public MainActivity getMainActivity() {
        return mainActivity;
    }

    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public WgController getWgController() {
        return wgController;
    }

    public void setWgController(WgController wgController) {
        this.wgController = wgController;
    }

    public Thread getWireGuardInitiationThread() {
        return wireGuardInitiationThread;
    }

    public void setWireGuardInitiationThread(Thread wireGuardInitiationThread) {
        this.wireGuardInitiationThread = wireGuardInitiationThread;
    }

    protected PersistentConnectionProperties(){}

    public static synchronized PersistentConnectionProperties getInstance() {
        if(null == mInstance){
            mInstance = new PersistentConnectionProperties();
        }
        return mInstance;
    }
}

