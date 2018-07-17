package com.axiis_ea.dashboard_navigation_axiis;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.Route;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.RoutingError;
import com.here.android.mpa.search.ErrorCode;
import com.here.android.mpa.search.GeocodeRequest;
import com.here.android.mpa.search.Location;
import com.here.android.mpa.search.ResultListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by João on 07/02/2017.
 */

public class MapActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener{


    public static MapRoute mapRoute = null;
    public View decorView;
    protected android.location.Location mLocation = null;
    GoogleApiClient mGoogleApiClient;
    private Map map = null;
    boolean mapCentered = false;
    private MapFragment mapFragment = null;
    MapMarker locationMarker, end;
    Button startNavigation;
    private LocationRequest mLocationRequest;
    EditText destination_final;
    ListView suggestionList;
    ArrayList suggestionArrayList;
    Handler suggestionHandler;
    RelativeLayout loadingGpsLayout, routeLoadingLayout, routeChoiceLayout;
    ProgressBar routeLoadingBar;
    RouteWaypoint waypointStart, waypointFinish;
    CheckBox shortestRouteCheckBox, fastestRouteCheckBox;
    public static Image locationMarkerImage = null;
    MapRoute[] tempRoute = new MapRoute[2];
    boolean isShortRouteSelected = false;
    float[] mapTouchCoordinates = new float[2];
    Button getDirections;
    private TextView etaTime, tripLength;
    LinearLayout etaLayoutMap;
    String[] tempEta = new String[2];
    String[] tempDist = new String[2];


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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildGoogleApiClient();
        decorView = getWindow().getDecorView();
        hideSystemUI(decorView);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED, WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        initializeMap();
        startNavigation = (Button) findViewById(R.id.navigationButton);
        startNavigation.setVisibility(View.INVISIBLE);
        destination_final = (EditText) findViewById(R.id.destinationString);
        suggestionList = (ListView) findViewById(R.id.suggestionListView);
        loadingGpsLayout = (RelativeLayout) findViewById(R.id.gps_loading_layout);
        routeLoadingLayout = (RelativeLayout) findViewById(R.id.route_loading_layout);
        routeLoadingBar = (ProgressBar) findViewById(R.id.route_calculation_bar);
        shortestRouteCheckBox = (CheckBox) findViewById(R.id.shortestTickBox);
        fastestRouteCheckBox = (CheckBox) findViewById(R.id.fastestTickBox);
        routeChoiceLayout = (RelativeLayout) findViewById(R.id.tickChoiceLayout);
        tempRoute[0] = null; tempRoute[1] = null;
        tempDist[0] = null; tempDist[1] = null;
        tempEta[0] = null; tempEta[1] = null;
        mapTouchCoordinates[0] = 0; mapTouchCoordinates[1] = 0;
        getDirections = (Button) findViewById(R.id.touchDestinationButton);
        etaTime = (TextView) findViewById(R.id.etaTime);
        tripLength = (TextView) findViewById(R.id.trip_length);
        etaLayoutMap = (LinearLayout) findViewById(R.id.etaLayoutMap);

