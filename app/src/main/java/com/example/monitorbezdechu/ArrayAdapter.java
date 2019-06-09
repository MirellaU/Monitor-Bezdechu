package com.example.monitorbezdechu;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class ArrayAdapter  extends android.widget.ArrayAdapter<BluetoothDevice> {
    private ArrayList<BluetoothDevice> devicesList;
    private LayoutInflater LayoutInflater;
    private int ViewResourceId;


    public ArrayAdapter(Context context, int tvResourceId, ArrayList<BluetoothDevice> devices) {
        super(context, tvResourceId, devices);
        this.devicesList = devices;
        LayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewResourceId = tvResourceId;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = LayoutInflater.inflate(ViewResourceId, null);
        BluetoothDevice device = devicesList.get(position);

        if (device != null) {
            TextView deviceName = (TextView) convertView.findViewById(R.id.device_name);
            TextView deviceAddress = (TextView) convertView.findViewById(R.id.device_address);
            if (deviceName != null) {
                deviceName.setText(device.getName());
            }
            if (deviceAddress != null) {
                deviceAddress.setText(device.getAddress());
            }
        }
        return convertView;
    }
}

