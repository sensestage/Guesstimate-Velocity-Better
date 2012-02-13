package com.steim.nescivi.android.gvb;

import java.io.IOException;

import org.achartengine.ChartFactory;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.ListActivity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class ResultsListActivity extends ListActivity {
	public static final String INTENT_SENSOR_SPECIFIER = "com.steim.nescivi.android.gvb.Sensor";
	
	private Integer mSensorType;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.results_list);
		
		// Get sensor type from previous saved state, if any
		if (savedInstanceState != null) {
			mSensorType = savedInstanceState.getInt(INTENT_SENSOR_SPECIFIER);
		}
		
		// If it didn't work, take them from the intent that created us
		if (mSensorType == null) {
			mSensorType = getIntent().getExtras().getInt(INTENT_SENSOR_SPECIFIER);
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();

		fillData();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(INTENT_SENSOR_SPECIFIER, mSensorType);
	}
	
	
	private void fillData() {
		// Get logs for selected sensor
		SensorLog logs[] = null;
		try {
			String type = SensorLog.SENSOR_TYPES[mSensorType];
			logs = SensorLog.fetchLogsForSensor(type);
		}
		catch (StorageErrorException ex) {
			// FIXME: mettere controllo e ritorno a chiamante con risultato errato e mess. errore
			ex.printStackTrace();
		};
		
		// Bind data to ListView
		setListAdapter(new ArrayAdapter<SensorLog>(this, android.R.layout.simple_list_item_1, logs));
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		SensorLog item = (SensorLog) l.getItemAtPosition(position);
		//Toast.makeText(this, item.getFullPath(), Toast.LENGTH_SHORT).show();
		
		// show chart for data
		try {
			displayChartForLog(item);
		}
		catch (IOException ex) {
			Toast.makeText(this, ex.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
		}
	}

	private void displayChartForLog(SensorLog item) throws IOException {
		XYMultipleSeriesDataset dataset = createGraphDataset(item);
		XYMultipleSeriesRenderer renderer = createGraphRenderer();
		
		Intent i = ChartFactory.getLineChartIntent(this, dataset, renderer, item.displayableName(true));
		startActivity(i);
	}
	
	private XYMultipleSeriesDataset createGraphDataset(SensorLog item) throws IOException {
		XYMultipleSeriesDataset result = new XYMultipleSeriesDataset();
		XYSeries x = new XYSeries("X axis");
		XYSeries y = new XYSeries("Y axis");
		XYSeries z = new XYSeries("Z axis");
		
		float readings[][] = item.getReadings();
		for (int i = 0; i < readings.length; i++) {
			x.add(readings[i][0], readings[i][1]);
			y.add(readings[i][0], readings[i][2]);
			z.add(readings[i][0], readings[i][3]);
		}
		
		result.addSeries(x);
		result.addSeries(y);
		result.addSeries(z);		
		return result;	
	}

	private XYMultipleSeriesRenderer createGraphRenderer() {
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		// X axis
		XYSeriesRenderer serie = new XYSeriesRenderer();
		serie.setColor(Color.RED);
		renderer.addSeriesRenderer(serie);
		
		// Y axis
		serie = new XYSeriesRenderer();
		serie.setColor(Color.GREEN);
		renderer.addSeriesRenderer(serie);
		
		// Z axis
		serie = new XYSeriesRenderer();
		serie.setColor(Color.BLUE);
		renderer.addSeriesRenderer(serie);
		
		return renderer;
	}

}