        //Code to close keyboard when clicking on the map
        mapFragment.setOnTouchListener(new View.OnTouchListener() {
            long touchTime = 0;
            float[] touchCoord = new float[2];
            float touchMargin = 20;
            MapMarker possibleDestination;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
                    touchTime = System.currentTimeMillis();
                    touchCoord[0] = event.getX();
                    touchCoord[1] = event.getY();
                    //System.out.println(touchTime);
                }
                else if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                    hideSystemUI(decorView);
                    hideSoftKeyboard(MapActivity.this);
                    //backButton.setVisibility(View.VISIBLE);

                    if(tempRoute[0] != null || tempRoute[1] != null) startNavigation.setVisibility(View.VISIBLE);

                    if(   System.currentTimeMillis() - touchTime >=500 &&
                            event.getX() <= touchCoord[0] + touchMargin &&
                            event.getX() >= touchCoord[0] - touchMargin &&
                            event.getY() <= touchCoord[1] + touchMargin &&
                            event.getY() >= touchCoord[1] - touchMargin){

                        Display display = getWindowManager().getDefaultDisplay();
                        Point screensize = new Point();
                        display.getSize(screensize);

                        if(event.getX() > (screensize.x) / 2) getDirections.setX(event.getX() -200);
                        else getDirections.setX(event.getX() + 50);

                        PointF markerCoordinates = new PointF(touchCoord[0],touchCoord[1]);

                        if(possibleDestination != null){
                            map.removeMapObject(possibleDestination);
                            possibleDestination = null;
                        }

                        possibleDestination = new MapMarker();
                        //TODO ADD IMAGE
                        possibleDestination.setCoordinate(map.pixelToGeo(markerCoordinates));
                        possibleDestination.setVisible(true);
                        map.addMapObject(possibleDestination);


                        getDirections.setY(event.getY());
                        getDirections.setVisibility(View.VISIBLE);

                        mapTouchCoordinates[0] = touchCoord[0];
                        mapTouchCoordinates[1] = touchCoord[1];

                    } else {
                        getDirections.setVisibility(View.INVISIBLE);
                        if(possibleDestination!= null){
                            map.removeMapObject(possibleDestination);
                            possibleDestination = null;
                        }
                    }

                }

