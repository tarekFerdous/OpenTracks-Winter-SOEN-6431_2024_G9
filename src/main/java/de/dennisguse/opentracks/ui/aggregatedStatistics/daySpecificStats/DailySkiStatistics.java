package de.dennisguse.opentracks.ui.aggregatedStatistics.daySpecificStats;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.stats.TrackStatistics;

public class DailySkiStatistics {
    private List<TrackPoint> trackPoints;

    // Constructor
    public DailySkiStatistics(List<TrackPoint> trackPoints) {
        this.trackPoints = trackPoints;
    }

    // Method to calculate the total skiing duration for the current day
    public Duration getTotalSkiingDuration() {
        return getTotalSkiingDuration(LocalDate.now());
    }

    // Method to calculate the total skiing duration for a specific date
    public Duration getTotalSkiingDuration(LocalDate date) {
        Duration totalSkiingDuration = Duration.ZERO;

        // Iterate through the track points to find skiing segments
        for (int i = 1; i < trackPoints.size(); i++) {
            TrackPoint previousPoint = trackPoints.get(i - 1);
            TrackPoint currentPoint = trackPoints.get(i);

            // Check if skiing is detected between these two points and on the specified date
            if (isSkiingSegment(previousPoint, currentPoint) && isSameDate(currentPoint.getTime(), date)) {
                totalSkiingDuration = totalSkiingDuration.plus(Duration.between(previousPoint.getTime(), currentPoint.getTime()));
            }
        }

        return totalSkiingDuration;
    }

    // Method to determine if skiing is detected between two track points
    private boolean isSkiingSegment(TrackPoint startPoint, TrackPoint endPoint) {
        // Thresholds to determine skiing activity
        double altitudeChangeThreshold = 10.0; // Meters
        double speedThreshold = 5.0; // Meters per second
        long timeThresholdInSeconds = 50; // Seconds

        // Check if altitude change is significant
        double altitudeChange = Math.abs(startPoint.getAltitude().toM() - endPoint.getAltitude().toM());
        if (altitudeChange < altitudeChangeThreshold) {
            return false; // Altitude change not significant, likely not skiing
        }

        // Calculate total distance
//        double totalDistance = startPoint.distanceTo(endPoint).toKM();

        // Calculate total time (in seconds)
        long totalTimeInSeconds = Duration.between(startPoint.getTime(), endPoint.getTime()).getSeconds();

        // Calculate average speed
//        double averageSpeed = totalDistance / totalTimeInSeconds;

        // Check if average speed is above the speed threshold
        return totalTimeInSeconds >= timeThresholdInSeconds;
    }

    // Method to check if two Instant objects belong to the same LocalDate
    private boolean isSameDate(Instant instant, LocalDate date) {
        return instant.atZone(ZoneId.systemDefault()).toLocalDate().isEqual(date);
    }
}