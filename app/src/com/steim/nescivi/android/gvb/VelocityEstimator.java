package com.steim.nescivi.android.gvb;

import org.achartengine.ChartFactory;

import android.app.Activity;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

public class VelocityEstimator extends Activity implements Runnable {

	private Thread mThread;
	private static final int THREAD_UPDATE_VELOCITY_DELAY = 10;

	
	@Override
	protected void onResume() {
		super.onResume();
		
		// start chart periodic update thread
		mThread = new Thread(this);
		mThread.start();
	}
	
	public void updateVelocityMeasurement(){
		
	}
	
	@Override
	public void run() {
		while (mThread != null) {
			updateVelocityMeasurement();
			try {
				Thread.sleep(THREAD_UPDATE_VELOCITY_DELAY);
			} catch (InterruptedException e) { }
		}
	}
}
