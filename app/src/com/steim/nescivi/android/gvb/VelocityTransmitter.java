package com.steim.nescivi.android.gvb;

import android.app.Service;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
//import android.os.RemoteException;
import android.util.Log;

import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class VelocityTransmitter extends Service {

    static final int MSG_REGISTER_GUI_CLIENT = 1;
    static final int MSG_UNREGISTER_GUI_CLIENT = 2;
    static final int MSG_REGISTER_TX_CLIENT = 3;
    static final int MSG_UNREGISTER_TX_CLIENT = 4;
    static final int MSG_SET_IP = 50;
    static final int MSG_SET_PORT = 51;
    static final int MSG_SET_BUFFER_SIZE = 52;
    static final int MSG_SET_UPDATE_INTERVAL = 53; 

	private Context mContext;
    
//	private Thread myThread;
	private Timer timer;
	
	// this could also be a setting!
	private static final int THREAD_SEND_HTTPDATA = 30000;
		
	private CircularStringArrayBuffer mBuffer;
	private int mBufferSize;
	
	private int mUpdateTime;
	
	private String host;
	private int port;
	private String ServletUri;
		
	// register with VelocityEstimator!
		
	private TimerTask mSendDataTimerTask;
	
    class IncomingHandler extends Handler
    {
	@Override
    	public void handleMessage(Message msg)
    	{
    		switch(msg.what)
    		{
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
//    			case VelocityEstimator.MSG_SPEED:
//    				addData( msg.arg1 );
//    				break;
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
    //	mRunning = false;
    	this.mUpdateTime = THREAD_SEND_HTTPDATA;

    // Prepare members
    this.mBuffer = new CircularStringArrayBuffer( this.mBufferSize );
    this.timer = new Timer();
    
    Toast.makeText(mContext, "VelocityTransmitter starting", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
    	Log.d("VelocityTransmitter", "onStartCommand");
	mSendDataTimerTask = new TimerTask() {
    		public void run() {
    			sendData();
    		}
		};
    	//timer.scheduleAtFixedRate( mSendDataTimerTask, 0, mUpdateTime );        
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy()
    {
    	Log.d("VelocityEstimator", "onDestroy");
    	Toast.makeText(mContext, "VelocityEstimator stopping", Toast.LENGTH_SHORT).show();

    	if (timer != null){
	    mSendDataTimerTask.cancel();
            timer.cancel();
        }
    }
    
    @Override
    public IBinder onBind(Intent intent)
    {
    	Log.d("VelocityTransmitter", "onBind");
    	return mIncomingMessenger.getBinder();
    }
    
    private void sendData(){
    	// send data to server!
    }
    
    private void addData( int newdata ){
    	// add data to circular buffer
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

    private void set_update_interval( int updtime ){
    	if ( this.mUpdateTime != updtime ){
    		mSendDataTimerTask.cancel();
    		timer.scheduleAtFixedRate( mSendDataTimerTask, 0, updtime );
    	}
    	this.mUpdateTime = updtime;
    }
}
