package com.steim.nescivi.android.gvb;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
//import android.app.Service;



import android.os.SystemClock;

public class GVBBootReceiver extends BroadcastReceiver {
	
	private SharedPreferences mPrefs;

   @Override
   public void onReceive(Context context, Intent intent) {
      Log.i( "GVB", "GVBBootReceiver invoked, configuring AlarmManager");
      AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
      
      mPrefs = context.getSharedPreferences( "GuesstimateVelocityBetterPrefs", Context.MODE_PRIVATE);
      
      int hour_start = mPrefs.getInt( "start_hour", 9 );
      int minute_start = mPrefs.getInt( "start_minute", 0 );
      int hour_stop = mPrefs.getInt( "stop_hour", 21 );
      int minute_stop = mPrefs.getInt( "stop_minute", 0 );

      
      PendingIntent pendingIntent =
               PendingIntent.getBroadcast(context, 0, new Intent(context, GVBAlarmReceiver.class), 0);

      // start now once, and then each day at a certain time
      // and stop service each day at a certain time
      
      // use inexact repeating which is easier on battery (system can phase events and not wake at exact times)
      alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
    		  SystemClock.elapsedRealtime() + 30000, // start 30 seconds from now
    		  //AlarmManager.INTERVAL_HOUR, // each hour ; INTERVAL_DAY for each hour
              pendingIntent);

//      AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
      
  	  Calendar calendar = Calendar.getInstance();
  	  Calendar calendar2 = Calendar.getInstance();

  	  // 	9:00 on 
  	  calendar.set(Calendar.HOUR_OF_DAY, hour_start);
  	  calendar.set(Calendar.MINUTE, minute_start);
  	  calendar.set(Calendar.SECOND, 0);
  	  //PendingIntent pi1 = PendingIntent.getService( context, 0, new Intent(context, GVBAlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
  	  PendingIntent pi1 = PendingIntent.getService( context, 0, new Intent(context, VelocityEstimator.class), PendingIntent.FLAG_UPDATE_CURRENT);
  	  alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi1);

  	  // 	21:00 off
  	  calendar2.set(Calendar.HOUR_OF_DAY, hour_stop);
  	  calendar2.set(Calendar.MINUTE, minute_stop);
  	  calendar2.set(Calendar.SECOND, 0);
  	  PendingIntent pi2 = PendingIntent.getBroadcast(context, 1, new Intent(context, GVBAlarmStopReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
  	  alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, calendar2.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi2);      
   }
}
