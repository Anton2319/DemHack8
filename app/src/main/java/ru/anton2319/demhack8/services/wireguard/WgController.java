package ru.anton2319.demhack8.services.wireguard;

import static com.wireguard.android.backend.Tunnel.State.DOWN;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.wireguard.android.backend.GoBackend;
import com.wireguard.android.backend.Tunnel;
import com.wireguard.config.Interface;
import com.wireguard.config.Peer;

import ru.anton2319.demhack8.data.singleton.PersistentConnectionProperties;

/**
 * Manages WireGuard connectivity. Encapsulates VPN backend and address management logic
 */
public class WgController {
    protected static final String TAG = "WgController";
    private final Context context;
    protected final Tunnel tunnel;
    protected final Interface.Builder interfaceBuilder = new Interface.Builder();
    protected final Peer.Builder peerBuilder = new Peer.Builder();
    protected GoBackend backend;
    protected ru.anton2319.demhack8.utils.CompletionHandler completionHandler;

    public WgController(Context context) {
        this.context = context;
        try {
            backend = PersistentConnectionProperties.getInstance().getBackend();
            backend.getRunningTunnelNames();
        } catch (NullPointerException e) {
            // backend cannot be created without context
            PersistentConnectionProperties.getInstance().setBackend(new GoBackend(context));
            backend = PersistentConnectionProperties.getInstance().getBackend();
            Log.d(TAG, "New GoBackend initialized");
        }
        tunnel = PersistentConnectionProperties.getInstance().getTunnel();
    }

    /**
     * Sets a {@link ru.anton2319.demhack8.utils.CompletionHandler} that will be executed after a successful configuration setup or an error
     * @param completionHandler
     */
    public void setCompletionHandler(ru.anton2319.demhack8.utils.CompletionHandler completionHandler) {
        this.completionHandler = completionHandler;
    }

    /**
     * Connects to a specified WireGuard server
     *
     * @param connectionAddress The IP and port in format of "127.0.0.1:51820"
     * @param ip                Internal IP for your WireGuard connection like "10.0.0.6"
     * @param privateKey        WireGuard privatekey encoded in base64. Must end with an "=" sign
     * @param LANsharing        Whether you want to lan share (use virtual private network) or just proxy the traffic
     * @param synchronize       Whether you want this call to be blocking or not
     */
    public void connect(String connectionAddress, String privateKey, String ip, boolean LANsharing, boolean synchronize) throws InterruptedException {
        Thread connectThread = new Thread(new ConnectRunnable(context, connectionAddress, privateKey, ip, LANsharing));
        connectThread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
                if(completionHandler != null) {
                    completionHandler.handle(e);
                }
            }
        });
        connectThread.start();
        PersistentConnectionProperties.getInstance().setWireGuardInitiationThread(connectThread);
        if(synchronize) {
            connectThread.join();
        }
    }

    public void shutdown() {
        try {
            Log.d(TAG, "Terminating connection");
            // dummy config here, we're not starting, but shutting down the connection
            PersistentConnectionProperties.getInstance().getWireGuardInitiationThread().interrupt();
            backend.setState(tunnel, DOWN, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Tunnel.State getState() {
        if (backend != null && tunnel != null) {
            return backend.getState(tunnel);
        }
        else {
            return DOWN;
        }
    }
}