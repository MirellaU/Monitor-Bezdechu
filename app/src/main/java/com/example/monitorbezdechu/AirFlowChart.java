package com.example.monitorbezdechu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Collections;

import butterknife.ButterKnife;

public class AirFlowChart extends AppCompatActivity {

    private static final String TAG = "airFlowChartActivity";
    private static final String ROLL_VALUE = "AirFlowTab";
    private ArrayList<Integer> airFlowTab = new ArrayList<>();
    private IntentFilter airFlowIntentFilter;
    private final Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALL);
    private Ringtone r;
    private int pick_numbers=0;
    private int pick_number=0;
    private int n=0;

    private LineChart chart;
    private LineChart airFlow_chart;

    private final BroadcastReceiver airFlowReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(ROLL_VALUE)) {
                airFlowTab = (ArrayList<Integer>) intent.getSerializableExtra("AIRFLOW_VALUE");
                addAirFlowEntry();
                addDetectedEntry();
            }
        }
    };

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

        LineDataSet set = new LineDataSet(values, "Air Flow");
        set.setLineWidth(1.0f);
        set.setDrawCircles(false);

        set.setColor(Color.BLUE);
        set.setCircleColor(Color.BLUE);
        set.setHighLightColor(Color.BLUE);
        set.setValueTextSize(0f);
        set.setDrawCircleHole(false);
        set.setCircleHoleColor(Color.BLUE);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
