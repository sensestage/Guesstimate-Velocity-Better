package com.steim.nescivi.android.gvb;

import java.util.Iterator;

import android.content.Context;
import android.app.Service;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import android.os.Looper;

public class GPSListener implements LocationListener, GpsStatus.Listener, Runnable {
	private static final int THREAD_IDLE_LOOP_DELAY = 1000;
	
	private boolean mRunning; //, mAccelerometer, mOrientation, mMagnetic, mGyro, mLight, mLinearAccelerometer;
	private Thread mThread;
	
	private LocationManager mLocationManager;
    private GpsStatus mGpsStatus = null;

//  private float mVelocity_at_last_gps_fix;
    private long mGps_prev_fix_time = 0;
    private float mGps_prev_fix_accuracy = 1000.f;
    private float mGps_precision = 0.f;
    private float mGps_speed = 0.f;

    private Context mContext;

    private float [] currentValues = { 0.0f, 0.0f };
	
	public GPSListener(Context context) {
		mRunning = false;
		mContext = context;
        // Get an instance of the LocationManager (for GPS)
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
	}

	public void startListening() {
		this.mRunning = true;
		
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0.f, this);

		// Start thread
		mThread = new Thread(this);
		mThread.start();
	}
	
	public void stopListening() {
		// Unregisterlistener moved to thread
		mRunning = false;
		unregister_GPS();
		mLocationManager.removeUpdates(this);
		// End recording thread
		mThread.interrupt();
		mThread = null;
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
    	
    	currentValues[0] = mGps_speed;
    	currentValues[1] = mGps_precision;
    	
    	//send_location_msg(location);    	
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

	
	public boolean isRunning() {
		return mRunning;
	}
	
	float [] getCurrentValues(){
		float [] vals;
		synchronized (this){
			vals = this.currentValues;
		}
		return vals;
	}
	
//	@Override
	public void run() {

		mLocationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);

	    if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
	        Looper.prepare();
	        //Toast.Make(mContext,"GPS",0);
	        //mLocationListener = new MyLocationListener();
	        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
	        Looper.loop(); 
	        Looper.myLooper().quit(); 
	        register_with_GPS();
	    /*
	      } else if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
	     
	        Looper.prepare();
	        //Toast.Make(mContext,"Triangulation",0);
	        //mLocationListener = new MyLocationListener();
	        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
	        Looper.loop();
	        Looper.myLooper().quit();
	    */
	    }else{
	        //Toast.Make(mContext,"no GPS or network provider",0);
	        Looper.prepare();
	        //handlerNormal.sendEmptyMessage(0);
	        Looper.loop();
	        Looper.myLooper().quit();
	    }   

		// Idle loop
		while (mRunning) {
			try {
				Thread.sleep(THREAD_IDLE_LOOP_DELAY);
			} catch (InterruptedException ex) { }
		}
		
    	unregister_GPS();

		// Here we must end the registration, so
		//mLocationManager.unregisterListener(this);
	}



}
