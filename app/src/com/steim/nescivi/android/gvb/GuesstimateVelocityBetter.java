package com.steim.nescivi.android.gvb;

//import com.mrstockinterfaces.speedometer.R;
//import com.mrstockinterfaces.speedometer.VelocityService;
import com.steim.nescivi.android.gvb.VelocityEstimator;

import android.app.Activity;

import android.content.ServiceConnection;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
//import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
//import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.EditText;
//import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
//import android.content.SharedPreferences;

public class GuesstimateVelocityBetter extends Activity {

	Messenger mVelService = null;
	
  //  private SharedPreferences mPrefs;
  //  private SharedPreferences.Editor mPrefsEdit;

	
	public static final String [] SpeedStates = {
		"Tram stopped", "Tram in steady motion", "Tram accelerating", "Tram decelerating"
	};


    class IncomingHandler extends Handler
    {
    	@Override
    	public void handleMessage(Message msg)
    	{
    		TextView tv;
    		//Vector3f vec;
    		//Location loc;
    		switch (msg.what)
    		{
    			case VelocityEstimator.MSG_SPEED:
    	    		tv = (TextView) findViewById(R.id.EstimatorStatusTextView);
    				tv.setText(String.format("Estimation running"));
    				tv = (TextView) findViewById(R.id.VelocityStatusTextView);
    				tv.setText(String.format("%.1f m/s", msg.arg1 / 10.f ) );
    				tv = (TextView) findViewById(R.id.VelocityStatusTextView2);
    				tv.setText(String.format("%.1f km/h", msg.arg1 * 0.36f ) );
    				tv = (TextView) findViewById(R.id.MotionStatusTextView);
    				tv.setText( GuesstimateVelocityBetter.SpeedStates[ msg.arg2 ] );
    				//tv.setText(String.format("hello world");
    				break;
    			case VelocityEstimator.MSG_FACC:
    				tv = (TextView) findViewById(R.id.FXMeanTextView);
    				tv.setText(String.format("%.2f m/s^2", msg.arg1 / 100.f ) );
    				tv = (TextView) findViewById(R.id.FXStdDevTextView);
    				tv.setText(String.format("%.2f m/s^2", msg.arg2 / 100f ) );
    				break;
    			case VelocityEstimator.MSG_SACC:
    				tv = (TextView) findViewById(R.id.SXMeanTextView);
    				tv.setText(String.format("%.2f m/s^2", msg.arg1 / 100.f ) );
    				tv = (TextView) findViewById(R.id.SXStdDevTextView);
    				tv.setText(String.format("%.2f m/s^2", msg.arg2 / 100f ) );
    				break;
    			case VelocityEstimator.MSG_GACC:
    				tv = (TextView) findViewById(R.id.GXMeanTextView);
    				tv.setText(String.format("%.2f m/s^2", msg.arg1 / 100.f ) );
    				tv = (TextView) findViewById(R.id.GXStdDevTextView);
    				tv.setText(String.format("%.2f m/s^2", msg.arg2 / 100f ) );
    				break;
    			default:
    				super.handleMessage(msg);
    		}
    	}
    }
    
    final Messenger mIncomingMessenger = new Messenger(new IncomingHandler());

    private boolean register_with_VelocityService()
    {
    	if (mVelService == null) {
    		return false;
    	}
    	
		Message msg = Message.obtain(null, VelocityEstimator.MSG_REGISTER_GUI_CLIENT);
		msg.replyTo = mIncomingMessenger;
		
		try {
			mVelService.send(msg);
			return true;
		} catch (RemoteException e) {
			return false;
		}
    }
    
    private boolean unregister_from_VelocityService()
    {
    	if (mVelService == null) {
    		return false;
    	}
    	
		Message msg = Message.obtain(null, VelocityEstimator.MSG_UNREGISTER_GUI_CLIENT);
		msg.replyTo = mIncomingMessenger;
		
		try {
			mVelService.send(msg);
			return true;
		} catch (RemoteException e)	{
			return false;
		}
    }
    
