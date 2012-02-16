package com.steim.nescivi.android.gvb;

import android.app.Service;

//import android.app.Activity;
import android.os.AsyncTask;
//import android.os.Thread;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
//import android.view.ViewGroup.LayoutParams;
//import android.widget.LinearLayout;

//import android.os.Looper;
//import android.os.HandlerThread;
//import android.os.Process;

import android.widget.CheckBox;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.IOException;

//import com.steim.nescivi.android.gvb.VelocityTransmitter.IncomingHandler;

public class VelocityEstimator extends Service {

    private Messenger mGuiClient = null;
    private Messenger mTransmitService = null;
    
//    private Messenger mIncomingMessenger;
    
//    private Looper mServiceLooper;
//    private IncomingHandler mServiceHandler;
    
    private Context mContext;

    static final int MSG_REGISTER_GUI_CLIENT = 1;
    static final int MSG_UNREGISTER_GUI_CLIENT = 2;
    static final int MSG_REGISTER_TX_CLIENT = 3;
    static final int MSG_UNREGISTER_TX_CLIENT = 4;

    static final int MSG_FACC = 10;
    static final int MSG_SACC = 11;
    static final int MSG_GACC = 12;
    static final int MSG_SPEED = 13;
    
    static final int MSG_SERVER_UPDATE_MSG = 14;
    static final int MSG_GUI_UPDATE_MSG = 15;

//    static final int MSG_REGISTER_VS_SRV = 5;
//    static final int MSG_UNREGISTER_VS_SRV = 6;
//    static final int MSG_SET_ACC_FLT_SMPS = 10;

    static final int MSG_SET_SENSOR = 20;
    static final int MSG_SET_FORWARD = 21;
    static final int MSG_SET_SIDEWAYS = 22;
    static final int MSG_SET_GRAVITY = 23;
    static final int MSG_SET_WINDOW = 24;
    static final int MSG_SET_UPDATETIME = 25;
    
    static final int MSG_SET_THRESH_ACC = 30;
    static final int MSG_SET_THRESH_STEADY = 31;
    static final int MSG_SET_THRESH_DECEL = 32;
    static final int MSG_SET_THRESH_STILL = 33;
    
    static final int MSG_ESTIMATE_SETTINGS = 39;
    static final int MSG_SERVER_SETTINGS = 40;
    
    static final int MSG_SET_IP = 50;
    static final int MSG_SET_PORT = 51;
    static final int MSG_SET_BUFFER_SIZE = 52;
    static final int MSG_SET_UPDATE_INTERVAL = 53; 

    
	private Timer timer = new Timer();

	//private Thread myThread;
	//private boolean mRunning;
			
	private SensorListener mListener;
	private CircularFloatArrayBuffer2 mBuffer;
	
	// timing:
	private long mLastTime;
	private long mDeltaTime;
	
	// sensing type (accelero, or linear acceleration)
	private int mType = 1;
	
	// window size for averaging and standard deviation
	private int mWindowSize = 200;
	private int mUpdateTime = 10;

	// offsets from 0 acceleration
	private float[] mOffsets = { (float) 0.0, (float) 0.0, (float) 0.0 };
	private float [][] mCurrentStats = { { (float) 0.0, (float) 0.0, (float) 0.0 }, { (float) 0.0, (float) 0.0, (float) 0.0 } };
	//float stats[][] = new float[2][3];
	
	private float mSpeed = (float) 0.0;
	
	private int mForward = 1;
	private int mSideways = 0;
	private int mGravity = 2;
	
	private int mState = 0;
	private float mStillTime = 0.0f;
	
	private float threshold_acceleration = 0.3f;
	private float threshold_still_side = 0.1f;
	private float threshold_still_forward = 0.1f;
	private float threshold_still_time = 3.0f;
	private float threshold_decel_std = 0.25f;
	private float threshold_decel_mean = -0.5f;
	private float threshold_steady = 0.5f;
	
	// moving average for offset:
	private float macoef = 0.99f;
	
	private float forwardsign = 1.f;

	private String mHost;
	private int mPort;
	private int mClientID;
	private int mBufferSize = 60;
	private int mUpdateServerTime = 30000;
	private CircularStringArrayBuffer mUploadBuffer;
	private String mTransmitStatus;

	private TimerTask mCalcTimerTask;
	private TimerTask mUploadTimerTask;
	private TimerTask mLogTimerTask;
	private int mLogUpdateTime = 500;

