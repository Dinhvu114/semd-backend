package com.semd.backend.util;

import org.locationtech.jts.geom.Point;

public class GeoUtils {

    /**
     * Lấy longitude từ Point JTS (getX() = longitude)
     */
    public static double lon(Point point) {
        if (point == null) throw new RuntimeException("Tọa độ null, không thể tính route");
        return point.getX();
    }

    /**
     * Lấy latitude từ Point JTS (getY() = latitude)
     */
    public static double lat(Point point) {
        if (point == null) throw new RuntimeException("Tọa độ null, không thể tính route");
        return point.getY();
    }
}