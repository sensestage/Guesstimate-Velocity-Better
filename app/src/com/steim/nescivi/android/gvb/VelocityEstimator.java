package com.steim.nescivi.android.gvb;

import android.app.Service;

import android.content.SharedPreferences;


//import android.app.Activity;
import android.os.AsyncTask;
//import android.os.Thread;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.IOException;

import android.hardware.SensorEventListener;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;


//import com.steim.nescivi.android.gvb.VelocityTransmitter.IncomingHandler;

public class VelocityEstimator extends Service implements LocationListener, GpsStatus.Listener {

    private Messenger mGuiClient = null;
//    private Messenger mTransmitService = null;
    
    private SharedPreferences mPrefs;
    
	private LocationManager mLocationManager;
    private GpsStatus mGpsStatus = null;

//  private float mVelocity_at_last_gps_fix;
    private long mGps_prev_fix_time = 0;
    private float mGps_prev_fix_accuracy = 1000.f;
    private float mGps_precision = 0.f;
    private float mGps_speed = 0.f;


    
//    private Messenger mIncomingMessenger;
    
//    private Looper mServiceLooper;
//    private IncomingHandler mServiceHandler;
    
    private Context mContext;

    static final int MSG_REGISTER_GUI_CLIENT = 1;
    static final int MSG_UNREGISTER_GUI_CLIENT = 2;
//    static final int MSG_REGISTER_TX_CLIENT = 3;
//    static final int MSG_UNREGISTER_TX_CLIENT = 4;

//    static final int MSG_FACC = 10;
//    static final int MSG_SACC = 11;
//    static final int MSG_GACC = 12;
//    static final int MSG_SPEED = 13;
    
    static final int MSG_SERVER_UPDATE_MSG = 14;
    static final int MSG_GUI_UPDATE_MSG = 15;
    
    static final int MSG_GPS_LOC = 16;

//    static final int MSG_REGISTER_VS_SRV = 5;
//    static final int MSG_UNREGISTER_VS_SRV = 6;
//    static final int MSG_SET_ACC_FLT_SMPS = 10;

//    static final int MSG_SET_SENSOR = 20;
//    static final int MSG_SET_FORWARD = 21;
//    static final int MSG_SET_SIDEWAYS = 22;
//    static final int MSG_SET_GRAVITY = 23;
//    static final int MSG_SET_WINDOW = 24;
//    static final int MSG_SET_UPDATETIME = 25;
//    
//    static final int MSG_SET_THRESH_ACC = 30;
//    static final int MSG_SET_THRESH_STEADY = 31;
//    static final int MSG_SET_THRESH_DECEL = 32;
//    static final int MSG_SET_THRESH_STILL = 33;
    
    static final int MSG_ESTIMATE_SETTINGS = 39;
    static final int MSG_SERVER_SETTINGS = 40;
    
//    static final int MSG_SET_IP = 50;
//    static final int MSG_SET_PORT = 51;
//    static final int MSG_SET_BUFFER_SIZE = 52;
//    static final int MSG_SET_UPDATE_INTERVAL = 53; 

    
	private Timer timer = new Timer();
	
	private Timer timer2 = new Timer();

			
	private SensorListener mListener;
//	private CircularFloatArrayBuffer2 mBuffer;
	
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
	//private float mStillTime = 0.0f;

	private float threshold_still_side = 0.04f;
	private float threshold_still_forward = 0.04f;

	private float threshold_motion_side = 0.1f;
	private float threshold_motion_forward = 0.1f;

	private float threshold_acceleration_forward = 0.2f;
	private float threshold_acceleration_mean = 0.1f;

	private float threshold_deceleration_forward = 0.3f;
	private float threshold_deceleration_mean = -0.1f;

