package com.axiis_ea.dashboard_navigation_axiis;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.SEND_SMS
    };
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;
    final int RECIEVE_MESSAGE = 1;
    public View decorView;
    BluetoothSocket btSocket = null;
    String address = null;
    Handler bluetoothHandler, timeHandler;
    private TextView outsideTemperature, speedValue, timeView;
    Button menuButton, definitionsButton, dataButton, goProButton, mapButton, alarmButton;
    //Bluetooth
    private BluetoothAdapter myBluetooth = null;
    private Set<BluetoothDevice> pairedDevices;
    private boolean isBtConnected = false;
    //private ProgressDialog progress, alarmProgress;
    private ConnectedThread connectedThread;
    private StringBuilder stringBuilder = new StringBuilder();
    private TimerThread timer;
    RelativeLayout loadingLayout, menuLayout;
    TextView loadingText;

    boolean startAlarm = false;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        /* CODE TO CHECK APP ID TO LAUNCH AS INTENT
        final PackageManager pm = getPackageManager();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> appList = pm.queryIntentActivities(mainIntent, 0);
        Collections.sort(appList, new ResolveInfo.DisplayNameComparator(pm));

        for (ResolveInfo temp : appList) {

            Log.v("my logs", "package and activity name = "
                    + temp.activityInfo.packageName + "    "
                    + temp.activityInfo.name);


        }
    } com.gopro.smarty    com.gopro.smarty.activity.HomeActivity*/


        timeView = (TextView) findViewById(R.id.clock_time);
        outsideTemperature = (TextView) findViewById(R.id.outside_temp);
        menuButton = (Button) findViewById(R.id.optionsButton);
        goProButton = (Button) findViewById(R.id.goProButton);
        mapButton = (Button) findViewById(R.id.mapButton);
        alarmButton = (Button) findViewById(R.id.alarmButton);
        dataButton = (Button) findViewById(R.id.dataButton);
        definitionsButton = (Button) findViewById(R.id.definitionsButton);
        loadingLayout = (RelativeLayout) findViewById(R.id.loading_layout);
        menuLayout = (RelativeLayout) findViewById(R.id.menuLayout);
        loadingText = (TextView) findViewById(R.id.loadingText);

        speedValue = (TextView) findViewById(R.id.speed_value);

        decorView = getWindow().getDecorView();
        hideSystemUI(decorView);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED, WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        //Data handler of what comes from arduino
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
                                if(sbprint.equals("V0")) menuLayout.setVisibility(View.VISIBLE);
                                else menuLayout.setVisibility(View.INVISIBLE);
                                sbprint = sbprint.substring(1);
                                stringBuilder.delete(0, stringBuilder.length());                 // and clear
                                speedValue.setText(sbprint);             // update TextView
                            } else if (sbprint.startsWith("T")) {
                                sbprint = sbprint.substring(1);
                                stringBuilder.delete(0, stringBuilder.length());                 // and clear
                                outsideTemperature.setText(sbprint);
                                System.out.print(sbprint);
                            } else if (sbprint.equals("A1")) {
                                if(loadingLayout.getVisibility() == View.VISIBLE){
                                    //alarmProgress.dismiss();
                                    loadingLayout.setVisibility(View.INVISIBLE);
                                }
                                msg("Alarm Engaged");
                                Intent alarmActivity = new Intent(MainActivity.this, AlarmActivity.class);
                                startActivity(alarmActivity);
                                connectedThread.resetConnection();
                                finish();
                            }

                            stringBuilder.delete(0, stringBuilder.length());
                            sbprint = null;
                        }
                        //Log.d(TAG, "...String:"+ sb.toString() +  "Byte:" + msg.arg1 + "...");
                        break;
                }
            }
        };

        checkPermissions(); // check manifest permissions

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

        else pairedDevicesList();

         //while(!myBluetooth.isEnabled()){} // pause app while there is no connection

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
                finish();
            }
    }


    @Override
    protected void onStart() {
        super.onStart();
        timer = new TimerThread();
        timer.start();
    }

    @Override
    protected void onPause() {
        //Disconnect socket when paused
        //if(connectedThread != null)
          //  connectedThread.resetConnection();

        super.onPause();
    }

    @Override
    protected void onStop() {
        //Disconnect socket when stoped
        if(connectedThread != null)
            connectedThread.resetConnection();

        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI(decorView);
    }

    protected void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<String>();
