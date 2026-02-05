# Spotless Setup for main-server

## Business Goal
main-server(Kotlin 프로젝트)에 코드 포맷팅 자동화를 적용하여 일관된 코드 스타일을 유지하고, CI에서 spotless 위반 시 merge를 차단하여 코드 품질을 보장한다.

## Scope
- **In Scope**:
  - main-server/build.gradle.kts에 spotless 플러그인 추가 (ktlint)
  - .github/workflows/test-visibility.yml에 spotlessCheck step 추가
  - main-server/README.md에 spotless 사용법 문서화
- **Out of Scope**:
  - matchday-api 설정 변경
  - 로컬 git hooks (lefthook) 설정

## Codebase Analysis Summary
- main-server는 Kotlin 1.9.25 + Spring Boot 3.5.7 프로젝트
- build.gradle.kts 사용 (Kotlin DSL)
- 기존 CI workflow(test-visibility.yml)가 main/dev 브랜치 PR 시 테스트 실행
- spotless 설정 없음

### Relevant Files
| File | Role | Action |
|------|------|--------|
| main-server/build.gradle.kts | Gradle 빌드 설정 | Modify |
| .github/workflows/test-visibility.yml | CI workflow | Modify |
| main-server/README.md | 프로젝트 문서 | Modify |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| Kotlin DSL | build.gradle.kts | Kotlin DSL 문법 사용 |
| YAML 들여쓰기 | test-visibility.yml | 2칸 들여쓰기 |
| README 구조 | 기존 README.md | 기존 섹션 유지, 새 섹션 추가 |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| Kotlin 포맷터 | ktlint | 사용자 선택, Kotlin 커뮤니티 표준 | ktfmt |
| Spotless 버전 | 6.25.0 | 최신 안정 버전 | 6.22.0 |
| CI 통합 방식 | 기존 workflow 수정 | 중복 job 방지 | 별도 workflow |
| spotlessCheck 실행 시점 | 테스트 전 | 포맷 위반 시 빠른 실패 | 테스트 후 |

## Implementation Todos

### Todo 1: Add Spotless plugin to build.gradle.kts
- **Priority**: 1
- **Dependencies**: none
- **Goal**: main-server에 ktlint 기반 spotless 플러그인 설정 추가
- **Work**:
  - `main-server/build.gradle.kts` plugins 블록에 `id("com.diffplug.spotless") version "6.25.0"` 추가
  - spotless 설정 블록 추가:
    ```kotlin
    spotless {
        kotlin {
            target("src/**/*.kt")
            ktlint("1.2.1")
            trimTrailingWhitespace()
            endWithNewline()
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint("1.2.1")
        }
    }
    ```
- **Convention Notes**: Kotlin DSL 문법 사용, 기존 plugins 블록 스타일 유지
- **Verification**: `./gradlew spotlessCheck` 실행 성공
- **Exit Criteria**: spotlessCheck 태스크가 정상 실행되고 현재 코드에 대해 pass 또는 명확한 위반 목록 출력
- **Status**: pending

### Todo 2: Add spotlessCheck to CI workflow
- **Priority**: 1
- **Dependencies**: none
- **Goal**: PR 시 spotless 위반이 있으면 merge 차단
- **Work**:
  - `.github/workflows/test-visibility.yml` 수정
  - "Run Tests" step 전에 "Spotless Check" step 추가:
    ```yaml
    - name: Spotless Check
      working-directory: ./main-server
      run: |
        chmod +x gradlew
        ./gradlew spotlessCheck
    ```
- **Convention Notes**: YAML 2칸 들여쓰기, 기존 step 스타일 따름
- **Verification**: GitHub Actions syntax 검증
- **Exit Criteria**: workflow 파일이 유효한 YAML이고, spotlessCheck step이 테스트 step 전에 위치
- **Status**: pending

### Todo 3: Update README with Spotless usage
- **Priority**: 2
- **Dependencies**: Todo 1
- **Goal**: 개발자가 spotless 명령어를 쉽게 찾을 수 있도록 문서화
- **Work**:
  - `main-server/README.md`에 "## Code Formatting (Spotless)" 섹션 추가
  - 내용:
    ```markdown
    ## Code Formatting (Spotless)

    ### 포맷 검사
    ```bash
    ./gradlew spotlessCheck
    ```

    ### 자동 포맷 적용
    ```bash
    ./gradlew spotlessApply
    ```

    CI에서 spotless 위반 시 PR merge가 차단됩니다.
    커밋 전에 `spotlessApply`를 실행하세요.
    ```
- **Convention Notes**: 기존 README 마크다운 스타일 유지
- **Verification**: 마크다운 렌더링 확인
- **Exit Criteria**: README에 spotless 섹션이 추가되고 명령어가 정확함
- **Status**: pending

### Todo 4: Apply spotless formatting to existing code
- **Priority**: 2
- **Dependencies**: Todo 1
- **Goal**: 기존 코드를 spotless 규칙에 맞게 포맷팅
- **Work**:
  - `cd main-server && ./gradlew spotlessApply` 실행
  - 변경된 파일 확인
- **Convention Notes**: ktlint 스타일 적용
- **Verification**: `./gradlew spotlessCheck` pass
- **Exit Criteria**: spotlessCheck가 위반 없이 통과
- **Status**: pending

## Verification Strategy
1. `cd main-server && ./gradlew spotlessCheck` - 포맷 검사 통과
2. `cd main-server && ./gradlew build` - 빌드 성공
3. GitHub Actions workflow syntax 검증

## Progress Tracking
- Total Todos: 4
- Completed: 0
- Status: Planning complete

## Change Log
- 2026-02-05: Plan created
