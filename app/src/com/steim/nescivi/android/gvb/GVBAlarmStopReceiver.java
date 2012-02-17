package com.steim.nescivi.android.gvb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
//import android.app.IntentService;
import android.util.Log;




import android.os.PowerManager;


public class GVBAlarmStopReceiver extends BroadcastReceiver {

   // onReceive must be very quick and not block, so it just fires up a Service
   @Override
   public void onReceive(Context context, Intent intent) {
   //public void onHandleIntent(Intent intent) {
	   Log.i( "GVB", "GVBAlarmStopReceiver invoked, stopping VelocityEstimator in background");
	   context.stopService(new Intent(context, VelocityEstimator.class));     
   }   
}
