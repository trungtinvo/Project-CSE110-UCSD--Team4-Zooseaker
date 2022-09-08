package com.example.zooapp.Viewer;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBar;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.zooapp.Data.PlannedAnimalDatabase;
import com.example.zooapp.Data.ZooNode;
import com.example.zooapp.Data.ZooNodeDatabase;
import com.example.zooapp.Interface.GraphAlgorithm;
import com.example.zooapp.Interface.PlannedAnimalDao;
import com.example.zooapp.Interface.ZooNodeDao;
import com.example.zooapp.R;
import com.example.zooapp.Ultility.ExhibitLocations;
import com.example.zooapp.Ultility.LocationHandler;
import com.example.zooapp.Ultility.LocationListenerImplementation;
import com.example.zooapp.Ultility.SetDirections;
import com.example.zooapp.Ultility.ShortestPathZooAlgorithm;
import com.example.zooapp.Ultility.Utilities;

import java.util.List;

/**
 * This class is for when the user is now seeing the directions for each exhibit
 */
public class DirectionsActivity extends AppCompatActivity {
    private SetDirections setDirections;
    private final LocationHandler locationHandler = new LocationHandler(this);
    //index that is incremented/decremented by next/back buttons
    //used to traverse through planned exhibits
    public int currIndex = 0;
    public static boolean check = false;
    public static boolean replanAlertShown = false;
    public static boolean canCheckReplan = true;
    public static boolean recentlyYesReplan = false;
    public static boolean directionsDetailedText;

    public List<ZooNode> userListShortestOrder;

    // Variable for the graph and path
    public GraphAlgorithm algorithm;
    public ActionBar actionBar;
    public AlertDialog alertMessage, locationMessage, replanMessage;
    public PlannedAnimalDao plannedAnimalDao;
    public ZooNodeDao zooNodeDao;
    private ExhibitLocations exhibitLocations;
    private Button previous, skip;
    private boolean backwards = false;
    private ZooNode previousClosestZooNode;
    private SharedPreferences preferences;

