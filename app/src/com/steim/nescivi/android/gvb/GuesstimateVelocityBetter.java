package com.steim.nescivi.android.gvb;

import com.steim.nescivi.android.gvb.VelocityEstimator;
//import com.steim.nescivi.android.gvb.VelocityTransmitter;
//import com.steim.nescivi.android.gvb.CircularStringArrayBuffer;

import android.app.Activity;

import android.content.ServiceConnection;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
//import android.location.Location;
//import android.os.AsyncTask;
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

/*
import java.util.Timer;
import java.util.TimerTask;

import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.IOException;
*/


public class GuesstimateVelocityBetter extends Activity {

	Messenger mVelService = null;
//	Messenger mTransmitService = null;
/*	
	private String mHost;
	private int mPort;
	private int mBufferSize;
	private int mUpdateServerTime;
	private float mSpeed;
	private CircularStringArrayBuffer mBuffer;
*/	
//	private Timer uploadTimer;
//	private TimerTask mSendDataTimerTask;
	
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
    		switch (msg.what)
    		{
    			case VelocityEstimator.MSG_SERVER_UPDATE_MSG:
    				tv = (TextView) findViewById(R.id.TransmitterStatusTextView);
    				tv.setText( msg.getData().getString("status") );
    			//	tv.setText(String.format("%.1f m/s", msg.arg1 / 10.f ) );
    				break;
    			case VelocityEstimator.MSG_GUI_UPDATE_MSG:
    				tv = (TextView) findViewById(R.id.VelocityStatusTextView);
    				tv.setText(String.format("%.1f m/s", msg.getData().getFloat( "speed" ) ) );
    				tv = (TextView) findViewById(R.id.VelocityStatusTextView2);
    				tv.setText(String.format("%.1f km/h", msg.getData().getFloat( "speed" ) * 3.6f ) );
    				tv = (TextView) findViewById(R.id.MotionStatusTextView);
    				tv.setText( GuesstimateVelocityBetter.SpeedStates[ msg.getData().getInt( "motion" ) ] );
    				tv = (TextView) findViewById(R.id.FXMeanTextView);
    				tv.setText(String.format("%.2f m/s^2", msg.getData().getFloat( "facc_mean" ) ) );
    				tv = (TextView) findViewById(R.id.FXStdDevTextView);
    				tv.setText(String.format("%.2f m/s^2", msg.getData().getFloat( "facc_std" ) ) );
    				tv = (TextView) findViewById(R.id.SXMeanTextView);
    				tv.setText(String.format("%.2f m/s^2", msg.getData().getFloat( "sacc_mean" ) ) );
    				tv = (TextView) findViewById(R.id.SXStdDevTextView);
    				tv.setText(String.format("%.2f m/s^2", msg.getData().getFloat( "sacc_std" ) ) );
    				tv = (TextView) findViewById(R.id.GXMeanTextView);
    				tv.setText(String.format("%.2f m/s^2", msg.getData().getFloat( "gacc_mean" ) ) );
    				tv = (TextView) findViewById(R.id.GXStdDevTextView);
    				tv.setText(String.format("%.2f m/s^2", msg.getData().getFloat( "gacc_std" ) ) );
    				break;
    				
    	/*			
    			case VelocityEstimator.MSG_SPEED:
    				// add the data to the buffer:
    				//addDataToBuffer( msg.arg1 );
    				// update GUI:
    				tv = (TextView) findViewById(R.id.VelocityStatusTextView);
    				tv.setText(String.format("%.1f m/s", msg.arg1 / 10.f ) );
    				tv = (TextView) findViewById(R.id.VelocityStatusTextView2);
    				tv.setText(String.format("%.1f km/h", msg.arg1 * 0.36f ) );
    				tv = (TextView) findViewById(R.id.MotionStatusTextView);
    				tv.setText( GuesstimateVelocityBetter.SpeedStates[ msg.arg2 ] );
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
    				tv.setText(String.format("%.2f m/s^2", msg.arg2 / 100.f ) );
    				break;
    	*/
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
/*
    private boolean register_with_TransmitterService()
    {
    	if (mTransmitService == null) {
    		return false;
    	}
    	
		Message msg = Message.obtain(null, VelocityTransmitter.MSG_REGISTER_GUI_CLIENT);
		msg.replyTo = mIncomingMessenger;
		
		try {
			mTransmitService.send(msg);
			return true;
		} catch (RemoteException e) {
			return false;
		}
    }
    
    private boolean unregister_from_TransmitterService()
    {
    	if (mTransmitService == null) {
    		return false;
    	}
    	
		Message msg = Message.obtain(null, VelocityTransmitter.MSG_UNREGISTER_GUI_CLIENT);
		msg.replyTo = mIncomingMessenger;
		
		try {
			mTransmitService.send(msg);
			return true;
		} catch (RemoteException e)	{
			return false;
		}
    }
    */

	/*
	if (interval != mAccel_filter_interval)	{
    	mPrefsEdit.putFloat("Accel_filter_interval", interval);
    	mPrefsEdit.apply();
	} 
	*/   	

    
    /*
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
	    	Message msg = Message.obtain(null, VelocityEstimator.MSG_SET_THRESH_DECEL, (int) (dec1 * 100), (int) (dec2 * 100) );
	    	try {
	    		mVelService.send(msg);
	    	} catch (RemoteException e) {
	    		mVelService = null;
	    	}
    	}
    	
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
	    	Message msg = Message.obtain(null, VelocityEstimator.MSG_SET_THRESH_STILL, (int) (dec1 * 100), (int) (dec2 * 100) );
	    	try {
	    		mVelService.send(msg);
	    	} catch (RemoteException e) {
	    		mVelService = null;
	    	}
    	}
    	
    } 

    public void set_ip(){    	
    	EditText ed = (EditText) findViewById(R.id.editHost);
    	String host = ed.getText().toString();
    	
    	if (mVelService != null) {
    		Bundle b = new Bundle();
        	b.putString("host", host );
	    	Message msg = Message.obtain(null, VelocityEstimator.MSG_SET_IP );
	    	msg.setData(b);
	    	
	    	try {
	    		mVelService.send(msg);
	    	} catch (RemoteException e) {
	    		mVelService = null;
	    	}
    	}
    }
    
    public void set_port(){
    	EditText ed = (EditText) findViewById(R.id.editPort );
    	int port = 5555;
    	try {
    	    port  = Integer.parseInt( ed.getText().toString() );
    	} catch(NumberFormatException nfe) {
    	   System.out.println("Could not parse " + nfe);
    	} 


    	if (mVelService != null) {
	    	Message msg = Message.obtain(null, VelocityEstimator.MSG_SET_PORT, port );
	    	try {
	    		mVelService.send(msg);
	    	} catch (RemoteException e) {
	    		mVelService = null;
	    	}
    	}    	
    }
    
    public void set_buffer_size(){    	
    	EditText ed = (EditText) findViewById(R.id.editBuffer );
    	//mBufferSize = Integer.parseInt( ed.getText().toString() );
    	int bufferSize = 60;
    	try {
    	    bufferSize = Integer.parseInt(ed.getText().toString());
    	} catch(NumberFormatException nfe) {
    	   System.out.println("Could not parse " + nfe);
    	} 
    	
    	if (mVelService != null) {
	    	Message msg = Message.obtain(null, VelocityEstimator.MSG_SET_BUFFER_SIZE, bufferSize );
	    	try {
	    		mVelService.send(msg);
	    	} catch (RemoteException e) {
	    		mVelService = null;
	    	}
    	}
    }

    public void set_update_server(){    	
    	EditText ed = (EditText) findViewById(R.id.editUpdateServer );
    	int updateServerTime = 30000;
    	try {
    	    updateServerTime = Integer.parseInt(ed.getText().toString()) * 1000;
    	} catch(NumberFormatException nfe) {
    	   System.out.println("Could not parse " + nfe);
    	} 
    	if (mVelService != null) {
	    	Message msg = Message.obtain(null, VelocityEstimator.MSG_SET_PORT, updateServerTime );
	    	try {
	    		mVelService.send(msg);
	    	} catch (RemoteException e) {
	    		mVelService = null;
	    	}
    	}    	
    }
*/
	public void send_estimate_settings(){
    	RadioGroup rg = (RadioGroup) findViewById(R.id.radioGroupAcc);
    	int selected = rg.getCheckedRadioButtonId();
    	int sensorid = -1;
    	switch ( selected ) {
    		case R.id.radioAcc:
    			sensorid = 0;
    			break;
    		case R.id.radioLinAcc:
    			sensorid = 1;
    			break;
    	}

    	rg = (RadioGroup) findViewById(R.id.radioGroupForward);
    	selected = rg.getCheckedRadioButtonId();
    	int forwardid = -1;
    	switch ( selected ) {
    		case R.id.radioFX:
    			forwardid = 0;
    			break;
    		case R.id.radioFY:
    			forwardid = 1;
    			break;
    		case R.id.radioFZ:
    			forwardid = 2;
    			break;
    	}

    	rg = (RadioGroup) findViewById(R.id.radioGroupGravity);
    	selected = rg.getCheckedRadioButtonId();
    	int gravityid = -1;
    	switch ( selected ) {
    		case R.id.radioGX:
    			gravityid = 0;
    			break;
    		case R.id.radioGY:
    			gravityid = 1;
    			break;
    		case R.id.radioGZ:
    			gravityid = 2;
    			break;
    	}

    	rg = (RadioGroup) findViewById(R.id.radioGroupSideways);
    	selected = rg.getCheckedRadioButtonId();
    	int sideid = -1;
    	switch ( selected ) {
    		case R.id.radioSX:
    			sideid = 0;
    			break;
    		case R.id.radioSY:
    			sideid = 1;
    			break;
    		case R.id.radioSZ:
    			sideid = 2;
    			break;
    	}
    	

    	EditText ed = (EditText) findViewById(R.id.acceleration);
    	float acc = 0.3f;
    	
    	try {
    	    acc = Float.parseFloat(ed.getText().toString());
    	} catch(NumberFormatException nfe) {
    	   System.out.println("Could not parse " + nfe);
    	} 

		ed = (EditText) findViewById(R.id.steady);
    	float steady = 0.5f;
    	
    	try {
    	    steady = Float.parseFloat(ed.getText().toString());
    	} catch(NumberFormatException nfe) {
    	   System.out.println("Could not parse " + nfe);
    	} 

    	ed = (EditText) findViewById(R.id.decelThresS);
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
    	
    	ed = (EditText) findViewById(R.id.stillThres1);
    	float still1 = 0.3f;
    	try {
    	    still1 = Float.parseFloat(ed.getText().toString());
    	} catch(NumberFormatException nfe) {
    	   System.out.println("Could not parse " + nfe);
    	} 

    	ed = (EditText) findViewById(R.id.stillThres2);
    	float still2 = 0.3f;
    	try {
    	    still2 = Float.parseFloat(ed.getText().toString());
    	} catch(NumberFormatException nfe) {
    	   System.out.println("Could not parse " + nfe);
    	} 

    	ed = (EditText) findViewById(R.id.stillThresTime);
    	float stilltime = 3.0f;
    	try {
    	    stilltime = Float.parseFloat(ed.getText().toString());
    	} catch(NumberFormatException nfe) {
    	   System.out.println("Could not parse " + nfe);
    	} 

    	ed = (EditText) findViewById(R.id.editWindow);
    	int window = 200;
    	
    	try {
    	    window= Integer.parseInt(ed.getText().toString());
    	} catch(NumberFormatException nfe) {
    	   System.out.println("Could not parse " + nfe);
    	} 

    	ed = (EditText) findViewById(R.id.editUpdate);
    	int updateTime = 10;
    	
    	try {
    	    updateTime= Integer.parseInt(ed.getText().toString());
    	} catch(NumberFormatException nfe) {
    	   System.out.println("Could not parse " + nfe);
    	} 
    	
		if (mVelService != null) {
    		Bundle b = new Bundle();
	    	Message msg = Message.obtain(null, VelocityEstimator.MSG_ESTIMATE_SETTINGS );
	    	b.putInt("sensor", sensorid );
	    	b.putInt("forward", forwardid );
	    	b.putInt("side", sideid );
	    	b.putInt("gravity", gravityid );
	    	b.putInt("window", window );
	    	b.putInt("updateTime", updateTime );
	    	b.putFloat("acc", acc );
	    	b.putFloat("steady", steady );
        	b.putFloat("dec1", dec1 );
        	b.putFloat("dec2", dec2 );
        	b.putFloat("still1", still1 );
        	b.putFloat("still2", still2 );
        	b.putFloat("stilltime", stilltime );
	    	msg.setData(b);

	    	try {
	    		mVelService.send(msg);
	    	} catch (RemoteException e) {
	    		mVelService = null;
	    	}
    	}
		
	}
	
	public void send_server_settings(){
    	EditText ed = (EditText) findViewById(R.id.editUpdateServer );
    	int updateServerTime = 30000;
    	try {
    	    updateServerTime = Integer.parseInt(ed.getText().toString()) * 1000;
    	} catch(NumberFormatException nfe) {
    	   System.out.println("Could not parse " + nfe);
    	}
    	
    	ed = (EditText) findViewById(R.id.editHost);
    	String host = ed.getText().toString();

    	ed = (EditText) findViewById(R.id.editPort );
    	int port = 5555;
    	try {
    	    port  = Integer.parseInt( ed.getText().toString() );
    	} catch(NumberFormatException nfe) {
    	   System.out.println("Could not parse " + nfe);
    	} 

    	ed = (EditText) findViewById(R.id.editClient );
    	int client = 0;
    	try {
    	    client  = Integer.parseInt( ed.getText().toString() );
    	} catch(NumberFormatException nfe) {
    	   System.out.println("Could not parse " + nfe);
    	} 

    	ed = (EditText) findViewById(R.id.editBuffer );
    	//mBufferSize = Integer.parseInt( ed.getText().toString() );
    	int bufferSize = 60;
    	try {
    	    bufferSize = Integer.parseInt(ed.getText().toString());
    	} catch(NumberFormatException nfe) {
    	   System.out.println("Could not parse " + nfe);
    	}
    	
    	if (mVelService != null) {
    		Bundle b = new Bundle();
	    	Message msg = Message.obtain(null, VelocityEstimator.MSG_SERVER_SETTINGS );
        	b.putString("host", host );
        	b.putInt("port", port );
        	b.putInt("client", client );
        	b.putInt("bufferSize", bufferSize );
        	b.putInt("updateServerTime", updateServerTime );
	    	msg.setData(b);

	    	try {
	    		mVelService.send(msg);
	    	} catch (RemoteException e) {
	    		mVelService = null;
	    	}
    	}
		
	}

    
    private ServiceConnection mVelServiceConnection = new ServiceConnection()
    {
    	public void onServiceConnected(ComponentName class_name, IBinder service) {
    		mVelService = new Messenger(service);
    		
    		if (!register_with_VelocityService()){
    			return;
    		}
    		
    		/*
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

    		// uploader
    		set_ip();
    		set_port();
    		set_buffer_size();
    		set_update_server();
			*/
    		
    		send_estimate_settings();
    		send_server_settings();

    		
    		/*
    		setupBuffer();
    		setupUploader();
		*/

    		Toast.makeText(GuesstimateVelocityBetter.this, "connected to VelocityEstimator", Toast.LENGTH_SHORT).show();
    		TextView tv1 = (TextView) findViewById(R.id.EstimatorStatusTextView);
		tv1.setText(String.format("Estimation running"));
    	}
    	
    	public void onServiceDisconnected(ComponentName class_name)
    	{
    		mVelService = null;
    		Toast.makeText(GuesstimateVelocityBetter.this, "disconnected from VelocityEstimator", Toast.LENGTH_SHORT).show();
    		TextView tv1 = (TextView) findViewById(R.id.EstimatorStatusTextView);
		tv1.setText(String.format("Estimation not running"));
    	}
    };

    /*
    private ServiceConnection mTransmitServiceConnection = new ServiceConnection()
    {
    	public void onServiceConnected(ComponentName class_name, IBinder service) {
    		mTransmitService = new Messenger(service);
    		
    		if (!register_with_TransmitterService()){
    			return;
    		}
    		
//    		set_ip();
//    		set_port();
//    		set_buffer_size();
//    		set_update_server();
    		
    		Toast.makeText(GuesstimateVelocityBetter.this, "connected to VelocityTransmitter", Toast.LENGTH_SHORT).show();
    		TextView tv1 = (TextView) findViewById(R.id.TransmitterStatusTextView);
			tv1.setText(String.format("Transmitter running"));
    	}
    	
    	public void onServiceDisconnected(ComponentName class_name)
    	{
    		mTransmitService = null;
    		Toast.makeText(GuesstimateVelocityBetter.this, "disconnected from VelocityTransmitter", Toast.LENGTH_SHORT).show();
    		TextView tv1 = (TextView) findViewById(R.id.TransmitterStatusTextView);
			tv1.setText(String.format("Transmitter not running"));
    	}
    };
*/


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
        /*
        if (register_with_TransmitterService())
        {
    		Toast.makeText(GuesstimateVelocityBetter.this, "VelocityTransmitter connected", Toast.LENGTH_SHORT).show();	
        }
        */
    }

