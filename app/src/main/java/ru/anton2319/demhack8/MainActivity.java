package ru.anton2319.demhack8;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import ru.anton2319.demhack8.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    protected ActivityMainBinding binding;
    Button connectButton;
    TextView protocolInfo;
    TextView statusInfo;
    MainActivityViewModel mainActivityViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectButton = findViewById(R.id.connect_button);
        protocolInfo = findViewById(R.id.protocol_info);
        statusInfo = findViewById(R.id.status_info);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mainActivityViewModel = new MainActivityViewModel(this);
    }
}