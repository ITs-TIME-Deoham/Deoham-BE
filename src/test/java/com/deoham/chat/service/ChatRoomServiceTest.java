package com.deoham.chat.service;

import com.deoham.TestcontainersConfiguration;
import com.deoham.chat.dto.ChatRoomCreateRequest;
import com.deoham.chat.dto.ChatRoomResponse;
import com.deoham.chat.entity.ChatRoomMemberRole;
import com.deoham.chat.repository.ChatRoomMemberRepository;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.user.entity.User;
import com.deoham.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ChatRoomService 통합 테스트
 *
 * - @SpringBootTest: 실제 애플리케이션 컨텍스트 전체를 로드합니다.
 * - @Transactional: 각 테스트가 끝나면 DB 상태를 자동으로 롤백해 테스트 간 격리를 보장합니다.
 * - TestcontainersConfiguration: 테스트 전용 Postgres/Redis 컨테이너를 기동합니다.
 *   (Docker가 실행 중이어야 하며, 최초 실행 시 이미지 다운로드로 인해 시간이 걸릴 수 있습니다)
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ChatRoomServiceTest {

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    // 각 테스트에서 공통으로 사용할 사용자 3명
    private User userA;
    private User userB;
    private User userC;

    @BeforeEach
    void setUp() {
        // 테스트마다 다른 이메일로 사용자를 생성합니다.
        // @Transactional 롤백 덕분에 각 테스트 종료 후 DB가 초기화됩니다.
        userA = savedUser("alice@test.com", "Alice");
        userB = savedUser("bob@test.com", "Bob");
        userC = savedUser("carol@test.com", "Carol");
    }

    // ───────────────────────────────────────────────────────────────────────────
    // createRoom
    // ───────────────────────────────────────────────────────────────────────────

    /**
     * 두 사람 간 1:1 채팅방이 처음 생성될 때:
     * - isDirect = true
     * - name = null (DM은 이름 없음)
     * - 멤버 수 = 2
     */
    @Test
    void createDirectRoom_createsNewRoom() {
        ChatRoomCreateRequest request = new ChatRoomCreateRequest(null, List.of(userB.getId()), null);

        ChatRoomResponse response = chatRoomService.createRoom(userA.getId(), request);

        assertThat(response.id()).isNotNull();
        assertThat(response.isDirect()).isTrue();
        assertThat(response.name()).isNull();
        assertThat(response.members()).hasSize(2);
    }

    /**
     * 동일한 두 사람 간 1:1 방이 이미 존재하면 새로 만들지 않고 기존 방을 반환해야 합니다.
     * (중복 DM 방 생성 방지 — ChatRoomRepository.findDirectRoomBetween 쿼리 검증)
     */
    @Test
    void createDirectRoom_returnsExistingRoom_whenAlreadyExists() {
        ChatRoomCreateRequest request = new ChatRoomCreateRequest(null, List.of(userB.getId()), null);

        ChatRoomResponse first = chatRoomService.createRoom(userA.getId(), request);
        ChatRoomResponse second = chatRoomService.createRoom(userA.getId(), request);

        assertThat(first.id()).isEqualTo(second.id());
    }

    /**
     * 3명 이상이 참여하면 그룹 채팅방으로 생성됩니다.
     * - isDirect = false
     * - name 이 저장됨
     * - 멤버 수 = 3
     */
    @Test
    void createGroupRoom_createsRoomWithNameAndAllMembers() {
        ChatRoomCreateRequest request = new ChatRoomCreateRequest(
                null, List.of(userB.getId(), userC.getId()), "팀 채팅");

        ChatRoomResponse response = chatRoomService.createRoom(userA.getId(), request);

        assertThat(response.isDirect()).isFalse();
        assertThat(response.name()).isEqualTo("팀 채팅");
        assertThat(response.members()).hasSize(3);
    }

    /**
     * 방을 개설한 요청자는 OWNER 역할을 가져야 합니다.
     * 나머지 초대된 멤버는 MEMBER 역할입니다.
     */
    @Test
    void createRoom_requester_hasOwnerRole() {
        ChatRoomCreateRequest request = new ChatRoomCreateRequest(null, List.of(userB.getId()), null);

        ChatRoomResponse response = chatRoomService.createRoom(userA.getId(), request);

        // 요청자(userA)의 역할이 OWNER인지 확인
        assertThat(response.members())
                .filteredOn(m -> m.userId().equals(userA.getId()))
                .extracting(m -> m.role())
                .containsExactly(ChatRoomMemberRole.OWNER.name());

        // 초대된 사람(userB)의 역할이 MEMBER인지 확인
        assertThat(response.members())
                .filteredOn(m -> m.userId().equals(userB.getId()))
                .extracting(m -> m.role())
                .containsExactly(ChatRoomMemberRole.MEMBER.name());
    }

    // ───────────────────────────────────────────────────────────────────────────
    // getRoom
    // ───────────────────────────────────────────────────────────────────────────

    /**
     * 채팅방 멤버가 아닌 사용자가 방 정보를 조회하면 FORBIDDEN 예외가 발생해야 합니다.
     */
    @Test
    void getRoom_throwsForbidden_whenRequesterIsNotMember() {
        ChatRoomResponse room = chatRoomService.createRoom(
                userA.getId(), new ChatRoomCreateRequest(null, List.of(userB.getId()), null));

        // userC는 이 방의 멤버가 아님
        assertThatThrownBy(() -> chatRoomService.getRoom(userC.getId(), room.id()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    // ───────────────────────────────────────────────────────────────────────────
    // leaveRoom
    // ───────────────────────────────────────────────────────────────────────────

    /**
     * 퇴장(leaveRoom) 후에는 해당 멤버의 leftAt이 설정되어 활성 멤버로 조회되지 않아야 합니다.
     */
    @Test
    void leaveRoom_deactivatesMembership() {
        ChatRoomResponse room = chatRoomService.createRoom(
                userA.getId(), new ChatRoomCreateRequest(null, List.of(userB.getId()), null));

        chatRoomService.leaveRoom(userB.getId(), room.id());

        // leftAt IS NULL 조건으로 조회하면 존재하지 않아야 함
        boolean stillActive = chatRoomMemberRepository
                .existsByChatRoomIdAndUserIdAndLeftAtIsNull(room.id(), userB.getId());
        assertThat(stillActive).isFalse();
    }

    /**
     * 퇴장 후에는 그 방을 getRoom으로 조회할 수 없어야 합니다 (FORBIDDEN).
     */
    @Test
    void leaveRoom_blocksFurtherGetRoom() {
        ChatRoomResponse room = chatRoomService.createRoom(
                userA.getId(), new ChatRoomCreateRequest(null, List.of(userB.getId()), null));

        chatRoomService.leaveRoom(userB.getId(), room.id());

        assertThatThrownBy(() -> chatRoomService.getRoom(userB.getId(), room.id()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    // ───────────────────────────────────────────────────────────────────────────
    // getMyRooms
    // ───────────────────────────────────────────────────────────────────────────

    /**
     * 내 방 목록은 현재 활성 멤버인 방만 반환해야 합니다.
     * 퇴장한 방(leftAt이 설정된 방)은 목록에서 제외됩니다.
     */
    @Test
    void getMyRooms_excludesRoomsWhereUserHasLeft() {
        // 활성 방 1개 생성
        chatRoomService.createRoom(
                userA.getId(), new ChatRoomCreateRequest(null, List.of(userB.getId()), null));

        // 퇴장할 방 1개 생성 후 즉시 퇴장
        ChatRoomResponse roomToLeave = chatRoomService.createRoom(
                userA.getId(), new ChatRoomCreateRequest(null, List.of(userC.getId()), null));
        chatRoomService.leaveRoom(userA.getId(), roomToLeave.id());

        Page<ChatRoomResponse> myRooms = chatRoomService.getMyRooms(userA.getId(), PageRequest.of(0, 10));

        // 활성 방 1개만 반환되어야 하고, 퇴장한 방은 포함되지 않아야 함
        assertThat(myRooms.getContent()).hasSize(1);
        assertThat(myRooms.getContent().get(0).id()).isNotEqualTo(roomToLeave.id());
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 헬퍼
    // ───────────────────────────────────────────────────────────────────────────

    private User savedUser(String email, String name) {
        return userRepository.save(User.builder().email(email).name(name).build());
    }
}
