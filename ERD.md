# Entity Relationship Diagram

## Overview

마이크로서비스별 독립된 데이터베이스를 사용하며, 서비스 간 참조는 ID로 관리

---

## Member Service

회원 정보 및 소셜 로그인 관리

```
┌─────────────────────────────────────────────────────────────┐
│                         MEMBER                               │
├─────────────────────────────────────────────────────────────┤
│ PK  id                    BIGINT                            │
├─────────────────────────────────────────────────────────────┤
│     email                 VARCHAR(100)      UNIQUE          │
│     password              VARCHAR(100)                      │
│     nickname              VARCHAR(30)       UNIQUE          │
│     profile_image_url     VARCHAR(500)                      │
│     introduction          VARCHAR(500)                      │
│     role                  ENUM(USER, ADMIN)                 │
│     status                ENUM(ACTIVE, SUSPENDED, WITHDRAWN)│
│     email_verified        BOOLEAN                           │
├─────────────────────────────────────────────────────────────┤
│ [Point & Level]                                             │
│     point                 INTEGER           DEFAULT 1000    │
│     activity_score        INTEGER           DEFAULT 0       │
│     level                 ENUM(NEWCOMER, BEGINNER, ...)     │
├─────────────────────────────────────────────────────────────┤
│ [Statistics]                                                │
│     completed_study_count     INTEGER                       │
│     average_attendance_rate   DECIMAL(5,2)                  │
│     total_attendance_count    INTEGER                       │
│     total_study_session_count INTEGER                       │
├─────────────────────────────────────────────────────────────┤
│     created_at            TIMESTAMP                         │
│     updated_at            TIMESTAMP                         │
└─────────────────────────────────────────────────────────────┘
          │
          │ 1:N
          ▼
┌─────────────────────────────────────────────────────────────┐
│                     SOCIAL_ACCOUNT                           │
├─────────────────────────────────────────────────────────────┤
│ PK  id                    BIGINT                            │
│ FK  member_id             BIGINT                            │
├─────────────────────────────────────────────────────────────┤
│     provider              ENUM(KAKAO, GOOGLE)               │
│     provider_id           VARCHAR(100)      UK(provider,id) │
│     email                 VARCHAR(100)                      │
│     name                  VARCHAR(100)                      │
│     profile_image_url     VARCHAR(500)                      │
│     created_at            TIMESTAMP                         │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    MEMBER_INTERESTS                          │
├─────────────────────────────────────────────────────────────┤
│ FK  member_id             BIGINT                            │
│     interest              ENUM(PROGRAMMING, LANGUAGE, ...)  │
└─────────────────────────────────────────────────────────────┘
```

---

## Study Service

스터디 관리, 세션, 출석, 게시판

```
┌─────────────────────────────────────────────────────────────┐
│                          STUDY                               │
├─────────────────────────────────────────────────────────────┤
│ PK  id                    BIGINT                            │
├─────────────────────────────────────────────────────────────┤
│     title                 VARCHAR(50)                       │
│     thumbnail_url         VARCHAR(500)                      │
│     category              ENUM(PROGRAMMING, LANGUAGE, ...)  │
│     level                 ENUM(BEGINNER, INTERMEDIATE, ...) │
│     visibility            ENUM(PUBLIC, PRIVATE)             │
│     leader_id             BIGINT            → Member(id)    │
├─────────────────────────────────────────────────────────────┤
│ [Schedule - Embedded]                                       │
│     start_date            DATE                              │
│     end_date              DATE                              │
│     recruit_end_date      DATE                              │
│     start_time            TIME                              │
│     end_time              TIME                              │
│     study_days            SET<DayOfWeek>                    │
├─────────────────────────────────────────────────────────────┤
│ [Participants]                                              │
│     min_participants      INTEGER                           │
│     max_participants      INTEGER                           │
│     current_participants  INTEGER                           │
│     deposit_amount        INTEGER           5000~50000P     │
├─────────────────────────────────────────────────────────────┤
│     introduction          VARCHAR(2000)                     │
│     curriculum            VARCHAR(2000)                     │
│     leader_message        VARCHAR(500)                      │
│     status                ENUM(PENDING, RECRUITING, ...)    │
│     created_at            TIMESTAMP                         │
│     updated_at            TIMESTAMP                         │
└─────────────────────────────────────────────────────────────┘
          │
          ├──────────────────────┬──────────────────────┐
          │ 1:N                  │ 1:N                  │ 1:N
          ▼                      ▼                      ▼
┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐
│   STUDY_MEMBER   │   │     SESSION      │   │       POST       │
├──────────────────┤   ├──────────────────┤   ├──────────────────┤
│ PK id            │   │ PK id            │   │ PK id            │
│ FK study_id      │   │ FK study_id      │   │ FK study_id      │
├──────────────────┤   ├──────────────────┤   ├──────────────────┤
│   member_id →    │   │   session_number │   │   writer_id →    │
│   nickname       │   │   session_date   │   │   writer_nickname│
│   role (LEADER/  │   │   start_time     │   │   category       │
│        MEMBER)   │   │   end_time       │   │   title          │
│   status         │   │   status         │   │   content        │
│   deposit_paid   │   │   title          │   │   view_count     │
│   refund_amount  │   │   description    │   │   comment_count  │
│   attendance_cnt │   │   material_url   │   │   pinned         │
│   absence_count  │   │   attendance_cnt │   │   deleted        │
│   attendance_rate│   │   absence_count  │   │   created_at     │
│   joined_at      │   │   attendance_    │   │   updated_at     │
│   withdrawn_at   │   │   check_start/end│   └──────────────────┘
└──────────────────┘   │   created_at     │            │ 1:N
                       └──────────────────┘            ▼
                                │ 1:N        ┌──────────────────┐
                                ▼            │   POST_COMMENT   │
                       ┌──────────────────┐  ├──────────────────┤
                       │    ATTENDANCE    │  │ PK id            │
                       ├──────────────────┤  │ FK post_id       │
                       │ PK id            │  ├──────────────────┤
                       │ FK session_id    │  │   writer_id →    │
                       ├──────────────────┤  │   writer_nickname│
                       │   member_id →    │  │   content        │
                       │   status (PRESENT│  │   deleted        │
                       │          /ABSENT)│  │   created_at     │
                       │   checked_at     │  │   updated_at     │
                       │   note           │  └──────────────────┘
                       │   activity_score │
                       │   created_at     │
                       └──────────────────┘
```

