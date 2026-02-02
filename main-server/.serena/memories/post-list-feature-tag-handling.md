# Session Changes - Post List Feature & Exception Handling

Branch: `feat/post-list`

## Summary

이 세션에서는 Post list 기능의 tag 응답 형식을 개선하고, Exception 처리 규칙을 준수하도록 변경했습니다.

**주요 변경사항:**
- Post list에서 tag의 postCount를 제외하는 새로운 DTO 생성
- 프로젝트의 ApiException 규칙에 맞게 Exception 처리 변경

## 세부 변경사항

### 1. PostListTagResponse.kt (신규 파일)

**목적:** Post list에서만 사용할 간단한 tag DTO (postCount 불필요)

**특징:**
- `id` (UUID): 태그 ID
- `name` (String): 태그 이름
- `from(tag: Tag)`: Tag 엔티티를 DTO로 변환하는 정적 팩토리 메서드

```kotlin
data class PostListTagResponse(
    val id: UUID,
    val name: String,
) {
    companion object {
        fun from(tag: Tag): PostListTagResponse = PostListTagResponse(
            id = tag.id!!,
            name = tag.name,
        )
    }
}
```

### 2. PostListItemResponse.kt (수정)

**변경 내용:**
- Line 32: `List<TagResponse>` → `List<PostListTagResponse>`
- Line 51: `TagResponse.from(it, 1L)` → `PostListTagResponse.from(it)`

**설계 원칙:**
- Post list에서는 tag의 postCount를 반환하지 않음
- TagResponse는 다른 API에서 postCount와 함께 사용 (기존 형태 유지)
- API별로 필요한 데이터만 응답하도록 구분

### 3. PostStatus.kt (수정)

**신규 상태 코드 추가:**
```kotlin
INVALID_SORT_TYPE(HttpStatus.BAD_REQUEST.value(), 3005, "유효하지 않은 정렬 타입입니다")
```

**규칙:**
- HTTP Status: 400 (Bad Request)
- Custom Code: 3005
- 설명: 유효하지 않은 정렬 타입입니다

### 4. PostRepositoryCustomImpl.kt (수정)

**Import 추가:**
```kotlin
import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.post.enums.PostStatus
```

**변경 내용:**
- Line 142: `throw IllegalArgumentException("LATEST는 buildLatestCursorCondition 사용")`
- 변경 후: `throw ApiException(PostStatus.INVALID_SORT_TYPE)`

**설명:**
- 내부 로직 에러 처리를 프로젝트의 ApiException 규칙에 맞게 변경
- 명확한 HTTP 상태 코드와 커스텀 상태 코드를 반환하도록 개선

## 아키텍처 의사결정

### DTO 분리 원칙 (Option B 선택)

**고려한 옵션:**
- Option A: `TagResponse`의 `postCount`를 nullable로 변경 (전체 API에 영향)
- **Option B: Post list용 별도 DTO 생성 (선택됨)**
- Option C: PostListItemResponse에서만 inline으로 tag 생성

**선택 이유:**
1. **명확한 책임 분리**: 각 API에서 필요한 데이터만 응답
2. **미래 확장성**: Post list와 detail 페이지에서 다른 데이터 필요 시 용이
3. **API 계약 명확화**: DTO 이름만으로 API의 의도가 드러남
4. **SOLID 원칙 준수**: Single Responsibility - 각 DTO는 하나의 목적만 가짐

### Exception 처리 규칙 준수

**프로젝트 규칙:**
- 모든 비즈니스 에러는 `ApiException`과 `StatusIfs` 구현체(Status enum) 사용
- HTTP 상태 코드와 커스텀 상태 코드 조합으로 상세한 에러 정보 제공
- Status 코드는 범위별로 관리 (PostStatus: 3001-3099)

**변경의 의미:**
- 내부 로직 에러도 외부 API 규칙에 맞게 일관되게 처리
- 클라이언트가 명확한 에러 코드를 받을 수 있음

## 테스트 고려사항

다음 테스트 케이스를 추가할 것으로 권장:

1. **PostListItemResponse 매핑 테스트**
   - Post 엔티티에서 PostListItemResponse로 변환 확인
   - 태그가 PostListTagResponse로 정확히 변환되는지 검증

2. **PostRepositoryCustomImpl 테스트**
   - buildCountCursorCondition에서 유효하지 않은 sortType 처리 확인
   - ApiException(PostStatus.INVALID_SORT_TYPE) 발생 검증

3. **API 응답 테스트**
   - Post list 응답에서 tag가 postCount를 포함하지 않는지 확인
   - HTTP 400, Custom Code 3005 에러 응답 검증

## 파일 변경 통계

| 상태 | 파일 | 설명 |
|------|------|------|
| A | PostListTagResponse.kt | Post list용 tag DTO (신규) |
| M | PostListItemResponse.kt | tag 필드 타입 변경 |
| M | PostStatus.kt | INVALID_SORT_TYPE 추가 |
| M | PostRepositoryCustomImpl.kt | Exception 처리 변경 |

## 다음 단계

1. **코드 검토**: 변경사항에 대한 code review 수행
2. **테스트 작성**: 위의 테스트 고려사항에 따라 테스트 추가
3. **문서화**: README에 새로운 Status 코드 기록
4. **커밋**: conventional commit으로 변경사항 기록

## 메모리 활용

이 메모리는 다음 세션에서 참고할 수 있습니다:
- Post list 기능 확장 시
- Exception 처리 규칙이 필요할 때
- DTO 분리 설계 패턴 참고