    /**
     * Method for onCreate of the activity
     *
     * @param savedInstanceState State of activity
     */
    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directions);
        setDirections = new SetDirections(this,
                getApplication().getApplicationContext());
        plannedAnimalDao = PlannedAnimalDatabase.getSingleton(this)
                .plannedAnimalDao();
        zooNodeDao = ZooNodeDatabase.getSingleton(this)
                .ZooNodeDao();
        exhibitLocations = new ExhibitLocations(zooNodeDao);
        locationHandler.resetMockLocation();

        // Get boolean, default false
        preferences = getSharedPreferences("DIRECTIONS", MODE_PRIVATE);
        directionsDetailedText = preferences.getBoolean("toggled", false);
        Log.d("Boolean Direction", "" + directionsDetailedText);

        // Set the Title Bar to Directions
        actionBar = getSupportActionBar();
        actionBar.setTitle("Directions");

        //Access the previous and skip buttons
        previous = findViewById(R.id.previous_button);
        skip = findViewById(R.id.skip_button);

        // Grabbing planned animals from planned list and inputting to new activity
        if( plannedAnimalDao.getAll().size() > 0 ){
            Log.d("Zoo Nodes", plannedAnimalDao.getAll().toString());

            // Our old algorithm
            algorithm = new ShortestPathZooAlgorithm(
                    getApplication().getApplicationContext(), plannedAnimalDao.getAll());
            setDirections.setGraphPaths(algorithm.runAlgorithm());
            userListShortestOrder = algorithm.getUserListShortestOrder();
            setDirections.setDisplay(userListShortestOrder.get(currIndex + 1));
            exhibitLocations.setupExhibitLocations(userListShortestOrder
                    .subList(currIndex+1, userListShortestOrder.size()-1));

            // Set text views
            setDirections.setHeader(findViewById(R.id.directions_header));
            setDirections.setDirections(findViewById(R.id.directions_text));

            //setDirectionsText(graphPaths.get(currIndex));
            setDirections.setGraphPath(algorithm.runPathAlgorithm(zooNodeDao.getById("entrance_exit_gate"),
                    userListShortestOrder.subList(currIndex + 1, userListShortestOrder.size() - 1)));
            previousClosestZooNode = zooNodeDao.getById("entrance_exit_gate");

            setDirections.setDirectionsText(directionsDetailedText);

            // Testing Replan Button
//            Location mockEntrance = new Location("Mock Entrance");
//            mockEntrance.setLatitude(32.72109826903826);
//            mockEntrance.setLongitude(-117.15952052282296);
//            setMockLocation(mockEntrance);
//            Location mockBenchly = new Location("Mock Benchly");
//            mockBenchly.setLatitude(32.74476120197887);
//            mockBenchly.setLongitude(-117.18369973246877);
//            setMockLocation(mockBenchly);
        }
        else{
            Log.d("null input", "User exhibits was null");
            throw new NullPointerException("UserExhibits was null");
        }

        locationHandler.setUpLocationListener();
    }

    /**
     * Displays alert asking user to replan route
     *
     */
    public void promptReplan() {
        replanAlertShown = true;
        alertMessage = Utilities.optionalAlert(this,
                "Would You like to Replan your Route?");
        alertMessage.show();
        return;
    }

    /**
     * Creates the custom menu bar
     *
     * @param menu Menu
     * @return True for creating the menu bar
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.directions_menu, menu);
        return true;
    }

    /**
     * Checks when an item on the menu has been clicked
     *
     * @param item Item that has been clicked
     * @return Result of that item being clicked
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.directions_settings_button:
                Log.d("Menu Click", "Settings has been clicked");
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Method for when the next button is clicked
     *
     * @param view The current view
     */
    public void onNextButtonClicked(View view) {
        if(locationHandler.getLocationToUse() == null) {
            runOnUiThread(() -> {
                locationMessage = Utilities.showAlert(this,"Please wait until " +
                        "your location has started updating.");
                locationMessage.show();
                //alertMessage.isShowing();
            });
            return;
        }
        //check to see if index is at the end
        if(currIndex >= userListShortestOrder.size()-2){
            runOnUiThread(() -> {
                alertMessage = Utilities.showAlert(this,"The Route is Completed");
                alertMessage.show();
                //alertMessage.isShowing();
            });
            return;
        }
        //else if index is not at end, increment
        if (currIndex < userListShortestOrder.size() - 1){
            currIndex++;
        }
        backwards = false;
        //making previous button visible after 1st exhibit
        previous.setVisibility(View.VISIBLE);

        skipButtonVisibilityCheck();

        // set text
        setDirections.setGraphPath((currIndex >= userListShortestOrder.size() - 2) ? algorithm.runPathAlgorithm(
                exhibitLocations.getZooNodeClosestToCurrentLocation(locationHandler.getLocationToUse()),
                userListShortestOrder.subList(userListShortestOrder.size() - 1,
                        userListShortestOrder.size())) : algorithm.runPathAlgorithm(
                exhibitLocations.getZooNodeClosestToCurrentLocation(locationHandler.getLocationToUse()),
                userListShortestOrder.subList(currIndex + 1,
                        userListShortestOrder.size() - 1)));

        // Used for live testing
//        switch(currIndex) {
//            case 1:
//                mockLocation = new Location("Mock Flamingos");
//                mockLocation.setLatitude(32.7440416465169);
//                mockLocation.setLongitude(-117.15952052282296);
//                break;
//            case 2:
//                mockLocation = new Location("Mock Gorillas");
//                mockLocation.setLatitude(32.74711745394194);
//                mockLocation.setLongitude(-117.18047982358976);
//                break;
//            case 3:
//                mockLocation = new Location("Mock Orangutans");
//                mockLocation.setLatitude(32.735851415117665);
//                mockLocation.setLongitude(-117.16626781198586);
//                break;
//            default:
//                break;
//        }
        setDirections.setDirectionsText(directionsDetailedText);

        canCheckReplan = true;
    }

    /**
     * Method to handle when the previous button is clicked
     *
     * @param view The current view
     */
    public void onPreviousButtonClicked(View view) {
        // make previous button invisible if we go back to 1st exhibit
        Button previous = findViewById(R.id.previous_button);
        if (currIndex == 1){
            previous.setVisibility(View.INVISIBLE);
        }
        // else if curr index is not 0, decrement on click
        if (currIndex > 0){
            currIndex--;
        }

        skipButtonVisibilityCheck();

        // Used for live testing
//        switch(currIndex) {
//            case 0:
//                mockLocation = new Location("Mock Gorillas");
//                mockLocation.setLatitude(32.74711745394194);
//                mockLocation.setLongitude(-117.18047982358976);
//                break;
//            case 1:
//                mockLocation = new Location("Mock Orangutans");
//                mockLocation.setLatitude(32.735851415117665);
//                mockLocation.setLongitude(-117.16626781198586);
//                break;
//            case 2:
//                mockLocation = new Location("Mock Entrance");
//                mockLocation.setLatitude(32.73459618734685);
//                mockLocation.setLongitude(-117.14936);
//                break;
//            default:
//                break;
//        }
        backwards = true;
        //set Text
        setDirections.setGraphPath(algorithm.runReversePathAlgorithm(exhibitLocations
                        .getZooNodeClosestToCurrentLocation(locationHandler.getLocationToUse()),
                userListShortestOrder.get(currIndex + 1)));
        setDirections.setDirectionsText(directionsDetailedText);
        canCheckReplan = true;
    }

    /**
     * Clears animals from planned list and returns to home screen
     *
     * @param view the current view
     */
    public void onStartOverButtonClicked(View view) {
        Log.d("StartOverButton", "Start Over Button Clicked");

        PlannedAnimalDatabase.getSingleton(this).plannedAnimalDao().deleteAll();
        Log.d("StartOverButton", "Cleared Planned Animal Dao");
        Log.d("StartOverButton", "Heading back to main activity");

        // Go back to main activity
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // clear stack of activities
        startActivity(intent);
    }

    /**
     * Skips exhibit currently navigating to and moves onto next exhibit
     *
     * @param view
     */
    public void onSkipButtonClicked(View view) {
        if(locationHandler.getLocationToUse() == null) {
            runOnUiThread(() -> {
                locationMessage = Utilities.showAlert(this,"Please wait until " +
                        "your location has started updating.");
                locationMessage.show();
                //alertMessage.isShowing();
            });
            return;
        }

        setDirections.skipNewDirections();

        skipButtonVisibilityCheck();

        setDirections.setDirectionsText(directionsDetailedText);

        Log.d("SkipButton", "List planned animal AFTER: " + plannedAnimalDao.getAll().toString());
    }

    /**
     * Replans route based on the user's current location and the next closest exhibit when the user clicks
     * the replan button
     *
     * @param view
     */
    public void onReplanButtonClicked(View view) {
        if(locationHandler.getLocationToUse() == null) {
            runOnUiThread(() -> {
                locationMessage = Utilities.showAlert(this,"Please wait until " +
                        "your location has started updating.");
                locationMessage.show();
                //alertMessage.isShowing();
            });
            return;
        }

        Log.d("Replan", "Replan was clicked");

        var nearestZooNode =
                exhibitLocations.getZooNodeClosestToCurrentLocation(getLocationToUse());
        locationHandler.replanRoute(nearestZooNode);
    }

    private void skipButtonVisibilityCheck() {
        // Check if the currIndex is at "Entrance and Exit" node
        // If so, the skip button is not visible
        // Otherwise, setVisible
        if (currIndex == userListShortestOrder.size() - 2)
            skip.setVisibility(View.INVISIBLE);
        else
            skip.setVisibility(View.VISIBLE);
    }

    public Location getLocationToUse() {
        return locationHandler.getLocationToUse();
    }

    public void setLocationToUse(Location locationToUse) {
        locationHandler.setLocationToUse(locationToUse);
    }

    public ExhibitLocations getExhibitLocations() {
        return exhibitLocations;
    }

    public List<ZooNode> getUserListShortestOrder() {
        return userListShortestOrder;
    }

    public int getCurrIndex() {
        return currIndex;
    }

    public ZooNodeDao getZooNodeDao() {
        return zooNodeDao;
    }

    public SetDirections getSetDirections() {
        return setDirections;
    }

    public boolean isBackwards() {
        return backwards;
    }

    public GraphAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setUserListShortestOrder(List<ZooNode> userListShortestOrder) {
        this.userListShortestOrder = userListShortestOrder;
    }

    public Location getMockLocation() {
        return locationHandler.getMockLocation();
    }

    public void setMockLocation(Location mockLocation) {
        locationHandler.setMockLocation(mockLocation);
    }

    @VisibleForTesting
    public LocationListenerImplementation getLocationListenerImplementation() {
        return locationHandler.getLocationListenerImplementation();
    }


}