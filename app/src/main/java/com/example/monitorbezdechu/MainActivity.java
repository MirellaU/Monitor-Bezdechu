package com.example.monitorbezdechu;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;

import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{

    private static final String TAG = "ConnectionActivity";
    private BluetoothAdapter mBluetoothAdapter;
    private final ArrayList<BluetoothDevice> devicesList = new ArrayList<>();
    private ArrayAdapter arrayAdapter;
    private final HashSet hashSet = new HashSet();
    private final static int REQUEST_ENABLE_BT = 1;

    private Button bluetoothOnBtn;
    private ListView devicesListView;
    private Button findDevicesBtn;
    private TextView noDevicesTextView;
    private RelativeLayout loadingPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        devicesListView = findViewById(R.id.devices_list);
        findDevicesBtn = findViewById(R.id.find_devices_btn);
        bluetoothOnBtn = findViewById(R.id.bluetooth_on_btn);
        noDevicesTextView = findViewById(R.id.no_devices);
        loadingPanel = findViewById(R.id.loadingPanel);

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        devicesListView.setOnItemClickListener(MainActivity.this);

        bluetoothOnBtn.setOnClickListener(v -> {
            if (mBluetoothAdapter == null) {
                Toast.makeText(getApplicationContext(), "Brak modułu Bluetooth", Toast.LENGTH_SHORT).show();
            } else {
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent bluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                    startActivity(discoverableIntent);
                    startActivityForResult(bluetoothIntent, REQUEST_ENABLE_BT);
                } else {
                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
                    Toast.makeText(getApplicationContext(), "Bluetooth jest włączone", Toast.LENGTH_SHORT).show();
                    startActivity(discoverableIntent);
                }
            }
        });

        findDevicesBtn.setOnClickListener(v -> {
            devicesList.clear();
            if (mBluetoothAdapter.isEnabled()) {
                loadingPanel.setVisibility(View.VISIBLE);
                mBluetoothAdapter.startDiscovery();
                registerReceiver(broadcastReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
                arrayAdapter = new ArrayAdapter(MainActivity.this, R.layout.device_item, devicesList);
                devicesListView.setAdapter(arrayAdapter);
            } else {
                Toast.makeText(getApplicationContext(), "Bluetooth nie włączone", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        mBluetoothAdapter.cancelDiscovery();
        BluetoothDevice mBluetoothDevice = devicesList.get(i);
        mBluetoothDevice.createBond();
        devicesList.clear();
        arrayAdapter.notifyDataSetChanged();
        Log.d(TAG, "onItemClick: deviceName = " +  mBluetoothDevice.getName());
        Log.d(TAG, "onItemClick: deviceAddress = " + mBluetoothDevice.getAddress());
        final Intent intent = new Intent(this, DataActivity.class);
        intent.putExtra(DataActivity.EXTRAS_DEVICE_NAME, mBluetoothDevice.getName());
        intent.putExtra(DataActivity.EXTRAS_DEVICE_ADDRESS, mBluetoothDevice.getAddress());
        startActivity(intent);
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            boolean flag = true;
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                loadingPanel.setVisibility(View.GONE);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG,"Devices found " + device.getAddress());
                if(!hashSet.contains(device.getAddress())){
                    hashSet.add(device.getAddress());
                    arrayAdapter.add(device);
                    arrayAdapter.notifyDataSetChanged();
                }
                else {
                    Log.d(TAG,"To urządzenie " + device.getAddress() + " już jest na liście");
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(getApplicationContext(), "Wyszukiwanie zakończone", Toast.LENGTH_SHORT).show();
                if (arrayAdapter.getCount() == 0) {
                    noDevicesTextView.setVisibility(View.VISIBLE);
                }
            }
        }
    };

    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }
}
