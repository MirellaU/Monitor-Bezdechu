package com.example.monitorbezdechu;

import android.app.IntentService;
import android.content.Intent;

import java.util.ArrayList;

public class AirFlowService extends IntentService {

    public static final String TAG = "AirFlowService";

    public AirFlowService() {
        super("AirFlowService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        ArrayList<Integer> airFlowTab = (ArrayList<Integer>) intent.getSerializableExtra("AIRFLOW_VALUE");
        //Log.d(TAG, force1_tab.toString());
        Intent force1Intent = new Intent("AirFlowTab");
        force1Intent.putExtra("AIRFLOW_VALUE", airFlowTab);
        sendBroadcast(force1Intent);
    }
}
