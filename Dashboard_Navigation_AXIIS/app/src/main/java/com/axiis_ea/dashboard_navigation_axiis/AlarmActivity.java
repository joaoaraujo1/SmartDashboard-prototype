package com.axiis_ea.dashboard_navigation_axiis;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;


/**
 * Created by João on 02/03/2017.
 */

public class AlarmActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    public View decorView;
    EditText password;
    TextView keyText;
    View staticMap;
    Button submit;
    private StringBuilder stringBuilder = new StringBuilder();
    boolean endAlarm = false;
    Handler bluetoothHandler;
    final int RECIEVE_MESSAGE = 1;
    boolean isBtConnected = false;
    private BluetoothAdapter myBluetooth = null;
    private ConnectedThread connectedThread;
    private BluetoothSocket btSocket = null;
    private Set<BluetoothDevice> pairedDevices;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    String address = null;
    private final int MAX_SMS_MESSAGE_LENGTH = 160; // http://stackoverflow.com/questions/7620150/can-i-automatically-send-sms-without-the-user-need-to-approve
    boolean isPasswordMenuShowing = false;
    boolean isMenuChangingEnabled = true;
    //public static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_SENT";

    //GPS
    GoogleApiClient mGoogleApiClient;
    protected android.location.Location mLocation = null;
    private LocationRequest mLocationRequest;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildGoogleApiClient();
        setContentView(R.layout.alarm_activity);
        decorView = getWindow().getDecorView();
        MainActivity.hideSystemUI(decorView);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED, WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        password = (EditText) findViewById(R.id.txtPassword);
        keyText = (TextView) findViewById(R.id.keyTxt);
        submit = (Button) findViewById(R.id.btnSubmit);
        staticMap = findViewById(R.id.staticMap);

        pairedDevicesList();

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
                            if (sbprint.equals("A0")) {
                                msg("Alarm disengaged");
                                Intent returnToMain = new Intent(AlarmActivity.this, MainActivity.class);
                                startActivity(returnToMain);
                                connectedThread.resetConnection();
                                finish();
                            }
                            else if (sbprint.equals("MSG")) {
                                //send message
                                //msg("Sending notification...");
                                String phoneNumber = "911111111"; // TODO update this to read a dat file
                                String coordinateString = "BIKE ALARM ACTIVE!\n";
                                if(mLocation == null) { coordinateString += "GPS tracking unavailable"; }
                                else {
                                    coordinateString += "Position:\nhttps://maps.google.com/maps?q=";
                                    coordinateString += mLocation.getLatitude() + ",";
                                    coordinateString += mLocation.getLongitude() + "\n";
                                }
                                sendCoordinates(phoneNumber,coordinateString);
                            }

                            stringBuilder.delete(0, stringBuilder.length());
                            sbprint = null;
                        }
                        //Log.d(TAG, "...String:"+ sb.toString() +  "Byte:" + msg.arg1 + "...");
                        break;
                }
            }
        };
    }

    //function to initialize the google API client
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API) //for location services
                .addApi(ActivityRecognition.API) //for activity recognition services
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Connect the client.
        mGoogleApiClient.connect();

    }

    @Override
    public void onConnected(Bundle bundle) {

        // for only one location request. first location we get. then onLocationChanged takes care of updates
        //while (mLocation == null) // force a value to emerge
        //mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        //to continuously check position
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(3000);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {
        msg("GoogleApiClient connection has been suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        msg("GoogleApiClient connection has failed");
    }

    @Override
    public void onLocationChanged(android.location.Location location) {
        mLocation = location;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP)
            {
                if((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {

                    isMenuChangingEnabled = false;

                    if (isPasswordMenuShowing) {// HIDE PASSWORD MENU
                        keyText.setVisibility(View.INVISIBLE);
                        password.setVisibility(View.INVISIBLE);
                        submit.setVisibility(View.INVISIBLE);
                        staticMap.setVisibility(View.INVISIBLE);
                        isPasswordMenuShowing = false;
                        return true;

                    } else {
                        keyText.setVisibility(View.VISIBLE);
                        password.setVisibility(View.VISIBLE);
                        submit.setVisibility(View.VISIBLE);
                        staticMap.setVisibility(View.VISIBLE);
                        isPasswordMenuShowing = true;
                        return true;
                    }
                }

                else if((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP)
                    isMenuChangingEnabled = true;
            }
        return true;//super.dispatchKeyEvent(event);
    }

   /* public void showUnlockMenu (View view) {
        if (defaultText.getVisibility() == View.VISIBLE) {
            defaultText.setVisibility(View.INVISIBLE);
            keyText.setVisibility(View.VISIBLE);
            password.setVisibility(View.VISIBLE);
            submit.setVisibility(View.VISIBLE);
            staticMap.setVisibility(View.VISIBLE);
        } else {
            defaultText.setVisibility(View.VISIBLE);
            keyText.setVisibility(View.INVISIBLE);
            password.setVisibility(View.INVISIBLE);
            submit.setVisibility(View.INVISIBLE);
            staticMap.setVisibility(View.INVISIBLE);
        }
    }*/

    public void validatePassword(View view) {
        String writtenPass = password.getText().toString();
        if (writtenPass.length() == 0) {
            msg("Key field is empty");
        } else {
            if(getSHA(writtenPass).equals(getSHA("axiis")))
            {
                //deactivate alarm
                endAlarm = true;
                msg("Disengaging...");
                // TODO COMMENT OUT WHEN FINISHED DEBUGGING
                msg("Alarm disengaged");
                Intent returnToMain = new Intent(AlarmActivity.this, MainActivity.class);
                startActivity(returnToMain);
                finish();

            } else {
                msg("Invalid Key");
                password.setText("");
            }
        }

    }

    public void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }



    String getSHA(String password) {
        MessageDigest digest=null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e1) {
            Toast.makeText(getApplicationContext(), "Algorithm Exception", Toast.LENGTH_LONG).show();
            e1.printStackTrace();
        }
        digest.reset();
        byte[] data =  digest.digest(password.getBytes());
        String final_hash = String.format("%0" + (data.length*2) + "X", new BigInteger(1, data));
        Log.v("Password",final_hash);
        return final_hash;
    }

    private void pairedDevicesList() {

        myBluetooth = BluetoothAdapter.getDefaultAdapter();
        pairedDevices = myBluetooth.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice bt : pairedDevices) {
                //check if the name and ID correspond to the arduino board
                if (bt.getName().equals("HC-05") && bt.getAddress().equals("20:16:08:29:91:23")) {
                    address = bt.getAddress();
                    new AlarmActivity.ConnectBT().execute();
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
                ConnectSuccess = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
               //msg("Error with Bluetooth connection");
                try {btSocket.close();} catch (Exception e) {}
                btSocket = null;
                pairedDevicesList(); // try again
                //finish();
            } else {
                //msg("Connected.");
                isBtConnected = true;
                connectedThread = new AlarmActivity.ConnectedThread(btSocket);
                connectedThread.start();
            }
        }
    }


    private class ConnectedThread extends Thread {
        private InputStream mmInStream;
        private OutputStream mmOutStream;

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

                    if(endAlarm) {
                        byte start = 2;
                        mmOutStream.write(start);
                        endAlarm = false;
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

    @Override
    protected void onPause() {
        //Disconnect socket when paused
        if(connectedThread != null)
            connectedThread.resetConnection();
        super.onPause();
    }

    @Override
    protected void onStop() {
        //Disconnect socket when stoped
        if(connectedThread != null)
            connectedThread.resetConnection();

        if (mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();

        super.onStop();
    }


    private void sendCoordinates(String phonenumber,String message)
    {
        SmsManager manager = SmsManager.getDefault();

        PendingIntent piSend = PendingIntent.getBroadcast(this, 0, new Intent("SMS_RECEIVED_ACTION"), 0);
        PendingIntent piDelivered = PendingIntent.getBroadcast(this, 0, new Intent("SMS_DELIVERED_ACTION"), 0);

            int length = message.length();

            if(length > MAX_SMS_MESSAGE_LENGTH)
            {
                ArrayList<String> messagelist = manager.divideMessage(message);

                manager.sendMultipartTextMessage(phonenumber, null, messagelist, null, null);
            }
            else
            {
                manager.sendTextMessage(phonenumber, null, message, piSend, piDelivered);
            }
    }



}