    protected void onPause() 
    {
        super.onPause();
        if (unregister_from_VelocityService())
        {
    		Toast.makeText(GuesstimateVelocityBetter.this, "VelocityEstimator disconnected", Toast.LENGTH_SHORT).show();	
        }
        /*
        if (unregister_from_TransmitterService())
        {
    		Toast.makeText(GuesstimateVelocityBetter.this, "VelocityTransmitter connected", Toast.LENGTH_SHORT).show();	
        }
        */
    }
 
    private void setButtonsListeners() {
    	Button startButton = (Button) findViewById(R.id.StartButton);
    	Button stopButton = (Button) findViewById(R.id.StopButton);
    	
    	// Start button
    	startButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startEstimateService();
//				startTransmitterService();
			}
		});
    	
    	// Stop button
    	stopButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				stopEstimateService();
//				stopTransmitterService();
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

/*    
    private void startTransmitterService()
    {
    	Intent intent = new Intent(GuesstimateVelocityBetter.this, VelocityTransmitter.class);
    	startService(intent);
    	bindService(intent, mTransmitServiceConnection, Context.BIND_AUTO_CREATE);
    	toggleUIStatus(true);
    }
    
    private void stopTransmitterService()
    {
    	Intent intent = new Intent(GuesstimateVelocityBetter.this, VelocityTransmitter.class);
    	unregister_from_TransmitterService();
    	unbindService(mTransmitServiceConnection);
    	stopService(intent);
    	//toggleUIStatus(false);
    }
*/

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

