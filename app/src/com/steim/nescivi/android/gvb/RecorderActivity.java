package com.steim.nescivi.android.gvb;

import java.util.ArrayList;
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

public class RecorderActivity extends Activity implements Runnable {
	public static final String INTENT_ACCELERATOR = "com.steim.nescivi.android.gvb.Activity.accelerator";
	public static final String INTENT_LINEARACCELERATOR = "com.steim.nescivi.android.gvb.Activity.linearaccelerator";
	public static final String INTENT_ORIENTATION = "com.steim.nescivi.android.gvb.Activity.orientation";
	public static final String INTENT_MAGNETIC = "com.steim.nescivi.android.gvb.Activity.magnetic";
	public static final String INTENT_GYRO = "com.steim.nescivi.android.gvb.Activity.gyro";
	public static final String INTENT_LIGHT = "com.steim.nescivi.android.gvb.Activity.light";
	public static final String INTENT_SAMPLING_RATE = "com.steim.nescivi.android.gvb.Activity.samplingrate";
	private static final int THREAD_UPDATE_GRAPH_DELAY = 250;
	
	private SensorRecorder mRecorder;
	private boolean mAccelerator, mOrientation, mMagnetic, mGyro, mLight, mLinearAccelerator;
	private int mSamplingRate;
	private GraphicalView mChartView;
	private XYMultipleSeriesDataset mDataset;
	private XYMultipleSeriesRenderer mRenderer;
	private Thread mThread;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.recorder);
		
		// Get parameters for starting
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mAccelerator = extras.getBoolean(INTENT_ACCELERATOR);
			mLinearAccelerator = extras.getBoolean(INTENT_LINEARACCELERATOR);
			mOrientation = extras.getBoolean(INTENT_ORIENTATION);
			mMagnetic = extras.getBoolean(INTENT_MAGNETIC);
			mGyro = extras.getBoolean(INTENT_GYRO);
			mLight = extras.getBoolean(INTENT_LIGHT);
			mSamplingRate = extras.getInt(INTENT_SAMPLING_RATE);
			
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		// Prepare members
		mRecorder = new SensorRecorder(this);		
		// Initialize UI elements
		populateSensorSpinner();
		setButtonListeners();
		
		startRecording();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if (mChartView == null) {
			LinearLayout layout = (LinearLayout) findViewById(R.id.graph);
			//Create dataset and renderer
			mDataset = createGraphDataset();
			mRenderer = createGraphRenderer();
			mChartView = ChartFactory.getLineChartView(this, mDataset, mRenderer);
			layout.addView(mChartView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		}
		else {
			mChartView.repaint();
		}
		
		// start chart periodic update thread
		mThread = new Thread(this);
		mThread.start();
	}
	
	@Override
	protected void onPause() {
		super.onPause();

		// stop chart update
		mThread.interrupt();
		mThread = null;
		
		stopRecording();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
		mRecorder = null;
	}
	
	private void startRecording() {
		try {
			mRecorder.startRecording(mAccelerator, mOrientation, mMagnetic, mGyro, mLight, mLinearAccelerator, mSamplingRate);
		} catch (StorageErrorException e) {
			manageError(e.message);
		}
		
	}
	
	private void stopRecording() {
		if (mRecorder.isRunning()) mRecorder.stopRecording();
				
		setResult(RESULT_OK);
		finish();
	}
	
	private void manageError(String error) {
		//TODO display information about error
		Toast.makeText(this, error, Toast.LENGTH_LONG).show();
		
		setResult(RESULT_CANCELED);
		finish();
	}
	
    private void populateSensorSpinner() {
    	ArrayList<String> al = new ArrayList<String>();
    	if (mAccelerator) al.add(SensorOutputWriter.TYPE_ACCELEROMETER);
    	if (mLinearAccelerator) al.add(SensorOutputWriter.TYPE_LINEARACCELERO);    	
    	if (mOrientation) al.add(SensorOutputWriter.TYPE_ORIENTATION);
    	if (mMagnetic) al.add(SensorOutputWriter.TYPE_MAGNETIC);
    	if (mGyro) al.add(SensorOutputWriter.TYPE_GYRO);
    	if (mLight) al.add(SensorOutputWriter.TYPE_LIGHT);
    	    	
    	Object array[] = al.toArray();
    	
    	Spinner spinner = (Spinner) findViewById(R.id.SensorSelectSpinner);
    	ArrayAdapter<Object> adapter = new ArrayAdapter<Object> (this, android.R.layout.simple_spinner_item, array);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
	}
        
	private void setButtonListeners() {
		Button stopButton = (Button) findViewById(R.id.StopRecordingButton);
		
		stopButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				stopRecording();
			}
		});
	}
	
	private XYMultipleSeriesDataset createGraphDataset() {
		XYMultipleSeriesDataset result = new XYMultipleSeriesDataset();
		
		XYSeries series = new XYSeries("X");
		result.addSeries(series);
		series = new XYSeries("Y");
		result.addSeries(series);
		series = new XYSeries("Z");
		result.addSeries(series);
		
		return result;	
	}
	
	private void updateGraph() {
		// Get selection from spinner
		Spinner spinner = (Spinner) findViewById(R.id.SensorSelectSpinner);
		Object selected = spinner.getSelectedItem();
		
		// Get last readings from selected sensor
		float[][] readings = mRecorder.getBufferForSensor(selected.toString());
		
		// update dataset with new data
		XYSeries x = mDataset.getSeriesAt(0);
		XYSeries y = mDataset.getSeriesAt(1);
		XYSeries z = mDataset.getSeriesAt(2);
		
		x.clear(); y.clear(); z.clear();
		for (float[] item: readings) {
			x.add(item[0], item[1]);
			y.add(item[0], item[2]);
			z.add(item[0], item[3]);
		}
		
		// tell the graph to repaint itself
		mChartView.repaint();
		
		// schedule next repaint
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

//	private XYMultipleSeriesDataset createSampleDataset() {
//		XYMultipleSeriesDataset result = new XYMultipleSeriesDataset();
//		
//		XYSeries series = new XYSeries("X");
//		series.add(1, 2);
//		result.addSeries(series);
//		series = new XYSeries("Y");
//		series.add(3, 4);
//		result.addSeries(series);
//		series = new XYSeries("Z");
//		series.add(5, 6);
//		result.addSeries(series);
//		
//		return result;
//	}
	
//	@Override
	public void run() {
		while (mThread != null) {
			// Update graph (but it must be run on UI thread, or can become messy.)
			runOnUiThread(new Runnable() {
			//	@Override
				public void run() {
					updateGraph();
				}
			});
			
			try {
				Thread.sleep(THREAD_UPDATE_GRAPH_DELAY);
			} catch (InterruptedException e) { }
		}
	}
}
