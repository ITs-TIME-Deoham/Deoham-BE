package com.deoham.global.util;

/**
 * 지리 좌표 관련 유틸리티.
 * DB(PostGIS ST_Distance) 왕복 없이 서버에서 두 지점 간 거리를 계산할 때 사용한다.
 */
public final class GeoUtils {

    /** 평균 지구 반지름(미터). 하버사인 공식용. */
    private static final double EARTH_RADIUS_M = 6_371_000.0;

    private GeoUtils() {
    }

    /**
     * 하버사인 공식으로 두 좌표 간 대원거리(미터)를 계산한다.
     * 결과는 정수 미터로 반올림한다(표시용 페이로드 정리 목적).
     */
    public static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (double) Math.round(EARTH_RADIUS_M * c);
    }
}
