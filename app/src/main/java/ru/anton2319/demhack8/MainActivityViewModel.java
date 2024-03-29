package ru.anton2319.demhack8;

import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.util.Log;
import android.view.View;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import ru.anton2319.demhack8.data.singleton.SocksPersistent;
import ru.anton2319.demhack8.services.SocksProxyService;

public class MainActivityViewModel extends ViewModel {
    String TAG = "MainActivityViewModel";

    private final MutableLiveData<String> connectionButtonText = new MutableLiveData<>();

    public MainActivityViewModel(MainActivity mainActivity) {
        connectionButtonText.setValue("connect");
        //noinspection Convert2Lambda
        mainActivity.connectButton = mainActivity.findViewById(R.id.connect_button);
        mainActivity.connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleVpn(mainActivity);
            }
        });
        Log.d(TAG, "ViewModel has initialized successfully!");
    }

    private void toggleVpn(Activity activity) {
        if(SocksPersistent.getInstance().getVpnThread() == null || !SocksPersistent.getInstance().getVpnThread().isAlive()) {
            Log.d(TAG, "Preparing VpnService");
            Intent intentPrepare = VpnService.prepare(activity);
            if (intentPrepare != null) {
                Log.d(TAG, "Toggled off due to missing permission");
                activity.startActivityForResult(intentPrepare, 0);
                return;
            }
            SocksPersistent.getInstance().setVpnIntent(new Intent(activity, SocksProxyService.class));
            activity.startService(SocksPersistent.getInstance().getVpnIntent());
            Log.d(TAG, "Toggled on");
        }
        else {
            Log.d(TAG, "Toggled off");
            Thread vpnThread = SocksPersistent.getInstance().getVpnThread();
            if(vpnThread != null) {
                vpnThread.interrupt();
            }
        }
    }
}
