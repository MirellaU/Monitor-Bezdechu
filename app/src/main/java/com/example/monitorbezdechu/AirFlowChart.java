package com.example.monitorbezdechu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

import butterknife.ButterKnife;

public class AirFlowChart extends AppCompatActivity {

    public static final String TAG = "airFlowChartActivity";

    public static String ROLL_VALUE = "AirFlowTab";

    public ArrayList<Integer> airFlowTab = new ArrayList<Integer>();

    IntentFilter airFlowIntentFilter;

    //@BindView(R.id.chart)
    LineChart chart;
    //@BindView(R.id.airFlow_chart)
    LineChart airFlow_chart;

    private BroadcastReceiver airFlowReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(ROLL_VALUE)) {
                airFlowTab = (ArrayList<Integer>) intent.getSerializableExtra("AIRFLOW_VALUE");
                //Log.d(TAG, roll_tab.toString());
                //feedMultiple();
                addAirFlowEntry();
            }
        }
    };

    //add data to roll graph
    private void addAirFlowEntry() {
        LineData data = chart.getData();

        if (data == null) {
            data = new LineData();
            chart.setData(new LineData());
        }

        ArrayList<Entry> values = new ArrayList<>();

        for (int i = 0; i < airFlowTab.size(); i++) {
            values.add(new Entry(i,(airFlowTab.get(i))));
        }

        removeDataSet(chart);

        LineDataSet set = new LineDataSet(values, "Air Flow");
        set.setLineWidth(2.5f);
        set.setCircleRadius(0f);

        set.setColor(Color.BLUE);
        set.setCircleColor(Color.BLUE);
        set.setHighLightColor(Color.BLUE);
        set.setValueTextSize(0f);
        set.setDrawCircleHole(true);
        set.setCircleHoleColor(Color.BLUE);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
//        set.setValueTextColor(Color.RED);

        data.addDataSet(set);
        data.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    //add data to tilt graph
    private void addTiltEntry() {
        LineData data = airFlow_chart.getData();

        if (data == null) {
            data = new LineData();
            airFlow_chart.setData(new LineData());
        }

        ArrayList<Entry> values = new ArrayList<>();

        for (int i = 0; i < airFlowTab.size(); i++) {
            values.add(new Entry(i,(airFlowTab.get(i))));
        }

        removeDataSet(airFlow_chart);

        LineDataSet set = new LineDataSet(values, "Kąt pomocniczy przód-tył");
        set.setLineWidth(2.5f);
        set.setCircleRadius(4.5f);

        set.setColor(Color.RED);
        set.setCircleColor(Color.RED);
        set.setDrawCircleHole(true);
        set.setCircleHoleColor(Color.RED);
        set.setHighLightColor(Color.RED);
        set.setValueTextSize(0f);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        //set.setDrawCubic(false);
//        set.setValueTextColor(Color.RED);

        data.addDataSet(set);
        data.notifyDataChanged();
        airFlow_chart.notifyDataSetChanged();
        airFlow_chart.invalidate();
    }

    private void removeDataSet(LineChart chart) {
        LineData data = chart.getData();
        if (data != null) {
            data.removeDataSet(data.getDataSetByIndex(data.getDataSetCount() - 1));
            chart.notifyDataSetChanged();
            chart.invalidate();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_air_flow_chart);
        ButterKnife.bind(this);

        chart=(LineChart) findViewById(R.id.chart);
        airFlow_chart=(LineChart) findViewById(R.id.airFlow_chart);

        airFlowIntentFilter = new IntentFilter("AirFlowTab");

        chart.setKeepPositionOnRotation(true);
        airFlow_chart.setKeepPositionOnRotation(true);

        chart.getDescription().setEnabled(true);
        chart.getDescription().setText("");
        airFlow_chart.getDescription().setEnabled(true);
        airFlow_chart.getDescription().setText("");

        LineData data = new LineData();
        chart.setData(data);
        LineData data2 = new LineData();
        airFlow_chart.setData(data2);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(false); // no grid lines
        leftAxis.setDrawZeroLine(true);   //draw a zero line
        leftAxis.setAxisMinimum(0f); // start at -180
        leftAxis.setAxisMaximum(100f); // the axis maximum is 180

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setDrawGridLines(false); // no grid lines
        rightAxis.setDrawZeroLine(true);   //draw a zero line
        rightAxis.setAxisMinimum(0f); // start at -180
        rightAxis.setAxisMaximum(100f); // the axis maximum is 180

        XAxis xAxis = chart.getXAxis();
        xAxis.setDrawGridLines(false); //no grid lines
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); //x axis on the bottom of chart

        YAxis leftTiltAxis = airFlow_chart.getAxisLeft();
        leftTiltAxis.setDrawGridLines(false); // no grid lines
        leftTiltAxis.setDrawZeroLine(true);   //draw a zero line
        leftTiltAxis.setAxisMinimum(0f); // start at -180
        leftTiltAxis.setAxisMaximum(100f); // the axis maximum is 180

        YAxis rightTiltAxis = airFlow_chart.getAxisRight();
        rightTiltAxis.setDrawGridLines(false); // no grid lines
        rightTiltAxis.setDrawZeroLine(true);   //draw a zero line
        rightTiltAxis.setAxisMinimum(0f); // start at -180
        rightTiltAxis.setAxisMaximum(100f); // the axis maximum is 180

        XAxis xTiltAxis = airFlow_chart.getXAxis();
        xTiltAxis.setDrawGridLines(false); //no grid lines
        xTiltAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        registerReceiver(airFlowReceiver, airFlowIntentFilter);
    }

    @Override
    public void onPause () {
        super.onPause();
    }

    @Override
    public void onDestroy () {
        super.onDestroy();
        unregisterReceiver(airFlowReceiver);
    }
}
