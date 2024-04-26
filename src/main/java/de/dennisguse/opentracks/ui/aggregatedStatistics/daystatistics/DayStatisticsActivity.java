package de.dennisguse.opentracks.ui.aggregatedStatistics.daystatistics;

import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.databinding.StatisticsPerDayBinding;

public class DayStatisticsActivity extends AbstractActivity {

    private StatisticsPerDayBinding viewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = StatisticsPerDayBinding.inflate(getLayoutInflater());

        // Assuming you have waitTimes data available here
        List<Integer> waitTimes = new ArrayList<>();
        waitTimes.add(300); // 5 minutes in seconds
        waitTimes.add(600); // 10 minutes in seconds
        waitTimes.add(120); // 2 minutes in seconds
        waitTimes.add(480); // 8 minutes in seconds
        setShortestWaitLabel(waitTimes);

        setContentView(viewBinding.getRoot());
    }

    @Override
    protected View getRootView() {
        viewBinding = StatisticsPerDayBinding.inflate(getLayoutInflater());
        return viewBinding.getRoot();
    }

    // the following code snippet is the corresponding code in the activity for back button
    @Override
    protected void onStart() {
        super.onStart();
        viewBinding.statsBackButton.setOnClickListener(v -> finish());
    }


    //code to calculate the shortest wait time and set the label in the layout
    private String calculateShortestWaitTime(List<Integer> waitTimes) {
        if (waitTimes == null || waitTimes.isEmpty()) {
            return "00:00:00"; // return default value if no wait times available
        }

        int shortestWaitTime = Collections.min(waitTimes); // find the shortest wait time

        // convert the shortest wait time to a string in the format "HH:MM:SS"
        int hours = shortestWaitTime / 3600;
        int minutes = (shortestWaitTime % 3600) / 60;
        int seconds = shortestWaitTime % 60;

        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    // binding the calculated shortest wait time to the layout
    public void setShortestWaitLabel(List<Integer> waitTimes) {
        String shortestWaitTime = calculateShortestWaitTime(waitTimes);
        viewBinding.statsShortestWaitValue.setText(shortestWaitTime);
    }

    //generate the commit message for this file



}
