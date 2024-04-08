package de.dennisguse.opentracks.data.models;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a chairlift.
 */
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

    /**
     * Constructs a new Chairlift object with specified attributes.
     *
     * @param name           The name of the chairlift.
     * @param number         The number of the chairlift.
     * @param chairliftSpeed The speed of the chairlift.
     * @param liftType       The type of the chairlift.
     */
    public Chairlift(String name, int number, double chairliftSpeed, String liftType) {
        this.name = name;
        this.number = number;
        this.chairliftSpeed = chairliftSpeed;
        this.liftType = liftType;
        this.id = nextId++;
    }

    // Getters and setters

    /**
     * Returns the name of the chairlift.
     *
     * @return The name of the chairlift.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the chairlift.
     *
     * @param name The name of the chairlift.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the number of the chairlift.
     *
     * @return The number of the chairlift.
     */
    public int getNumber() {
        return number;
    }

    /**
     * Sets the number of the chairlift.
     *
     * @param number The number of the chairlift.
     */
    public void setNumber(int number) {
        this.number = number;
    }

    /**
     * Returns the speed of the chairlift.
     *
     * @return The speed of the chairlift.
     */
    public double getChairliftSpeed() {
        return chairliftSpeed;
    }

    /**
     * Sets the speed of the chairlift.
     *
     * @param chairliftSpeed The speed of the chairlift.
     */
    public void setChairliftSpeed(double chairliftSpeed) {
        this.chairliftSpeed = chairliftSpeed;
    }

    /**
     * Returns the type of the chairlift.
     *
     * @return The type of the chairlift.
     */
    public String getLiftType() {
        return liftType;
    }

    /**
     * Sets the type of the chairlift.
     *
     * @param liftType The type of the chairlift.
     */
    public void setLiftType(String liftType) {
        this.liftType = liftType;
    }

    /**
     * Returns the ID of the chairlift.
     *
     * @return The ID of the chairlift.
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the ID of the chairlift.
     *
     * @param id The ID of the chairlift.
     */
    public void setId(int id) {
        this.id = id;
    }

    // Methods

    /**
     * Checks if the user is riding the chairlift based on the provided track points.
     *
     * @param trackPoints The list of track points.
     * @return True if the user is riding the chairlift, otherwise false.
     */
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
        double totalTime = calculateTotalTravelTime(trackPoints);

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

    /**
     * Calculates the total distance traveled based on the provided track points.
     *
     * @param trackPoints The list of track points.
     * @return The total distance traveled.
     */
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

    /**
     * Evaluates the total travel time based on the provided track points.
     *
     * @param trackPoints The list of track points.
     * @return The total travel time in minutes.
     */

    private double calculateTotalTravelTime(List<TrackPoint> trackPoints) {
        Duration totalDuration = Duration.ZERO;
        for (int i = 1; i < trackPoints.size(); i++) {
            TrackPoint previousPoint = trackPoints.get(i - 1);
            TrackPoint currentPoint = trackPoints.get(i);
            Duration rideDuration = Duration.between(previousPoint.getTime(), currentPoint.getTime());
            totalDuration = totalDuration.plus(rideDuration);
        }
        return totalDuration.toMinutes();
    }

    /**
     * Calculates the average speed based on the provided track points.
     *
     * @param trackPoints The list of track points.
     * @return The average speed.
     */
    private double calculateChairliftSpeed(List<TrackPoint> trackPoints) {
        // Calculate total distance and time
        double totalDistance = calculateTotalDistance(trackPoints);
        double totalTime = calculateTotalTravelTime(trackPoints);

        // Calculate average speed
        return totalDistance / totalTime;
    }

    /**
     * Retrieves the list of valid chairlifts.
     *
     * @return The list of valid chairlifts.
     */
    public static List<Chairlift> getValidChairlifts() {
        return new ArrayList<>(validChairlifts.values());
    }
}


