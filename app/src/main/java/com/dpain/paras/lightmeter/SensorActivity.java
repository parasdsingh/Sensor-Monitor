package com.dpain.paras.lightmeter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.List;


public class SensorActivity extends Activity implements SensorEventListener {
    public static final int CUSTOM_BLUE = Color.argb(255,0,120,255);
    private static double graph2LastXValue = 0;
    private static int sensorMax;
    private Handler mHandler;
    private SensorManager mSensorManager;
    private Sensor mLight;
    private TextView sensorStatus, userHint;
    private Button btnRecord;
    private LinearLayout graphContainer;
    private GraphView graph;
    private LineGraphSeries<DataPoint> dataStream;
    // States to handle sensor registers
    private boolean btnState=false;
    private boolean wasPaused = false;
    private float sensorValue=0;

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Hide Status bar
        this.getWindow().setFlags(WindowManager.LayoutParams.
                FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();

        // Get an instance of the sensor service, and use that to get an instance of
        // a particular sensor.
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorStatus = (TextView) findViewById(R.id.sensor_status);

        GraphInit();

        userHint = (TextView) findViewById(R.id.txt_user_hint);
        userHint.setVisibility(View.INVISIBLE);
        btnRecord = (Button) findViewById(R.id.btn_record);
        btnState = false;
        wasPaused = false;

        if(SensorCheck()) {
            // Start the thread
            new Thread(new Runnable() {
                @Override
                public void run() {
                        while (true) {
                            if (btnState) {
                            try {
                                Thread.sleep(500);
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        UpdateGraph(sensorValue);
                                    }
                                });
                            } catch (Exception e) {
                                // Do nothing
                            }
                        }
                    }
                }
            }).start();
        }
    }

    private void GraphInit() {
        graphContainer = (LinearLayout) findViewById(R.id.graph_container);
        dataStream = new LineGraphSeries<>(new DataPoint[]{new DataPoint(0, 0)});
        dataStream.setThickness(5);
        dataStream.setColor(CUSTOM_BLUE);
        sensorMax = (int) mLight.getMaximumRange();
        graph = new GraphView(this);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMinY(0);

        graph.getViewport().setScalable(true);
        // graph.setHorizontalScrollBarEnabled(true);

        graph.getGridLabelRenderer().setGridColor(CUSTOM_BLUE);
        graph.getGridLabelRenderer().setHorizontalLabelsColor(CUSTOM_BLUE);
        graph.getGridLabelRenderer().setVerticalLabelsColor(CUSTOM_BLUE);
        graph.getGridLabelRenderer().setHorizontalAxisTitleColor(CUSTOM_BLUE);
        graph.getGridLabelRenderer().setVerticalAxisTitleColor(CUSTOM_BLUE);
        // graph.getGridLabelRenderer().setHorizontalAxisTitle("Reading");
        // graph.getGridLabelRenderer().setVerticalAxisTitle("Luminance");

        graph.addSeries(dataStream); // data
        graphContainer.addView(graph);
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        sensorValue = event.values[0];
        // Update TextView with Sensor data
        sensorStatus.setText("Luminance: " + sensorValue + " Lux");
    }

    private void UpdateGraph(float sensorValue) {
        // When unlocked, add new value and set it as the new max if its the largest yet and refresh
        graph2LastXValue += 1;
        dataStream.appendData(new DataPoint(graph2LastXValue, sensorValue), true, sensorMax);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMinY(0);

        graph.getViewport().setMaxX((int)graph2LastXValue + 2);
        if(graph.getViewport().getMaxY(true) < sensorValue){
            graph.getViewport().setMaxY((int) sensorValue + 2);
        } else graph.getViewport().setMaxY(graph.getViewport().getMaxY(true));

        graph.refreshDrawableState();
    }

    @Override
    protected void onResume() {
        // Register a listener for the sensor.
        super.onResume();
        // wasPaused only indicates if the Activity was paused (true = yes)
        if (!btnState && wasPaused) {
            btnState = true;
            wasPaused = false;
            mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        // Be sure to unregister the sensor when the activity pauses.
        super.onPause();
        if (btnState) {
            btnState = false;
            wasPaused = true;
            mSensorManager.unregisterListener(this);
        }
    }

    // Finds all light sensors and returns false if none were found
    private boolean SensorCheck() {
        List<Sensor> mList = mSensorManager.getSensorList(Sensor.TYPE_LIGHT);
        if (mList == null) {
            sensorStatus.setText("Status: Sensor unavailable!");
            btnRecord.setEnabled(false);
            return false;
        }
        sensorStatus.setText("Status: Ready");
        btnRecord.setEnabled(true);
        return true;
    }

    /* Toggle Sensor registers
    * btnState = true -> user wants to read sensor data
    * btnState = false -> user does not want read sensor data
    * */
    public void ToggleSensor(View view) {
        if (btnState) {
            btnState = false;
            btnRecord.setText("START");
            userHint.setVisibility(View.INVISIBLE);
            mSensorManager.unregisterListener(this);
        } else {
            btnState = true;
            btnRecord.setText("STOP");
            userHint.setVisibility(View.VISIBLE);
            mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }
}