// check all required dynamic permissions
        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
// request all missing permissions
            final String[] permissions = missingPermissions
                    .toArray(new String[missingPermissions.size()]);
            ActivityCompat.requestPermissions(this, permissions,
                    REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS,
                    REQUIRED_SDK_PERMISSIONS,
                    grantResults);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int index = permissions.length - 1; index >= 0; --index) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
// exit the app if one permission is not granted
                        Toast.makeText(this, "Required permission '" + permissions[index]
                                + "' not granted, exiting", Toast.LENGTH_LONG).show();
                        finish();
                        return;
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

    public void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    public void openMenu(View view) {
        if (goProButton.getVisibility() == View.VISIBLE) {
            goProButton.setVisibility(View.INVISIBLE);
            mapButton.setVisibility(View.INVISIBLE);
            alarmButton.setVisibility(View.INVISIBLE);
            dataButton.setVisibility(View.INVISIBLE);
            definitionsButton.setVisibility(View.INVISIBLE);
        } else {
            goProButton.setVisibility(View.VISIBLE);
            mapButton.setVisibility(View.VISIBLE);
            alarmButton.setVisibility(View.VISIBLE);
            dataButton.setVisibility(View.VISIBLE);
            definitionsButton.setVisibility(View.VISIBLE);
        }
    }

    public void zeroSpeed(View view){
        menuLayout.setVisibility(View.VISIBLE);
        speedValue.setText("0");

    }

    public void openGoPro(View view) {

        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.gopro.smarty");
        if (launchIntent != null) {
            startActivity(launchIntent);//null pointer check in case package name was not found
        } else msg("Could not open GoPro app");
    }

    public void openMap(View view) {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            /*AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage("Enable GPS to initialize AXIIS Dashboard")
                    .setCancelable(false)
                    .setPositiveButton("Enable GPS",
                            new DialogInterface.OnClickListener(){
                                public void onClick(DialogInterface dialog, int id){
                                    Intent callGPSSettingIntent = new Intent(
                                            android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                    startActivityForResult(callGPSSettingIntent,1);
                                }
                            });*/
            Intent callGPSSettingIntent = new Intent(
                    android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(callGPSSettingIntent);
        } else {

            Intent mapActivity = new Intent(MainActivity.this, MapActivity.class);
            startActivity(mapActivity);
            finish();
        }
    }

    public void initAlarm(View view) {
        startAlarm = true;
        //alarmProgress = ProgressDialog.show(MainActivity.this, "Activating Alarm", "Please wait...");
        loadingAction("Activating Alarm...");

    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute() {
            //progress = ProgressDialog.show(MainActivity.this, "Connecting to Arduino", "Please wait...");  //show a progress dialog
            loadingAction("Connecting to Arduino...");
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
                loadingLayout.setVisibility(View.INVISIBLE);
                //msg("Connection Failed. Trying again.");
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
            loadingLayout.setVisibility(View.INVISIBLE);
        }
    }

    private class ConnectedThread extends Thread {
        private  InputStream mmInStream;
        private  OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
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

                    if(startAlarm) {
                        byte start = 1;
                            mmOutStream.write(start);
                            startAlarm = false;
                        }

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

            if (mmOutStream != null) {
                try {mmOutStream.close();} catch (Exception e) {}
                mmOutStream = null;
            }

            if (btSocket != null) {
                try {btSocket.close();} catch (Exception e) {}
                btSocket = null;
            }

        }
    }


    //Clock
    private class TimerThread extends Thread {

        public TimerThread() {
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

    private void loadingAction (String text) {
        loadingLayout.setVisibility(View.VISIBLE);
        loadingText.setText(text);
    }

}

