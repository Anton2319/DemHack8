package ru.anton2319.demhack8.services.wireguard;

import static com.wireguard.android.backend.Tunnel.State.UP;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.wireguard.config.Config;
import com.wireguard.config.InetEndpoint;
import com.wireguard.config.InetNetwork;


class ConnectRunnable extends WgController implements Runnable {
    Context context;
    String ip;
    String privateKey;
    String connectionAddress;
    boolean LANsharing;
    public ConnectRunnable(Context context, String connectionAddress, String privateKey, String ip, boolean LANsharing) {
        super(context);
        this.context = context;
        this.ip = ip;
        this.privateKey = privateKey;
        this.LANsharing = LANsharing;
        this.connectionAddress = connectionAddress;
    }

    @Override
    public void run() throws RuntimeException {
        try {
            Log.d(TAG, ip);
            Log.d(TAG, privateKey);
            Log.d(TAG, "Firing up tunnel, establishing connection");
            String dns_ip = "1.1.1.1";
            String mtu_size = "1400";
            if (LANsharing) {
                Log.d(TAG, "LAN sharing enabled, including every local route");
                backend.setState(tunnel, UP, new Config.Builder()
                        .setInterface(interfaceBuilder.addAddress(InetNetwork.parse(ip)).addDnsServer(InetNetwork.parse(dns_ip).getAddress()).parsePrivateKey(privateKey).parseMtu(mtu_size).build())
                        .addPeer(peerBuilder.addAllowedIp(InetNetwork.parse("0.0.0.0/0")).setEndpoint(InetEndpoint.parse(connectionAddress)).parsePublicKey("gLXvjRXiSv3HHeXbnRxBxLPMfXDesAnJ2mpJveabNjM=").setPersistentKeepalive(60).build())
                        .build());
            } else {
                Log.d(TAG, "LAN sharing disabled, restricting local routes");
                backend.setState(tunnel, UP, new Config.Builder()
                        .setInterface(interfaceBuilder.addAddress(InetNetwork.parse(ip)).addDnsServer(InetNetwork.parse(dns_ip).getAddress()).parsePrivateKey(privateKey).parseMtu(mtu_size).build())
                        .addPeer(peerBuilder.addAllowedIp(
                                InetNetwork.parse("::/0")).addAllowedIp(
                                InetNetwork.parse("1.0.0.0/8")).addAllowedIp(
                                InetNetwork.parse("2.0.0.0/8")).addAllowedIp(
                                InetNetwork.parse("3.0.0.0/8")).addAllowedIp(
                                InetNetwork.parse("4.0.0.0/6")).addAllowedIp(
                                InetNetwork.parse("8.0.0.0/7")).addAllowedIp(
                                InetNetwork.parse("11.0.0.0/8")).addAllowedIp(
                                InetNetwork.parse("12.0.0.0/6")).addAllowedIp(
                                InetNetwork.parse("16.0.0.0/4")).addAllowedIp(
                                InetNetwork.parse("32.0.0.0/3")).addAllowedIp(
                                InetNetwork.parse("64.0.0.0/2")).addAllowedIp(
                                InetNetwork.parse("128.0.0.0/3")).addAllowedIp(
                                InetNetwork.parse("160.0.0.0/5")).addAllowedIp(
                                InetNetwork.parse("168.0.0.0/6")).addAllowedIp(
                                InetNetwork.parse("172.0.0.0/12")).addAllowedIp(
                                InetNetwork.parse("172.32.0.0/11")).addAllowedIp(
                                InetNetwork.parse("172.64.0.0/10")).addAllowedIp(
                                InetNetwork.parse("172.128.0.0/9")).addAllowedIp(
                                InetNetwork.parse("173.0.0.0/8")).addAllowedIp(
                                InetNetwork.parse("174.0.0.0/7")).addAllowedIp(
                                InetNetwork.parse("176.0.0.0/4")).addAllowedIp(
                                InetNetwork.parse("192.0.0.0/9")).addAllowedIp(
                                InetNetwork.parse("192.128.0.0/11")).addAllowedIp(
                                InetNetwork.parse("192.160.0.0/13")).addAllowedIp(
                                InetNetwork.parse("192.169.0.0/16")).addAllowedIp(
                                InetNetwork.parse("192.170.0.0/15")).addAllowedIp(
                                InetNetwork.parse("192.172.0.0/14")).addAllowedIp(
                                InetNetwork.parse("192.176.0.0/12")).addAllowedIp(
                                InetNetwork.parse("192.192.0.0/10")).addAllowedIp(
                                InetNetwork.parse("193.0.0.0/8")).addAllowedIp(
                                InetNetwork.parse("194.0.0.0/7")).addAllowedIp(
                                InetNetwork.parse("196.0.0.0/6")).addAllowedIp(
                                InetNetwork.parse("200.0.0.0/5")).addAllowedIp(
                                InetNetwork.parse("208.0.0.0/4")).addAllowedIp(
                                InetNetwork.parse("1.1.1.1/32")).setEndpoint(InetEndpoint.parse(connectionAddress)).parsePublicKey("gLXvjRXiSv3HHeXbnRxBxLPMfXDesAnJ2mpJveabNjM=").build())
                        .build());
            }
            if(completionHandler != null) {
                completionHandler.handle();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
}
