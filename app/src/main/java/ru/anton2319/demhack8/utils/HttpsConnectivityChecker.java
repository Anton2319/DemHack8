package ru.anton2319.demhack8.utils;

import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import ru.anton2319.demhack8.data.singleton.PersistentConnectionProperties;


/**
Initialize with thread in the constructor if used in parallel
 */
public class HttpsConnectivityChecker {
    private Thread thread;
    public HttpsConnectivityChecker() {}

    public HttpsConnectivityChecker(Thread thread) {

    }
    public boolean sendRequestAndCheckResponseWithRetries() {
        final int MAX_RETRY_ATTEMPTS = 5;
        String TAG = "HttpsConnectivityChecker";
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                String apiUrl = "https://connectivitycheck.gstatic.com/generate_204";
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestMethod("GET");
                int responseCode = connection.getResponseCode();
                connection.disconnect();
                if (responseCode == 204) {
                    Log.d(TAG, "Successfully passed connectivity check!");
                    return true;
                }
            } catch (IOException e) {
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    if(thread != null) {
                        if (thread.isInterrupted()) {
                            return false;
                        }
                    }
                    Log.d(TAG, "Attempt " + attempt + " failed. Retrying...");
                    continue;
                }
                return false;
            }
        }
        return false;
    }
}

