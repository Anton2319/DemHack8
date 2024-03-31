package ru.anton2319.demhack8;

import static com.wireguard.android.backend.Tunnel.State.UP;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Insets;
import android.net.VpnService;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.wireguard.android.backend.Backend;

import ru.anton2319.demhack8.data.singleton.PersistentConnectionProperties;
import ru.anton2319.demhack8.data.singleton.SocksPersistent;
import ru.anton2319.demhack8.services.SocksProxyService;
import ru.anton2319.demhack8.services.wireguard.WgController;
import ru.anton2319.demhack8.services.wireguard.WgTunnel;
import ru.anton2319.demhack8.utils.HttpsConnectivityChecker;

public class MainActivityViewModel extends ViewModel {
    boolean wireguardIsUp = false;
    String TAG = "MainActivityViewModel";

    WgController wgController;

    private final MutableLiveData<String> connectionButtonText = new MutableLiveData<>();
    private final MutableLiveData<String> protocolText = new MutableLiveData<>();

    private final MutableLiveData<String> statusText = new MutableLiveData<>();

    public MainActivityViewModel(MainActivity mainActivity) {
        connectionButtonText.setValue("connect");
        protocolText.setValue(null);
        statusText.setValue(null);
        wgController  = new WgController(mainActivity);
        //noinspection Convert2Lambda
        mainActivity.connectButton = mainActivity.findViewById(R.id.connect_button);
        mainActivity.connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleVpn(mainActivity);
            }
        });
        connectionButtonText.observe(mainActivity, new Observer<String>() {
            @Override
            public void onChanged(String newData) {
                mainActivity.runOnUiThread(new Runnable() {
                   @Override
                   public void run() {
                       mainActivity.binding.connectButton.setText(newData);
                   }
               });
            }
        });

        protocolText.observe(mainActivity, new Observer<String>() {
            @Override
            public void onChanged(String newData) {
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (newData == null || newData.equals("")) {
                            mainActivity.binding.protocolInfo.setVisibility(View.INVISIBLE);
                        }
                        else {
                            mainActivity.binding.protocolInfo.setText("Protocol: " + newData);
                            mainActivity.binding.protocolInfo.setVisibility(View.VISIBLE);
                        }

                    }
                });
            }
        });

        statusText.observe(mainActivity, new Observer<String>() {
            @Override
            public void onChanged(String newData) {
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (newData == null || newData.equals("")) {
                            mainActivity.binding.statusInfo.setVisibility(View.INVISIBLE);
                        }
                        else {
                            mainActivity.binding.statusInfo.setText(newData);
                            mainActivity.binding.statusInfo.setVisibility(View.VISIBLE);
                        }

                    }
                });
            }
        });
        Log.d(TAG, "ViewModel has initialized successfully!");
    }

    private void toggleVpn(Activity activity) {
        if(!getState()) {
            connectionButtonText.setValue("connecting");
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
                            activity.runOnUiThread(new Runnable() {
                                   @Override
                                   public void run() {
                                       activity.runOnUiThread(() -> statusText.setValue(null));
                                       connectionButtonText.setValue("disconnect");
                                   }
                               }
                            );
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        activity.runOnUiThread(new Runnable() {
                               @Override
                               public void run() {
                                   connectionButtonText.setValue("disconnecting");
                                   shutdownAll(activity);
                                   connectionButtonText.setValue("connect");
                               }
                           }
                        );
                    }
                }
            }));
            PersistentConnectionProperties.getInstance().getAutoProtocolThread().start();
        }
        else {
            Log.d(TAG, "Toggled off");
            connectionButtonText.setValue("disconnecting");
            shutdownAll(activity);
            connectionButtonText.setValue("connect");
        }
    }

    private void shutdownAll(Activity activity) {
        activity.runOnUiThread(() -> statusText.setValue(null));
        activity.runOnUiThread(() -> protocolText.setValue(null));
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

    public static boolean getState() {
        try {
            Thread autoProtocolThread = PersistentConnectionProperties.getInstance().getAutoProtocolThread();
            if (autoProtocolThread != null) {
                if (autoProtocolThread.isAlive()) {
                    return false;
                }
            }
            if (PersistentConnectionProperties.getInstance().getBackend().getState(PersistentConnectionProperties.getInstance().getTunnel()) == UP || SocksPersistent.getInstance().getVpnThread().isAlive()) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
