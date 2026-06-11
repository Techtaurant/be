안녕하세요! `origin/main` 대비 admin 관련 변경을 제외하고, FE에서 링크 콘텐츠 페이지 구현 시 참고해야 할 API/화면 범위를 다시 정리드립니다.

이번 변경의 핵심은 링크 콘텐츠를 게시물처럼 보여줄 수 있는 공개 정적 조회 API가 추가된 점입니다. 기존 게시물 목록/상세 페이지와 UI 흐름을 최대한 비슷하게 가져가 주시고, 공개 정적 API는 SSG/ISR 캐싱을 고려해서 구현해 주세요.

## 만들어야 할 페이지

- 링크 목록 페이지
  - `GET /open-api/links`: 링크 정적 콘텐츠 목록 조회
  - 기존 게시물 목록 페이지와 유사한 카드/리스트, 태그, 발행일/생성일 노출 방식으로 구성해 주세요.
  - 목록 데이터는 로그인 사용자 상태가 빠진 정적 콘텐츠라, Next.js `revalidate` 기준 ISR 적용을 고려해 주세요.

- 링크 상세 페이지
  - `GET /open-api/links/{linkId}`: 링크 정적 콘텐츠 상세 조회
  - 기존 게시물 상세 페이지와 유사한 레이아웃, 타이포, 태그 노출 방식으로 맞춰 주세요.
  - 상세 페이지도 정적 콘텐츠 성격이므로 ISR 대상입니다.

- 링크 클릭/조회 기록
  - `POST /open-api/links/{linkId}/view-logs`: 링크 클릭 등 실제 조회 이벤트 발생 시 호출
  - 비로그인 사용자도 호출 가능하므로, 원문 링크 클릭 시 조회 로그를 남겨 주세요.

- 로그인 사용자 상호작용
  - `POST /api/links/{linkId}/like`: 좋아요 상태 변경
  - request: `{ "likeStatus": "LIKE" | "DISLIKE" | "NONE" }`
  - 로그인 사용자 액션 영역에 좋아요/싫어요/취소 처리를 붙이면 됩니다.

## 포함되어야 하는 기능

- 목록 조회/페이지네이션
  - `cursor`: 이전 응답의 `nextCursor`
  - `size`: 기본 20, 1-100
  - 응답: `content`, `nextCursor`, `hasNext`, `size`

- 정렬
  - 현재 API는 최신순 고정입니다.
  - 별도 `sort` 파라미터는 없으므로 정렬 선택 UI는 이번 범위에서 제외하거나, 필요 시 BE 추가 협의가 필요합니다.

- 필터
  - `sourceCompanyUserId`: 출처 회사 사용자 ID 필터
  - `tag`: 링크 태그명 필터

- 검색
  - 현재 공개 링크 목록 API에는 keyword/search 파라미터가 없습니다.
  - 검색 UI가 필요하면 BE 검색 조건 추가가 먼저 필요합니다. 현 범위에서는 검색 제외가 맞습니다.

- 목록/상세 표시 필드
  - `title`, `url`, `summary`, `sourceCompanyUserId`, `publishedAt`, `tags`, `createdAt`, `updatedAt`
  - 공개 정적 API에는 로그인 사용자별 `isSaved`, `isRead` 상태가 포함되지 않습니다. ISR 캐싱 대상 데이터와 사용자별 상태 데이터는 분리해서 다뤄 주세요.

- 사용자 상태/집계가 필요한 인증 API
  - `GET /api/companies/{companyUserId}/links`: 기존 회사 링크 목록 응답에 `sourceCompanyUserId`, `viewCount`, `likeCount`가 추가되었습니다.
  - `POST /api/links/{linkId}/like`: 좋아요 상태 변경을 지원합니다.

요약하면, FE 구현 범위는 링크 목록/상세 페이지, 기존 게시물 페이지와 유사한 UI, ISR 적용, 커서 페이지네이션, 태그/출처 회사 필터, 원문 링크 클릭 조회 로그, 로그인 사용자 좋아요 처리입니다. 검색과 자유 정렬은 현재 API 범위에 없어서 이번 구현 대상에서 제외하는 것이 맞습니다.