    public void set_sensor()
    {   
    	RadioGroup rg = (RadioGroup) findViewById(R.id.radioGroupAcc);
    	int selected = rg.getCheckedRadioButtonId();
    	int id = -1;
    	switch ( selected ) {
    		case R.id.radioAcc:
    			id = 0;
    			break;
    		case R.id.radioLinAcc:
    			id = 1;
    			break;
    	}
    	
    	if (mVelService != null) {
	    	Message msg = Message.obtain(null, VelocityEstimator.MSG_SET_SENSOR, id, 0);
	    	try {
	    		mVelService.send(msg);
	    	} catch (RemoteException e) {
	    		mVelService = null;
	    	}
    	}
    	
    	/*
    	if (interval != mAccel_filter_interval)	{
	    	mPrefsEdit.putFloat("Accel_filter_interval", interval);
	    	mPrefsEdit.apply();
    	} 
    	*/   	
    } 

    public void set_forward_axis()
    {   
    	RadioGroup rg = (RadioGroup) findViewById(R.id.radioGroupForward);
    	int selected = rg.getCheckedRadioButtonId();
    	int id = -1;
    	switch ( selected ) {
    		case R.id.radioFX:
    			id = 0;
    			break;
    		case R.id.radioFY:
    			id = 1;
    			break;
    		case R.id.radioFZ:
    			id = 2;
    			break;
    	}
    	
    	if (mVelService != null) {
	    	Message msg = Message.obtain(null, VelocityEstimator.MSG_SET_FORWARD, id, 0);
	    	try {
	    		mVelService.send(msg);
	    	} catch (RemoteException e) {
	    		mVelService = null;
	    	}
    	}
    	
    	/*
    	if (interval != mAccel_filter_interval)	{
	    	mPrefsEdit.putFloat("Accel_filter_interval", interval);
	    	mPrefsEdit.apply();
    	} 
    	*/   	
    } 

    public void set_sideways_axis()
    {   
    	RadioGroup rg = (RadioGroup) findViewById(R.id.radioGroupSideways);
    	int selected = rg.getCheckedRadioButtonId();
    	int id = -1;
    	switch ( selected ) {
    		case R.id.radioSX:
    			id = 0;
    			break;
    		case R.id.radioSY:
    			id = 1;
    			break;
    		case R.id.radioSZ:
    			id = 2;
    			break;
    	}
    	
    	if (mVelService != null) {
	    	Message msg = Message.obtain(null, VelocityEstimator.MSG_SET_SIDEWAYS, id, 0);
	    	try {
	    		mVelService.send(msg);
	    	} catch (RemoteException e) {
	    		mVelService = null;
	    	}
    	}
    	
    	/*
    	if (interval != mAccel_filter_interval)	{
	    	mPrefsEdit.putFloat("Accel_filter_interval", interval);
	    	mPrefsEdit.apply();
    	} 
    	*/   	
    } 

    public void set_gravity_axis()
    {   
    	RadioGroup rg = (RadioGroup) findViewById(R.id.radioGroupGravity);
    	int selected = rg.getCheckedRadioButtonId();
    	int id = -1;
    	switch ( selected ) {
    		case R.id.radioGX:
    			id = 0;
    			break;
    		case R.id.radioGY:
    			id = 1;
    			break;
    		case R.id.radioGZ:
    			id = 2;
    			break;
    	}
    
    	if (mVelService != null) {
	    	Message msg = Message.obtain(null, VelocityEstimator.MSG_SET_GRAVITY, id, 0);
	    	try {
	    		mVelService.send(msg);
	    	} catch (RemoteException e) {
	    		mVelService = null;
	    	}
    	}
    	
    	/*
    	if (interval != mAccel_filter_interval)	{
	    	mPrefsEdit.putFloat("Accel_filter_interval", interval);
	    	mPrefsEdit.apply();
    	} 
    	*/   	
    }
    
    public void set_window()
    {   
    	EditText ed = (EditText) findViewById(R.id.editWindow);
    	int window = 200;
    	
    	try {
    	    window= Integer.parseInt(ed.getText().toString());
    	} catch(NumberFormatException nfe) {
    	   System.out.println("Could not parse " + nfe);
    	} 
    	
    	if (mVelService != null) {
	    	Message msg = Message.obtain(null, VelocityEstimator.MSG_SET_WINDOW, window, 0);
	    	try {
	    		mVelService.send(msg);
	    	} catch (RemoteException e) {
	    		mVelService = null;
	    	}
    	}
    	
    	/*
    	if (interval != mAccel_filter_interval)	{
	    	mPrefsEdit.putFloat("Accel_filter_interval", interval);
	    	mPrefsEdit.apply();
    	} 
    	*/   	
    } 