### Study Status Flow
```
PENDING → RECRUITING → RECRUIT_CLOSED → IN_PROGRESS → COMPLETED → SETTLED
                 ↓
             CANCELLED
```

---

## Payment Service

Toss Payments 연동 결제 관리

```
┌─────────────────────────────────────────────────────────────┐
│                         PAYMENT                              │
├─────────────────────────────────────────────────────────────┤
│ PK  id                    BIGINT                            │
├─────────────────────────────────────────────────────────────┤
│     member_id             BIGINT            → Member(id)    │
│     study_id              BIGINT            → Study(id)     │
├─────────────────────────────────────────────────────────────┤
│ [Order Info]                                                │
│     order_id              VARCHAR           UNIQUE          │
│     order_name            VARCHAR                           │
│     amount                INTEGER                           │
├─────────────────────────────────────────────────────────────┤
│ [Toss Response]                                             │
│     payment_key           VARCHAR                           │
│     method                VARCHAR           (카드, 토스페이 등)│
├─────────────────────────────────────────────────────────────┤
│     status                ENUM(PENDING, COMPLETED,          │
│                                FAILED, CANCELLED)           │
│     requested_at          TIMESTAMP                         │
│     approved_at           TIMESTAMP                         │
│     cancelled_at          TIMESTAMP                         │
│     fail_reason           VARCHAR                           │
└─────────────────────────────────────────────────────────────┘
```

### Payment Flow
```
PENDING → COMPLETED
    ↓         ↓
  FAILED   CANCELLED
```

---

## Chat Service

WebSocket STOMP + Redis Pub/Sub 실시간 채팅

```
┌─────────────────────────────────────────────────────────────┐
│                        CHAT_ROOM                             │
├─────────────────────────────────────────────────────────────┤
│ PK  id                    BIGINT                            │
├─────────────────────────────────────────────────────────────┤
│     study_id              BIGINT            UNIQUE → Study  │
│     name                  VARCHAR(100)                      │
│     is_active             BOOLEAN                           │
│     created_at            TIMESTAMP                         │
└─────────────────────────────────────────────────────────────┘
          │
          ├────────────────────────┐
          │ 1:N                    │ 1:N
          ▼                        ▼
┌──────────────────────┐  ┌──────────────────────┐
│   CHAT_ROOM_MEMBER   │  │     CHAT_MESSAGE     │
├──────────────────────┤  ├──────────────────────┤
│ PK id                │  │ PK id                │
│ FK chat_room_id      │  │ FK chat_room_id      │
├──────────────────────┤  ├──────────────────────┤
│   member_id →        │  │   sender_id →        │
│   last_read_msg_id   │  │   content      TEXT  │
│   joined_at          │  │   message_type       │
│   left_at            │  │   created_at         │
└──────────────────────┘  └──────────────────────┘
```

---

## Cross-Service References

서비스 간 데이터 참조는 ID 기반으로 처리 (분산 환경)

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│    Member    │     │    Study     │     │   Payment    │
│   Service    │     │   Service    │     │   Service    │
├──────────────┤     ├──────────────┤     ├──────────────┤
│ member.id ◄──┼─────┼─ leader_id   │     │              │
│              │     │              │◄────┼─ study_id    │
│              │◄────┼─ member_id   │     │              │
│              │     │              │     │ member_id ───┼─►
└──────────────┘     └──────────────┘     └──────────────┘
                            │
                            ▼
                     ┌──────────────┐
                     │    Chat      │
                     │   Service    │
                     ├──────────────┤
                     │ study_id ────┼─►
                     │ member_id ───┼─►
                     └──────────────┘
```

---

## Key Design Decisions

| Pattern | Description |
|---------|-------------|
| **Database per Service** | 마이크로서비스별 독립 DB |
| **ID Reference** | 외래 키 대신 ID 참조로 서비스 간 결합도 최소화 |
| **Soft Delete** | Post, Comment 등 삭제 플래그 사용 |
| **Embedded Value Object** | StudySchedule, Email, NotificationSetting |
| **Denormalization** | writerNickname 등 조회 성능 최적화 |