                return false; // if true, map loses mobility
            }
        });


        //set listener to editText of location to change suggestion list
        destination_final.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
                if(suggestionList.getVisibility() == View.VISIBLE)
                    suggestionList.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {

                if(destination_final.getText().toString().length() > 0) {
                    if(suggestionList.getVisibility() == View.INVISIBLE)
                        suggestionList.setVisibility(View.VISIBLE);


                    GeocodeRequest requestCoordinates = new GeocodeRequest(destination_final.getText().toString())
                            .setSearchArea(new GeoCoordinate(mLocation.getLatitude(), mLocation.getLongitude(), mLocation.getAltitude()), 5000); // select a 5000 km radius from starting point to find the geocode

                    if (requestCoordinates.execute( new GeocodeListener() ) != ErrorCode.NONE) {
                        // Handle request error
                        msg("Error requesting coordinates from listener");
                    }
                }

            }
        });

        suggestionHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {

                        final List<Location> extractData = (List<Location>) msg.obj;
                        suggestionArrayList = new ArrayList();

                        for (Location i : extractData) {
                            String tempAddress = i.getAddress().getText();
                            if(tempAddress.contains("<br/>")) tempAddress = tempAddress.replace("<br/>",", ");
                            suggestionArrayList.add(tempAddress);
                        }

                        ArrayAdapter adapter = new ArrayAdapter(MapActivity.this, android.R.layout.simple_list_item_1, suggestionArrayList);
                        suggestionList.setAdapter(adapter);

                        suggestionList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view,int position, long id)
                    {
                        int suggestionIndex = (int) suggestionList.getItemIdAtPosition(position);
                        destination_final.setText(suggestionArrayList.get(suggestionIndex).toString());
                        CoreRouter routeManager = new CoreRouter();

                        // 3. Select routing options
                        RoutePlan routePlan = new RoutePlan();

                        // 4. Select Waypoints for your routes
                        // START: Get current position
                        waypointStart = new RouteWaypoint(new GeoCoordinate(mLocation.getLatitude(), mLocation.getLongitude(), mLocation.getAltitude()));
                        routePlan.addWaypoint(waypointStart);

                        System.out.println("Suggestion index: " + suggestionIndex);
                        waypointFinish= new RouteWaypoint(extractData.get(suggestionIndex).getCoordinate());
                        routePlan.addWaypoint(waypointFinish);

                        RouteOptions routeOptions = new RouteOptions();
                        routeOptions.setTransportMode(RouteOptions.TransportMode.CAR);
                        routeOptions.setRouteType(RouteOptions.Type.FASTEST);
                        routeOptions.setRouteCount(1);
                        routePlan.setRouteOptions(routeOptions);

                        if(mapRoute != null) map.removeMapObject(mapRoute);
                        if(tempRoute[0] != null) map.removeMapObject(tempRoute[0]);
                        if(tempRoute[1] != null) map.removeMapObject(tempRoute[1]);
                        if(end != null) map.removeMapObject(end);
                        tempRoute[0] = null; tempRoute[1] = null;
                        tempDist[0] = null; tempDist[1] = null;
                        tempEta[0] = null; tempEta[1] = null;


                        suggestionList.setVisibility(View.INVISIBLE);
                        hideSoftKeyboard(MapActivity.this);
                        routeLoadingLayout.setVisibility(View.VISIBLE);

                        routeManager.calculateRoute(routePlan, new RouteListener());

                        fastestRouteCheckBox.setEnabled(false);
                        fastestRouteCheckBox.setChecked(true);
                        shortestRouteCheckBox.setEnabled(true);
                        shortestRouteCheckBox.setChecked(false);
                        shortestRouteCheckBox.setTypeface(Typeface.DEFAULT);
                        fastestRouteCheckBox.setTypeface(Typeface.DEFAULT_BOLD);

                    }});

                suggestionList.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {

                        hideSoftKeyboard(MapActivity.this);
                        startNavigation.setVisibility(View.INVISIBLE);
                        //backButton.setVisibility(View.INVISIBLE);

                        return false;
                    }
                });

            }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == 0) {
            String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_MODE);
            if (provider == null || provider.isEmpty()) {
                msg("Activate GPS to initialize Maps!");
                finish();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Connect the client.
        mGoogleApiClient.connect();

    }

    @Override
    protected void onStop() {
        // Disconnecting the client invalidates it.
        if (mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();

        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {

        // for only one location request. first location we get. then onLocationChanged takes care of updates
        //while (mLocation == null) // force a value to emerge
        mLocation = null;

        //to continuously check position
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(3000);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {
        msg("GoogleApiClient connection has been suspend");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        msg("GoogleApiClient connection has failed");
    }

    @Override
    public void onLocationChanged(android.location.Location location) {

        mLocation = location;

        if(map != null && !mapCentered) {
            map.setCenter(new GeoCoordinate(location.getLatitude(), location.getLongitude(), 0.0),
                    Map.Animation.NONE);
            mapCentered = true;
            loadingGpsLayout.setVisibility(View.GONE);
            locationMarkerImage= new Image();
            int drawableID = MapActivity.this.getResources().getIdentifier("positionmarker_s", "drawable", getPackageName());
            try{
                locationMarkerImage.setImageResource(drawableID);

            } catch (IOException e) {msg("Error on drawable ID");}

            locationMarker = new MapMarker();
            locationMarker.setIcon(locationMarkerImage);
            locationMarker.setVisible(true);
            map.addMapObject(locationMarker);

        }

        if(locationMarker != null)  locationMarker.setCoordinate(new GeoCoordinate(mLocation.getLatitude(), mLocation.getLongitude(), mLocation.getAltitude()));

    }

    private void initializeMap() {
        setContentView(R.layout.map_activity);

        // Search for the map fragment to finish setup by calling init().
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(
                R.id.mapfragment);
        mapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(
                    OnEngineInitListener.Error error) {
                if (error == OnEngineInitListener.Error.NONE) {
                    // retrieve a reference of the map from the map fragment
                    map = mapFragment.getMap();
                    // Set the map center to the Vancouver region (no animation)
                    map.setCenter(new GeoCoordinate(49.196261, -123.004773, 0.0),
                            Map.Animation.NONE);
                    // Set the zoom level to the average between min and max
                    map.setZoomLevel(
                            (map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2);

                    List<String> schemes = map.getMapSchemes();
                    map.setMapScheme(schemes.get(1)); // night

                } else {
                    Toast.makeText(getApplicationContext(),
                            "MapFragment Error: " + error.toString() + ". Try again.", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
    }

    public void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
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

    public void backToMain(View view) {
        Intent returnToMain = new Intent(MapActivity.this, MainActivity.class);
        startActivity(returnToMain);
        finish();
    }

    public void resetAddress(View view) {
        destination_final.setText("");
        //backButton.setVisibility(View.VISIBLE);
        if(tempRoute[0] != null || tempRoute[1] != null) startNavigation.setVisibility(View.VISIBLE);
    }


    public void beginNavigationActivity(View view){
        if(isShortRouteSelected) mapRoute = tempRoute[1];
        else mapRoute = tempRoute[0];

        Intent navigationActivity = new Intent(MapActivity.this, NavigationActivity.class);

        startActivity(navigationActivity);
    }

    // Implementation of ResultListener in GeoCode data search
    class GeocodeListener implements ResultListener<List<Location>> {

        @Override
        public void onCompleted(List<Location> data, ErrorCode error) {
            if (error != ErrorCode.NONE) {
                Toast.makeText(getApplicationContext(),
                        "GeocodeListener Error: " + error.toString() + ". Try again.", Toast.LENGTH_SHORT)
                        .show();

            } else {
                if(data.size() > 0) {

                   // Log.v("DATA = ", suggestionArrayList.toString());
                    Message msg = new Message();
                    msg.obj = data;
                    suggestionHandler.sendMessage(msg);

                }

                else System.out.println("Invalid address");

            }

        }
    }

    private class RouteListener implements CoreRouter.Listener {
        // Method defined in Listener
        public void onProgress(int percentage) {

            routeLoadingBar.setProgress(percentage);

        }
        // Method defined in Listener
        public void onCalculateRouteFinished(List<RouteResult> routeResult, RoutingError error)
        {
            // If the route was calculated successfully
            if (error == RoutingError.NONE) {
                // Render the route on the map
                mapRoute = new MapRoute(routeResult.get(0).getRoute());
                if(tempRoute[0] == null) tempRoute[0] = mapRoute;
                else if(tempRoute[1] == null) tempRoute[1] = mapRoute;
                map.addMapObject(mapRoute);

                // Get the bounding box containing the route and zoom in (no animation)
                GeoBoundingBox gbb = routeResult.get(0).getRoute().getBoundingBox();
                map.zoomTo(gbb, Map.Animation.NONE, Map.MOVE_PRESERVE_ORIENTATION);

                end = new MapMarker();
                end.setCoordinate(mapRoute.getRoute().getDestination());
                end.setVisible(true);
                map.addMapObject(end);

                routeLoadingLayout.setVisibility(View.INVISIBLE);
                startNavigation.setVisibility(View.VISIBLE);
                routeChoiceLayout.setVisibility(View.VISIBLE);
                etaLayoutMap.setVisibility(View.VISIBLE);

                String timeToArrival = Integer.toString(mapRoute.getRoute().getTta(Route.TrafficPenaltyMode.DISABLED, Route.WHOLE_ROUTE).getDuration() / 60);
                etaTime.setText(timeToArrival);
                String distanceToArrival = Integer.toString(mapRoute.getRoute().getLength() / 1000);
                tripLength.setText(distanceToArrival);

                if(tempEta[0] == null) tempEta[0] = timeToArrival;
                else if(tempEta[1] == null) tempEta[1] = timeToArrival;

                if(tempDist[0] == null) tempDist[0] = distanceToArrival;
                else if(tempDist[1] == null) tempDist[1] = distanceToArrival;

                //if(backButton.getVisibility() == View.INVISIBLE) backButton.setVisibility(View.VISIBLE);

            }
            else {
            // Display a message indicating route calculation failure
                Toast.makeText(getApplicationContext(),
                        "Route Calculation Failed. Error: " + error.toString() + " Try again.", Toast.LENGTH_SHORT)
                        .show();
                routeLoadingLayout.setVisibility(View.INVISIBLE);

            }
        }
    }

    public void shortestRouteTick(View view) {
        isShortRouteSelected = true;
        if(tempRoute[1] != null) {
            if(tempRoute[0] != null) map.removeMapObject(tempRoute[0]);
            map.addMapObject(tempRoute[1]);
            etaTime.setText(tempEta[1]);
            tripLength.setText(tempDist[1]);

        }
        else recalculateRoute(0);

        fastestRouteCheckBox.setEnabled(true);
        fastestRouteCheckBox.setChecked(false);
        shortestRouteCheckBox.setEnabled(false);
        shortestRouteCheckBox.setChecked(true);
        fastestRouteCheckBox.setTypeface(Typeface.DEFAULT);
        shortestRouteCheckBox.setTypeface(Typeface.DEFAULT_BOLD);
    }

    public void fastestRouteTick(View view) {
        isShortRouteSelected = false;
        if(tempRoute[0] != null) {
            if(tempRoute[1] != null) map.removeMapObject(tempRoute[1]);
            map.addMapObject(tempRoute[0]);
            etaTime.setText(tempEta[0]);
            tripLength.setText(tempDist[0]);

        }
        else recalculateRoute(1);

        fastestRouteCheckBox.setEnabled(false);
        fastestRouteCheckBox.setChecked(true);
        shortestRouteCheckBox.setEnabled(true);
        shortestRouteCheckBox.setChecked(false);
        shortestRouteCheckBox.setTypeface(Typeface.DEFAULT);
        fastestRouteCheckBox.setTypeface(Typeface.DEFAULT_BOLD);
    }

    private void recalculateRoute(int routeType){
        CoreRouter routeManager = new CoreRouter();

        RoutePlan routePlan = new RoutePlan();

        routePlan.addWaypoint(waypointStart);
        routePlan.addWaypoint(waypointFinish);

        RouteOptions routeOptions = new RouteOptions();
        routeOptions.setTransportMode(RouteOptions.TransportMode.CAR);
        if(routeType == 1) routeOptions.setRouteType(RouteOptions.Type.FASTEST);
        else routeOptions.setRouteType(RouteOptions.Type.SHORTEST);
        routeOptions.setRouteCount(1);
        routePlan.setRouteOptions(routeOptions);

        suggestionList.setVisibility(View.INVISIBLE);
        hideSoftKeyboard(MapActivity.this);
        routeLoadingLayout.setVisibility(View.VISIBLE);
        if(mapRoute != null) map.removeMapObject(mapRoute);
        if(end != null) map.removeMapObject(end);

        routeManager.calculateRoute(routePlan, new RouteListener());

    }

    public void getDirectionsButton(View view){
        isShortRouteSelected = false;
        PointF clicked = new PointF(mapTouchCoordinates[0],mapTouchCoordinates[1]);

        CoreRouter routeManager = new CoreRouter();

        RoutePlan routePlan = new RoutePlan();
        waypointStart = new RouteWaypoint(new GeoCoordinate(mLocation.getLatitude(), mLocation.getLongitude(), mLocation.getAltitude()));
        routePlan.addWaypoint(waypointStart);

        waypointFinish = new RouteWaypoint(map.pixelToGeo(clicked));
        routePlan.addWaypoint(waypointFinish);

        RouteOptions routeOptions = new RouteOptions();
        routeOptions.setTransportMode(RouteOptions.TransportMode.CAR);
        routeOptions.setRouteType(RouteOptions.Type.FASTEST);
        routeOptions.setRouteCount(1);
        routePlan.setRouteOptions(routeOptions);

        suggestionList.setVisibility(View.INVISIBLE);
        getDirections.setVisibility(View.INVISIBLE);
        hideSoftKeyboard(MapActivity.this);
        routeLoadingLayout.setVisibility(View.VISIBLE);
        if(mapRoute != null) map.removeMapObject(mapRoute);
        if(tempRoute[0] != null) map.removeMapObject(tempRoute[0]);
        if(tempRoute[1] != null) map.removeMapObject(tempRoute[1]);
        if(end != null) map.removeMapObject(end);
        tempRoute[0] = null; tempRoute[1] = null;
        tempDist[0] = null; tempDist[1] = null;
        tempEta[0] = null; tempEta[1] = null;
        destination_final.setText("");

        routeManager.calculateRoute(routePlan, new RouteListener());

        fastestRouteCheckBox.setEnabled(false);
        fastestRouteCheckBox.setChecked(true);
        shortestRouteCheckBox.setEnabled(true);
        shortestRouteCheckBox.setChecked(false);
        shortestRouteCheckBox.setTypeface(Typeface.DEFAULT);
        fastestRouteCheckBox.setTypeface(Typeface.DEFAULT_BOLD);
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }

}
