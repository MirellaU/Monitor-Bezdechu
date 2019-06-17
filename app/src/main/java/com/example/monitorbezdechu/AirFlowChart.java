package com.example.monitorbezdechu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

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

    public static final String TAG = "airFlowChartActivity";

    public static String ROLL_VALUE = "AirFlowTab";

    public ArrayList<Integer> airFlowTab = new ArrayList<Integer>();

    IntentFilter airFlowIntentFilter;

    //@BindView(R.id.chart)
    LineChart chart;
    //@BindView(R.id.airFlow_chart)
    LineChart airFlow_chart;
    TextView pick;
    TextView pick_number;

    private BroadcastReceiver airFlowReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(ROLL_VALUE)) {
                airFlowTab = (ArrayList<Integer>) intent.getSerializableExtra("AIRFLOW_VALUE");
                addAirFlowEntry();
                addDetectedEntry();
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

    //add data to tilt graph
    private void addDetectedEntry() {

        ArrayList <Double> hann = HannWindow();
        Log.d(TAG,"Okno Hanna" + hann);
        ArrayList <Double> splot = new ArrayList<Double>();

        Convolve(airFlowTab,airFlowTab.size(),hann,hann.size(),splot);
        Log.d(TAG,"Splot "+splot);
        int liczba_pikow = PickDetection(splot);
        Log.d(TAG,"Detekcja pików: "+liczba_pikow);
        //pick_number.setText(liczba_pikow);
    }

    void Convolve(ArrayList <Integer> Signal, int SignalLen,
                  ArrayList <Double> Kernel, int KernelLen,
                  ArrayList <Double> Result)
    {
        for (int n = 0; n < SignalLen + KernelLen - 1; n++)
        {
            int kmin, kmax, k;

            Result.add(n,0.0);

            kmin = (n >= KernelLen - 1) ? n - (KernelLen - 1) : 0;
            kmax = (n < SignalLen - 1) ? n : SignalLen - 1;

            double suma =0;
            for (k = kmin; k <= kmax; k++)
            {
                suma+=Signal.get(k) * Kernel.get(n-k);
                Result.set(n,suma);
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

        chart=(LineChart) findViewById(R.id.chart);
        pick = findViewById(R.id.pick);
        pick_number = findViewById(R.id.pick_number);

        airFlowIntentFilter = new IntentFilter("AirFlowTab");

        chart.setKeepPositionOnRotation(true);
        chart.getDescription().setEnabled(true);
        chart.getDescription().setText("");

        LineData data = new LineData();
        chart.setData(data);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(false); // no grid lines
        leftAxis.setDrawZeroLine(true);   //draw a zero line
        leftAxis.setAxisMinimum(0f); // start at 0
        leftAxis.setAxisMaximum(255f); // the axis maximum is 180

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setDrawGridLines(false); // no grid lines
        rightAxis.setDrawZeroLine(true);   //draw a zero line
        rightAxis.setAxisMinimum(0f); // start at 0
        rightAxis.setAxisMaximum(255f); // the axis maximum is 180

        XAxis xAxis = chart.getXAxis();
        xAxis.setDrawGridLines(false); //no grid lines
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); //x axis on the bottom of chart

        registerReceiver(airFlowReceiver, airFlowIntentFilter);
    }

    ArrayList<Double> Sin (ArrayList<Double> airFlowTab1) {
        ArrayList<Double> wynik = new ArrayList<Double>(Collections.nCopies(airFlowTab1.size(), 0.0));
        for(int i=0; i<airFlowTab1.size(); i++) {
            if (airFlowTab1.get(i) == 0) {
                wynik.set(i,1.0);
            } else {
                double wyn = Math.sin(Math.PI * airFlowTab1.get(i)) / (Math.PI * airFlowTab1.get(i));
                wynik.set(i, wyn);
            }
        }
        Log.d(TAG,"Sin: "+wynik.toString());
        return wynik;
    }

    ArrayList <Double> HannWindow () {
        double fc=0.025;
        double b = 0.174;
        double Nn = Math.round((4 / b));
        if (Nn%2==0) {
            Nn = Nn+1;
        } else {Nn=Nn;}
        int N=(int)Nn;
        ArrayList <Double> n = new ArrayList<Double>(Collections.nCopies(N, 0.0));
        n.set(0,0.0);
        for(int i=0;i<n.size()-1;i++){
            n.set(i+1,i+1.0);
        }
        //macierz okna Hanna
        ArrayList <Double> okno = new ArrayList<Double>((Collections.nCopies(n.size(), 0.0)));
        for(int i=0;i<n.size();i++) {
             okno.set(i, 0.5 * (1 - Math.cos(2 * Math.PI * n.get(i) / (Nn - 1))));
        }
        Log.d(TAG,"Okno :" + okno);
        //iloczyn funkcji sinc i okna
        ArrayList <Double> funkcja_sinc = new ArrayList<Double>((Collections.nCopies(okno.size(), 0.0)));
        for(int i=0;i<okno.size();i++) {
            funkcja_sinc.set(i, Math.sin(2 * fc * (n.get(i) - (Nn - 1) / 2)));
        }
        funkcja_sinc=Sin(funkcja_sinc);
        Log.d(TAG,"Iloczyn funkcji sin i okna: " + funkcja_sinc);
        ArrayList <Double>wynik = new ArrayList<Double>((Collections.nCopies(okno.size(), 0.0)));
        double mnozenie;
        for (int i=0;i<okno.size();i++) {
            mnozenie = funkcja_sinc.get(i)*okno.get(i);
            wynik.set(i,mnozenie);
        }
        Log.d(TAG,"Wynik: "+wynik);
        return wynik;
    }
    
    int PickDetection(ArrayList<Double> sygnal) {
        //zwraca ile razy sredni poziom sygnalu zostal przekroczony
        //w sygnale, co mozna przetlumaczyc na liczbe pikow
        int suma = 0;
        for(int i=0; i<sygnal.size()-1;i++) {
            suma += sygnal.get(i);
        }
        int srednia = suma/(sygnal.size());
        int piki = 0;
        for(int i=0; i<sygnal.size()-1;i++) {
            if (sygnal.get(i) <= srednia && sygnal.get(i+1)>srednia) {
                piki += 1;
            }
        }
        return piki;
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
