package com.axiis_ea.dashboard_navigation_axiis;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.here.android.mpa.common.CopyrightLogoPosition;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.Route;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Set;
import java.util.UUID;

import static com.axiis_ea.dashboard_navigation_axiis.MapActivity.locationMarkerImage;
import static com.here.android.mpa.routing.Maneuver.Action;

/**
 * Created by João on 09/02/2017.
 */

public class NavigationActivity extends AppCompatActivity {

    private MapRoute mapRoute = null;
    public View decorView;
    private Map map = null;
    private MapFragment mapFragment = null;
    Image positionImage;
    NavigationManager navigationManager;
    boolean positionIndicatorSet = false;
    MapMarker locationMarker;
    final int RECIEVE_MESSAGE = 1;
    private StringBuilder stringBuilder = new StringBuilder();
    Handler bluetoothHandler, timeHandler;
    private TextView speedValue, timeView, etaTime, tripLength, maneuverText, maneuverDist;
    private TimerThread timer;
    BluetoothSocket btSocket = null;
    private BluetoothAdapter myBluetooth = null;
    private Set<BluetoothDevice> pairedDevices;
    private boolean isBtConnected = false;
    private ConnectedThread connectedThread;
    String address = null;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_NETWORK_STATE
    };
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation_activity);
        decorView = getWindow().getDecorView();
        hideSystemUI(decorView);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED, WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        timeView = (TextView) findViewById(R.id.clockTime);
        speedValue = (TextView) findViewById(R.id.speed_value);
        etaTime = (TextView) findViewById(R.id.etaTime);
        tripLength = (TextView) findViewById(R.id.trip_length);
        maneuverText = (TextView) findViewById(R.id.maneuverTxt);
        maneuverDist = (TextView) findViewById(R.id.maneuverDist);

        bluetoothHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECIEVE_MESSAGE:
                        //if(stringBuilder.length()!=0) stringBuilder.delete(0, stringBuilder.length());
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, msg.arg1);                  // create string from bytes array
                        stringBuilder.append(strIncom);                                      // append string
                        int endOfLineIndex = stringBuilder.indexOf("\r\n");                  // determine the end-of-line
                        if (endOfLineIndex > 0) {                                            // if end-of-line,
                            String sbprint = stringBuilder.substring(0, endOfLineIndex);     // extract string
                            Log.v("sbprint", sbprint);
                            if (sbprint.startsWith("V")) {
                                sbprint = sbprint.substring(1);
                                stringBuilder.delete(0, stringBuilder.length());                 // and clear
                                speedValue.setText(sbprint);             // update TextView
                            }

                            stringBuilder.delete(0, stringBuilder.length());
                            sbprint = null;
                        }
                        //Log.d(TAG, "...String:"+ sb.toString() +  "Byte:" + msg.arg1 + "...");
                        break;
                }
            }
        };

        initializeNavigation();

        //if the device has bluetooth
        myBluetooth = BluetoothAdapter.getDefaultAdapter();

        if (myBluetooth == null) {
            //Show a mensag. that the device has no bluetooth adapter
            Toast.makeText(getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();

            //finish apk
            finish();
        } else if (!myBluetooth.isEnabled()) {
            //Ask to the user turn the bluetooth on
            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTon, 10);
        }

        timeHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECIEVE_MESSAGE:
                        timeView.setText((String) msg.obj);
                        break;
                }
            }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 10 && resultCode == RESULT_OK) {
            //String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_MODE);
            //if (provider == null || provider.isEmpty()) {
            pairedDevicesList();
        }

        else {
            msg("Activate Bluetooth to initialize AXIIS Dashboard");
            //finish(); TODO UNCOMMENT WHEN FINISHED DEBUGGING
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        timer = new TimerThread();
        timer.start();

    }

    @Override
    protected void onStop() {

        if (navigationManager != null) {
            navigationManager.stop();
        }

        if(connectedThread != null)
            connectedThread.resetConnection();

        super.onStop();
    }

    public static void hideSystemUI(View mDecorView) {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        mDecorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void initializeNavigation() {

        // Search for the map fragment to finish setup by calling init().
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(
                R.id.mapfragment_navigation);
        navigationManager = NavigationManager.getInstance();

        mapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(
                    OnEngineInitListener.Error error) {
                if (error == OnEngineInitListener.Error.NONE) {
                    // retrieve a reference of the map from the map fragment
                    map = mapFragment.getMap();
                    positionImage = locationMarkerImage;
                    mapFragment.getPositionIndicator().setMarker(positionImage);

                    //SET NAVIGATION DETAILS

                    map.setMapScheme(map.getMapSchemes().get(45));

                    mapFragment.getPositionIndicator().setVisible(true);
                    //mapFragment.getPositionIndicator().setAccuracyIndicatorVisible(true);
                    //mapFragment.getPositionIndicator().setAccuracyIndicatorColor(Color.argb());

                    //set the map where the navigation will be performed
                    navigationManager.setMap(map);

                    //Calculate route and set position
                    //routeManager.calculateRoute(routePlan, new RouteListener());
                    mapRoute = MapActivity.mapRoute;
                    map.setCenter(mapRoute.getRoute().getStart(), Map.Animation.NONE);
                    map.setZoomLevel(map.getMaxZoomLevel() / 1.175);
                    mapFragment.setCopyrightLogoPosition(CopyrightLogoPosition.TOP_CENTER);
                    map.addMapObject(mapRoute);

                    //add right away a location marker while position marker is loaded
                    locationMarker = new MapMarker();
                    locationMarker.setIcon(positionImage);
                    locationMarker.setVisible(true);
                    locationMarker.setCoordinate(mapRoute.getRoute().getStart());
                    map.addMapObject(locationMarker);

                    MapMarker end = new MapMarker();
                    end.setCoordinate(mapRoute.getRoute().getDestination());
                    end.setVisible(true);
                    map.addMapObject(end);

                    navigationManager.simulate(mapRoute.getRoute(),60);
                    navigationManager.setMapUpdateMode(NavigationManager.MapUpdateMode.ROADVIEW_NOZOOM);
                    mapRoute.setManeuverNumberVisible(true);

        /* Register a PositionListener to monitor the position updates */
                    navigationManager.addPositionListener(
                            new WeakReference<NavigationManager.PositionListener>(positionListener));

                } else {
                    msg("ERROR: Cannot initialize Map Fragment");
                }
            }
        });

        navigationManager.addNewInstructionEventListener(
                new WeakReference<NavigationManager.NewInstructionEventListener>(instructListener));

    }
    //LISTENER FOR POSITION UPDATE EVENTS
    private NavigationManager.PositionListener positionListener = new NavigationManager.PositionListener() {
        @Override
        public void onPositionUpdated(GeoPosition geoPosition) {
            if (!positionIndicatorSet){
                //when the position of the marker is updated, it is loaded, remove marker
                map.removeMapObject(locationMarker);
                positionIndicatorSet = true;

            }

            if(navigationManager != null) {
                String timeToArrival = Long.toString(navigationManager.getTta(Route.TrafficPenaltyMode.DISABLED, true).getDuration() / 60);
                etaTime.setText(timeToArrival);
                String distanceToArrival = String.format("%.1f", (double)navigationManager.getDestinationDistance() / 1000.0);
                tripLength.setText(distanceToArrival);

                if(navigationManager.getNextManeuver()!= null)
                    maneuverDist.setText(Long.toString(navigationManager.getNextManeuverDistance()));

            }

            //Log.e("HELLO TAG","TEST DEBUUGGGGGGG");

        }
    };

    private NavigationManager.NewInstructionEventListener instructListener
            = new NavigationManager.NewInstructionEventListener() {
        @Override
        public void onNewInstructionEvent() {
// Interpret and present the Maneuver object as it contains
// turn by turn navigation instructions for the user.
            if(navigationManager != null) {
                if(navigationManager.getNextManeuver()!= null) {
                    String maneuver = navigationManager.getNextManeuver().getTurn().toString();
                    //Icon maneuverIcon = navigationManager.getNextManeuver().getIcon();

                    if(navigationManager.getNextManeuver().getAction() != Action.END) {

                        if(maneuver.equals("UNDEFINED") || maneuver.equals("NO_TURN"))
                            maneuver = "Go forward";

                        else if(maneuver.equals("KEEP_MIDDLE"))
                            maneuver = "Keep in middle";

                        else if(maneuver.equals("KEEP_RIGHT"))
                            maneuver = "Keep right";

                        else if(maneuver.equals("KEEP_LEFT"))
                            maneuver = "Keep left";

                        else if(maneuver.equals("LIGHT_RIGHT"))
                            maneuver = "Right turn (light)";

                        else if(maneuver.equals("QUITE_RIGHT"))
                            maneuver = "Right turn";

                        else if(maneuver.equals("HEAVY_RIGHT"))
                            maneuver = "Right turn (heavy)";

                        else if(maneuver.equals("LIGHT_LEFT"))
                            maneuver = "Left turn (light)";

                        else if(maneuver.equals("QUITE_LEFT"))
                            maneuver = "Left turn";

                        else if(maneuver.equals("HEAVY_LEFT"))
                            maneuver = "Left turn (heavy)";

                        else if(maneuver.equals("RETURN"))
                            maneuver = "U-Turn";

                        else if(maneuver.contains("ROUNDABOUT_")){
                            for (int i = 1; i<=12; i++){
                                String exit = Integer.toString(i);
                                if(maneuver.equals("ROUNDABOUT_" + exit)){
                                    maneuver = "Roundabout exit " + exit;
                                    break;
                                }
                            }

                        }


                        maneuverText.setText(maneuver);
                    }
                    else maneuverText.setText("Destination at");
                }

            }
        }
    };

    public void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    //Clock
    private class TimerThread extends Thread {

        private TimerThread() {
        }

        @Override
        public void run() {


            while (true) {
                Calendar now = Calendar.getInstance();
                int hour = now.get(Calendar.HOUR_OF_DAY);
                int minute = now.get(Calendar.MINUTE);
                String time = hour + ":";
                if (minute < 10) time += "0" + minute;
                else time += minute;
                //DateFormat mDateFormat = new SimpleDateFormat("h:mm a");
                //time = mDateFormat.format(Calendar.getInstance().getTime());
                timeHandler.obtainMessage(RECIEVE_MESSAGE, time.length(), -1, time).sendToTarget();
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                }
            }
        }
    }

    private void pairedDevicesList() {

        pairedDevices = myBluetooth.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice bt : pairedDevices) {
                //check if the name and ID correspond to the arduino board
                if (bt.getName().equals("HC-05") && bt.getAddress().equals("20:16:08:29:91:23")) {
                    address = bt.getAddress();
                    new ConnectBT().execute();
                    break;
                }
            }
        } else {
            Toast.makeText(getApplicationContext(), "No Paired Arduino Boards Found.", Toast.LENGTH_LONG).show();
        }
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute() {
            //progress = ProgressDialog.show(MainActivity.this, "Connecting to Arduino", "Please wait...");  //show a progress dialog
            //loadingAction("Connecting to Arduino...");
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try {
                if (btSocket == null || !isBtConnected) {
                    BluetoothDevice arduinoShield = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = arduinoShield.createRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            } catch (IOException e) {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                //progress.dismiss();
                //loadingLayout.setVisibility(View.INVISIBLE);
                msg("Connection Failed. Trying again.");
                try {btSocket.close();} catch (Exception e) {}
                btSocket = null;
                pairedDevicesList();
                //finish();
            } else {
                //msg("Connected.");
                isBtConnected = true;
                connectedThread = new ConnectedThread(btSocket);
                connectedThread.start();
            }
            //loadingLayout.setVisibility(View.INVISIBLE);
        }
    }

    private class ConnectedThread extends Thread {
        private InputStream mmInStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
        }

        public void run() {

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    byte[] buffer = new byte[256];  // buffer store for the stream
                    int bytes; // bytes returned from read()
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);        // Get number of bytes and message in "buffer"
                    //Log.v("RECEIVED","Bluetooth received");// if receive massage
                    bluetoothHandler.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();     // Send to message queue Handler
                    buffer = null;
                    bytes = 0;

                } catch (IOException e) {
                    break;
                }

            }
        }

        public void resetConnection() {
            if (mmInStream != null) {
                try {mmInStream.close();} catch (Exception e) {}
                mmInStream = null;
            }

            if (btSocket != null) {
                try {btSocket.close();} catch (Exception e) {}
                btSocket = null;
            }

        }
    }

}
        /*
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: normal.day
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: normal.night
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: terrain.day
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: satellite.day
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: satellite.night
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: hybrid.day
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: hybrid.night
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: pedestrian.day
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: pedestrian.night
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: pedestrian.day.hybrid
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: pedestrian.night.hybrid
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: normal.day.grey
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: normal.night.grey
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: hybrid.grey.day
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: hybrid.grey.night
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: normal.traffic.day
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: normal.traffic.night
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: hybrid.traffic.day
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: hybrid.traffic.night
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: normal.day.transit
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: normal.night.transit
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: hybrid.day.transit
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: hybrid.night.transit
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: reduced.day
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: reduced.night
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: hybrid.reduced.day
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: hybrid.reduced.night
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: carnav.day
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: carnav.night
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: carnav.hybrid.day
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: carnav.hybrid.night
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: carnav.traffic.day
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: carnav.traffic.night
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: carnav.traffic.hybrid.day
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: carnav.traffic.hybrid.night
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: maneuver.day
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: truck.day
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: truck.night
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: hybrid.truck.day
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: hybrid.truck.night
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: trucknav.day
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: trucknav.night
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: trucknav.hybrid.day
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: trucknav.hybrid.night
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: carnav.day.grey
        02-10 03:18:43.816 31579-31579/com.axiis_ea.dashboard_navigation_axiis I/System.out: carnav.night.grey
        */
