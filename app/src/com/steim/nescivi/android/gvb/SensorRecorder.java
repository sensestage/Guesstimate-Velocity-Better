package com.steim.nescivi.android.gvb;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class SensorRecorder implements SensorEventListener, Runnable {
	public static final int [] SENSOR_AVAILABLE_DELAYS = {
		SensorManager.SENSOR_DELAY_FASTEST,
		SensorManager.SENSOR_DELAY_GAME,
		SensorManager.SENSOR_DELAY_UI,
		SensorManager.SENSOR_DELAY_NORMAL
	};
	private static final int THREAD_IDLE_LOOP_DELAY = 1000;
	
	private boolean mRunning, mAccelerometer, mOrientation, mMagnetic, mGyro, mLight, mLinearAccelerometer;
	private int mRecordingRate;
	private SensorOutputWriter mAccelerometerLog, mLinearAccelerometerLog, mOrientationLog, mMagneticLog, mGyroLog, mLightLog;
	private SensorManager mSensorManager;
	private Thread mThread;
	
	public SensorRecorder(Context context) {
		mRunning = false;
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
	}
	
	public void startRecording(boolean accelerometer, boolean orientation, boolean magnetic, boolean gyro, boolean light, boolean linearaccelerometer, int rate) throws StorageErrorException {
		this.mAccelerometer = accelerometer;
		this.mLinearAccelerometer = linearaccelerometer;
		this.mOrientation = orientation;
		this.mMagnetic = magnetic;
		this.mGyro = gyro;
		this.mLight = light;
		this.mRecordingRate = rate;
		this.mRunning = true;
		
		// Create loggers
		createLoggers();
		
		// Start recording thread
		mThread = new Thread(this);
		mThread.start();
	}
	
	public void stopRecording() {
		// Unregisterlistener moved to thread
		mRunning = false;
		
		// End recording thread
		mThread.interrupt();
		mThread = null;
	}
	
	public boolean isRunning() {
		return mRunning;
	}
	
	private void createLoggers() throws StorageErrorException {
		if (mAccelerometer) mAccelerometerLog = new SensorOutputWriter(SensorOutputWriter.TYPE_ACCELEROMETER);
		if (mLinearAccelerometer) mLinearAccelerometerLog = new SensorOutputWriter(SensorOutputWriter.TYPE_LINEARACCELERO);
		if (mOrientation) mOrientationLog = new SensorOutputWriter(SensorOutputWriter.TYPE_ORIENTATION);
		if (mMagnetic) mMagneticLog = new SensorOutputWriter(SensorOutputWriter.TYPE_MAGNETIC);
		if (mGyro) mGyroLog = new SensorOutputWriter(SensorOutputWriter.TYPE_GYRO);
		if (mLight) mLightLog = new SensorOutputWriter(SensorOutputWriter.TYPE_LIGHT);
	}
	
	private void registerListeners() {
		if (mAccelerometer) mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), mRecordingRate);			
		if (mLinearAccelerometer) mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), mRecordingRate);			
		if (mOrientation) mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), mRecordingRate);
		if (mMagnetic) mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), mRecordingRate);			
		if (mGyro) mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), mRecordingRate);			
		if (mLight) mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT), mRecordingRate);
	}

//	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void onSensorChanged(SensorEvent event) {
		int type = event.sensor.getType();
		try {
			switch (type) {
			case Sensor.TYPE_ACCELEROMETER:
				mAccelerometerLog.writeReadings(event.values);
				break;
			case Sensor.TYPE_LINEAR_ACCELERATION:
				mLinearAccelerometerLog.writeReadings(event.values);
				break;
			case Sensor.TYPE_ORIENTATION:
				mOrientationLog.writeReadings(event.values);
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:
				mMagneticLog.writeReadings(event.values);
				break;
			case Sensor.TYPE_GYROSCOPE:
				mGyroLog.writeReadings(event.values);
				break;
			case Sensor.TYPE_LIGHT:
				mLightLog.writeReadings(event.values);
				break;
			}
		}
		catch (StorageErrorException e) {
			e.printStackTrace();
		}
	}
	
	public float[][] getBufferForSensor(String sensorType) {
		if (sensorType.equals(SensorOutputWriter.TYPE_ACCELEROMETER))
			return getAccelerometerBuffer();
		if (sensorType.equals(SensorOutputWriter.TYPE_LINEARACCELERO))
			return getLinearAccelerometerBuffer();
		if (sensorType.equals(SensorOutputWriter.TYPE_ORIENTATION))
			return getOrientationBuffer();
		if (sensorType.equals(SensorOutputWriter.TYPE_MAGNETIC))
			return getMagneticBuffer();
		if (sensorType.equals(SensorOutputWriter.TYPE_GYRO))
			return getGyroBuffer();
		if (sensorType.equals(SensorOutputWriter.TYPE_LIGHT))
			return getLightBuffer();
		
		//Else throw argument error
		throw new IllegalArgumentException();
	}
	
	public float[][] getAccelerometerBuffer() {
		if (mAccelerometer) {
			return mAccelerometerLog.getBuffer();
		}
		else return null;
	}

	public float[][] getLinearAccelerometerBuffer() {
		if (mLinearAccelerometer) {
			return mLinearAccelerometerLog.getBuffer();
		}
		else return null;
	}

	public float[][] getOrientationBuffer() {
		if (mOrientation) {
			return mOrientationLog.getBuffer();
		}
		else return null;
	}
	
	public float[][] getMagneticBuffer() {
		if (mMagnetic) {
			return mMagneticLog.getBuffer();
		}
		else return null;
	}

	public float[][] getGyroBuffer() {
		if (mGyro) {
			return mGyroLog.getBuffer();
		}
		else return null;
	}

	public float[][] getLightBuffer() {
		if (mLight) {
			return mLightLog.getBuffer();
		}
		else return null;
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
		
		try {
			if (mAccelerometer) mAccelerometerLog.close();
			if (mLinearAccelerometer) mLinearAccelerometerLog.close();
			if (mOrientation) mOrientationLog.close();
			if (mMagnetic) mMagneticLog.close();
			if (mGyro) mGyroLog.close();
			if (mLight) mLightLog.close();
		}
		catch (StorageErrorException ex) {
			ex.printStackTrace();
		}
	}



}
