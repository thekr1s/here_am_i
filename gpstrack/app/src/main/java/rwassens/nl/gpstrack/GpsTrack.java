package rwassens.nl.gpstrack;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class GpsTrack extends Activity {
	private static final String TAG = "GpsTrack";
	
	private static TextView mOutput;
	private EditText mDist;
	private EditText mTime;
	private EditText mSpeed;
	private EditText mAvg;
    private boolean mIsBound;
    private GpsTrackService mBoundService;
    
    /******** Activity callbacks *********/
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mBoundService = null;
        mOutput = (TextView) findViewById(R.id.output);
        mDist = (EditText) findViewById(R.id.dist);
        mTime = (EditText) findViewById(R.id.time);
        mSpeed = (EditText) findViewById(R.id.speed);
        mAvg = (EditText) findViewById(R.id.average);

        Trace("onCreate");

  		ComponentName n = startService(new Intent(GpsTrack.this,
				 GpsTrackService.class));
		if (n == null) {
			 Trace("StartService FAILED");
		} else {
			doBindService();
		}
		
        // Hook up button presses to the appropriate event handler.
        ((Button) findViewById(R.id.exit)).setOnClickListener(mExitListener);
        ((Button) findViewById(R.id.startstop)).setOnClickListener(mStartStopListener);       
   }
    
    @Override
    public void onStop() {
    	super.onStop();
    	Trace("onStop");
    	finish(); // Force destroy here. If I don't multiple instances will be possible
    }

    public void onDestroy() {
    	 
    	mBoundService.SetDisplayFields(null, null, null, null, null);
     	doUnbindService();
    	super.onDestroy();
    	Trace("onDestroy");

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // We are going to create two menus. Note that we assign them
        // unique integer IDs, labels from our string resources, and
        // given them shortcuts.
        menu.add(0, 1, 0, "Options").setShortcut('0', 'b');

        return true;
    }

    /**
     * Called when a menu item is selected.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case 1:
			Log.d(TAG, "Options");
			try {
	            Intent settingsActivity = new Intent(getBaseContext(),
	                    Preferences.class);
	            startActivity(settingsActivity);
			} catch (Exception e) {
				Log.d(TAG, e.toString());
				
			}

            return true;
        }
        
        return false;
    }
    /******** Service binding *********/ 
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = ((GpsTrackService.LocalBinder)service).getService();
        	Button ssButton = ((Button) findViewById(R.id.startstop));
            if (mBoundService.IsRunning()) {
             	ssButton.setText("stop");               	
            } else {
            	ssButton.setText("start");
            	
            }
   
            mBoundService.SetDisplayFields(mOutput, mDist, mTime, mSpeed, mAvg);
            // Tell the user about this for our demo.
            Toast.makeText(GpsTrack.this, R.string.local_service_connected,
                    Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
        	mBoundService.SetDisplayFields(null, null, null, null, null);
            mBoundService = null;
            Toast.makeText(GpsTrack.this, R.string.local_service_disconnected,
                    Toast.LENGTH_SHORT).show();
        }
    };
    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(GpsTrack.this, 
        		GpsTrackService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }
    
    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    /******** Button handling *********/
    /**
     * A call-back for when the user presses the exit button.
     */
	private OnClickListener mExitListener = new OnClickListener() {
        public void onClick(View v) {
        	boolean running = mBoundService.IsRunning();
            if (!running) {
            	stopService(new Intent(GpsTrack.this, GpsTrackService.class));
            }

            finish();
        }
    };

    /**
     * A call-back for when the user presses the stop button.
     */
    private OnClickListener mStartStopListener = new OnClickListener() {
        public void onClick(View v) {
        	Button ssButton = ((Button) findViewById(R.id.startstop));
        	try {
	            if (!mBoundService.IsRunning()) {
	
	            	mBoundService.Start(true);
	             	ssButton.setText("stop");               	
	            } else {
	            	mBoundService.Stop();
	            	ssButton.setText("start");
	            	
	            }
        	} catch (Exception exc) {
        		Trace("mStartStopListener.onClick " + exc.getMessage());
        	}
        }
    };
    

	public static void Trace(String str) {
		GpsTrackService.Trace(TAG, str);
	}
}