package rwassens.nl.gpstrack;

/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.xmlpull.v1.XmlSerializer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Xml;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.content.Context;



public class GpsTrackService extends Service implements LocationListener {
	
	
	private static final String TAG = "GpsTrackService";
    private NotificationManager mNM;
    
   	private static FileOutputStream   traceFile = null;
   	private final String traceFileName = "trace.txt";
   	
	private static TextView mOutput = null;
	private EditText mDist = null;
	private EditText mTime = null;
	private EditText mSpeed = null;
	private EditText mAvg = null;

	private final String dataDir = Environment.getExternalStorageDirectory() + "/gpstrack/";
	private final String dataFileName = "gpstrack.dat";
	private final String trackFileNamePrefix = "track";
	private final String trackFileNameExt = ".tcx";
	private final int minDistUpdate = 0;

	private int lastTrackIndex = 0;

	private long startMSec;
	private double currentDistMeters;
	private long currentTimeMSec;
	private double lastSpeed;
	
	private double maxSpeedMeterPerSec;
	
	private Location lastLocation;

	private LocationManager mLM;
	private String provider;
	
	private static String traceText = "";

	
	private enum State {
		Idle,
		WaitinForFirstLocation,
		Started
	};
	
	private State state = State.Idle;

	private boolean fStoreTrackPonts;
	private FileOutputStream tcxFile;
	private XmlSerializer tcxSerializer;

	private String mServerUrl;
	private HereAmI hereAmI;