/*
    public void setupBuffer(){
    	mBuffer = new CircularStringArrayBuffer( mBufferSize );
    }

    private String formatDate(Date date) {
		String format = "yy-MM-dd HH.mm.ss";
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		return formatter.format(date); 	
	}

    private void addDataToBuffer( int newdata ){
    	// add data to circular buffer
    	String [] currentVals = new String[2];
    	Date now = new Date();
    	currentVals[0] = formatDate( now );
    	currentVals[1] = String.format("%.2f", (float) newdata * 0.36f );
    	mSpeed = (float) newdata * 0.36f;
    	mBuffer.add( currentVals );
    }
        
    public void setupUploader(){    	
        this.uploadTimer = new Timer();
    	mSendDataTimerTask = new TimerTask() {
    		public void run() {
    			uploadData();
    		}
		};
		uploadTimer.scheduleAtFixedRate( mSendDataTimerTask, 0, mUpdateServerTime );

    }
    public void uploadData(){
    	//VelocityBufferTransmitter uploader = new VelocityBufferTransmitter();
    	//uploader.execute( new String[] { mHost, String.format("%i", mPort ) } );
    	Toast.makeText(GuesstimateVelocityBetter.this, String.format("Velocity: %.2f", mSpeed ), Toast.LENGTH_SHORT).show();    	
    }
*/

    /*
    private class VelocityBufferTransmitter extends AsyncTask< String, Void, String > {
    	@Override
    	protected String doInBackground(String... urls) {
    		String response = "";
    		Toast.makeText(GuesstimateVelocityBetter.this, String.format("Velocity: %.2f", mSpeed ), Toast.LENGTH_SHORT).show();
			
    		try {
    			
    	        Socket s = new Socket( urls[0], Integer.parseInt( urls[1] ) );
    	       
    	        //outgoing stream redirect to socket
    	        OutputStream out = s.getOutputStream();
    	       
    	        PrintWriter output = new PrintWriter(out);
    	        // print the circular buffer
    	        output.println("Hello Android!");
    	        
    	        //Close connection
    	        s.close();
    	           	       
    		} catch (UnknownHostException e) {
    	        // TODO Auto-generated catch block
    	        e.printStackTrace();
    		} catch (IOException e) {
    	        // TODO Auto-generated catch block
    	        e.printStackTrace();
    		}
    	        
    		return response;
    	}
    	
    	@Override
    	protected void onPostExecute(String result) {
    		TextView tv1 = (TextView) findViewById(R.id.TransmitterStatusTextView);
			tv1.setText( result );
    	}    	
    }
    */
}

