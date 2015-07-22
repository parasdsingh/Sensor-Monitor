package com.dpain.paras.lightmeter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    public final static String SENSOR_NAME = "com.dpain.paras.lightmeter.MESSAGE";

    private static SensorManager mSensorManager;
    private static List<Sensor> mSensorList;

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Sensor Init
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        List<CharSequence> mSensorTitleList = new ArrayList<>();

        // GUI Init
        TextView titleMsg = (TextView) findViewById(R.id.txt_title);
        ListView sensorListView = (ListView) findViewById(R.id.sensor_list_view);

        titleMsg.setText("Choose a sensor!");
        titleMsg.setTextColor(Color.argb(255, 0, 120, 255));

        if (mSensorList == null) {
            titleMsg.setText("No sensors found :(");
        } else {
            for (int i = 0; i<mSensorList.size();i++) {
                // Copy sensor list to a local ArrayList
                mSensorTitleList.add(mSensorList.get(i).getName());
            }
            // Add to the ListView and register click listener
            sensorListView.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, mSensorTitleList));
            sensorListView.setOnItemClickListener(new DrawerItemClickListener());
        }
        sensorListView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
    }

    private class DrawerItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {

            // Create a new Intent and send it to SensorActivity
            Sensor mSensor = mSensorManager.getDefaultSensor(mSensorList.get(position).getType());
            int mSensorName = mSensor.getType();

            Intent intent = new Intent(parent.getContext(), SensorActivity.class);
            intent.putExtra(SENSOR_NAME, mSensorName);
            startActivity(intent);
        }
    }
}