    private boolean mMakeLocalLog = false;
    private boolean mWritingLocalLog = false;
    private SensorOutputWriter mLocalLog;

    class IncomingHandler extends Handler
    {
    	/*
    	public IncomingHandler( Looper looper ){
    		super( looper );
    	}
    	*/
    	@Override
    	public void handleMessage(Message msg)
    	{
    		switch(msg.what)
    		{
    		case MSG_ESTIMATE_SETTINGS:
    			set_sensor( msg.getData().getInt("sensor") );
    			set_forward_axis( msg.getData().getInt("forward") );
				set_sideways_axis( msg.getData().getInt("side"));
				set_gravity_axis( msg.getData().getInt("gravity") );
				set_window( msg.getData().getInt("window") );
				set_update_time(msg.getData().getInt("updateTime"));
				set_threshold_acceleration( msg.getData().getFloat("acc") );
				set_threshold_steady( msg.getData().getFloat("steady") );
				set_threshold_deceleration( msg.getData().getFloat("dec1"),msg.getData().getFloat("dec2") );
				set_threshold_still( msg.getData().getFloat("still1"), msg.getData().getFloat("still2"), msg.getData().getFloat( "stilltime") );
				set_offset_macoef( msg.getData().getFloat("offsetma") );
				set_sign_forward( msg.getData().getInt("signForward") );
				mMakeLocalLog = msg.getData().getBoolean("makeLocalLog");
				mLogUpdateTime = msg.getData().getInt("updateLogTime");
				startEstimation();
    			break;
    		case MSG_SERVER_SETTINGS:
				set_ip( msg.getData().getString("host") );
				set_port( msg.getData().getInt("port") );
				set_client( msg.getData().getInt("client") );
				set_buffer_size( msg.getData().getInt("bufferSize") );
				set_update_interval( msg.getData().getInt("updateServerTime"));
				mMakeLocalLog = msg.getData().getBoolean("makeLocalLog");
				mLogUpdateTime = msg.getData().getInt("updateLogTime");
				startServerAndLog();
    			break;
   			case MSG_REGISTER_GUI_CLIENT:
    				mGuiClient = msg.replyTo;
    				break;
   			case MSG_UNREGISTER_GUI_CLIENT:
    				mGuiClient = null;
    				break;
    				/*
    			case MSG_REGISTER_TX_CLIENT:
    				mTransmitService = msg.replyTo;
    				break;
    			case MSG_UNREGISTER_TX_CLIENT:
    				mTransmitService = null;
    				break;
    				*/
    				/*
    			case MSG_SET_SENSOR:
    				set_sensor(msg.arg1);
    				break;
    			case MSG_SET_FORWARD:
    				set_forward_axis(msg.arg1);
    				break;
    			case MSG_SET_SIDEWAYS:
    				set_sideways_axis(msg.arg1);
    				break;
    			case MSG_SET_GRAVITY:
    				set_gravity_axis(msg.arg1);
    				break;
    			case MSG_SET_WINDOW:
    				set_window(msg.arg1);
    				break;
    			case MSG_SET_UPDATETIME:
    				set_update_time(msg.arg1);
    				break;
    			case MSG_SET_THRESH_ACC:
    				set_threshold_acceleration(msg.arg1);
    				break;
    			case MSG_SET_THRESH_STEADY:
    				set_threshold_steady(msg.arg1);
    				break;
    			case MSG_SET_THRESH_DECEL:
    				set_threshold_deceleration(msg.arg1, msg.arg2);
    				break;
    			case MSG_SET_THRESH_STILL:
    				set_threshold_still(msg.arg1, msg.arg2);
    				break;
    			case MSG_SET_IP:
    				set_ip( msg.getData().getString("host") );
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
    				*/
    			default:
    				super.handleMessage(msg);
    		}
    	}
    }

    private Messenger mIncomingMessenger = new Messenger(new IncomingHandler());

