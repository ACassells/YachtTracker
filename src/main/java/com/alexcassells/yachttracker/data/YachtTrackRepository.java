package com.alexcassells.yachttracker.data;

import android.location.Location;

import com.alexcassells.yachttracker.data.entities.TrackDataPoint;

import java.util.List;

/**
 * Created by alexcassells on 08/06/2015.
 */
public class YachtTrackRepository {

    public long SaveTrackingPoint(Location location, float azimuth, float pitch, float roll)
    {
        TrackDataPoint point = new TrackDataPoint(location, azimuth, pitch, roll);
        point.save();
        return point.getId();
    }

    public TrackDataPoint getLatestTrackingPoint() {
        List<TrackDataPoint> points = TrackDataPoint.find(TrackDataPoint.class,
                null,
                null,
                null,
                "gps_time DESC",
                "1"
        );

        if (points.isEmpty()) {
            return null;
        }

        return points.get(0);
    }


    public void deleteTrackingPoint(Long id) {
        TrackDataPoint point = TrackDataPoint.findById(TrackDataPoint.class, id);
        point.delete();
    }
}