//        set.setValueTextColor(Color.RED);

        removeDataSet(chart);

        data.addDataSet(set);
        data.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    private void addDetectedEntry() {
        ArrayList <Double> hann = HannWindow();
        Log.d(TAG,"Okno Hanna" + hann);
        ArrayList <Double> splot = new ArrayList<>();

        Convolve(airFlowTab,airFlowTab.size(),hann,hann.size(),splot);
        Log.d(TAG,"Splot "+splot);
        pick_number = PickDetection(splot);
        Log.d(TAG,"Detekcja pików: "+pick_number);
        //pick_number.setText(liczba_pikow);

        LineData data = airFlow_chart.getData();

        if (data == null) {
            data = new LineData();
            airFlow_chart.setData(new LineData());
        }

        ArrayList<Entry> values = new ArrayList<>();

        for (int i = 0; i < splot.size(); i++) {
            values.add(new Entry(i,(splot.get(i).intValue())));
        }

        LineDataSet set = new LineDataSet(values, "Detected picks");
        set.setLineWidth(1.0f);
        set.setDrawCircles(false);

        set.setColor(Color.BLUE);
        set.setCircleColor(Color.BLUE);
        set.setHighLightColor(Color.BLUE);
        set.setValueTextSize(0f);
        set.setDrawCircleHole(false);
        set.setCircleHoleColor(Color.BLUE);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        removeDataSet(airFlow_chart);

        data.addDataSet(set);
        data.notifyDataChanged();
        airFlow_chart.notifyDataSetChanged();
        airFlow_chart.invalidate();

        if(n>1) {
            if(pick_numbers<5){
                try {
                    r= RingtoneManager.getRingtone(getApplicationContext(), notification);
                    r.play();
                    AlertDialog alertDialog = new AlertDialog.Builder(AirFlowChart.this).create();
                    alertDialog.setTitle(getString(R.string.alarm));
                    alertDialog.setMessage(getString(R.string.instruction));
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.button),
                            (dialog, which) -> {
                                dialog.dismiss();
                                r.stop();
                            });
                    alertDialog.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            n=0;
            pick_numbers=0;
        } else {
            n++;
            pick_numbers+=pick_number;
            Log.d(TAG,"Pick number " + pick_numbers);
        }
    }

    private void Convolve(ArrayList<Integer> Signal, int SignalLen,
                          ArrayList<Double> Kernel, int KernelLen,
                          ArrayList<Double> Result)
    {
        for (int n = 0; n < SignalLen + KernelLen - 1; n++)
        {
            int kmin, kmax, k;

            Result.add(n,0.0);

            kmin = (n >= KernelLen - 1) ? n - (KernelLen - 1) : 0;
            kmax = (n < SignalLen - 1) ? n : SignalLen - 1;

            double sum =0;
            for (k = kmin; k <= kmax; k++)
            {
                sum+=Signal.get(k) * Kernel.get(n-k);
                Result.set(n,sum);
            }
        }
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

        chart= findViewById(R.id.chart);
        airFlow_chart= findViewById(R.id.airFlow_chart);

        airFlowIntentFilter = new IntentFilter("AirFlowTab");

        chart.setKeepPositionOnRotation(true);
        chart.getDescription().setEnabled(true);
        chart.getDescription().setText("");

        airFlow_chart.setKeepPositionOnRotation(true);
        airFlow_chart.getDescription().setEnabled(true);
        airFlow_chart.getDescription().setText("");

        LineData data = new LineData();
        chart.setData(data);
        LineData airFlowData = new LineData();
        airFlow_chart.setData(airFlowData);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setDrawZeroLine(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(300f);

        YAxis leftAxis2 = airFlow_chart.getAxisLeft();
        leftAxis2.setDrawGridLines(false);
        leftAxis2.setDrawZeroLine(true);
        leftAxis2.setAxisMinimum(0f);
        leftAxis2.setAxisMaximum(3000f);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setDrawGridLines(false);
        rightAxis.setDrawZeroLine(true);
        rightAxis.setAxisMinimum(0f);
        rightAxis.setAxisMaximum(300f);

        YAxis rightAxis2 = airFlow_chart.getAxisRight();
        rightAxis2.setDrawGridLines(false);
        rightAxis2.setDrawZeroLine(true);
        rightAxis2.setAxisMinimum(0f);
        rightAxis2.setAxisMaximum(3000f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        XAxis xAxis2 = airFlow_chart.getXAxis();
        xAxis2.setDrawGridLines(false);
        xAxis2.setPosition(XAxis.XAxisPosition.BOTTOM);

        registerReceiver(airFlowReceiver, airFlowIntentFilter);
    }

    private ArrayList<Double> Sin(ArrayList<Double> airFlowTab1) {
        ArrayList<Double> result = new ArrayList<>(Collections.nCopies(airFlowTab1.size(), 0.0));
        for(int i=0; i<airFlowTab1.size(); i++) {
            if (airFlowTab1.get(i) == 0) {
                result.set(i,1.0);
            } else {
                double res = Math.sin(Math.PI * airFlowTab1.get(i)) / (Math.PI * airFlowTab1.get(i));
                result.set(i, res);
            }
        }
        Log.d(TAG,"Sin: "+result.toString());
        return result;
    }

    private ArrayList <Double> HannWindow() {
        double fc=0.025;
        double b = 0.174;
        double Nn = Math.round((4 / b));
        if (Nn%2==0) {
            Nn = Nn+1;
        } else {Nn=Nn;}
        int N=(int)Nn;
        ArrayList <Double> n = new ArrayList<>(Collections.nCopies(N, 0.0));
        n.set(0,0.0);
        for(int i=0;i<n.size()-1;i++){
            n.set(i+1,i+1.0);
        }
        //macierz okna Hanna
        ArrayList <Double> window = new ArrayList<>((Collections.nCopies(n.size(), 0.0)));
        for(int i=0;i<n.size();i++) {
            window.set(i, 0.5 * (1 - Math.cos(2 * Math.PI * n.get(i) / (Nn - 1))));
        }
        Log.d(TAG,"Okno :" + window);
        //iloczyn funkcji sinc i okna
        ArrayList <Double> funkction_sinc = new ArrayList<>((Collections.nCopies(window.size(), 0.0)));
        for(int i=0;i<window.size();i++) {
            funkction_sinc.set(i, Math.sin(2 * fc * (n.get(i) - (Nn - 1) / 2)));
        }
        funkction_sinc=Sin(funkction_sinc);
        Log.d(TAG,"Iloczyn funkcji sin i okna: " + funkction_sinc);
        ArrayList <Double>result = new ArrayList<>((Collections.nCopies(window.size(), 0.0)));
        double mnozenie;
        for (int i=0;i<window.size();i++) {
            mnozenie = funkction_sinc.get(i)*window.get(i);
            result.set(i,mnozenie);
        }
        Log.d(TAG,"Wynik: "+result);
        return result;
    }

    private int PickDetection(ArrayList<Double> signal) {
        //zwraca ile razy sredni poziom sygnalu zostal przekroczony
        //w sygnale, co mozna przetlumaczyc na liczbe pikow
        int sum = 0;
        for(int i=0; i<signal.size()-1;i++) {
            sum += signal.get(i);
        }
        int mean = sum/(signal.size());
        int picks = 0;
        for(int i=0; i<signal.size()-1;i++) {
            if (signal.get(i) <= mean && signal.get(i+1)>mean) {
                picks += 1;
            }
        }
        return picks;
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
