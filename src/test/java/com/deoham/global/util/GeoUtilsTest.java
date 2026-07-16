package com.deoham.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

@DisplayName("GeoUtils 테스트")
class GeoUtilsTest {

    // 서울시청, 강남역
    private static final double CITY_HALL_LAT = 37.5665;
    private static final double CITY_HALL_LNG = 126.9780;
    private static final double GANGNAM_LAT = 37.4979;
    private static final double GANGNAM_LNG = 127.0276;

    @Test
    @DisplayName("동일 좌표 간 거리는 0이다")
    void haversineMeters_returnsZero_forSameCoordinates() {
        assertThat(GeoUtils.haversineMeters(CITY_HALL_LAT, CITY_HALL_LNG, CITY_HALL_LAT, CITY_HALL_LNG))
                .isZero();
    }

    @Test
    @DisplayName("서울시청-강남역 거리는 약 8.7km이다")
    void haversineMeters_matchesKnownDistance() {
        double distance = GeoUtils.haversineMeters(CITY_HALL_LAT, CITY_HALL_LNG, GANGNAM_LAT, GANGNAM_LNG);

        assertThat(distance).isCloseTo(8_700.0, withinPercentage(2));
    }

    @Test
    @DisplayName("거리는 좌표 순서에 무관하게 대칭이다")
    void haversineMeters_isSymmetric() {
        double forward = GeoUtils.haversineMeters(CITY_HALL_LAT, CITY_HALL_LNG, GANGNAM_LAT, GANGNAM_LNG);
        double backward = GeoUtils.haversineMeters(GANGNAM_LAT, GANGNAM_LNG, CITY_HALL_LAT, CITY_HALL_LNG);

        assertThat(forward).isEqualTo(backward);
    }

    @Test
    @DisplayName("결과는 정수 미터로 반올림된다")
    void haversineMeters_roundsToWholeMeters() {
        double distance = GeoUtils.haversineMeters(CITY_HALL_LAT, CITY_HALL_LNG, GANGNAM_LAT, GANGNAM_LNG);

        assertThat(distance % 1).isZero();
    }
}
