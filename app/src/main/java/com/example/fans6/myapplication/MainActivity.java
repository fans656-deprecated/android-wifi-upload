package com.example.fans6.myapplication;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import org.nanohttpd.util.ServerRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.StringTokenizer;

public class MainActivity extends AppCompatActivity {

    private HelloServer server;
    private DatagramSocket sock;
    private HashSet<String> peers = new HashSet<>();
    private ArrayList<String> peersList = new ArrayList<>();
    private Handler handler;
    final Handler purgeHandler = new Handler();
    private Runnable purgeRunnable = new Runnable() {
        @Override
        public void run() {
            peers.clear();
            purgeHandler.postDelayed(purgeRunnable, 2000);
        }
    };
    String selectedAddr = "";

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final Uri uri = data.getData();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("test", "fpath: " + uri);
                byte[] buf = new byte[1024];
                String path = uri.getPath();
                File file = new File(path);
                if (file.canRead()) {
                    String url = "http://" + selectedAddr.split(":")[0] + ":8080/"
                            + file.getName();
                    try {
                        int response = HttpRequest.post(url).send(file).code();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int netmask = wifiManager.getDhcpInfo().netmask;
        final int broadcastAddr = (wifiInfo.getIpAddress() | ~netmask);
        String ipAddr = intToIpAddr(wifiInfo.getIpAddress());
        String subnetMask = intToIpAddr(wifiManager.getDhcpInfo().netmask);

        try {
            sock = new DatagramSocket(8888);
            sock.setBroadcast(true);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, peersList);
        final ListView listView = (ListView)findViewById(R.id.peers_list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedAddr = (String)listView.getItemAtPosition(position);
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("file/*");
                startActivityForResult(intent, 8888);
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String msg = "kindle wifi broadcast";
                    DatagramPacket p = new DatagramPacket(
                            msg.getBytes(),
                            msg.length(),
                            InetAddress.getByName(intToIpAddr(broadcastAddr)),
                            8888);
                    while (true) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        sock.send(p);
                        //Log.d("test", "sent");
                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        //purgeHandler.post(purgeRunnable);

        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                String addr = msg.getData().getString("address");
                peers.add(addr);
                peersList.clear();
                peersList.addAll(peers);
                Collections.sort(peersList);
                adapter.notifyDataSetChanged();
                return true;
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] msg = new byte[512];
                try {
                    while (true) {
                        DatagramPacket p = new DatagramPacket(msg, msg.length);
                        sock.receive(p);
                        String recvedMsg = new String(msg, 0, p.getLength());
                        //Log.d("test", "recved: " + recvedMsg + " | " + p.getSocketAddress().toString());
                        String addr = p.getAddress().getHostAddress();
                        int port = p.getPort();
                        Message handlerMessage = new Message();
                        Bundle bundle = new Bundle();
                        bundle.putString("address", addr + ":" + port);
                        handlerMessage.setData(bundle);
                        handler.sendMessage(handlerMessage);
                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        Log.d("test", "ip: " + ipAddr + ", mask: " + subnetMask);
        Log.d("test", "broadcast: " + intToIpAddr(broadcastAddr));

        server = new HelloServer();
        try {
            server.start();
        } catch (IOException e) {
            ;
        }
    }

    private static String intToIpAddr(int ip) {
        return (ip & 0xff) + "." + ((ip>>8)&0xff) + "." + ((ip>>16)&0xff) + "." + ((ip>>24)&0xff);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        server.stop();
    }
}
