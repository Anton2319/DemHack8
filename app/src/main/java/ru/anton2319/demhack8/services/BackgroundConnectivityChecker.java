package ru.anton2319.demhack8.services;

import static com.wireguard.android.backend.Tunnel.State.UP;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.net.VpnService;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.wireguard.android.backend.Backend;

import ru.anton2319.demhack8.data.singleton.SocksPersistent;
import ru.anton2319.demhack8.services.wireguard.WgController;
import ru.anton2319.demhack8.services.wireguard.WgTunnel;
import ru.anton2319.demhack8.utils.HttpsConnectivityChecker;
import ru.anton2319.demhack8.data.singleton.PersistentConnectionProperties;

public class BackgroundConnectivityChecker extends Service {
    private static final String TAG = "ConnectivityChecker";
    private Handler handler;
    private Runnable connectivityCheckRunnable;
    private final long CHECK_INTERVAL = 30000;
    private final MutableLiveData<String> connectionButtonText = PersistentConnectionProperties.getInstance().getConnectionButtonText();
    private final MutableLiveData<String> protocolText = PersistentConnectionProperties.getInstance().getProtocolText();
    private final MutableLiveData<String> statusText = PersistentConnectionProperties.getInstance().getStatusText();

    Boolean wireguardIsUp = false;

    HandlerThread handlerThread;

    private final Activity activity = PersistentConnectionProperties.getInstance().getMainActivity();
    WgController wgController = PersistentConnectionProperties.getInstance().getWgController();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handlerThread = new HandlerThread("ConnectivityCheckerThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        connectivityCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (!sendRequestAndCheckResponseWithRetries()) {
                    executeFailureProcedure();
                }
                handler.postDelayed(connectivityCheckRunnable, CHECK_INTERVAL);
            }
        };
        handler.post(connectivityCheckRunnable);
        Log.d(TAG, "BackgroundConnectivityChecker have initialized successfully!");
    }

    private boolean sendRequestAndCheckResponseWithRetries() {
        HttpsConnectivityChecker checker = new HttpsConnectivityChecker();
        return checker.sendRequestAndCheckResponseWithRetries();
    }

    private void executeFailureProcedure() {
        shutdownAll(PersistentConnectionProperties.getInstance().getMainActivity());
        activity.runOnUiThread(() -> connectionButtonText.setValue("connecting"));
        // TODO: replace thread with cancellable
        PersistentConnectionProperties.getInstance().setAutoProtocolThread(new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Preparing VpnService");
                Intent intentPrepare = VpnService.prepare(activity);
                if (intentPrepare != null) {
                    Log.d(TAG, "Toggled off due to missing permission");
                    activity.startActivityForResult(intentPrepare, 0);
                    return;
                }
                try {
                    Log.d(TAG, "Toggling on");
                    activity.runOnUiThread(() -> protocolText.setValue("WireGuard"));
                    if (PersistentConnectionProperties.getInstance().getWireGuardInitiationThread() != null) {
                        PersistentConnectionProperties.getInstance().getWireGuardInitiationThread().interrupt();
                    }
                    wgController.connect("REDACTED:40001", "OGldQ4F+94FX2XAUfZb6hx30U3/aeZ8Xn6V07Egw/3M=", "172.16.0.2", false, false);
                    activity.runOnUiThread(() -> statusText.setValue("Establishing wireguard connection"));
                    waitForWireguard(10000);
                    if (!wireguardIsUp) {
                        Log.d(TAG, "WireGuard failure, falling back to Shadowsocks");
                        wgController.shutdown();
                    }
                    if (Thread.interrupted()) {
                        return;
                    }
                    boolean connectivityCheckSuccessful = false;
                    activity.runOnUiThread(() -> statusText.setValue("Performing connectivity check"));
                    if (wireguardIsUp) {
                        connectivityCheckSuccessful = new HttpsConnectivityChecker(PersistentConnectionProperties.getInstance().getAutoProtocolThread()).sendRequestAndCheckResponseWithRetries();
                    }
                    if (Thread.interrupted()) {
                        wgController.shutdown();
                        return;
                    }
                    if (!connectivityCheckSuccessful) {
                        activity.runOnUiThread(() -> statusText.setValue("Falling back to shadowsocks"));
                        activity.runOnUiThread(() -> protocolText.setValue("Shadowsocks"));
                        activity.runOnUiThread(() -> statusText.setValue(null));
                        SocksPersistent.getInstance().setVpnIntent(new Intent(activity, SocksProxyService.class));
                        activity.startService(SocksPersistent.getInstance().getVpnIntent());
                        activity.runOnUiThread(() -> connectionButtonText.setValue("disconnect"));
                    }
                    else {
                        activity.runOnUiThread(() -> {
                            activity.runOnUiThread(() -> statusText.setValue(null));
                            connectionButtonText.setValue("disconnect");
                        }
                        );
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    activity.runOnUiThread(() -> {
                        connectionButtonText.setValue("disconnecting");
                        shutdownAll(activity);
                        connectionButtonText.setValue("connect");
                    }
                    );
                }
            }
        }));
        PersistentConnectionProperties.getInstance().getAutoProtocolThread().start();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Shutting down gracefully");
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacks(connectivityCheckRunnable);
        }
        if (handlerThread != null) {
            handlerThread.quit();
        }
        Log.d(TAG, "onDestroy completed, now calling stopSelf");
        stopSelf();
    }

    private void shutdownAll(Activity activity) {
        activity.runOnUiThread(() -> PersistentConnectionProperties.getInstance().getStatusText().setValue(null));
        activity.runOnUiThread(() -> PersistentConnectionProperties.getInstance().getProtocolText().setValue(null));
        Thread autoProtocolThread = PersistentConnectionProperties.getInstance().getAutoProtocolThread();
        if(autoProtocolThread != null) {
            autoProtocolThread.interrupt();
        }
        Thread wireGuardThread = PersistentConnectionProperties.getInstance().getWireGuardInitiationThread();
        if(wireGuardThread != null) {
            wireGuardThread.interrupt();
        }
        wgController.shutdown();
        Thread vpnThread = SocksPersistent.getInstance().getVpnThread();
        if(vpnThread != null) {
            vpnThread.interrupt();
        }
        Intent vpnIntent = SocksPersistent.getInstance().getVpnIntent();
        if(vpnIntent != null) {
            activity.stopService(vpnIntent);
        }
    }

    public void waitForWireguard(long timeout) throws InterruptedException {
        Thread thread = new Thread() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    boolean wireguardIsReady = checkForWg();
                    if (wireguardIsReady) {
                        wireguardIsUp = true;
                        break;
                    }
                    try {
                        sleep(200);
                    } catch (InterruptedException e) {
                    }
                }
            }
        };
        thread.start();
        thread.join(timeout);
    }

    private boolean checkForWg() {
        if (PersistentConnectionProperties.getInstance().getBackend() != null && PersistentConnectionProperties.getInstance().getTunnel() != null) {
            Backend backend = PersistentConnectionProperties.getInstance().getBackend();
            WgTunnel tunnel = PersistentConnectionProperties.getInstance().getTunnel();
            try {
                if (backend.getState(tunnel) == UP) {
                    // wireguard initial welcome message has a length of 92 bytes
                    if (backend.getStatistics(tunnel).totalRx() > 92) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        } else {
            return false;
        }
    }
}
