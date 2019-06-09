package com.example.monitorbezdechu;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DataActivity extends AppCompatActivity {

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String TAG = "DataActivity";
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket mSocket;
    private Handler mHandler;
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothAdapter mBluetoothAdapter;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    boolean stopWorker;

    public ArrayList <Integer> airFlowTab=new ArrayList<Integer>();
    Intent airFlowIntent;

    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status

    //@BindView(R.id.name)
    public TextView name;
    //@BindView(R.id.address)
    public TextView address;
    //@BindView(R.id.link_status)
    public TextView link_status;
    //@BindView(R.id.message)
    public TextView message;
    public Button charts_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);
        ButterKnife.bind(this);

        name = (TextView) findViewById(R.id.name);
        address = (TextView) findViewById(R.id.address);
        link_status = (TextView) findViewById(R.id.link_status);
        message = (TextView) findViewById(R.id.message);
        charts_btn = (Button) findViewById(R.id.charts_btn);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        name.setText(mDeviceName);
        address.setText(mDeviceAddress);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        feedMultiple();

        airFlowIntent = new Intent(this,AirFlowService.class);

        charts_btn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DataActivity.this,AirFlowChart.class);
                startActivity(intent);
            }
        });
//        mHandler = new Handler() {
//            public void handleMessage(android.os.Message msg) {
//                if (msg.what == MESSAGE_READ) {
//                    String readMessage = null;
//                    try {
//                        readMessage = new String((byte[]) msg.obj, "UTF-8");
//                    } catch (UnsupportedEncodingException e) {
//                        Log.d(TAG,e.toString());
//                    }
//                    message.setText(readMessage);
//                }
//                if (msg.what == CONNECTING_STATUS) {
//                    if (msg.arg1 == 1)
//                        link_status.setText("Connected");
//                    else
//                        link_status.setText("Connection Failed");
//                }
//            }

//        };
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
                    //final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
                    //return (BluetoothSocket) m.invoke(device, uuid);
                } catch (Exception e) {
                    fail = true;
                    Log.d(TAG, "Could not create RFComm Connection",e);
                }
                // Establish the Bluetooth socket connection.
                try {
                    mSocket.connect();
                    Log.d(TAG,"Connected");
                    fail=false;
//                    mHandler.obtainMessage(CONNECTING_STATUS, 1, -1)
//                            .sendToTarget();
                } catch (IOException connectException) {
                    // Unable to connect; close the socket and return.
                    try {
                        mSocket = createBluetoothSocket(device);
                        mSocket.connect();
                        Log.d(TAG, "Connected");
//                        mHandler.obtainMessage(CONNECTING_STATUS, 1, -1)
//                                .sendToTarget();
                        fail=false;
                    } catch(IOException ee)
                    { Log.d(TAG, "Not connected");
                        try {
                            mSocket.close();
                            Log.d(TAG, connectException.toString());
//                            mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
//                                    .sendToTarget();
                        } catch (IOException closeException) {
                            Log.d(TAG, "Could not close the client socket", closeException);
//                            mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
//                                    .sendToTarget();
                        }
                    }
                }
                if (fail == false) {
                    beginListenForData();
                    Log.d(TAG, "starting beginListenForData");
                    fail=true;
                    link_status.setText("Connected");
                } else{
                    link_status.setText("Connection failed");
                }
            }
        }.start();
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            //final Method m = device.getClass().getMethod("createRfcommSocketToServiceRecord", new Class[]{int.class});
            Log.d(TAG, "Create Insecure RFComm Connection");
            return (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(device, 1);
        } catch (Exception e) {
            Log.d(TAG, "Could not create Insecure RFComm Connection",e);
        }
        return  device.createRfcommSocketToServiceRecord(uuid);
    }

    void beginListenForData()
    {
        Log.d(TAG,"Starting");
        BluetoothSocket mmSocket;
        OutputStream mmOutStream;
        InputStream mmInStream;
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];

        mmSocket = mSocket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = mSocket.getInputStream();
                tmpOut = mSocket.getOutputStream();
                Log.d(TAG, "Get the data");
            } catch (IOException e) {
                Log.d(TAG, "Could not get a message",e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;

        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                Log.d(TAG,"I'm in worker thread");
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    //Log.d(TAG,"After while statement");
                    try
                    {
                        int bytesAvailable = mmInStream.available();
                        if(bytesAvailable > 0)
                        {
                            //Log.d(TAG,"Available data");
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                int [] dataInt = new int [packetBytes.length];
                                Log.d(TAG,"Długość: "+packetBytes.length);
                                if(packetBytes.length>0) {
                                    for(int j=0; j<packetBytes.length;j++){
                                        dataInt[j]=packetBytes[j];}

                                    Log.d(TAG,Arrays.toString(dataInt));

                                    for (int k : dataInt)
                                    {
                                        airFlowTab.add(k);
                                    }
                                    SendArray("AIRFLOW_VALUE", airFlowTab, airFlowIntent);

                                    dataInt = null;
                                    airFlowTab.clear();
                                }
//                                if(b == delimiter)
//                                {
//                                    byte[] encodedBytes = new byte[readBufferPosition];
//                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
//                                    final String data = new String(encodedBytes, "UTF-8");
//                                    readBufferPosition = 0;
//
//                                    //message.setText(Arrays.toString(dataInt));
//                                    Log.d(TAG,Arrays.toString(dataInt));
//
//
//                                    handler.post(new Runnable()
//                                    { public void run()
//                                        {
//                                            message.setText(data);
//                                        }
//                                    });
//                                }
                                else
                                {
                                    Log.d(TAG,"Something gone wrong");
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        Log.d(TAG,"Stop the thread");
                        stopWorker = true;
                    }
                }
            }
        });
        workerThread.start();
    }

    private void SendArray (String name, ArrayList<Integer> tab, Intent intent ) {
            intent.putExtra(name, tab);
            //Log.d(TAG, tab.toString());
            startService(intent);
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