	private float speed_decay = 0.99f;
	private float mean_weight = 0.65f;
	private float raw_weight = 0.65f;
	
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
	//private TimerTask mLogTimerTask;
	//private int mLogUpdateTime = 500;

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
				mWindowSize = msg.getData().getInt("window");
				mUpdateTime = msg.getData().getInt("updateTime");
				threshold_acceleration_forward = msg.getData().getFloat("acceleration_forward");
				threshold_acceleration_mean = msg.getData().getFloat("acceleration_mean");
				threshold_deceleration_forward = msg.getData().getFloat("deceleration_forward");
				threshold_deceleration_mean = msg.getData().getFloat("deceleration_mean");
				threshold_still_forward = msg.getData().getFloat("still_forward");
				threshold_still_side = msg.getData().getFloat("still_side");
				threshold_motion_forward = msg.getData().getFloat("motion_forward");
				threshold_motion_side = msg.getData().getFloat("motion_side");
				mean_weight = msg.getData().getFloat("mean_weight");
				raw_weight = msg.getData().getFloat("raw_weight");
				speed_decay = msg.getData().getFloat("speed_decay");
				macoef = msg.getData().getFloat("offsetma");
				forwardsign = msg.getData().getInt("signForward");
				mMakeLocalLog = msg.getData().getBoolean("makeLocalLog");
				//mLogUpdateTime = msg.getData().getInt("updateLogTime");
				//startEstimation();
    			break;
    		case MSG_SERVER_SETTINGS:
    			mHost = msg.getData().getString("host");
				mPort = msg.getData().getInt("port");
				mClientID = msg.getData().getInt("client");
				set_buffer_size( msg.getData().getInt("bufferSize") );
				mUpdateServerTime = msg.getData().getInt("updateServerTime");
				mMakeLocalLog = msg.getData().getBoolean("makeLocalLog");
				//mLogUpdateTime = msg.getData().getInt("updateLogTime");
				//startServerAndLog();
    			break;
   			case MSG_REGISTER_GUI_CLIENT:
    				mGuiClient = msg.replyTo;
    				break;
   			case MSG_UNREGISTER_GUI_CLIENT:
    				mGuiClient = null;
    				break;

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
    	Log.d("VelocityEstimator", "onCreate");
    	mContext = getApplicationContext();
   // 	mRunning = false;

        // Get an instance of the LocationManager (for GPS)
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

	this.mLastTime = System.currentTimeMillis();
	
	// Prepare members
	mListener = new SensorListener(this);		
//	mBuffer = new CircularFloatArrayBuffer2( 3, this.mWindowSize );
    this.mUpdateTime = 10;
    this.mUpdateServerTime = 30000;
	this.mUploadBuffer = new CircularStringArrayBuffer( this.mBufferSize );
	
	
	readPreferences();
	startEstimation();
	startServerAndLog();
	
	mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0.f, this);

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
						//mStillTime,
						currentReadings[0],currentReadings[1], currentReadings[2],
						mGps_speed, mGps_precision
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
    	timer2.scheduleAtFixedRate( mUploadTimerTask, 0, mUpdateServerTime );
        
    	Toast.makeText(mContext, "VelocityEstimator sender and logger starting", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy()
    {
    	Log.d("VelocityEstimator", "onDestroy");
    	Toast.makeText(mContext, "VelocityEstimator stopping", Toast.LENGTH_SHORT).show();

    	mListener.stopListening();
    	
    	if (timer != null){
    		mCalcTimerTask.cancel();
            timer.cancel();
        }
    	if (timer2 != null){
    		mUploadTimerTask.cancel();
            timer2.cancel();
        }

    	closeLocalLog();
    	unregister_GPS();

    }

    private void register_with_GPS()
    {
    	try
    	{
    		if(mLocationManager.addGpsStatusListener(this)) // && mLocationManager.addNmeaListener(this))
    		{
            	Toast.makeText(mContext, "GPS Connected", Toast.LENGTH_SHORT).show();        	        	    			
    		}
    	}
    	catch (SecurityException e)
    	{
        	Toast.makeText(mContext, "No permission to use GPS", Toast.LENGTH_SHORT).show();        	        	    			    		
    	}
    }
    
    private void unregister_GPS()
    {
    	mLocationManager.removeGpsStatusListener(this);
 //   	mLocationManager.removeNmeaListener(this);
    }

    /*
    private void compute_speed()
    {
    	float tot_weight = mGps_precision + mVelocity_precision;
    	float gps_weight = mGps_precision / tot_weight;
    	float vel_weight = mVelocity_precision / tot_weight;
    	
    	float speed_ms = (mVelocity_vec.mag() * vel_weight) + (mGps_speed * gps_weight);
    	mSpeed = speed_ms * 3.6f;	// change scale to km/h
    }
    */
    
    public void onStatusChanged(String provider, int status, Bundle extras)
    {
    	Log.d("VelocityServie", "onStatusChanged - " + provider);
    	
    	switch (status)
    	{
    		case LocationProvider.OUT_OF_SERVICE:
            	Toast.makeText(mContext, provider + " out of service", Toast.LENGTH_SHORT).show();
            	break;
    		case LocationProvider.AVAILABLE:
            	Toast.makeText(mContext, provider + " available", Toast.LENGTH_SHORT).show();
            	break;
    		case LocationProvider.TEMPORARILY_UNAVAILABLE:
            	Toast.makeText(mContext, provider + " temporarily unavailable", Toast.LENGTH_SHORT).show();
            	break;
    	}
    }
    
    public void onProviderEnabled(String provider)
    {
    	Log.d("VelocityServie", "onProviderEnabled - " + provider);
    	Toast.makeText(mContext, provider + " enabled", Toast.LENGTH_SHORT).show();
    	register_with_GPS();
    }
    
    public void onProviderDisabled(String provider)
    {
    	Log.d("VelocityServie", "onProviderDisabled - " + provider);
    	Toast.makeText(mContext, provider + " disabled", Toast.LENGTH_SHORT).show();
    	unregister_GPS();
    }
    
    public void onLocationChanged(Location location)
    {
    	Log.d("VelocityService", "onLocationChanged");
    
    	long time = location.getTime();
    	float dt = (time - mGps_prev_fix_time) / 1000.f;
    	mGps_prev_fix_time = time;
    	
    	float accuracy = location.getAccuracy();
    	mGps_precision = Math.min(8.f / (dt * (mGps_prev_fix_accuracy + accuracy)), 1.f);
    	mGps_prev_fix_accuracy = accuracy;
    	
    	mGps_speed = location.getSpeed();
    	
//    	mVelocity_at_last_gps_fix = mVelocity_vec.mag();
    	
    	send_location_msg(location);    	
    }
    
    public void onGpsStatusChanged(int status)
    {
    	Log.d("VelocityServie", "onGpsStatusChanged");
    	mGpsStatus = mLocationManager.getGpsStatus(mGpsStatus);
    	
    	switch (status)
    	{
    		case GpsStatus.GPS_EVENT_STARTED:
            	Toast.makeText(mContext, "GPS on", Toast.LENGTH_SHORT).show();        	        	
    			break;
    		case GpsStatus.GPS_EVENT_STOPPED:
            	Toast.makeText(mContext, "GPS off", Toast.LENGTH_SHORT).show();        	        	
    			break;
    		case GpsStatus.GPS_EVENT_FIRST_FIX:
            	Toast.makeText(mContext, "Have GPS fix", Toast.LENGTH_SHORT).show();        	        	
    			break;
    		case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
    			Iterator<GpsSatellite> sats = mGpsStatus.getSatellites().iterator();
//    			String satlist = "GPS sats:\n";
//    			int satcount = 0;
    			while (sats.hasNext())
    			{
    				GpsSatellite sat = sats.next();
//    				satlist += String.format("%d: %.1f\n", sat.getPrn(), sat.getSnr());
//    				satcount++;
    				Log.d("VelocityService", String.format("GPS sat %d: %.1f", sat.getPrn(), sat.getSnr()));
    			}
/*    			if (satcount > 0)
    			{
    				Toast.makeText(mContext, satlist, Toast.LENGTH_SHORT).show();
    			} */
    			break;
    	}
    }

    private void send_location_msg(Location location)
    {
    	Bundle b = new Bundle();
    	Message msg = Message.obtain(null, MSG_GPS_LOC );
    	b.putFloat("gps_precision", mGps_precision );
    	b.putFloat("gps_speed", mGps_speed );
    	
    	if (mGuiClient != null)
		{
    		try
    		{
    			mGuiClient.send(msg);
    		}
    		catch (RemoteException e)
    		{
    			mGuiClient = null;
    		}
		}    	
    }

    
    @Override
    public IBinder onBind(Intent intent)
    {
    	Log.d("VelocityEstimator", "onBind");
    	return mIncomingMessenger.getBinder();
    }
    
    private void readPreferences(){
 	   	// read settings from preferences
 	   	//mPrefs = getSharedPreferences( "GuesstimateVelocityBetter_preferences", 0);
 	   	mPrefs = getSharedPreferences( "GuesstimateVelocityBetterPrefs", MODE_PRIVATE);

 	   	mHost = mPrefs.getString("host", "82.161.162.51" );
 	   	mPort = mPrefs.getInt("port", 5858 );
 	   	mClientID = mPrefs.getInt("client", 1 );
 	    set_buffer_size( mPrefs.getInt("bufferSize", 60 ) );
 	   	mUpdateServerTime = mPrefs.getInt("updateServerTime", 30 ) * 1000;
 	   	//mLogUpdateTime = mPrefs.getInt("updateLogTime", 500 );
 	   	mMakeLocalLog = mPrefs.getBoolean( "localLog", false );
 	   
 	   	set_sensor( mPrefs.getInt("sensor", 1 ) );
 	   	set_forward_axis( mPrefs.getInt("forward", 1 ) );
	   	set_sideways_axis( mPrefs.getInt("side", 0 ) );
	   	set_gravity_axis( mPrefs.getInt("gravity", 2 ) );
	   	
	   	mWindowSize = mPrefs.getInt("window", 200 );
	   	mUpdateTime = mPrefs.getInt("updateTime", 10 );
	     
	   	threshold_acceleration_forward = mPrefs.getFloat("acceleration_forward", 0.2f );
	   	threshold_acceleration_mean = mPrefs.getFloat("acceleration_mean", 0.1f );
	   	threshold_deceleration_forward = mPrefs.getFloat("deceleration_forward", 0.3f );
	   	threshold_deceleration_mean = mPrefs.getFloat("deceleration_mean", -0.1f );
       	threshold_still_forward = mPrefs.getFloat("still_forward", 0.04f );
       	threshold_still_side = mPrefs.getFloat("still_side", 0.04f );
       	threshold_motion_forward = mPrefs.getFloat("motion_forward", 0.1f );
       	threshold_motion_side = mPrefs.getFloat("motion_side", 0.1f );

	   	mean_weight = mPrefs.getFloat("mean_weight", 0.65f );
	   	raw_weight = mPrefs.getFloat("raw_weight", 0.35f );
	     
       	speed_decay = mPrefs.getFloat("speed_decay", 0.99f );
       	macoef = mPrefs.getFloat("offsetma", 0.99f );

   		forwardsign = 1.f;
   		if ( mPrefs.getBoolean( "signForward", false ) ){
   			forwardsign = -1.f;
   		} 	   
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
    	currentVals[1] = String.format("%.3f", Math.max( mSpeed * 3.6f, 0. )  );
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
    
    /*
    private void set_ip( String newhost ){
    	this.mHost = newhost;
    }

    private void set_port( int newport ){
    	this.mPort = newport;
    }

    private void set_client( int newclient ){
    	this.mClientID = newclient;
    }
    */
    
    private void set_buffer_size( int windowsize ){
    	if ( this.mBufferSize != windowsize ){
        	mUploadBuffer = null;
        	mUploadBuffer = new CircularStringArrayBuffer( windowsize );
    	}
    	this.mBufferSize = windowsize;
    }

//    private void set_update_interval( int updtime ){
//    	/*
//    	if ( this.mUpdateServerTime != updtime ){
//    		mUploadTimerTask.cancel();
//    		timer.scheduleAtFixedRate( mUploadTimerTask, 0, updtime );
//    	}
//    	*/
//    	this.mUpdateServerTime = updtime;
//    }

    
    /// --------- END DATA UPLOADER -------------

//
//    private void set_sign_forward( int sign ){    	
//    	this.forwardsign = (float) sign;
//    }
//
//    private void set_window( int windowsize ){
//    	/*
//    	if ( this.mWindowSize != windowsize ){
//        	mBuffer = null;
//        	mBuffer = new CircularFloatArrayBuffer2( 3, windowsize );    		
//    	}
//    	*/
//    	this.mWindowSize = windowsize;
//    }
//
//    private void set_update_time( int updtime ){
//    	/*
//    	if ( this.mUpdateTime != updtime ){
//    		mCalcTimerTask.cancel();
//    		timer.scheduleAtFixedRate( mCalcTimerTask, 0, updtime );
//    	}
//    	*/
//    	this.mUpdateTime = updtime;
//    }
//
    
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
    
//    private void set_threshold_acceleration( float newth ){
//    	threshold_acceleration = newth;
//    }
//
//    private void set_threshold_steady( float newth ){
//    	threshold_steady = newth;
//    }
//    
//    private void set_threshold_deceleration( float newth1, float newth2 ){
//    	threshold_decel_std = newth1;
//    	threshold_decel_mean = newth2;
//    }
//
//    private void set_threshold_still( float newth1, float newth2, float newth3 ){
//    	threshold_still_side = newth1;
//    	threshold_still_forward = newth2;
//    	threshold_still_time = newth3;
//    }
//
//    private void set_offset_macoef( float newth1 ){
//    	macoef = newth1;
//    }

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
//    	b.putFloat("stilltime", this.mStillTime );
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
		float [] currentReadings = this.mListener.getCurrentValues();
				
		// put readings in circular buffer
		//mBuffer.add( currentReadings );
		
		// calculate mean and standard deviation of buffers
		//float [][] curStats = mBuffer.getStats();
		//mCurrentStats = mBuffer.getStats();
		float [][] curStats = this.mListener.getCurrentStats();
		
		// reset offsets when we are in still:
		if ( this.mState == 0 ) {
			for ( int axis = 0; axis < 3; axis++ ){
				if ( curStats[0][axis] < 20.f ){ // just to make sure we're not killed by NaN's
					this.mOffsets[axis] = this.mOffsets[axis] * macoef + curStats[0][axis] * (1-macoef);
				}
			}
		}
		
		// substract offset from mean
		for ( int axis = 0; axis < 3; axis++ ){
			curStats[0][axis] = curStats[0][axis] - this.mOffsets[axis];  
		}
		
		// correct sign for forward axis
		curStats[0][0] = curStats[0][0] * forwardsign;
		currentReadings[0] = (currentReadings[0] - this.mOffsets[0]) * forwardsign;
		
		// determine state
		
		if ( curStats[1][0] > threshold_motion_forward && curStats[1][1] > threshold_motion_side ){
			this.mState = 1; // moving
		}
		if ( curStats[1][0] < threshold_still_forward && curStats[1][1] < threshold_still_side ){
			this.mState = 0; // still
		}
		if ( curStats[1][0] > threshold_acceleration_forward && curStats[0][0] > threshold_acceleration_mean ){
			this.mState = 2; // accelerating
		}
		if ( curStats[1][0] > threshold_deceleration_forward && curStats[0][0] < threshold_deceleration_mean ){
			this.mState = 3; // decelerating
		}
		
		/*
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
		*/
				
		// calculate forward speed
		if ( this.mState == 0 ){
			this.mSpeed = this.mSpeed * speed_decay;
		} else {
			this.mSpeed += ( mean_weight*curStats[0][0] + raw_weight*currentReadings[0]) * this.mDeltaTime * 0.001;
		}
		
		/*
		switch (this.mState){
		case 0: // still
			this.mSpeed = this.mSpeed * speed_decay_factor;
	//		this.mSpeed = (float) 0.0;
			break;
		case 2: // accelerating
			// set the sign of what is forward:
		case 1: // steady motion
		case 3: // decelerating
			this.mSpeed += curStats[0][0] * forwardsign * this.mDeltaTime * 0.001;
			break;
		default:
			break;
		}
		*/
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
}


