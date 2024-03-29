package com.example.monitorbezdechu;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;

public class DataActivity extends AppCompatActivity {

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private static final String TAG = "DataActivity";
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothSocket mSocket;
    private String mDeviceAddress;
    private BluetoothAdapter mBluetoothAdapter;
    private Thread workerThread;
    private boolean stopWorker;

    private final ArrayList <Integer> airFlowTab= new ArrayList<>();
    private Intent airFlowIntent;

    private TextView name;
    private TextView address;
    private TextView link_status;
    private TextView message;
    private Button charts_btn;
    private Button stop_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);

        name = findViewById(R.id.name);
        address = findViewById(R.id.address);
        link_status = findViewById(R.id.link_status);
        message = findViewById(R.id.message);
        charts_btn = findViewById(R.id.charts_btn);
        stop_btn = findViewById(R.id.stop_btn);

        final Intent intent = getIntent();
        String mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        name.setText(mDeviceName);
        address.setText(mDeviceAddress);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        feedMultiple();

        airFlowIntent = new Intent(this,AirFlowService.class);

        charts_btn.setOnClickListener(v -> {
            Intent intent_1 = new Intent(DataActivity.this,AirFlowChart.class);
            startActivity(intent_1);
        });

        stop_btn.setOnClickListener(v -> {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Intent intent_2 = new Intent(DataActivity.this,MainActivity.class);
            startActivity(intent_2);
        });
    }

    private void feedMultiple() {
        new Thread() {
            public void run() {
                boolean fail = false;
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
                Log.d(TAG,"Device:" + mDeviceAddress);
                try {
                    mSocket=device.createRfcommSocketToServiceRecord(uuid);
                    Log.d(TAG, "Create RFComm Connection");
                } catch (Exception e) {
                    fail = true;
                    Log.d(TAG, "Could not create RFComm Connection",e);
                }
                try {
                    mSocket.connect();
                    Log.d(TAG,"Connected");
                    fail=false;
                } catch (IOException connectException) {
                    try {
                        mSocket = createBluetoothSocket(device);
                        mSocket.connect();
                        Log.d(TAG, "Connected");
                        fail=false;
                    } catch(IOException ee)
                    { Log.d(TAG, "Not connected");
                        try {
                            mSocket.close();
                            Log.d(TAG, connectException.toString());
                        } catch (IOException closeException) {
                            Log.d(TAG, "Could not close the client socket", closeException);
                        }
                    }
                }
                if (!fail) {
                    beginListenForData();
                    Log.d(TAG, "starting beginListenForData");
                    link_status.setText(R.string.connected);
                } else{
                    link_status.setText(R.string.notconnected);
                }
            }
        }.start();
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            Log.d(TAG, "Create Insecure RFComm Connection");
            return (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(device, 1);
        } catch (Exception e) {
            Log.d(TAG, "Could not create Insecure RFComm Connection",e);
        }
        return  device.createRfcommSocketToServiceRecord(uuid);
    }

    private void beginListenForData()
    {
        InputStream mmInStream;
        stopWorker = false;

        InputStream tmpIn = null;

            try {
                tmpIn = mSocket.getInputStream();
                Log.d(TAG, "Get the data");
            } catch (IOException e) {
                Log.d(TAG, "Could not get a message",e);
            }
            mmInStream = tmpIn;

        workerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted() && !stopWorker)
            {
                try {
                    int bytesAvailable = mmInStream.available();
                    Thread.sleep(2800);
                    Log.d(TAG, String.valueOf(bytesAvailable));
                    if (bytesAvailable > 101)
                    {
                        byte[] packetBytes = new byte[bytesAvailable];
                        mmInStream.read(packetBytes);
                        int[] tab = new int[100];
                        for (int i = 1; i < bytesAvailable; i++) {
                            byte b = packetBytes[i];
                            Log.d(TAG,String.valueOf(b));
                            if (packetBytes[i] == -86) {
                                if (packetBytes[i - 1] == -86) {
                                    while (bytesAvailable < 101) {
                                        bytesAvailable = mmInStream.available();
                                    }
                                    for (int j = 0; j < 100; j++) {
                                        tab[j] = packetBytes[j];
                                    }
                                }
                            }
                        }
                        FillArray(tab);
                    }
                } catch (IOException ex) {
                    Log.d(TAG, "Stop the thread");
                    stopWorker = true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        workerThread.start();
    }

    private void SendArray(ArrayList<Integer> tab, Intent intent) {
            intent.putExtra("AIRFLOW_VALUE", tab);
            startService(intent);
    }

    private void FillArray(int[] tab) {
        for (int k : tab) {
            airFlowTab.add(k&0xFF);
        }
        if(airFlowTab.size()>199){
            SendArray(airFlowTab, airFlowIntent);
            airFlowTab.clear();
        }
    }

    public void onDestroy() {
        super.onDestroy();
        try {
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
