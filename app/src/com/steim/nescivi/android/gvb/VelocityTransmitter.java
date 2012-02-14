package com.steim.nescivi.android.gvb;

import android.app.Service;

//import android.app.Activity;
import android.content.Context;
//import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;



public class VelocityTransmitter extends Service {

	public static ServiceUpdateUIListener UI_UPDATE_LISTENER;
	private static MainActivity MAIN_ACTIVITY;
	
    private Messenger mTransmitService = null;
    
    public static void setMainActivity(GuesstimateVelocityBetter activity) {
    	  MAIN_ACTIVITY = activity;
    	}

    	public static void setUpdateListener(ServiceUpdateUIListener l) {
    	  UI_UPDATE_LISTENER = l;
    	}

    private Context mContext;

    static final int MSG_REGISTER_TX_CLIENT = 3;
    static final int MSG_UNREGISTER_TX_CLIENT = 4;

    
//	private Thread myThread;
	private Timer timer = new Timer();
	
	//private boolean mRunning;
	
	// this could also be a setting!
	private static final int THREAD_SEND_HTTPDATA = 30000;
		
	private CircularStringArrayBuffer mBuffer;
	private int mBufferSize;
	
	private int mUpdateTime;
	
	private String host;
	private int port;
	private String ServletUri;
	
	
		
	private TimerTask mSendDataTimerTask;
	
    class IncomingHandler extends Handler
    {
    	public void handleMessage(Message msg)
    	{
    		switch(msg.what)
    		{
    			case MSG_REGISTER_GUI_CLIENT:
    				mGuiClient = msg.replyTo;
    				break;
    			case MSG_UNREGISTER_GUI_CLIENT:
    				mGuiClient = null;
    				break;
    			case MSG_SET_IP:
    				set_ip(msg.arg1);
    				break;
    			case MSG_SET_PORT:
    				set_port(msg.arg1);
    				break;
    			case MSG_SET_BUFFER_SIZE:
    				set_buffer_size(msg.arg1);
    				break;    				
    			case MSG_SET_UPDATE_INTERVAL:
    				set_update_interval(msg.arg1);
    				break;    				
    			default:
    				super.handleMessage(msg);
    		}
    	}
    }
    
    public void set_ip( String newhost ){
    	host = newhost;
    	ServletUri = "http://" + host + ":" + Integer.toString(port);    	
    }

    public void set_port( int newport ){
    	port = newport;
    	ServletUri = "http://" + host + ":" + Integer.toString(port);    	
    }

    private void set_buffer_size( int windowsize ){
    	if ( this.mBufferSize != windowsize ){
        	mBuffer = null;
        	mBuffer = new CircularStringArrayBuffer( windowsize );    		
    	}
    	this.mBufferSize = windowsize;
    }

    private void set_update_time( int updtime ){
    	if ( this.mUpdateTime != updtime ){
    		mSendDataTimerTask.cancel();
    		timer.scheduleAtFixedRate( mSendDataTimerTask, 0, updtime );
    	}
    	this.mUpdateTime = updtime;
    }

    private Messenger mIncomingMessenger = new Messenger(new IncomingHandler());


