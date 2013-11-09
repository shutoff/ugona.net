package net.ugona.plus;

import java.io.Serializable;
import java.util.Vector;

public class Tracks {

    public static class Point implements Serializable {
        double latitude;
        double longitude;
        double speed;
        long time;
    }

    public static class Track implements Serializable {
        long begin;
        long end;
        double mileage;
        double day_mileage;
        double avg_speed;
        double max_speed;
        double day_max_speed;
        String start;
        String finish;
        Vector<Point> track;
    }

}
