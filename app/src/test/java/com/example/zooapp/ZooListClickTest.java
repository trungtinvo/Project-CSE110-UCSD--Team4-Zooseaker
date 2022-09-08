package com.example.zooapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.view.View;

import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.zooapp.Data.PlannedAnimalDatabase;
import com.example.zooapp.Data.ZooNode;
import com.example.zooapp.Data.ZooNodeDatabase;
import com.example.zooapp.Interface.PlannedAnimalDao;
import com.example.zooapp.Interface.ZooNodeDao;
import com.example.zooapp.Viewer.SearchActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ZooListClickTest {
    ZooNodeDatabase zooDb;
    ZooNodeDao zooDao;
    PlannedAnimalDao planDao;
    PlannedAnimalDatabase testPlanDb;

    private static void forceLayout(RecyclerView recyclerView) {
        recyclerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        recyclerView.layout(0,0,1080,2280);
    }

    @Before
    public void resetDatabase() {
        Context context = ApplicationProvider.getApplicationContext();
        zooDb = Room.inMemoryDatabaseBuilder(context, ZooNodeDatabase.class)
                .allowMainThreadQueries()
                .build();
        ZooNodeDatabase.injectTestDatabase(zooDb);

        testPlanDb = Room.inMemoryDatabaseBuilder(context, PlannedAnimalDatabase.class)
                .allowMainThreadQueries()
                .build();
        PlannedAnimalDatabase.injectTestDatabase(testPlanDb);

        List<ZooNode> todos = ZooNode.loadJSON(context, "sample_node_info.json");
        zooDao = zooDb.ZooNodeDao();
        zooDao.insertAll(todos);

        planDao = testPlanDb.plannedAnimalDao();
    }

    @Test
    public void testClickBeforeSearchFirst() {
        ActivityScenario<SearchActivity> scenario
                = ActivityScenario.launch(SearchActivity.class);
        scenario.moveToState(Lifecycle.State.CREATED);
        scenario.moveToState(Lifecycle.State.STARTED);
        scenario.moveToState(Lifecycle.State.RESUMED);

        scenario.onActivity(activity -> {
            RecyclerView recyclerView = activity.recyclerView;
            RecyclerView.ViewHolder firstVH = recyclerView.findViewHolderForAdapterPosition(0);
            assertNotNull(firstVH);
            firstVH.itemView.performClick();
            assertEquals(1, activity.exhibitsSetup.getUserExhibits().size());
            assertEquals("Bali Mynah", activity.exhibitsSetup.getUserExhibits().get(0).name);
            assertEquals("exhibit", activity.exhibitsSetup.getUserExhibits().get(0).kind);
        });
    }

    @Test
    public void testClickBeforeSearchLast() {
        ActivityScenario<SearchActivity> scenario
                = ActivityScenario.launch(SearchActivity.class);
        scenario.moveToState(Lifecycle.State.CREATED);
        scenario.moveToState(Lifecycle.State.STARTED);
        scenario.moveToState(Lifecycle.State.RESUMED);

        scenario.onActivity(activity -> {
            RecyclerView recyclerView = activity.recyclerView;
            RecyclerView.ViewHolder firstVH = recyclerView.findViewHolderForAdapterPosition(4);
            assertNotNull(firstVH);
            firstVH.itemView.performClick();
            assertEquals(1, activity.exhibitsSetup.getUserExhibits().size());
            assertEquals("Emerald Dove", activity.exhibitsSetup.getUserExhibits().get(0).name);
            assertEquals("exhibit", activity.exhibitsSetup.getUserExhibits().get(0).kind);
        });
    }
}
