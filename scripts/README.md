# Scripts

개발 및 테스트용 유틸리티 스크립트 모음.

> **전제조건**: [uv](https://docs.astral.sh/uv/) 설치 필요 (`curl -LsSf https://astral.sh/uv/install.sh | sh`)

---

## test_presigned_upload.py

S3 Presigned URL 업로드 플로우를 end-to-end로 테스트하는 스크립트.

### 전제조건

- 서버가 **dev 프로파일**로 실행 중이어야 함 (`SPRING_PROFILES_ACTIVE=dev`)
  - 단, `--access-token`으로 직접 토큰을 넘기면 dev 로그인 단계는 건너뜀
- `uv` 설치 (`curl -LsSf https://astral.sh/uv/install.sh | sh`)

### 모듈 구성

| 파일 | 역할 |
|------|------|
| `auth.py` | dev 로그인 → accessToken 발급 |
| `test_presigned_upload.py` | Presigned URL 발급 + S3 업로드 |

### 수행 단계

1. `auth.py`: `POST /open-api/dev/auth/login` → accessToken 발급
   - `--access-token`이 있으면 이 단계 생략
2. `POST /api/attachments/presigned-url` → presigned URL + objectKey 수신
3. presigned URL로 S3 직접 PUT 업로드

> confirm (게시물 생성/수정)은 별도 처리.

### 사용법

`scripts/` 디렉토리 안에서 실행한다 (auth 모듈 import 경로 때문).

```bash
cd scripts

# 더미 PNG 파일 자동 생성하여 테스트
uv run test_presigned_upload.py

# 실제 파일 지정
uv run test_presigned_upload.py --file ~/Downloads/photo.jpg

# 참조 타입 지정 (POST | PROFILE, 기본값: POST)
uv run test_presigned_upload.py --file ~/image.png --type PROFILE

# 다른 서버 주소 사용
uv run test_presigned_upload.py --base-url http://localhost:9090

# 직접 access token 사용
uv run test_presigned_upload.py --access-token <YOUR_ACCESS_TOKEN>

# 다른 dev 사용자 식별자
uv run test_presigned_upload.py --identifier my-user

# 모든 옵션을 함께 사용
uv run test_presigned_upload.py --file ~/Downloads/photo.jpg --type PROFILE --base-url http://localhost:9090 --access-token <YOUR_ACCESS_TOKEN>
```

### 출력 예시

```
══════════════════════════════════════════════
  S3 Presigned Upload 테스트
══════════════════════════════════════════════
  서버:      http://localhost:8080
  파일:      test-upload-1234.png (68 bytes, image/png)
  참조타입:  POST
══════════════════════════════════════════════

▶ Step 1. Dev 로그인 (test-uploader)...
   ✅ 로그인 성공 (accessToken 획득)

▶ Step 2. Presigned URL 발급 요청...
   ✅ Presigned URL 발급 성공
   objectKey:    tmp/550e8400-e29b.../test-upload-1234.png
   attachmentId: 550e8400-e29b-41d4-a716-446655440000

▶ Step 3. S3 직접 업로드 (PUT ...)...
   ✅ S3 업로드 성공 (HTTP 200)

══════════════════════════════════════════════
  ✅ 업로드 완료
══════════════════════════════════════════════
  objectKey:    tmp/550e8400-e29b.../test-upload-1234.png
  attachmentId: 550e8400-e29b-41d4-a716-446655440000

  📌 게시물 content에 삽입할 형식:
     Markdown: ![이미지](tmp/550e8400-e29b.../test-upload-1234.png)
     HTML:     <img src="tmp/550e8400-e29b.../test-upload-1234.png" />
══════════════════════════════════════════════
```