	private static Handler handler;
    private PowerManager pm;

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        GpsTrackService getService() {
            return GpsTrackService.this;
        }
    }
    private void getPrefs() {
        // Get the xml/preferences.xml preferences
        SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(getBaseContext());
        mServerUrl = prefs.getString("serverUrl", "http://hereami.justanurl.nl");
<<<<<<< HEAD
		if (mServerUrl.toLowerCase().indexOf("http://") != 0) {
			Trace(mServerUrl + " does not start with http://. Prepending it");
		}
		mServerUrl = "http://" + mServerUrl;
=======
>>>>>>> cc2567ec62e8d94470558f98368ed459fd6c37fe
    	Trace("service:mServerUrl " + mServerUrl);
    }
        
    @Override
    public void onCreate() {
    	super.onCreate();
    	getPrefs();
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    	hereAmI = new HereAmI(mServerUrl);
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		handler = new Handler(Looper.getMainLooper());
    	Trace("onCreate");
        // Create data dir in case it does not exists
        try {
        	File path = new File(dataDir);
        	path.mkdir();
        } catch (Exception e) {
        	//ignore
        }
        state = State.Idle;

        ReadDatFile();
        
        try {
        	openTraceFile();
	        mLM = (LocationManager)getSystemService(LOCATION_SERVICE);
	        Criteria c = new Criteria();
	        //c.setAccuracy(Criteria.ACCURACY_FINE);
	        c.setAccuracy(Criteria.NO_REQUIREMENT);
	        List<String> p = mLM.getAllProviders();
	        Trace("Providers : " + p.toString());
	        provider = mLM.getBestProvider(c, false);
	        Trace("using: " + provider);

	        mLM.requestLocationUpdates  (provider, 5000, minDistUpdate, this);
        } catch (Exception e) {
        	Trace(e.toString());
        }
        
        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification();
        Start(false);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Trace("onStartCommand Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
    	WriteDatFile();
       	mLM.removeUpdates(this);
        mNM.cancel(R.string.local_service_started);
    	Trace("GpsTrackService.onDestroy");
    	
    	hereAmI.stop();

        // Tell the user we stopped.
        Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
    	Trace("onDestroy");
    	closeTraceFile();
    	
    	super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
    	Trace("onBind");
        return mBinder;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.local_service_started);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.stat_sample, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, GpsTrack.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.local_service_label),
                       text, contentIntent);

        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        //mNM.notify(R.string.local_service_started, notification);

		startForeground(100, notification);

    }
    
    public void SetDisplayFields (	
    	TextView mOutput,
		EditText mDist,
		EditText mTime,
		EditText mSpeed,
		EditText mAvg)
    {
    	GpsTrackService.mOutput = mOutput;
    	if (mOutput != null) {
    		mOutput.append(traceText);
    	}
    	this.mDist = mDist;
    	this.mTime = mTime;
    	this.mSpeed = mSpeed;
    	this.mAvg = mAvg;  	

    	updateDisplayFields();

    }
    
    /******** Track file methods *********/
    private String NewTrackFileName() {
    	String name = "";
    	
    	lastTrackIndex++;
    	lastTrackIndex %= 100;
    	name = String.format("%s%s%03d%s", dataDir, trackFileNamePrefix, lastTrackIndex, trackFileNameExt);
    	
    	return name;
    	
    }
    
    private void createFile() { 
        File newxmlfile = new File(NewTrackFileName());
        Trace("created " + newxmlfile.getPath());
        try{
        	newxmlfile.createNewFile();
	        //we have to bind the new file with a FileOutputStream
	        ;       	
         	tcxFile = new FileOutputStream(newxmlfile);
 
         	//we create a XmlSerializer in order to write xml data
	        tcxSerializer = Xml.newSerializer();
	       	//we set the FileOutputStream as output for the serializer, using UTF-8 encoding
			tcxSerializer.setOutput(tcxFile, "UTF-8");
			//Write <?xml declaration with encoding (if encoding not null) and standalone flag (if standalone not null) 
			tcxSerializer.startDocument(null, Boolean.valueOf(true)); 
			//set indentation option
			tcxSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true); 
			tcxSerializer.startTag(null, "TrainingCenterDatabase");
			tcxSerializer.attribute(null, "xmlns", "http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2");
			tcxSerializer.attribute(null, "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			tcxSerializer.attribute(null, "xsi:schemaLocation", "http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2 http://www.garmin.com/xmlschemas/TrainingCenterDatabasev2.xsd");
			//start a tag called "root"
			tcxSerializer.startTag(null, "Activities"); 
			//i indent code just to have a view similar to xml-tree
			tcxSerializer.startTag(null, "Activity");
			tcxSerializer.attribute(null, "Sport", "Running");
			
//			tcxSerializer.text("\<Id\> " + formatTime(startMSec) + " \</Id\>");
			tcxSerializer.startTag(null, "Id");
			tcxSerializer.text(formatTime(startMSec));
			tcxSerializer.endTag(null, "Id");

			//tcxSerializer.text("\<Lap\> " + formatTime(startMSec));
			tcxSerializer.startTag(null, "Lap");
			tcxSerializer.attribute(null, "StartTime", formatTime(startMSec));

			tcxSerializer.startTag(null, "Track");
			
			
        } catch (Exception e) {
        	Trace("createFile: " + e);
        }
		
    }
    
    private void closeFile() {
    	try {
    		Trace("closing file");
    		tcxSerializer.endTag(null, "Track");
    		AddTagValue("TotalTimeSeconds", String.format("%d", (int)(currentTimeMSec/ 1000)));
    		AddTagValue("DistanceMeters", String.format("%f", currentDistMeters));
    		AddTagValue("MaximumSpeed", String.format("%f", maxSpeedMeterPerSec));
    		AddTagValue("Calories", "0");
    		AddTagValue("Intensity", "Active");
    		AddTagValue("TriggerMethod", "Manual");
    		tcxSerializer.endTag(null, "Lap");
    		//tcxSerializer.text("\</Lap\>");
    		tcxSerializer.endTag(null, "Activity");
    		tcxSerializer.endTag(null, "Activities");
			tcxSerializer.endTag(null, "TrainingCenterDatabase");
    		tcxSerializer.endDocument();
			//write xml data into the FileOutputStream
    		tcxSerializer.flush();
			//finally we close the file stream
			tcxFile.close();
    	} catch (Exception e) {
    		Trace("Could not close file " + e.getMessage());
    	}
    }
    
    private void AddTagValue(String tag, String value) throws IllegalArgumentException, IllegalStateException, IOException {
		tcxSerializer.startTag(null,tag);
		tcxSerializer.text(value);
		tcxSerializer.endTag(null, tag);

    }
    private String formatTime(long mSec) {
	    Date d = new Date(mSec);
    	return String.format("%d-%02d-%02dT%02d:%02d:%02dZ", 
	    		d.getYear() + 1900, d.getMonth() + 1, d.getDate(), d.getHours(), d.getMinutes(), d.getSeconds());
    }
    
    private void AddTrackPoint(Location l, long time) {
    	
    	try {
			tcxSerializer.startTag(null, "Trackpoint");
			

		    AddTagValue("Time", formatTime(time));
		    
			tcxSerializer.startTag(null, "Position");		
			AddTagValue("LatitudeDegrees", Double.toString(l.getLatitude()));
			AddTagValue("LongitudeDegrees", Double.toString(l.getLongitude()));
			tcxSerializer.endTag(null, "Position");
			
			AddTagValue("AltitudeMeters", Double.toString(l.getAltitude()));
			
			tcxSerializer.endTag(null, "Trackpoint");
    	} catch (Exception e) {
    		Trace ("AddTrackPoint: " + e);
    	}
    }

 
    private void ReadDatFile() {
    	FileInputStream   datFile = null;
    	try {
	    	datFile = new FileInputStream(new File(dataDir + dataFileName));
	        
	    	lastTrackIndex = datFile.read();
	    	
	    	datFile.close();
    	} catch (Exception e) {
    		Trace("ReadDatFile " + e);
    		try {
    			datFile.close();
    		} catch (Exception exc) {
    		    // Ignore
    		}
    	}
    }
    
    private void WriteDatFile() {
    	FileOutputStream   datFile = null;
    	try {
	    	datFile = new FileOutputStream(new File(dataDir + dataFileName));
	        
	    	datFile.write(lastTrackIndex);
	    	
	    	datFile.close();
    	} catch (Exception e) {
    		Trace("WriteDatFile " + e);
    		try {
    			datFile.close();
    		} catch (Exception exc) {
    		    // Ignore
    		}
    	}
    }

	private void updateDisplayFields(){
		if (mDist != null) {
		    mDist.setText(String.format("%.2f", currentDistMeters/1000));
		}
		if (mAvg != null) {
			double temp = currentDistMeters/1000;
			temp /= currentTimeMSec;
			temp = temp * 1000 * 60 * 60;
			mAvg.setText(String.format("%.2f", temp));
		}
		if (mTime != null) {
			Date t = new Date(currentTimeMSec);
			mTime.setText(String.format("%d:%d", t.getMinutes(), t.getSeconds()));
		}
		if (mSpeed != null) {
			mSpeed.setText(String.format("%.2f",lastSpeed * 3.6));
		}

	}
	
	
    private void start() {
    	currentDistMeters = 0;
    	currentTimeMSec = 0;
    	lastSpeed = 0;
    	maxSpeedMeterPerSec = 0;
    	startMSec = lastLocation.getTime();
    	lastLocation = mLM.getLastKnownLocation(provider);
    	updateDisplayFields();
    	if (fStoreTrackPonts){
    		createFile();
    		AddTrackPoint(lastLocation, lastLocation.getTime());
    	}
		
		state = State.Started;
    }

	public void Start(boolean fStoreTrackPoints) {
	    this.fStoreTrackPonts = fStoreTrackPoints;
    	startMSec = 0;
    	state = State.WaitinForFirstLocation;
    	
    	// After the first location update, the track is actually started.
	}
	
	public void Stop() throws Exception {
	    if (state == State.Idle) {
	    	throw new Exception("Not running");
	    }
    	state = State.Idle;

    	if (fStoreTrackPonts){
    		closeFile();
    	}
	}
	
	public boolean IsRunning() {
		return (state != State.Idle);
	}
	
    private void openTraceFile() {

    	try {
    		traceFile = new FileOutputStream(new File(dataDir + traceFileName), true); // append to existing file
	        Trace(dataDir + traceFileName);
    	} catch (Exception e) {
			traceFile = null;
			Trace("ReadDatFile " + e);
    		try {
    			traceFile.close();
    		} catch (Exception exc) {
    		    // Ignore
    		}
    	}
    }
    
    private void closeTraceFile() {
   		try {
			traceFile = null;
			traceFile.close();
		} catch (Exception exc) {
		    // Ignore
		}
   
    }

	public static void Trace(String str) {
		Trace(TAG, str);
	}
	static class Tracer implements Runnable{
		String mStr;
		TextView mOutput;
		Tracer(String str, TextView output){
			mStr = str;
			mOutput = output;
		}
		public void run() {
			mOutput.append("S: " + mStr + "\n");
		}

	}
	public static void Trace(String tag, String str) {
		try {

			if (mOutput != null) {
				traceText += str;
                handler.post(new Tracer(str, mOutput));
            }
			if (traceFile != null) {
				Calendar now = Calendar.getInstance();
				String s = String.format("%02d:%02d:%02d.%03d S ", 
						now.get(Calendar.HOUR_OF_DAY),
						now.get(Calendar.MINUTE),
						now.get(Calendar.SECOND),
						now.get(Calendar.MILLISECOND) );
				s += str + "\n";
				traceFile.write(s.getBytes());
			}
		} catch (Exception e){
			Log.e(TAG, "Exception " + e);
		}
		Log.e(TAG, str);
	}
    @Override
    public void onLocationChanged(Location location) {

        //long timeMSec = location.getTime();
        long timeMSec = java.lang.System.currentTimeMillis();
        if (location.hasSpeed()) {
            lastSpeed = location.getSpeed();
        }

        if ((state == State.WaitinForFirstLocation) && (startMSec == 0)) {
            lastLocation = location;
            start();
            Trace("Started");
        } else if (state == State.Started) {

            currentDistMeters += location.distanceTo(lastLocation);
            currentTimeMSec = timeMSec - startMSec;
            maxSpeedMeterPerSec = lastSpeed > maxSpeedMeterPerSec ? lastSpeed : maxSpeedMeterPerSec;
            lastLocation = location;
            if (fStoreTrackPonts){
                AddTrackPoint(location, timeMSec);
            }
            updateDisplayFields();
        }
        Trace("L");
        hereAmI.update(location);


    }

    @Override
    public void onProviderDisabled(String provider) {
        GpsTrackService.Trace(String.format("onProviderDisabled: %s", provider));

    }

    @Override
    public void onProviderEnabled(String provider) {
        GpsTrackService.Trace(String.format("onProviderEnabled: %s\n", provider));

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        GpsTrackService.Trace(String.format("onStatusChanged %s %d\n", provider, status));

    }

}