    @Override
    public void onCreate()
    {
    	//super.onCreate();
    	//HandlerThread thread = new HandlerThread("ServiceVelocityEstimator", Process.THREAD_PRIORITY_BACKGROUND);
        //thread.start();
        
        // Get the HandlerThread's Looper and use it for our Handler 
        //mServiceLooper = thread.getLooper();
        //mServiceHandler = new IncomingHandler(mServiceLooper);
    	//mIncomingMessenger = new Messenger( mServiceHandler );
        
    	Log.d("VelocityEstimator", "onCreate");
    	mContext = getApplicationContext();
   // 	mRunning = false;


	this.mLastTime = System.currentTimeMillis();
	
	// Prepare members
	mListener = new SensorListener(this);		
//	mBuffer = new CircularFloatArrayBuffer2( 3, this.mWindowSize );
    this.mUpdateTime = 10;
    this.mUpdateServerTime = 30000;
	this.mUploadBuffer = new CircularStringArrayBuffer( this.mBufferSize );
	
	
	Toast.makeText(mContext, "VelocityEstimator created", Toast.LENGTH_SHORT).show();
	}
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {            	
    	Log.d("VelocityEstimator", "onStartCommand");		

        return Service.START_STICKY;
    }
    
    public void startEstimation(){
		mListener.startListening( this.mType, 0, this.mForward, this.mSideways, this.mGravity, 3, this.mWindowSize );
		
		if ( mMakeLocalLog ){
			createLocalLog();
		}
		
		mCalcTimerTask = new TimerTask() {
		  public void run() {
    			updateVelocityMeasurement();
				if ( mWritingLocalLog ){
					float [] currentReadings = mListener.getCurrentValues();
					float [] logdata = { 
						(float) mState,
						mSpeed, mSpeed * 3.6f,
						mCurrentStats[0][0], mCurrentStats[1][0],
						mCurrentStats[0][1], mCurrentStats[1][1],
						mCurrentStats[0][2], mCurrentStats[1][2],
						mOffsets[0],mOffsets[1],mOffsets[2],
						mStillTime,
						currentReadings[0],currentReadings[1], currentReadings[2]  
					};
					writeLogData( logdata );
				}
		  	}
		};
		/*
		mUploadTimerTask = new TimerTask() {
		  public void run() {
    			//uploadData();
    			uploadDataTCP();
		  }
		};
		
		if ( mMakeLocalLog ){
			createLocalLog();
			mLogTimerTask = new TimerTask() {
				public void run(){
					if ( mWritingLocalLog ){
						float [] currentReadings = mListener.getCurrentValues();
						float [] logdata = { 
							(float) mState,
							mSpeed, mSpeed * 3.6f,
							mCurrentStats[0][0], mCurrentStats[1][0],
							mCurrentStats[0][1], mCurrentStats[1][1],
							mCurrentStats[0][2], mCurrentStats[1][2],
							mOffsets[0],mOffsets[1],mOffsets[2],
							mStillTime,
							currentReadings[0],currentReadings[1], currentReadings[2]  
						};
						writeLogData( logdata );
					}
				}
			}
		}
		*/
		TimerTask msgTimerTask = new TimerTask() {
    		public void run() {
			// add data to circular string buffer:
    			add_data_to_buffer();    			
			// send messages to UI:
    			send_gui_update_msg();
    		//	send_speed_msg();
    		//	send_forward_accel_msg();
    		//	send_sideways_accel_msg();
    		//	send_gravity_accel_msg();
    		}
		};
    	timer.scheduleAtFixedRate( mCalcTimerTask, 0, mUpdateTime );
    	//timer.scheduleAtFixedRate( mLogTimerTask, 0, mLogUpdateTime );
    	timer.scheduleAtFixedRate( msgTimerTask, 0, 1000 );
    	//timer.scheduleAtFixedRate( mUploadTimerTask, 0, mUpdateServerTime );

		// start chart periodic update thread
    	//mRunning = true;
		//myThread = new Thread();
		//myThread.start();
    	//this.runUploaderThread();
        
    	Toast.makeText(mContext, "VelocityEstimator starting", Toast.LENGTH_SHORT).show();
    }

