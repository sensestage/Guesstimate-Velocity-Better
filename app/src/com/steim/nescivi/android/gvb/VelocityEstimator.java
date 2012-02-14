package com.steim.nescivi.android.gvb;

import android.app.Service;

//import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import com.steim.nescivi.android.gvb.VelocityTransmitter.IncomingHandler;



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
    
    static final int MSG_SET_IP = 50;
    static final int MSG_SET_PORT = 51;
    static final int MSG_SET_BUFFER_SIZE = 52;
    static final int MSG_SET_UPDATE_INTERVAL = 53; 

    
//	private Thread myThread;
	private Timer timer = new Timer();
	
	//private boolean mRunning;
	
	// this could also be a setting!
	private static final int THREAD_UPDATE_VELOCITY_DELAY = 10;
		
	private SensorListener mListener;
	private CircularFloatArrayBuffer mBuffer;
	
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
	
	private float threshold_acceleration = 0.3f;
	private float threshold_still_side = 0.1f;
	private float threshold_still_forward = 0.1f;
	private float threshold_decel_std = 0.25f;
	private float threshold_decel_mean = -0.5f;
	private float threshold_steady = 0.5f;	

	private String mHost;
	private int mPort;
	private int mBufferSize = 60;
	private int mUpdateServerTime = 30000;
	private CircularStringArrayBuffer mUploadBuffer;

	private TimerTask mCalcTimerTask;
	private TimerTask mUploadTimerTask;
	
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
    			case MSG_REGISTER_GUI_CLIENT:
    				mGuiClient = msg.replyTo;
    				break;
    			case MSG_UNREGISTER_GUI_CLIENT:
    				mGuiClient = null;
    				break;
    			case MSG_REGISTER_TX_CLIENT:
    				mTransmitService = msg.replyTo;
    				break;
    			case MSG_UNREGISTER_TX_CLIENT:
    				mTransmitService = null;
    				break;
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
    //	mRunning = false;


	this.mLastTime = System.currentTimeMillis();
	
	// Prepare members
	mListener = new SensorListener(this);		
	mBuffer = new CircularFloatArrayBuffer( this.mWindowSize );
    	this.mUpdateTime = 30000;
	this.mUploadBuffer = new CircularStringArrayBuffer( this.mBufferSize );
	
	
	Toast.makeText(mContext, "VelocityEstimator created", Toast.LENGTH_SHORT).show();
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
		mUploadTimerTask = new TimerTask() {
		  public void run() {
    			uploadData();
		  }
		};
		TimerTask msgTimerTask = new TimerTask() {
    		public void run() {
			// add data to circular string buffer:
			add_data_to_buffer();
			// send messages to UI:
    			send_speed_msg();
    			send_forward_accel_msg();
    			send_sideways_accel_msg();
    			send_gravity_accel_msg();
    		}
		};
    	timer.scheduleAtFixedRate( mCalcTimerTask, 0, THREAD_UPDATE_VELOCITY_DELAY );
    	timer.scheduleAtFixedRate( msgTimerTask, 0, 1000 );
    	timer.scheduleAtFixedRate( mSendDataTimerTask, 0, mUpdateServerTime );
		//this.runThread();
        
    	Toast.makeText(mContext, "VelocityEstimator starting", Toast.LENGTH_SHORT).show();
    	
        return Service.START_STICKY;
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
    	
//    	mRunning = false;
//    	unregister_GPS();
//    	mSensorManager.unregisterListener(this); 
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
    	currentVals[1] = String.format("%.2f", mSpeed * 0.36f );
    	mUploadBuffer.add( currentVals );
    }

    public void uploadData(){
	Toast.makeText(GuesstimateVelocityBetter.this, String.format("Velocity: %.2f", mSpeed ), Toast.LENGTH_SHORT).show();    	
    	//VelocityBufferTransmitter uploader = new VelocityBufferTransmitter();
    	//uploader.execute( new String[] { mHost, String.format("%i", mPort ) } );
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
    
    private void set_ip( String newhost ){
    	this.mHost = newhost;
    }

    private void set_port( int newport ){
    	this.mPort = newport;
    }
    
    private void set_buffer_size( int windowsize ){
    	if ( this.mBufferSize != windowsize ){
        	mBuffer = null;
        	mBuffer = new CircularStringArrayBuffer( windowsize );
    	}
    	this.mBufferSize = windowsize;
    }

    private void set_update_interval( int updtime ){
    	if ( this.mUpdateServerTime != updtime ){
    		mSendDataTimerTask.cancel();
    		timer.scheduleAtFixedRate( mSendDataTimerTask, 0, updtime );
    	}
    	this.mUpdateServerTime = updtime;
    }

    
    /// --------- END DATA UPLOADER -------------

    private void set_window( int windowsize ){
    	if ( this.mWindowSize != windowsize ){
        	mBuffer = null;
        	mBuffer = new CircularFloatArrayBuffer( windowsize );    		
    	}
    	this.mWindowSize = windowsize;
    }

    private void set_update_time( int updtime ){
    	if ( this.mUpdateTime != updtime ){
    		mCalcTimerTask.cancel();
    		timer.scheduleAtFixedRate( mCalcTimerTask, 0, updtime );
    	}
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
	  } catch (RemoteException e) {
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
