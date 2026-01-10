# API Documentation

## Overview

모든 API는 Gateway(`/member-service`, `/study-service` 등)를 통해 라우팅되며, 인증이 필요한 API는 JWT 토큰을 통해 `X-User-Id` 헤더가 자동 주입됩니다.

**Base URL**: `http://localhost:8080`

---

## Member Service

### Authentication API

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/api/auth/register` | 회원가입 | - |
| `POST` | `/api/auth/login` | 로그인 (JWT 발급) | - |
| `POST` | `/api/auth/social` | 소셜 로그인 (Kakao/Google) | - |
| `POST` | `/api/auth/logout` | 로그아웃 | O |
| `POST` | `/api/auth/refresh` | Access Token 갱신 | - |

### Member API

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/api/member/{id}` | 회원 정보 조회 | O |

---

## Study Service

### Study Management API

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/api/study/create` | 스터디 생성 (multipart) | O |
| `POST` | `/api/study/enroll` | 스터디 참가 신청 | O |

### Study Query API

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/api/study/get/{id}` | 스터디 상세 조회 | - |
| `GET` | `/api/study/pages` | 스터디 목록 (페이징) | - |
| `GET` | `/api/study/list` | 스터디 목록 (커서 기반) | - |
| `GET` | `/api/study/board/{id}` | 스터디 대시보드 | O |
| `GET` | `/api/study/summary/{id}` | 스터디 요약 정보 | - |
| `GET` | `/api/study/my` | 내 스터디 목록 | O |

**Query Parameters (목록 조회)**
```
level       : BEGINNER | INTERMEDIATE | ADVANCED
category    : PROGRAMMING | LANGUAGE | CERTIFICATE | ...
minDeposit  : 최소 보증금
maxDeposit  : 최대 보증금
sortType    : LATEST | DEADLINE_SOON
```

### Study Recommendation API (AI)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/api/v1/studies/{id}/recommendations/similar` | 유사 스터디 추천 | - |
| `POST` | `/api/v1/studies/recommendations/by-interest` | 관심사 기반 추천 | - |
| `GET` | `/api/v1/studies/recommendations/by-category` | 카테고리별 추천 | - |

> **Tech**: OpenAI Embedding + pgvector 유사도 검색

### Attendance API

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/api/study/attendance/check` | 출석 체크 | O |
| `GET` | `/api/study/attendance/my/{studyId}` | 내 출석 목록 | O |
| `GET` | `/api/study/attendance/session/{sessionId}` | 세션별 출석 현황 (스터디장) | O |
| `PUT` | `/api/study/attendance/{id}` | 출석 상태 수정 (스터디장) | O |
| `POST` | `/api/study/attendance/process-absences/{sessionId}` | 결석 자동 처리 | - |

### Post API (게시판)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/api/study/{studyId}/posts` | 게시글 작성 | O |
| `GET` | `/api/study/{studyId}/posts` | 게시글 목록 | O |
| `GET` | `/api/study/{studyId}/posts/{postId}` | 게시글 상세 | O |
| `PUT` | `/api/study/{studyId}/posts/{postId}` | 게시글 수정 | O |
| `DELETE` | `/api/study/{studyId}/posts/{postId}` | 게시글 삭제 | O |
| `PATCH` | `/api/study/{studyId}/posts/{postId}/pin` | 게시글 고정/해제 | O |
| `GET` | `/api/study/{studyId}/posts/notices` | 공지글 목록 | O |
| `GET` | `/api/study/{studyId}/posts/pinned` | 고정글 목록 | O |
| `GET` | `/api/study/{studyId}/posts/search` | 게시글 검색 | O |

