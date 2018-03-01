package rwassens.nl.gpstrack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;


import android.content.Context;
import android.location.Location;
import android.os.PowerManager;

public class HereAmI implements Runnable{
	
	Thread _runner;
	String _serverUrl;
	Location _location = null;
    private final Semaphore _updated = new Semaphore(0, true);


	public HereAmI(String serverUrl) {
		_serverUrl = serverUrl;
		_runner = new Thread(this); // (1) Create a new thread.
		_runner.start(); // (2) Start the thread.
 
 
	}
	
	public void run() {
		PowerManager pm;
		PowerManager.WakeLock wl;
		Location loc;
		Boolean interrupted = false;

		Trace("HereIsRobert.run, URL: " + _serverUrl);

		while (!interrupted) {
			try {
				loc =  _location;  // NOTE: is this a atomic action?
				if (loc != null) {
					send(loc);
				}
				
			} catch (Exception e) {
				Trace("HereIsRobert.run() exception: " + e);
			}
			try {
				//Thread.sleep(10000);
				_updated.tryAcquire(10L, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Trace("HereIsRobert.run() interupted. exception: " + e);
				interrupted = true;
			}
		}
		
		Trace("HereIsRobert thread stopped");

	}
	
	public void stop() {
		_runner.interrupt();
		try {

			_runner.join();
		} catch (Exception e) {
			Trace("HereIsRobert.stop() exception: " + e);
			
		}
	}
	
	private void send(Location location) {
		Trace("+");
		HttpClient httpclient;  
		HttpPost httppost;  

		
		// Create a new HttpClient and Post Header  
		httpclient = new DefaultHttpClient();  
		httpclient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Anrdoid / Robert Wassens hierbenik");
		httppost = new HttpPost(_serverUrl + "/set.php"); 
		httppost.addHeader("Accept", "*/*");

		try {  
		    // Add your data  
		    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);  
		    nameValuePairs.add(new BasicNameValuePair("lat", Double.toString(location.getLatitude())));  
		    nameValuePairs.add(new BasicNameValuePair("lon", Double.toString(location.getLongitude())));  
		    nameValuePairs.add(new BasicNameValuePair("time", Long.toString(location.getTime())));  		    
		    httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));  

		    // Execute HTTP Post Request  
		    httpclient.execute(httppost);  		
		    //GpsTrackService.Trace("resp: " + resp.getStatusLine());
		} catch (ClientProtocolException e) {  
			Trace("HereIsRobert " + e);
		} catch (IOException e) {  
			Trace("HereIsRobert " + e);
		}
		Trace("x");
	}

	public void update(Location loc) {
		GpsTrackService.Trace(".");
		_location = loc; // NOTE: is this a atomic action?
		_updated.release();
	}
	
	public static void Trace(String str) {
		GpsTrackService.Trace("HereAmI", str);
	}

}