    public void set_update_time()
    {   
    	EditText ed = (EditText) findViewById(R.id.editUpdate);
    	int window = 10;
    	
    	try {
    	    window= Integer.parseInt(ed.getText().toString());
    	} catch(NumberFormatException nfe) {
    	   System.out.println("Could not parse " + nfe);
    	} 
    	
    	if (mVelService != null) {
	    	Message msg = Message.obtain(null, VelocityEstimator.MSG_SET_UPDATETIME, window, 0);
	    	try {
	    		mVelService.send(msg);
	    	} catch (RemoteException e) {
	    		mVelService = null;
	    	}
    	}
    	
    	/*
    	if (interval != mAccel_filter_interval)	{
	    	mPrefsEdit.putFloat("Accel_filter_interval", interval);
	    	mPrefsEdit.apply();
    	} 
    	*/   	
    } 

    public void set_threshold_acceleration()
    {   
    	EditText ed = (EditText) findViewById(R.id.acceleration);
    	float acc = 0.3f;
    	
    	try {
    	    acc = Float.parseFloat(ed.getText().toString());
    	} catch(NumberFormatException nfe) {
    	   System.out.println("Could not parse " + nfe);
    	} 
    	
    	if (mVelService != null) {
	    	Message msg = Message.obtain(null, VelocityEstimator.MSG_SET_THRESH_ACC, (int) (acc * 100), 0);
	    	try {
	    		mVelService.send(msg);
	    	} catch (RemoteException e) {
	    		mVelService = null;
	    	}
    	}
    	
    	/*
    	if (interval != mAccel_filter_interval)	{
	    	mPrefsEdit.putFloat("Accel_filter_interval", interval);
	    	mPrefsEdit.apply();
    	} 
    	*/   	
    } 

    public void set_threshold_steady()
    {   
    	EditText ed = (EditText) findViewById(R.id.steady);
    	float acc = 0.3f;
    	
    	try {
    	    acc = Float.parseFloat(ed.getText().toString());
    	} catch(NumberFormatException nfe) {
    	   System.out.println("Could not parse " + nfe);
    	} 
    	
    	if (mVelService != null) {
	    	Message msg = Message.obtain(null, VelocityEstimator.MSG_SET_THRESH_STEADY, (int) (acc * 100), 0);
	    	try {
	    		mVelService.send(msg);
	    	} catch (RemoteException e) {
	    		mVelService = null;
	    	}
    	}
    	
    	/*
    	if (interval != mAccel_filter_interval)	{
	    	mPrefsEdit.putFloat("Accel_filter_interval", interval);
	    	mPrefsEdit.apply();
    	} 
    	*/   	
    } 
    
    public void set_threshold_deceleration()
    {   
    	EditText ed = (EditText) findViewById(R.id.decelThresS);
    	float dec1 = 0.3f;
    	try {
    	    dec1 = Float.parseFloat(ed.getText().toString());
    	} catch(NumberFormatException nfe) {
    	   System.out.println("Could not parse " + nfe);
    	} 

    	ed = (EditText) findViewById(R.id.decelThresM);
    	float dec2 = 0.3f;
    	try {
    	    dec2 = Float.parseFloat(ed.getText().toString());
    	} catch(NumberFormatException nfe) {
    	   System.out.println("Could not parse " + nfe);
    	} 

    	if (mVelService != null) {
	    	Message msg = Message.obtain(null, VelocityEstimator.MSG_SET_THRESH_DECEL, (int) (dec1 * 100), (int) (dec1 * 100) );
	    	try {
	    		mVelService.send(msg);
	    	} catch (RemoteException e) {
	    		mVelService = null;
	    	}
    	}
    	
    	/*
    	if (interval != mAccel_filter_interval)	{
	    	mPrefsEdit.putFloat("Accel_filter_interval", interval);
	    	mPrefsEdit.apply();
    	} 
    	*/   	
    } 


