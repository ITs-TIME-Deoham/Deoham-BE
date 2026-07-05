package com.deoham.card.repository;

import com.deoham.TestcontainersConfiguration;
import com.deoham.card.entity.Card;
import com.deoham.card.entity.CardCategory;
import com.deoham.user.entity.User;
import com.deoham.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class CardRepositoryTest {

    private static final double BASE_LAT = 37.5326;
    private static final double BASE_LNG = 126.9903;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findNearbyCards_usesEachCardsRadiusM() {
        User requester = userRepository.save(User.builder()
                .firebaseUid("radius-requester")
                .nickname("radius-requester")
                .build());

        Card included = cardRepository.save(card(requester, "inside card radius", BASE_LAT + 0.00135, 200));
        Card excluded = cardRepository.save(card(requester, "outside card radius", BASE_LAT + 0.00135, 100));

        List<UUID> resultIds = cardRepository.findNearbyCards(BASE_LAT, BASE_LNG, null, null).stream()
                .map(row -> (UUID) row[0])
                .toList();

        assertThat(resultIds).contains(included.getId());
        assertThat(resultIds).doesNotContain(excluded.getId());
    }

    private Card card(User requester, String description, double lat, Integer radiusM) {
        return Card.builder()
                .requester(requester)
                .category(CardCategory.OTHER)
                .description(description)
                .location(point(BASE_LNG, lat))
                .radiusM(radiusM)
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
    }

    private Point point(double lng, double lat) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
        point.setSRID(4326);
        return point;
    }
}