    @Override
    public void onCreate()
    {
    	super.onCreate();
    	Log.d("VelocityEstimator", "onCreate");
    	mContext = getApplicationContext();
    //	mRunning = false;
		
		this.mLastTime = System.currentTimeMillis();
		
		// Prepare members
		mListener = new SensorListener(this);		
		mBuffer = new CircularFloatArrayBuffer( this.mWindowSize );
		
    	Toast.makeText(mContext, "VelocityEstimator starting", Toast.LENGTH_SHORT).show();        	        	    			

	}
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {            	
    	Log.d("VelocityEstimator", "onStartCommand");
		// start chart periodic update thread
    	//mRunning = true;
//		myThread = new Thread();
//		myThread.start();
		mListener.startListening( this.mType, 0, this.mForward, this.mSideways, this.mGravity );
		mCalcTimerTask = new TimerTask() {
    		public void run() {
    			updateVelocityMeasurement();
    		}
		};
		TimerTask msgTimerTask = new TimerTask() {
    		public void run() {
    			send_speed_msg();
    			send_forward_accel_msg();
    			send_sideways_accel_msg();
    			send_gravity_accel_msg();
    		}
		};
    	timer.scheduleAtFixedRate( mCalcTimerTask, 0, THREAD_UPDATE_VELOCITY_DELAY );
    	timer.scheduleAtFixedRate( msgTimerTask, 0, 1000 );
		//this.runThread();
        
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy()
    {
    	Log.d("VelocityEstimator", "onDestroy");
    	Toast.makeText(mContext, "VelocityEstimator stopping", Toast.LENGTH_SHORT).show();        	        	    			

    	mListener.stopListening();
    	
    	if (timer != null){
            timer.cancel();
        }
    	
//    	mRunning = false;
//    	unregister_GPS();
//    	mSensorManager.unregisterListener(this); 
    }
    
    @Override
    public IBinder onBind(Intent intent)
    {
    	Log.d("VelocityTransmitter", "onBind");
    	return mIncomingMessenger.getBinder();
    }
    

    private void set_sensor( int newsensor ){
    	if ( this.mType != newsensor ){
    		mListener.changeListening( newsensor, 0 );
    	}
    	this.mType = newsensor;
    }
    
    private void set_forward_axis( int newaxis ){
    	if ( newaxis != -1 ){
    		mListener.changeAxis( 0, newaxis );
    	}
    }

    private void set_sideways_axis( int newaxis ){
    	if ( newaxis != -1 ){
    		mListener.changeAxis( 1, newaxis );
    	}
    }

    private void set_gravity_axis( int newaxis ){
    	if ( newaxis != -1 ){
    		mListener.changeAxis( 2, newaxis );
    	}
    }
    
    private void set_threshold_acceleration( int newth ){
    	threshold_acceleration = (float) newth / 100.0f;
    }

    private void set_threshold_steady( int newth ){
    	threshold_steady = (float) newth / 100.0f;
    }
    
    private void set_threshold_deceleration( int newth1, int newth2 ){
    	threshold_decel_std = (float) newth1 / 100.0f;
    	threshold_decel_mean = (float) newth2 / 100.0f;
    }

    private void set_threshold_still( int newth1, int newth2 ){
    	threshold_still_side = (float) newth1 / 100.0f;
    	threshold_still_forward = (float) newth2 / 100.0f;
    }

    private void send_speed_msg()
    {
		Message msg = Message.obtain(null, MSG_SPEED, (int)(this.mSpeed * 10.f), (int) this.mState );

		if (mTransmitService != null){
    		try	{
    			mTransmitService.send(msg);
    		} catch (RemoteException e)	{
    			mTransmitService = null;
    		}
		}

    	if (mGuiClient != null)	{
    		try {
    			mGuiClient.send(msg);
    		} catch (RemoteException e) {
    			mGuiClient = null;
    		}
		}    	
    }

    private void send_forward_accel_msg()
    {
		Message msg = Message.obtain(null, MSG_FACC, (int)(this.mCurrentStats[0][0] * 100.f), (int)(this.mCurrentStats[1][0] * 100.f) );
		/*
		if (mTransmitService != null) {
    		try	{
    			mTransmitService.send(msg);
    		} catch (RemoteException e)	{
    			mTransmitService = null;
    		}
		} */

    	if (mGuiClient != null) {
    		try	{
    			mGuiClient.send(msg);
    		} catch (RemoteException e)	{
    			mGuiClient = null;
    		}
		}    	
    }

    private void send_sideways_accel_msg()
    {
		Message msg = Message.obtain(null, MSG_SACC, (int)(this.mCurrentStats[0][1] * 100.f), (int)(this.mCurrentStats[1][1] * 100.f) );
		/*
		if (mTransmitService != null) {
    		try	{
    			mTransmitService.send(msg);
    		} catch (RemoteException e)	{
    			mTransmitService = null;
    		}
		} */

    	if (mGuiClient != null) {
    		try	{
    			mGuiClient.send(msg);
    		} catch (RemoteException e)	{
    			mGuiClient = null;
    		}
		}    	
    }

    private void send_gravity_accel_msg()
    {
		Message msg = Message.obtain(null, MSG_GACC, (int)(this.mCurrentStats[0][2] * 100.f), (int)(this.mCurrentStats[1][2] * 100.f) );
		/*
		if (mTransmitService != null) {
    		try	{
    			mTransmitService.send(msg);
    		} catch (RemoteException e)	{
    			mTransmitService = null;
    		}
		} */

    	if (mGuiClient != null) {
    		try	{
    			mGuiClient.send(msg);
    		} catch (RemoteException e)	{
    			mGuiClient = null;
    		}
		}    	
    }

	public void updateVelocityMeasurement(){
		// calculate the time interval:
		long newtime = System.currentTimeMillis();
		this.mDeltaTime = newtime - this.mLastTime;
		this.mLastTime = newtime;
		
		// get readings
		float [] currentReadings = this.mListener.getCurrentValues();
				
		// put readings in circular buffer
		mBuffer.add( currentReadings );
		
		// calculate mean and standard deviation of buffers
		//mCurrentStats = 
		mBuffer.getStats( mCurrentStats );
		
		// substract offset from mean
		for ( int axis = 0; axis < 3; axis++ ){
			mCurrentStats[0][axis] = mCurrentStats[0][axis] - this.mOffsets[axis];  
		}
		
		// determine state
		switch (this.mState){
			case 0: // still
				if ( mCurrentStats[1][0] > threshold_acceleration ){
					this.mState = 2;
				}
				break;
			case 1: // steady motion
				if ( mCurrentStats[1][0] > threshold_decel_std && mCurrentStats[0][0] < threshold_decel_mean ){
					this.mState = 3;
				}
				break;
			case 2: // accelerating
				if ( mCurrentStats[0][0] < threshold_steady ){
					this.mState = 1;
				}
				break;	
			case 3: // decelerating
			default:
				// could add a time window before being still
				if ( mCurrentStats[1][1] < threshold_still_side && mCurrentStats[1][0] < threshold_still_forward ){
					this.mState = 0;
				}
				break;	
			}
		
		// if state == still, adjust offset
		
		// calculate forward speed
		switch (this.mState){
		case 0: // still
			this.mSpeed = (float) 0.0;
			break;
		case 1: // steady motion
		case 2: // accelerating
		case 3: // decelerating
			this.mSpeed += mCurrentStats[0][0] * this.mDeltaTime * 0.001;
			break;
		default:
			break;
		}
		
		// send speed message
		//send_speed_msg();
	}
	
	/*
	//@Override
	public void runThread() {
		mListener.startListening( this.mType, 0, this.mForward, this.mSideways, this.mGravity );
		while (myThread != null && mRunning ) {
			updateVelocityMeasurement();
			try {
				Thread.sleep(THREAD_UPDATE_VELOCITY_DELAY);
			} catch (InterruptedException e) { }
		}
		mListener.stopListening();
	}
	*/
}
