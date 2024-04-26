package de.dennisguse.opentracks.data.models;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Chairlift {
    // Fields
    private String name;
    private int number;
    private double chairliftSpeed;
    private String liftType;
    private int id;

    // Static fields
    private static int nextId = 1;
    private static final Map<Integer, Chairlift> validChairlifts = new HashMap<>();

    // Constructors
    public Chairlift(String name, int number, double chairliftSpeed, String liftType) {
        this.name = name;
        this.number = number;
        this.chairliftSpeed = chairliftSpeed;
        this.liftType = liftType;
        this.id = nextId++;
    }

    public Chairlift() {

    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public double getchairliftSpeed() {
        return chairliftSpeed;
    }

    public void setchairliftSpeed(double chairliftSpeed) {
        this.chairliftSpeed = chairliftSpeed;
    }

    public String getLiftType() {
        return liftType;
    }

    public void setLiftType(String liftType) {
        this.liftType = liftType;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // Methods
    public boolean isUserRidingChairlift(List<TrackPoint> trackPoints) {
        if (trackPoints.size() < 2) {
            return false; // Not enough data
        }

        // Thresholds
        double altitudeChangeThreshold = 2.0;
        double speedThreshold = 2;
        double timeThreshold = 1;

        // Check altitude change
        double altitudeChange = Math.abs(trackPoints.get(0).getAltitude().toM() -
                trackPoints.get(trackPoints.size() - 1).getAltitude().toM());
        if (altitudeChange > altitudeChangeThreshold) {
            return false; // Likely not on chairlift
        }

        // Calculate total time
        double totalTime = calculateTotalTime(trackPoints);

        // Calculate average speed
        double chairliftSpeed = calculateChairliftSpeed(trackPoints);

        // Check average speed and total time
        if ((chairliftSpeed < speedThreshold || chairliftSpeed > 6) ||
                (totalTime > timeThreshold && totalTime < 7.7)) {
            return false;
        }

        // Add chairlift to valid chairlifts and mark track points as chairlift segment
        Chairlift validChairlift = new Chairlift(name, number, chairliftSpeed, liftType);
        validChairlifts.put(validChairlift.getId(), validChairlift);
//        trackPoints.forEach(trackPoint -> trackPoint.setChairliftSegment(true));

        return true;
    }

    private double calculateTotalDistance(List<TrackPoint> trackPoints) {
        double totalDistance = 0.0;
        for (int i = 1; i < trackPoints.size(); i++) {
            TrackPoint pPoint = trackPoints.get(i - 1);
            TrackPoint cPoint = trackPoints.get(i);
            Distance distance = pPoint.distanceToPrevious(cPoint);
            totalDistance += distance.toKM();
        }
        return totalDistance;
    }

    private double calculateTotalTime(List<TrackPoint> trackPoints) {
        Duration totalTime = Duration.ZERO;
        for (int i = 1; i < trackPoints.size(); i++) {
            TrackPoint pPoint = trackPoints.get(i - 1);
            TrackPoint cPoint = trackPoints.get(i);
            Duration rideDuration = Duration.between(pPoint.getTime(), cPoint.getTime());
            totalTime = totalTime.plus(rideDuration);
        }
        return totalTime.toMinutes();
    }

    private double calculateChairliftSpeed(List<TrackPoint> trackPoints){
        // Calculate total distance and time
        double totalDistance = calculateTotalDistance(trackPoints);
        double totalTime = calculateTotalTime(trackPoints);

        // Calculate average speed
        return totalDistance / totalTime;
    }

    public Duration getTotalChairliftTime(List<TrackPoint> trackPoints) {
        if (trackPoints==null){
            return Duration.ZERO;
        }
        Duration totalDuration = Duration.ZERO;
        for (int i = 1; i < trackPoints.size(); i++) {
            TrackPoint previousPoint = trackPoints.get(i - 1);
            TrackPoint currentPoint = trackPoints.get(i);
            Duration rideDuration = Duration.between(previousPoint.getTime(), currentPoint.getTime());
            totalDuration = totalDuration.plus(rideDuration);
        }
        return totalDuration;
    }

    public static List<Chairlift> getValidChairlifts() {
        return new ArrayList<>(validChairlifts.values());
    }
}