### Comment API

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/api/study/{studyId}/posts/{postId}/comments` | 댓글 작성 | O |
| `GET` | `/api/study/{studyId}/posts/{postId}/comments` | 댓글 목록 | O |
| `GET` | `/api/study/{studyId}/posts/{postId}/comments/paged` | 댓글 목록 (페이징) | O |
| `PUT` | `/api/study/{studyId}/posts/{postId}/comments/{commentId}` | 댓글 수정 | O |
| `DELETE` | `/api/study/{studyId}/posts/{postId}/comments/{commentId}` | 댓글 삭제 | O |

---

## Payment Service

### Payment API

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/api/payments` | 결제 요청 (주문 생성) | O |
| `POST` | `/api/payments/confirm` | 결제 승인 (Toss 콜백) | - |
| `POST` | `/api/payments/{orderId}/cancel` | 결제 취소 | O |
| `GET` | `/api/payments/{orderId}` | 결제 조회 | O |
| `GET` | `/api/payments/study/{studyId}/completed` | 스터디별 완료 결제 목록 | O |
| `GET` | `/api/payments/check` | 결제 완료 여부 확인 | O |

**Payment Flow**
```
1. POST /api/payments          → orderId 발급
2. Toss SDK 결제창 호출
3. POST /api/payments/confirm  → 결제 승인
```

---

## Chat Service

### REST API

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/api/chat/rooms` | 채팅방 생성 | O |
| `POST` | `/api/chat/rooms/member` | 채팅방 멤버 추가 | O |
| `GET` | `/api/chat/rooms/study/{studyId}` | 스터디별 채팅방 조회 | O |
| `POST` | `/api/chat/rooms/{id}/join` | 채팅방 입장 | O |
| `POST` | `/api/chat/rooms/{id}/leave` | 채팅방 퇴장 | O |
| `GET` | `/api/chat/rooms/{id}/messages` | 채팅 히스토리 조회 | O |
| `POST` | `/api/chat/rooms/{id}/read` | 메시지 읽음 처리 | O |
| `GET` | `/api/chat/rooms/{id}/unread` | 읽지 않은 메시지 수 | O |

### WebSocket API (STOMP)

**Connection**: `ws://localhost:8080/ws-chat`

| Destination | Description |
|-------------|-------------|
| `/app/chat.sendMessage` | 메시지 전송 |
| `/app/chat.join` | 채팅방 입장 |
| `/app/chat.leave` | 채팅방 퇴장 |
| `/topic/chatroom/{id}` | 채팅방 구독 (메시지 수신) |
| `/topic/chatroom/{id}/presence` | 접속자 목록 구독 |
| `/user/queue/errors` | 에러 메시지 수신 |

**Message Format**
```json
{
  "chatRoomId": 1,
  "senderId": 123,
  "senderNickname": "홍길동",
  "content": "안녕하세요!",
  "messageType": "CHAT"
}
```

---

## Common

### Request Headers

| Header | Description | Required |
|--------|-------------|----------|
| `Authorization` | Bearer {accessToken} | 인증 필요 API |
| `X-User-Id` | 사용자 ID (Gateway 자동 주입) | - |
| `Content-Type` | application/json | O |

### Response Format

**Success**
```json
{
  "id": 1,
  "title": "스터디 제목",
  "status": "RECRUITING"
}
```

**Error**
```json
{
  "timestamp": "2024-01-01T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "유효성 검증 실패"
}
```

### Pagination

**Request**
```
?page=0&size=10&sort=createdAt,desc
```

**Response**
```json
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 10,
  "size": 10,
  "number": 0
}
```

### Cursor-based Pagination

**Request**
```
?cursor=eyJpZCI6MTAwfQ==
```

**Response**
```json
{
  "items": [...],
  "nextCursor": "eyJpZCI6OTB9",
  "hasNext": true
}
```

---

## API Documentation (Swagger)

각 서비스별 Swagger UI 접근:
- **Gateway (통합)**: `http://localhost:8080/swagger-ui.html`
- Member Service: `/member-service/v3/api-docs`
- Study Service: `/study-service/v3/api-docs`
- Payment Service: `/payment-service/v3/api-docs`
