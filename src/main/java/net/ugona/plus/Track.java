package net.ugona.plus;

import java.io.Serializable;
import java.util.Vector;

public class Track implements Serializable {
    long begin;
    long end;
    float mileage;
    float day_mileage;
    float avg_speed;
    int max_speed;
    int day_max_speed;
    String start;
    String finish;
    String track;

    public static class Point {
        double latitude;
        double longitude;
        long time;

        Point(String data) {
            String[] p = data.split(",");
            latitude = Double.parseDouble(p[0]);
            longitude = Double.parseDouble(p[1]);
            time = Long.parseLong(p[3]);
        }
    }

    static class TimeInterval {
        long begin;
        long end;
    }

    static class Marker {
        double latitude;
        double longitude;
        String address;
        Vector<TimeInterval> times;
    }
}
