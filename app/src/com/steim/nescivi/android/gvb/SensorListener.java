package com.steim.nescivi.android.gvb;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class SensorListener implements SensorEventListener, Runnable {
	public static final int AXIS_X = 0;
	public static final int AXIS_Y = 1;
	public static final int AXIS_Z = 2;

	public static final int [] SENSOR_AVAILABLE_DELAYS = {
		SensorManager.SENSOR_DELAY_FASTEST,
		SensorManager.SENSOR_DELAY_GAME,
		SensorManager.SENSOR_DELAY_UI,
		SensorManager.SENSOR_DELAY_NORMAL
	};
	private static final int THREAD_IDLE_LOOP_DELAY = 1000;
	
	private boolean mRunning; //, mAccelerometer, mOrientation, mMagnetic, mGyro, mLight, mLinearAccelerometer;
	private int mType;
	//private long mLastTime;
	//private long mTimeInterval;
	private int mRecordingRate;
	
	private int mForward;
	private int mSideways;
	private int mGravity;
	
	private CircularFloatArrayBuffer2 mBuffer;
	private int mWindow;
	private int mDim;
	
	private float [] currentValues = {(float) 0.0, (float) 0.0, (float) 0.0};
	private float [][] currentStats = { {(float) 0.0, (float) 0.0, (float) 0.0}, {(float) 0.0, (float) 0.0, (float) 0.0} };	
	
//	private SensorOutputWriter mAccelerometerLog, mLinearAccelerometerLog, mOrientationLog, mMagneticLog, mGyroLog, mLightLog;
	private SensorManager mSensorManager;
	private Thread mThread;
	
	public SensorListener(Context context) {
		mRunning = false;
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
	}

	public void startListening( int type, int rate, int forward, int sideways, int gravity, int dim, int window) {
		this.mType = type; // true is linear_acceleration, false is accelerometer
		this.mRecordingRate = rate;
		this.mRunning = true;
		
		this.mForward = forward;
		this.mSideways = sideways;
		this.mGravity = gravity;
		
		this.mWindow = window;
		this.mDim = dim;
		
		mBuffer = new CircularFloatArrayBuffer2( this.mDim, this.mWindow );
				
		// Start recording thread
		mThread = new Thread(this);
		mThread.start();
	}
	
	public void stopListening() {
		// Unregisterlistener moved to thread
		mRunning = false;
		
		// End recording thread
		mThread.interrupt();
		mThread = null;
	}
	
	public boolean isRunning() {
		return mRunning;
	}
	
	public void changeAxis( int axis, int which ) {
		switch( axis ){
			case 0: // forward
				this.mForward = which;
				break;
			case 1: 
				this.mSideways = which;
				break;
			case 2: 
				this.mGravity = which;
				break;
		}
	}

	public void changeListening( int type, int rate ) {
		if ( this.mRunning ){
			boolean hasChanged = false;
			if ( this.mType != type ){
				hasChanged = true;
			}
			if ( this.mRecordingRate != rate ){
				hasChanged = true;
			}
			this.mType = type; // 1 is linear_acceleration, 0 is acceleromater
			this.mRecordingRate = rate;			
			if ( hasChanged ){
				mSensorManager.unregisterListener(this);
				registerListeners();
			}
		}
	}

	
	private void registerListeners() {
		if (mType == 1){			
			mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), mRecordingRate);
		} else {
			mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), mRecordingRate);
		}
	}

//	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
	
	public void storeValues(float readings[]) {
	//	long newtime = System.currentTimeMillis();
	//	this.mTimeInterval = newtime - mLastTime;
	//	this.mLastTime = newtime;
		synchronized ( this ){
			this.currentValues[ 0 ] = readings[ this.mForward ];
			this.currentValues[ 1 ] = readings[ this.mSideways ];
			this.currentValues[ 2 ] = readings[ this.mGravity ];
			mBuffer.add( currentValues );			 
		}
	}
	
	float [][] getCurrentStats(){
		float [][] curStats;
		synchronized (this){
			this.currentStats = mBuffer.getStats();
			curStats = this.currentStats; 
		}
		return curStats;
	}
	
	float [] getCurrentValues(){
		float [] vals;
		synchronized (this){
			vals = this.currentValues;
		}
		return vals;
	}


	//@Override
	public void onSensorChanged(SensorEvent event) {
		int type = event.sensor.getType();
		//try {
			switch (type) {
			case Sensor.TYPE_ACCELEROMETER:
				this.storeValues(event.values);
			//	mAccelerometerLog.writeReadings(event.values);
				break;
			case Sensor.TYPE_LINEAR_ACCELERATION:
				this.storeValues(event.values);
			//	mLinearAccelerometerLog.writeReadings(event.values);
				break;
			}
		//}
//		catch (StorageErrorException e) {
//			e.printStackTrace();
//		}
	}

	
//	@Override
	public void run() {
		registerListeners();
		
		// Idle loop
		while (mRunning) {
			try {
				Thread.sleep(THREAD_IDLE_LOOP_DELAY);
			} catch (InterruptedException ex) { }
		}
		
		// Here we must end the registration, so
		mSensorManager.unregisterListener(this);
	}



}
