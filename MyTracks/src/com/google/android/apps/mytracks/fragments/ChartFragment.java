/*
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.apps.mytracks.fragments;

import com.google.android.apps.mytracks.ChartView;
import com.google.android.apps.mytracks.ChartView.Mode;
import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.MyTracksApplication;
import com.google.android.apps.mytracks.content.MyTracksLocation;
import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.apps.mytracks.content.Sensor.SensorDataSet;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TrackDataHub;
import com.google.android.apps.mytracks.content.TrackDataHub.ListenerDataType;
import com.google.android.apps.mytracks.content.TrackDataListener;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.stats.DoubleBuffer;
import com.google.android.apps.mytracks.stats.TripStatisticsBuilder;
import com.google.android.apps.mytracks.util.LocationUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.mytracks.R;
import com.google.common.annotations.VisibleForTesting;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ZoomControls;

import java.util.ArrayList;
import java.util.EnumSet;

/**
 * A fragment to display track chart to the user.
 * 
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class ChartFragment extends Fragment implements TrackDataListener {

  // Android reports 128 when the speed is invalid
  private static final int INVALID_SPEED = 128;
  private final DoubleBuffer elevationBuffer = new DoubleBuffer(
      Constants.ELEVATION_SMOOTHING_FACTOR);
  private final DoubleBuffer speedBuffer = new DoubleBuffer(Constants.SPEED_SMOOTHING_FACTOR);
  private final ArrayList<double[]> pendingPoints = new ArrayList<double[]>();
  
  private TrackDataHub trackDataHub;

  // Stats gathered from the received data
  private double totalDistance = 0.0;
  private long startTime = -1L;
  private Location lastLocation = null;
  private double trackMaxSpeed = 0.0;

  // Modes of operation
  private boolean metricUnits = true;
  private boolean reportSpeed = true;

  // UI elements
  private ChartView chartView;
  private LinearLayout busyPane;
  private ZoomControls zoomControls;

  /**
   * A runnable that will remove the spinner (if any), enable/disable zoom
   * controls and orange pointer as appropriate and redraw.
   */
  private final Runnable updateChart = new Runnable() {
    @Override
    public void run() {
      if (trackDataHub == null) {
        return;
      }

      busyPane.setVisibility(View.GONE);
      zoomControls.setIsZoomInEnabled(chartView.canZoomIn());
      zoomControls.setIsZoomOutEnabled(chartView.canZoomOut());
      chartView.setShowPointer(isRecording());
      chartView.invalidate();
    }
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    /*
     * Create a chartView here to store data thus won't need to reload all the
     * data on every onStart or onResume.
     */
    chartView = new ChartView(getActivity());
  };

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.mytracks_charts, container, false);
    busyPane = (LinearLayout) view.findViewById(R.id.elevation_busypane);
    zoomControls = (ZoomControls) view.findViewById(R.id.elevation_zoom);
    zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        zoomIn();
      }
    });
    zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        zoomOut();
      }
    });
    return view;
  }

  @Override
  public void onStart() {
    super.onStart();
    ViewGroup layout = (ViewGroup) getActivity().findViewById(R.id.elevation_chart);
    LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
        LayoutParams.MATCH_PARENT);
    layout.addView(chartView, layoutParams);
  }
  
  @Override
  public void onResume() {
    super.onResume();
    resumeTrackDataHub();
    getActivity().runOnUiThread(updateChart);
  }

  @Override
  public void onPause() {
    super.onPause();
    pauseTrackDataHub();
  }

  @Override
  public void onStop() {
    super.onStop();
    ViewGroup layout = (ViewGroup) getActivity().findViewById(R.id.elevation_chart);
    layout.removeView(chartView);
  }
 
  /**
   * Sets the chart view mode.
   * 
   * @param mode the chart view mode
   */
  public void setMode(Mode mode) {
    if (chartView.getMode() != mode) {
      chartView.setMode(mode);
      reloadTrackDataHub();
    }
  }

  /**
   * Gets the chart view mode.
   */
  public Mode getMode() {
    return chartView.getMode();
  }

  /**
   * Enables or disables the chart value series.
   * 
   * @param index the index of the series
   * @param enabled true to enable, false to disable
   */
  public void setChartValueSeriesEnabled(int index, boolean enabled) {
    chartView.setChartValueSeriesEnabled(index, enabled);
  }

  /**
   * Returns true if the chart value series is enabled.
   * 
   * @param index the index of the series
   */
  public boolean isChartValueSeriesEnabled(int index) {
    return chartView.isChartValueSeriesEnabled(index);
  }

  /**
   * Returns true to report speed instead of pace.
   */
  public boolean isReportSpeed() {
    return reportSpeed;
  }

  /**
   * Updates the chart.
   */
  public void update() {
    chartView.postInvalidate();
  }

  @Override
  public void onProviderStateChange(ProviderState state) {
    // We don't care.
  }

  @Override
  public void onCurrentLocationChanged(Location loc) {
    // We don't care.
  }

  @Override
  public void onCurrentHeadingChanged(double heading) {
    // We don't care.
  }

  @Override
  public void onSelectedTrackChanged(Track track, boolean isRecording) {
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        busyPane.setVisibility(View.VISIBLE);
      }
    });
  }

  @Override
  public void onTrackUpdated(Track track) {
    if (track == null || track.getStatistics() == null) {
      trackMaxSpeed = 0.0;
      return;
    }
    trackMaxSpeed = track.getStatistics().getMaxSpeed();
  }

  @Override
  public void clearTrackPoints() {
    totalDistance = 0.0;
    startTime = -1L;
    lastLocation = null;
    
    elevationBuffer.reset();
    speedBuffer.reset();
    pendingPoints.clear();
    
    chartView.reset();

    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        chartView.resetScroll();
      }
    });
  }

  @Override
  public void onNewTrackPoint(Location location) {
    if (LocationUtils.isValidLocation(location)) {
      double[] data = new double[6];
      fillDataPoint(location, data);
      pendingPoints.add(data);
    }
  }

  @Override
  public void onSampledOutTrackPoint(Location location) {
    if (LocationUtils.isValidLocation(location)) {
      // Still account for the point in the smoothing buffers.
      fillDataPoint(location, null);
    }
  }

  @Override
  public void onSegmentSplit() {
    // Do nothing.
  }

  @Override
  public void onNewTrackPointsDone() {
    chartView.addDataPoints(pendingPoints);
    pendingPoints.clear();
    getActivity().runOnUiThread(updateChart);
  }

  @Override
  public void clearWaypoints() {
    chartView.clearWaypoints();
  }

  @Override
  public void onNewWaypoint(Waypoint waypoint) {
    if (waypoint != null && LocationUtils.isValidLocation(waypoint.getLocation())) {
      chartView.addWaypoint(waypoint);
    }
  }

  @Override
  public void onNewWaypointsDone() {
    getActivity().runOnUiThread(updateChart);
  }

  @Override
  public boolean onUnitsChanged(boolean metric) {
    if (metricUnits == metric) {
      return false;
    }
    metricUnits = metric;
    chartView.setMetricUnits(metricUnits);
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        chartView.requestLayout();
      }
    });
    return true;
  }

  @Override
  public boolean onReportSpeedChanged(boolean speed) {
    if (reportSpeed == speed) {
      return false;
    }
    reportSpeed = speed;
    chartView.setReportSpeed(speed, getActivity());
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        chartView.requestLayout();
      }
    });
    return true;
  }

  /**
   * Resumes the trackDataHub. Needs to be synchronized because trackDataHub can be
   * accessed by multiple threads.
   */
  private synchronized void resumeTrackDataHub() {
    trackDataHub = ((MyTracksApplication) getActivity().getApplication()).getTrackDataHub();
    trackDataHub.registerTrackDataListener(this, EnumSet.of(
        ListenerDataType.SELECTED_TRACK_CHANGED,
        ListenerDataType.TRACK_UPDATES,
        ListenerDataType.WAYPOINT_UPDATES,
        ListenerDataType.POINT_UPDATES,
        ListenerDataType.SAMPLED_OUT_POINT_UPDATES,
        ListenerDataType.DISPLAY_PREFERENCES));
  }
  
  /**
   * Pauses the trackDataHub. Needs to be synchronized because trackDataHub can be
   * accessed by multiple threads. 
   */
  private synchronized void pauseTrackDataHub() {
    trackDataHub.unregisterTrackDataListener(this);
    trackDataHub = null;
  }
  
  /**
   * Returns true if recording. Needs to be synchronized because trackDataHub
   * can be accessed by multiple threads.
   */
  private synchronized boolean isRecording() {
    return trackDataHub != null && trackDataHub.isRecordingSelected();
  }
  
  /**
   * Reloads the trackDataHub. Needs to be synchronized because trackDataHub can be
   * accessed by multiple threads. 
   */
  private synchronized void reloadTrackDataHub() {
    if (trackDataHub != null) {
      trackDataHub.reloadDataForListener(this);
    }
  }
  
  /**
   * To zoom in.
   */
  private void zoomIn() {
    chartView.zoomIn();
    zoomControls.setIsZoomInEnabled(chartView.canZoomIn());
    zoomControls.setIsZoomOutEnabled(chartView.canZoomOut());
  }

  /**
   * To zoom out.
   */
  private void zoomOut() {
    chartView.zoomOut();
    zoomControls.setIsZoomInEnabled(chartView.canZoomIn());
    zoomControls.setIsZoomOutEnabled(chartView.canZoomOut());
  }
  
  /**
   * Given a location, fill in a data point, an array of double[6]. <br>
   * data[0] = time/distance <br>
   * data[1] = elevation <br>
   * data[2] = speed <br>
   * data[3] = power <br>
   * data[4] = cadence <br>
   * data[5] = heart rate <br>
   * 
   * @param location the location
   * @param data the data point to fill in, can be null
   */
  @VisibleForTesting
  void fillDataPoint(Location location, double data[]) {
    double timeOrDistance = Double.NaN;
    double elevation = Double.NaN;
    double speed = Double.NaN;
    double power = Double.NaN;
    double cadence = Double.NaN;
    double heartRate = Double.NaN;  
   
    // TODO: Use TripStatisticsBuilder
    if (chartView.getMode() == Mode.BY_DISTANCE) {
      if (lastLocation != null) {
        double distance = lastLocation.distanceTo(location) * UnitConversions.M_TO_KM;
        if (metricUnits) {
          totalDistance += distance;
        } else {
          totalDistance += distance * UnitConversions.KM_TO_MI;
        }
      }
      timeOrDistance = totalDistance;
    } else {
      if (startTime == -1L) {
        startTime = location.getTime();
      }
      timeOrDistance = location.getTime() - startTime;
    }
  
    elevationBuffer.setNext(metricUnits ? location.getAltitude() : location.getAltitude()
        * UnitConversions.M_TO_FT);
    elevation = elevationBuffer.getAverage();
  
    if (lastLocation == null) {
      if (Math.abs(location.getSpeed() - INVALID_SPEED) > 1) {
        speedBuffer.setNext(location.getSpeed());
      }
    } else if (TripStatisticsBuilder.isValidSpeed(location.getTime(), location.getSpeed(),
        lastLocation.getTime(), lastLocation.getSpeed(), speedBuffer)
        && (location.getSpeed() <= trackMaxSpeed)) {
      speedBuffer.setNext(location.getSpeed());
    }
    speed = speedBuffer.getAverage() * UnitConversions.MS_TO_KMH;
    if (!metricUnits) {
      speed *= UnitConversions.KM_TO_MI;
    }
    if (!reportSpeed) {
      speed = speed == 0 ? 0.0 : 60.0 / speed;
    }
  
    if (location instanceof MyTracksLocation
        && ((MyTracksLocation) location).getSensorDataSet() != null) {
      SensorDataSet sensorDataSet = ((MyTracksLocation) location).getSensorDataSet();
      if (sensorDataSet.hasPower() && sensorDataSet.getPower().getState() == Sensor.SensorState.SENDING
          && sensorDataSet.getPower().hasValue()) {
        power = sensorDataSet.getPower().getValue();
      }
      if (sensorDataSet.hasCadence()
          && sensorDataSet.getCadence().getState() == Sensor.SensorState.SENDING
          && sensorDataSet.getCadence().hasValue()) {
        cadence = sensorDataSet.getCadence().getValue();
      }
      if (sensorDataSet.hasHeartRate()
          && sensorDataSet.getHeartRate().getState() == Sensor.SensorState.SENDING
          && sensorDataSet.getHeartRate().hasValue()) {
        heartRate = sensorDataSet.getHeartRate().getValue();
      }
    }
    
    if (data != null) {
      data[0] = timeOrDistance;
      data[1] = elevation;
      data[2] = speed;
      data[3] = power;
      data[4] = cadence;
      data[5] = heartRate;
    }
    lastLocation = location;
  }

  @VisibleForTesting
  ChartView getChartView() {
    return chartView;
  }

  @VisibleForTesting
  void setChartView(ChartView view) {
    chartView = view;
  }
  
  @VisibleForTesting
  void setTrackMaxSpeed(double value) {
    trackMaxSpeed = value;
  }
  
  @VisibleForTesting
  void setMetricUnits(boolean value) {
    metricUnits = value;
  }
  
  @VisibleForTesting
  void setReportSpeed(boolean value) {
    reportSpeed = value;
  }
}