    public void startServerAndLog(){
		mUploadTimerTask = new TimerTask() {
		  public void run() {
    			//uploadData();
    			uploadDataTCP();
		  }
		};
		
		/*
		if ( mMakeLocalLog ){
			createLocalLog();
			mLogTimerTask = new TimerTask() {
				public void run(){
					if ( mWritingLocalLog ){
						float [] currentReadings = mListener.getCurrentValues();
						float [] logdata = { 
							(float) mState,
							mSpeed, mSpeed * 3.6f,
							mCurrentStats[0][0], mCurrentStats[1][0],
							mCurrentStats[0][1], mCurrentStats[1][1],
							mCurrentStats[0][2], mCurrentStats[1][2],
							mOffsets[0],mOffsets[1],mOffsets[2],
							mStillTime,
							currentReadings[0],currentReadings[1], currentReadings[2]  
						};
						writeLogData( logdata );
					}
				}
			};
		}
		*/
		
    	//timer.scheduleAtFixedRate( mLogTimerTask, 0, mLogUpdateTime );
    	timer.scheduleAtFixedRate( mUploadTimerTask, 0, mUpdateServerTime );
        
    	Toast.makeText(mContext, "VelocityEstimator sender and logger starting", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy()
    {
    	Log.d("VelocityEstimator", "onDestroy");
    	Toast.makeText(mContext, "VelocityEstimator stopping", Toast.LENGTH_SHORT).show();

    	mListener.stopListening();
    	closeLocalLog();
    	
    	if (timer != null){
	    mCalcTimerTask.cancel();
            timer.cancel();
        }    	
    }
    
    @Override
    public IBinder onBind(Intent intent)
    {
    	Log.d("VelocityEstimator", "onBind");
    	return mIncomingMessenger.getBinder();
    }
    
 
  /// --------- DATA UPLOADER -------------
    private String formatDate(Date date) {
		String format = "yy-MM-dd HH.mm.ss";
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		return formatter.format(date); 	
	}

    
    private void add_data_to_buffer( ){
    	// add data to circular buffer
    	String [] currentVals = new String[2];
    	Date now = new Date();
    	currentVals[0] = formatDate( now );
    	currentVals[1] = String.format("%.2f", mSpeed * 3.6f );
    	mUploadBuffer.add( currentVals );
    }

    /*
    public void uploadData(){
    	send_server_update_msg();
    //	Toast.makeText(mContext, String.format("Velocity: %.2f", mSpeed ), Toast.LENGTH_SHORT).show();    	
    //	VelocityBufferTransmitter uploader = new VelocityBufferTransmitter();
    //	uploader.execute( new String[] { mHost, String.format("%i", mPort ) } );
    }

    private class VelocityBufferTransmitter extends AsyncTask< String, Void, String > {
    	@Override
    	protected String doInBackground(String... urls) {
    		String response = "";
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
    	//	TextView tv1 = (TextView) findViewById(R.id.TransmitterStatusTextView);
	//	tv1.setText( result );
    	} 
    }
    */
    
    private void set_ip( String newhost ){
    	this.mHost = newhost;
    }

    private void set_port( int newport ){
    	this.mPort = newport;
    }

    private void set_client( int newclient ){
    	this.mClientID = newclient;
    }
    
    private void set_buffer_size( int windowsize ){
    	if ( this.mBufferSize != windowsize ){
        	mUploadBuffer = null;
        	mUploadBuffer = new CircularStringArrayBuffer( windowsize );
    	}
    	this.mBufferSize = windowsize;
    }

    private void set_update_interval( int updtime ){
    	/*
    	if ( this.mUpdateServerTime != updtime ){
    		mUploadTimerTask.cancel();
    		timer.scheduleAtFixedRate( mUploadTimerTask, 0, updtime );
    	}
    	*/
    	this.mUpdateServerTime = updtime;
    }

    
    /// --------- END DATA UPLOADER -------------

    private void set_sign_forward( int sign ){    	
    	this.forwardsign = (float) sign;
    }

    private void set_window( int windowsize ){
    	/*
    	if ( this.mWindowSize != windowsize ){
        	mBuffer = null;
        	mBuffer = new CircularFloatArrayBuffer2( 3, windowsize );    		
    	}
    	*/
    	this.mWindowSize = windowsize;
    }

    private void set_update_time( int updtime ){
    	/*
    	if ( this.mUpdateTime != updtime ){
    		mCalcTimerTask.cancel();
    		timer.scheduleAtFixedRate( mCalcTimerTask, 0, updtime );
    	}
    	*/
    	this.mUpdateTime = updtime;
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
    
    private void set_threshold_acceleration( float newth ){
    	threshold_acceleration = newth;
    }

    private void set_threshold_steady( float newth ){
    	threshold_steady = newth;
    }
    
    private void set_threshold_deceleration( float newth1, float newth2 ){
    	threshold_decel_std = newth1;
    	threshold_decel_mean = newth2;
    }

    private void set_threshold_still( float newth1, float newth2, float newth3 ){
    	threshold_still_side = newth1;
    	threshold_still_forward = newth2;
    	threshold_still_time = newth3;
    }

    private void set_offset_macoef( float newth1 ){
    	macoef = newth1;
    }

    private void send_server_update_msg()
    {
    	Bundle b = new Bundle();
    	Message msg = Message.obtain(null, MSG_SERVER_UPDATE_MSG );
    	b.putString("status",  mTransmitStatus );
    	msg.setData(b);
    	//Message msg = Message.obtain(null, MSG_SERVER_UPDATE_MSG, (int)(this.mSpeed * 10.f) );

    	/*
	if (mTransmitService != null){
    		try	{
    			mTransmitService.send(msg);
    		} catch (RemoteException e)	{
    			mTransmitService = null;
    		}
	}
	*/

    	if (mGuiClient != null)	{
	  try {
    			mGuiClient.send(msg);
    		} catch (RemoteException e) {
    			mGuiClient = null;
    		}
	}    	
    }
    
    private void send_gui_update_msg(){
    	Bundle b = new Bundle();
    	Message msg = Message.obtain(null, MSG_GUI_UPDATE_MSG );
    	b.putInt("motion", mState );
    	b.putFloat("speed", mSpeed );
		synchronized( this ){
			b.putFloat("facc_mean", this.mCurrentStats[0][0] );
			b.putFloat("facc_std", this.mCurrentStats[1][0]  );
			b.putFloat("sacc_mean", this.mCurrentStats[0][1] );
			b.putFloat("sacc_std", this.mCurrentStats[1][1]  );
			b.putFloat("gacc_mean", this.mCurrentStats[0][2] );
			b.putFloat("gacc_std", this.mCurrentStats[1][2]  );
			b.putFloat("offset", this.mOffsets[0]  );
		}
    	b.putFloat("sign", this.forwardsign );
    	b.putFloat("stilltime", this.mStillTime );
    	msg.setData(b);
    	if (mGuiClient != null)	{
    		try {
    			mGuiClient.send(msg);
    		} catch (RemoteException e) {
    			mGuiClient = null;
    		}
    	}    	

    }

	/*
	if (mTransmitService != null){
    		try	{
    			mTransmitService.send(msg);
    		} catch (RemoteException e)	{
    			mTransmitService = null;
    		}
	}
	*/

    /*
    private void send_speed_msg()
    {
	Message msg = Message.obtain(null, MSG_SPEED, (int)(this.mSpeed * 10.f), (int) this.mState );


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

    	if (mGuiClient != null) {
	  try	{
    		mGuiClient.send(msg);
	  } catch (RemoteException e) {
    		mGuiClient = null;
	  }
	} 
    }

    private void send_sideways_accel_msg()
    {
	Message msg = Message.obtain(null, MSG_SACC, (int)(this.mCurrentStats[0][1] * 100.f), (int)(this.mCurrentStats[1][1] * 100.f) );

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

    	if (mGuiClient != null) {
    		try	{
    			mGuiClient.send(msg);
    		} catch (RemoteException e)	{
    			mGuiClient = null;
    		}
		}    	
    }
    */

    public void updateVelocityMeasurement(){
		// calculate the time interval:
		long newtime = System.currentTimeMillis();
		this.mDeltaTime = newtime - this.mLastTime;
		this.mLastTime = newtime;
				
		// get readings
		//float [] currentReadings = this.mListener.getCurrentValues();
				
		// put readings in circular buffer
		//mBuffer.add( currentReadings );
		
		// calculate mean and standard deviation of buffers
		//float [][] curStats = mBuffer.getStats();
		//mCurrentStats = mBuffer.getStats();
		float [][] curStats = this.mListener.getCurrentStats();
		
		// reset offsets when we are in still:
		if ( this.mState == 0 ) {
			for ( int axis = 0; axis < 3; axis++ ){
				if ( curStats[0][axis] < 20.f ){
					this.mOffsets[axis] = this.mOffsets[axis] * macoef + curStats[0][axis] * (1-macoef);
				}
			}
		}
		
		// substract offset from mean
		for ( int axis = 0; axis < 3; axis++ ){
			curStats[0][axis] = curStats[0][axis] - this.mOffsets[axis];  
		}
		
		// determine state
		switch (this.mState){
			case 0: // still
				mStillTime = 0.0f;
				if ( curStats[1][0] > threshold_acceleration ){
					this.mState = 2;
					mStillTime = 0.0f;
				}
				break;
			case 1: // steady motion
				if ( curStats[1][0] > threshold_decel_std && curStats[0][0] < threshold_decel_mean ){
					this.mState = 3;
					mStillTime = 0.0f;
				}
				break;
			case 2: // accelerating
				if ( curStats[1][0] < threshold_steady ){
					this.mState = 1;
					mStillTime = 0.0f;
				}
				break;	
			case 3: // decelerating
				if ( curStats[1][0] > threshold_steady ){
					this.mState = 2;
					mStillTime = 0.0f;
				}
			default:
				break;	
			}
		if ( this.mState > 0 ){
			// 	could add a time window before being still
			if ( curStats[1][1] < threshold_still_side && curStats[1][0] < threshold_still_forward ){
				this.mStillTime += (float) mDeltaTime * 0.001;
				if ( mStillTime > threshold_still_time ){
					this.mState = 0;
				}
			}
		}
				
		// calculate forward speed
		switch (this.mState){
		case 0: // still
			this.mSpeed = (float) 0.0;
			break;
		case 2: // accelerating
			// set the sign of what is forward:
			/*
			if ( mCurrentStats[0][0] < 0 ){
				forwardsign = -1.f;				
			} else {
				forwardsign = 1.f;
			}
			*/
		case 1: // steady motion
		case 3: // decelerating
			this.mSpeed += curStats[0][0] * forwardsign * this.mDeltaTime * 0.001;
			break;
		default:
			break;
		}
		
		synchronized( this ){
			for ( int axis = 0; axis < 3; axis++ ){
				mCurrentStats[0][axis] = curStats[0][axis];
				mCurrentStats[1][axis] = curStats[1][axis]; 
			}
		}

		// send speed message
		//send_speed_msg();
	}
    	
    public void uploadDataTCP(){
    	mTransmitStatus = "no host and port defined";
		if ( mHost != null && mPort != 0 ){
			try {			
				Socket s = new Socket( mHost, mPort );
	       
	        //	outgoing stream redirect to socket
				OutputStream out = s.getOutputStream();
	       
				PrintWriter output = new PrintWriter(out);
	        // print the circular buffer
				
				String[][] bufContents = mUploadBuffer.getContents();
				output.println( mClientID );
				for (String[] item: bufContents) {		
					output.print( item[0]);
					output.print( " " );
					output.println( item[1]);	
				}
				output.flush();
				output.close();

	        //	Close connection
				s.close();
				mTransmitStatus = "data sent to server";
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				mTransmitStatus = "unknown host";
				e.printStackTrace();
			} catch (IOException e) {
				mTransmitStatus = "I/O problem sending data";
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
       	}
		send_server_update_msg();
    }
    
    
    public void createLocalLog() {
    	//CheckBox cb = (CheckBox) findViewById(R.id.localLog);
    	mWritingLocalLog = false;
    	//mBuffer = new CircularStringArrayBuffer( mBufferSize );
    	if ( mMakeLocalLog ){    		
    		try {
    			mLocalLog = new SensorOutputWriter(SensorOutputWriter.TYPE_GVB);
    			mWritingLocalLog = true;
    		} catch (StorageErrorException ex) {
    			ex.printStackTrace();
    		}
    	}
    }
    
    public void writeLogData( float[] values ){
    	if ( mWritingLocalLog ){
    		try {
    			mLocalLog.writeReadings(values);
    		} catch (StorageErrorException ex) {
    			ex.printStackTrace();
    		}	
    	}
    }
    
    public void closeLocalLog(){
    	if ( mWritingLocalLog ){
    		try {
    			mLocalLog.close();
    		} catch (StorageErrorException ex) {
    			ex.printStackTrace();
    		}
    	}
    	mWritingLocalLog = false;
    }

	
    /*
	//@Override
	public void runUploaderThread() {
		while (myThread != null && mRunning ) {
			send_server_update_msg();
			
			if ( mHost != null && mPort != 0 ){
				try {
    			
					Socket s = new Socket( mHost, mPort );
    	       
    	        //	outgoing stream redirect to socket
					OutputStream out = s.getOutputStream();
    	       
					PrintWriter output = new PrintWriter(out);
    	        // print the circular buffer
					output.println("Hello Android!");
    	        
    	        //	Close connection
					s.close();
    	           	       
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			
			try {
				Thread.sleep( mUpdateServerTime );
			} catch (InterruptedException e) { }
		}
	}
	*/
	
}