    public void set_threshold_still()
    {   
    	EditText ed = (EditText) findViewById(R.id.stillThres1);
    	float dec1 = 0.3f;
    	try {
    	    dec1 = Float.parseFloat(ed.getText().toString());
    	} catch(NumberFormatException nfe) {
    	   System.out.println("Could not parse " + nfe);
    	} 

    	ed = (EditText) findViewById(R.id.stillThres2);
    	float dec2 = 0.3f;
    	try {
    	    dec2 = Float.parseFloat(ed.getText().toString());
    	} catch(NumberFormatException nfe) {
    	   System.out.println("Could not parse " + nfe);
    	} 

    	if (mVelService != null) {
	    	Message msg = Message.obtain(null, VelocityEstimator.MSG_SET_THRESH_STILL, (int) (dec1 * 100), (int) (dec1 * 100) );
	    	try {
	    		mVelService.send(msg);
	    	} catch (RemoteException e) {
	    		mVelService = null;
	    	}
    	}
    	
    	/*
    	if (interval != mAccel_filter_interval)	{
	    	mPrefsEdit.putFloat("Accel_filter_interval", interval);
	    	mPrefsEdit.apply();
    	} 
    	*/   	
    } 

    private ServiceConnection mVelServiceConnection = new ServiceConnection()
    {
    	public void onServiceConnected(ComponentName class_name, IBinder service) {
    		mVelService = new Messenger(service);
    		
    		if (!register_with_VelocityService()){
    			return;
    		}
    		
    		set_sensor();
    		set_window();
    		set_update_time();
    		set_forward_axis();
    		set_sideways_axis();
    		set_gravity_axis();
    		set_threshold_acceleration();
    		set_threshold_deceleration();
    		set_threshold_steady();
    		set_threshold_still();
    		
    		Toast.makeText(GuesstimateVelocityBetter.this, "connected to VelocityEstimator", Toast.LENGTH_SHORT).show();
    	}
    	
    	public void onServiceDisconnected(ComponentName class_name)
    	{
    		mVelService = null;
    		Toast.makeText(GuesstimateVelocityBetter.this, "disconnected from VelocityEstimator", Toast.LENGTH_SHORT).show();
    		TextView tv1 = (TextView) findViewById(R.id.EstimatorStatusTextView);
			tv1.setText(String.format("Estimation not running"));
    	}
    };
    

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guesstimator);
        
   // 	mPrefs = getSharedPreferences("GVBetterPrefs", Context.MODE_PRIVATE);
   // 	mPrefsEdit = mPrefs.edit();

    }

    @Override
    protected void onStart() {
    	super.onStart();
    	        
        // Prepare UI elements
        setButtonsListeners();
        
        // Enable UI buttons
        toggleUIStatus(false);
    }

    protected void onResume()
    {
        super.onResume();
        if (register_with_VelocityService())
        {
    		Toast.makeText(GuesstimateVelocityBetter.this, "VelocityEstimator connected", Toast.LENGTH_SHORT).show();	
        }
    }

    protected void onPause() 
    {
        super.onPause();
        if (unregister_from_VelocityService())
        {
    		Toast.makeText(GuesstimateVelocityBetter.this, "VelocityEstimator disconnected", Toast.LENGTH_SHORT).show();	
        }
    }
 
    private void setButtonsListeners() {
    	Button startButton = (Button) findViewById(R.id.StartButton);
    	Button stopButton = (Button) findViewById(R.id.StopButton);
    	
    	// Start button
    	startButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startEstimateService();
			}
		});
    	
    	// Stop button
    	stopButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				stopEstimateService();
			}
		});
    }
    
    private void startEstimateService()
    {
    	Intent intent = new Intent(GuesstimateVelocityBetter.this, VelocityEstimator.class);
    	startService(intent);
    	bindService(intent, mVelServiceConnection, Context.BIND_AUTO_CREATE);
    	toggleUIStatus(true);
    }
    
    private void stopEstimateService()
    {
    	Intent intent = new Intent(GuesstimateVelocityBetter.this, VelocityEstimator.class);
    	unregister_from_VelocityService();
    	unbindService(mVelServiceConnection);
    	stopService(intent);
    	toggleUIStatus(false);
    }
        
    	private void toggleUIStatus(boolean recording) {
    		// Toggle start, stop, show logs buttons
        	findViewById(R.id.StartButton).setEnabled(!recording);
        	findViewById(R.id.StopButton).setEnabled(recording);

        	// Enable or disable options

        	// disable sensor select
        	findViewById(R.id.radioAcc).setEnabled(!recording);
        	findViewById(R.id.radioLinAcc).setEnabled(!recording);
        	// disable window size setting
        	findViewById(R.id.editWindow).setEnabled(!recording);
        	        	
        	// Change status text
//        	TextView tv = ((TextView) findViewById(R.id.RecordingStatusTextView));
//        	tv.setText((recording) ? R.string.status_recording_running : R.string.status_recording_stopped);
    	}


}
