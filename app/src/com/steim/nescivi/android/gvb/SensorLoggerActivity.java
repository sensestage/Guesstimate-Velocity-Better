package com.steim.nescivi.android.gvb;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SensorLoggerActivity extends Activity {
	private final int ACTIVITY_RECORD = 0;
//	private final int ACTIVITY_SHOW_LOGS = 1;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	
        new SensorRecorder(this);
        
        // Prepare UI elements
        populateSamplingRateSpinner();
        setButtonsListeners();
        
        // Enable UI buttons
        toggleUIStatus(false);
    }
    
	@Override
    protected void onPause() {
    	super.onPause();
    	stopLog();
    	
    }
	
	@Override
	protected void onStop() {
		super.onStop();
	}
    
    private void populateSamplingRateSpinner() {
    	Spinner spinner = (Spinner) findViewById(R.id.SamplingRateSpinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
				R.array.sampling_rate_values, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
	}
    
    private void setButtonsListeners() {
    	Button startButton = (Button) findViewById(R.id.StartButton);
    	Button stopButton = (Button) findViewById(R.id.StopButton);
    	Button showLogsButton = (Button) findViewById(R.id.ShowResultsButton);
    	
    	// Start button
    	startButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				try {
					startLog();
				} catch (SamplingRateNotSelectedException ex) {
					Toast.makeText(v.getContext(), R.string.error_sampling_rate_not_selected, Toast.LENGTH_SHORT).show();
				} catch (StorageErrorException ex) {
					String error = v.getContext().getString(R.string.error_storage_error);
					Toast.makeText(v.getContext(), error + "\n" + ex.getMessage(), Toast.LENGTH_LONG).show();					
				} catch (NoSensorSelected e) {
					Toast.makeText(v.getContext(), R.string.error_no_sensor_selected, Toast.LENGTH_SHORT).show();
				}
			}
		});
    	
    	// Stop button
    	stopButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				stopLog();
			}
		});
    	
    	// Show recorded logs button
    	showLogsButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent i = new Intent(v.getContext(), SensorsListActivity.class);
		        startActivity(i);			
			}
		});
    }

    private void startLog() throws SamplingRateNotSelectedException, StorageErrorException, NoSensorSelected {
    	// Get options for recording from view
    	boolean accelerometer = ((CheckBox) findViewById(R.id.AccelerometerCheckBox)).isChecked();
    	boolean linearaccelerometer = ((CheckBox) findViewById(R.id.LinearAccelerometerCheckBox)).isChecked();
    	boolean orientation = ((CheckBox) findViewById(R.id.OrientationCheckBox)).isChecked();
    	boolean magnetic = ((CheckBox) findViewById(R.id.MagneticCheckBox)).isChecked();
    	boolean gyro = ((CheckBox) findViewById(R.id.GyroCheckBox)).isChecked();
    	boolean light = ((CheckBox) findViewById(R.id.LightCheckBox)).isChecked();
    	int samplingRate = getSelectedSamplingRate();
    	
    	// If no sensor is selected, launch exception
    	if (!(accelerometer || linearaccelerometer || orientation || magnetic || gyro || light)) {
    		throw new NoSensorSelected();
    	}
    	
    	// Start recording
//    	mSensorRecorder.startRecording(accelerometer, orientation, magnetic, gyro, light, samplingRate);
    	
    	Intent i = new Intent(this, RecorderActivity.class);
    	i.putExtra(RecorderActivity.INTENT_ACCELERATOR, accelerometer);
    	i.putExtra(RecorderActivity.INTENT_LINEARACCELERATOR, linearaccelerometer);
    	i.putExtra(RecorderActivity.INTENT_ORIENTATION, orientation);
    	i.putExtra(RecorderActivity.INTENT_MAGNETIC, magnetic);
    	i.putExtra(RecorderActivity.INTENT_GYRO, gyro);
    	i.putExtra(RecorderActivity.INTENT_LIGHT, light);
    	i.putExtra(RecorderActivity.INTENT_SAMPLING_RATE, samplingRate);
    	startActivityForResult(i, ACTIVITY_RECORD);
    	
    	toggleUIStatus(true);
    }
    
    private void stopLog() {
    	// Stop recording (if running)
//    	if (mSensorRecorder.isRunning())
//    		mSensorRecorder.stopRecording();
    	
    	toggleUIStatus(false);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	
    	switch (requestCode) {
		case ACTIVITY_RECORD:
			stopLog();
			break;
		}
    }

	private void toggleUIStatus(boolean recording) {
		// Toggle start, stop, show logs buttons
    	findViewById(R.id.StartButton).setEnabled(!recording);
    	findViewById(R.id.StopButton).setEnabled(recording);
    	findViewById(R.id.ShowResultsButton).setEnabled(!recording);
    	
    	// Enable or disable options
    	findViewById(R.id.AccelerometerCheckBox).setEnabled(!recording);
    	findViewById(R.id.LinearAccelerometerCheckBox).setEnabled(!recording);
    	findViewById(R.id.OrientationCheckBox).setEnabled(!recording);
    	findViewById(R.id.MagneticCheckBox).setEnabled(!recording);
    	findViewById(R.id.GyroCheckBox).setEnabled(!recording);
    	findViewById(R.id.LightCheckBox).setEnabled(!recording);
    	findViewById(R.id.SamplingRateSpinner).setEnabled(!recording);
    	
    	// Change status text
    	TextView tv = ((TextView) findViewById(R.id.RecordingStatusTextView));
    	tv.setText((recording) ? R.string.status_recording_running : R.string.status_recording_stopped);
	}
    
    private int getSelectedSamplingRate() throws SamplingRateNotSelectedException {
    	Spinner spinner = (Spinner) findViewById(R.id.SamplingRateSpinner);
    	int selectedItem = spinner.getSelectedItemPosition();
    	
    	if (selectedItem == Spinner.INVALID_POSITION) {
    		throw new SamplingRateNotSelectedException();
    	}
    	
    	return SensorRecorder.SENSOR_AVAILABLE_DELAYS[selectedItem];
    }
    
    private class SamplingRateNotSelectedException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 2357996218011686837L; }
    
    private class NoSensorSelected extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = -3325254008407183499L;}
}