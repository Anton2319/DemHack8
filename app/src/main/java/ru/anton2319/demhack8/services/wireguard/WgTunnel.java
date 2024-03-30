package ru.anton2319.demhack8.services.wireguard;

import com.wireguard.android.backend.Tunnel;


public class WgTunnel implements Tunnel {

    String name = "wgpreconf";

    public void setName(String name) { this.name = name; }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void onStateChange(State newState) {
    }
}
