package com.example.zooapp;
import android.content.Context;
import android.location.Location;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.lifecycle.Lifecycle;
import androidx.room.Room;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import com.example.zooapp.Data.PlannedAnimalDatabase;
import com.example.zooapp.Data.ZooNode;
import com.example.zooapp.Data.ZooNodeDatabase;
import com.example.zooapp.Interface.PlannedAnimalDao;
import com.example.zooapp.Interface.ZooNodeDao;
import com.example.zooapp.Viewer.DirectionsActivity;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class PreviousButtonTest {
    Context context;
    ZooNodeDao dao;
    ZooNodeDatabase testDb;
    PlannedAnimalDao planDao;
    PlannedAnimalDatabase testPlanDb;
    List<ZooNode> allExhibits;

    //Set up the database of animals in the zoo
    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        testDb = Room.inMemoryDatabaseBuilder(context, ZooNodeDatabase.class)
                .allowMainThreadQueries()
                .build();
        ZooNodeDatabase.injectTestDatabase(testDb);

        testPlanDb = Room.inMemoryDatabaseBuilder(context, PlannedAnimalDatabase.class)
                .allowMainThreadQueries()
                .build();
        PlannedAnimalDatabase.injectTestDatabase(testPlanDb);

        List<ZooNode> allZooNodes = ZooNode.loadJSON(context, "sample_node_info.json");
        dao = testDb.ZooNodeDao();
        dao.insertAll(allZooNodes);
        planDao = testPlanDb.plannedAnimalDao();
        allExhibits = dao.getZooNodeKind("exhibit");
    }

    @Test
    public void testPreviousInvisibility(){
        planDao.insert(allExhibits.get(0));

        assertEquals(1, planDao.getAll().size());

        //Start in Directions Activity
        ActivityScenario<DirectionsActivity> scenario = ActivityScenario.launch(DirectionsActivity.class);
        scenario.moveToState(Lifecycle.State.CREATED);
        scenario.moveToState(Lifecycle.State.STARTED);
        scenario.moveToState(Lifecycle.State.RESUMED);

        scenario.onActivity(activity -> {

            assertEquals(1, planDao.getAll().size());

            activity.setContentView(R.layout.activity_directions);

            //Next should be visible, Previous should be invisible
            Button previous = activity.findViewById(R.id.previous_button);
            assertEquals(View.INVISIBLE, previous.getVisibility());
        });

    }

    @Test
    public void testPreviousCounter(){

        planDao.insert(allExhibits.get(0));
        planDao.insert(allExhibits.get(1));

        //Start DirectionsActivity
        ActivityScenario<DirectionsActivity> scenario2 = ActivityScenario.launch(DirectionsActivity.class);
        scenario2.moveToState(Lifecycle.State.CREATED);
        scenario2.moveToState(Lifecycle.State.STARTED);
        scenario2.moveToState(Lifecycle.State.RESUMED);

        scenario2.onActivity(activity2 -> {
            Button next = activity2.findViewById(R.id.next_button);
            Button previous = activity2.findViewById(R.id.previous_button);
            TextView name = activity2.findViewById(R.id.directions_header);

            //Two animals in planned list, but path should be length four including entrance and exit
            assertEquals(2, planDao.getAll().size());
            assertEquals(4, activity2.userListShortestOrder.size());

            activity2.setLocationToUse(new Location("Mock Location"));
            activity2.getLocationToUse().setLatitude(32.73459618734685);
            activity2.getLocationToUse().setLongitude(-117.14936);

            //currIndex starts at first animal, index at 0
            //Previous should be invisible, Next should be visible
            assertEquals(0, activity2.currIndex);

            //make sure first animal is correct
            assertEquals(name.getText().toString(), "Flamingos");

            //click Next to move to the second animal, currIndex at 1
            next.performClick();
            assertEquals(1, activity2.currIndex);
            assertEquals(View.VISIBLE, previous.getVisibility());

            //make sure second animal name is correct
            assertEquals(name.getText().toString(), "Koi Fish");

            //click Next to move to the third animal, currIndex at 2
            next.performClick();
            assertEquals(2, activity2.currIndex);
            assertEquals(View.VISIBLE, previous.getVisibility());
            //make sure end name is correct
            assertEquals( "Entrance and Exit Gate", name.getText().toString());

            //click Previous to return to third animal, Previous button is invisible
            previous.performClick();
            assertEquals(View.VISIBLE, previous.getVisibility());
            assertEquals(1, activity2.currIndex);
            //make sure second animal name is correct
            assertEquals(name.getText().toString(), "Koi Fish");

            //click Previous to return to first animal, Previous button is invisible
            previous.performClick();
            assertEquals(View.INVISIBLE, previous.getVisibility());
            assertEquals(0, activity2.currIndex);
            //make sure first animal is correct
            assertEquals(name.getText().toString(), "Flamingos");

        });
        scenario2.close();
    }
}
