package com.dpain.paras.lightmeter;

/*
* TODO
* Y axis labels
* units display of the sensors
* units truncation to 2 decimal place
 * */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.List;

public class SensorActivity extends Activity implements SensorEventListener {

    // Constants
    private static final int CUSTOM_BLUE = Color.argb(255, 0, 120, 255);

    // Threads
    Handler mHandler = new Handler();

    // GraphView Components
    private static GraphView graph;
    private static List<LineGraphSeries<DataPoint>> listOfStreams;

    // Misc Labels
    private static TextView sensorStatus;

    // Button Handlers
    private static Button btnRecord;
    private static boolean btnState = false;
    private static boolean wasPaused = false;

    // Sensor Components
    private static SensorManager mSensorManager;
    private static Sensor mSensor;
    private static float[] sensorValue;
    private static double xAxisLabelCounter = 0;
    private static int sensorMax;
    private static int sensorChannel = 0;


    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_graph);

        Intent intent = getIntent();

        // Sensor components
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(intent.getIntExtra(MainActivity.SENSOR_NAME, 0));
        sensorStatus = (TextView) findViewById(R.id.sensor_status);

        FindChannels();
        sensorValue = new float[sensorChannel];
        sensorMax = (int) mSensor.getMaximumRange();
        xAxisLabelCounter = 0;

        // Misc Labels
        TextView userHint = (TextView) findViewById(R.id.txt_user_hint);
        userHint.setText(mSensor.getName() + " Channels: " + sensorChannel);
        btnRecord = (Button) findViewById(R.id.btn_record);

        GraphInit();
        GraphSeriesController(sensorChannel);

        ThreadControl();
    }

    @Override
    protected void onResume() {
        // Register a listener for the sensor.
        super.onResume();
        // wasPaused only indicates if the Activity was paused (true = yes)
        if (!btnState && wasPaused) {
            btnState = true;
            wasPaused = false;
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
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

    @Override
    public final void onSensorChanged(SensorEvent event) {
        // Don't use ArrayCopy, avoids array size mismatch due to FIFO stack
        for(int i = 0; i<sensorValue.length;i++){
            sensorValue[i] = event.values[i];
        }
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
        // Change the thickness of the data point with the difference in accuracy
    }

    // Initialize GraphView Components
    private void GraphInit() {
        LinearLayout graphContainer = (LinearLayout) findViewById(R.id.graph_container);

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

        graphContainer.addView(graph);
    }

    private void UpdateGraph(float[] sensorValue) {

        // When setToManual, add new value and set it as the new max if its the largest yet and refresh
        xAxisLabelCounter += 1;
        sensorStatus.setText("");
        try {
            for (int i = 0; i <= sensorChannel; i++) {
                // Update TextView with Sensor data
                sensorStatus.setText("Channel " + i + ": " + sensorValue[i] + " units");
                listOfStreams.get(i).appendData(new DataPoint(xAxisLabelCounter, sensorValue[i]), true, sensorMax);

                graph.getViewport().setMinX(0);
                if (sensorValue[i] <= 0) {
                    graph.getViewport().setMinY((int) sensorValue[i] - 2);
                } else graph.getViewport().setMinY(graph.getViewport().getMinY(true));

                graph.getViewport().setMaxX((int) xAxisLabelCounter + 2);
                if (graph.getViewport().getMaxY(true) < sensorValue[i]) {
                    graph.getViewport().setMaxY((int) sensorValue[i] + 2);
                } else graph.getViewport().setMaxY(graph.getViewport().getMaxY(true));

                graph.refreshDrawableState();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Toggle Sensor registers
    * btnState = true -> user wants to read sensor data
    * btnState = false -> user does not want read sensor data
    * */
    public void ToggleSensor(View view) {
        if (btnState) {
            btnState = false;
            btnRecord.setText("START");
            sensorStatus.setText("Status: Ready!");
            mSensorManager.unregisterListener(this);
        } else {
            btnState = true;
            btnRecord.setText("STOP");
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    private void ResetGraph() {
        xAxisLabelCounter = 0;
        sensorChannel = 0;
        if (btnState) {
            btnState = false;
            btnRecord.setText("START");
            sensorStatus.setText("Status: Ready!");
            mSensorManager.unregisterListener(this);
        }
    }

    private void FindChannels() {
        switch (mSensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                sensorChannel = 3;
                break;
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                sensorChannel = 1;
                break;
            case Sensor.TYPE_GRAVITY:
                sensorChannel = 3;
                break;
            case Sensor.TYPE_GYROSCOPE:
                sensorChannel = 3;
                break;
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                sensorChannel = 6;
                break;
            case Sensor.TYPE_LIGHT:
                sensorChannel = 1;
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                sensorChannel = 3;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                sensorChannel = 3;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                sensorChannel = 6;
                break;
            case Sensor.TYPE_PRESSURE:
                sensorChannel = 1;
                break;
            case Sensor.TYPE_PROXIMITY:
                sensorChannel = 1;
                break;
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                sensorChannel = 1;
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                sensorChannel = 3;
                break;
            default:
                sensorChannel = 1;
                break;
        }
    }

    // Init DataSeries with a given number of channels
    private void GraphSeriesController(int channels) {
        graph.removeAllSeries();
        listOfStreams = new ArrayList<>(channels+1);
        int colorControl = 0;
        for (int i =0;i<channels;i++) {
            listOfStreams.add(new LineGraphSeries<>(new DataPoint[]{new DataPoint(0, 0)}));
            listOfStreams.get(i).setThickness(5);
            listOfStreams.get(i).setColor(Color.argb(255, 0, 120, (255 - colorControl)));
            colorControl += 50;
            if (colorControl >= 255) {
                colorControl = 0;
            }
            graph.addSeries(listOfStreams.get(i));
        }
    }

    // Update graph every 500ms via UpdateGraph()
    private void ThreadControl() {
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
                            // Do Nothing
                        }
                    }
                }
            }
        }).start();
    }
}


