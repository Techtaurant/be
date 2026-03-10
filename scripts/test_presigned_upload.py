#!/usr/bin/env python3
"""S3 Presigned URL 업로드 플로우 테스트 스크립트.

사용법:
    uv run test_presigned_upload.py [옵션]

옵션:
    --file PATH         업로드할 파일 경로 (기본값: 더미 1x1 PNG 자동 생성)
    --type TYPE         참조 타입 POST | PROFILE (기본값: POST)
    --base-url URL      서버 주소 (기본값: http://localhost:8080)
    --identifier ID     dev 로그인 식별자 (기본값: test-uploader)

예시:
    uv run test_presigned_upload.py
    uv run test_presigned_upload.py --file ~/Downloads/photo.jpg
    uv run test_presigned_upload.py --file ~/image.png --type PROFILE
    uv run test_presigned_upload.py --base-url http://localhost:9090
"""

import argparse
import mimetypes
import os
import sys
import tempfile

import requests

from auth import login

# 1x1 투명 PNG (바이너리 리터럴)
_DUMMY_PNG = bytes([
    0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
    0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
    0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
    0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, 0xC4,
    0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
    0x54, 0x78, 0x9C, 0x62, 0x00, 0x01, 0x00, 0x00,
    0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, 0xB4, 0x00,
    0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, 0xAE,
    0x42, 0x60, 0x82,
])

DIVIDER = "═" * 46


def _guess_content_type(file_path: str) -> str:
    mime, _ = mimetypes.guess_type(file_path)
    return mime or "application/octet-stream"


def _issue_presigned_url(
    base_url: str,
    access_token: str,
    file_name: str,
    content_type: str,
    file_size: int,
    reference_type: str,
) -> dict:
    """Presigned URL 발급 API를 호출하고 응답 data를 반환한다."""
    resp = requests.post(
        f"{base_url}/api/attachments/presigned-url",
        json={
            "fileName": file_name,
            "contentType": content_type,
            "fileSize": file_size,
            "referenceType": reference_type,
        },
        headers={"Authorization": f"Bearer {access_token}"},
    )

    if not resp.ok:
        raise RuntimeError(f"Presigned URL 발급 실패 (HTTP {resp.status_code}): {resp.text}")

    data = resp.json().get("data")
    if not data or not data.get("presignedUrl"):
        raise RuntimeError(f"응답에 presignedUrl이 없습니다: {resp.text}")

    return data


def _upload_to_s3(presigned_url: str, file_path: str, content_type: str) -> None:
    """presigned URL로 파일을 S3에 직접 PUT 업로드한다."""
    with open(file_path, "rb") as f:
        resp = requests.put(presigned_url, data=f, headers={"Content-Type": content_type})

    if not resp.ok:
        raise RuntimeError(f"S3 업로드 실패 (HTTP {resp.status_code})")


def main() -> None:
    parser = argparse.ArgumentParser(description="S3 Presigned URL 업로드 테스트")
    parser.add_argument("--file", help="업로드할 파일 경로 (생략 시 더미 PNG 자동 생성)")
    parser.add_argument("--type", dest="reference_type", default="POST", help="참조 타입 (기본값: POST)")
    parser.add_argument("--base-url", default="http://localhost:8080", help="서버 주소")
    parser.add_argument("--identifier", default="test-uploader", help="dev 로그인 식별자")
    args = parser.parse_args()

    # ── 파일 준비 ──────────────────────────────────────────────────────────
    cleanup_file = False
    file_path = args.file

    if not file_path:
        tmp = tempfile.NamedTemporaryFile(suffix=".png", delete=False)
        tmp.write(_DUMMY_PNG)
        tmp.close()
        file_path = tmp.name
        cleanup_file = True
        print(f"📄 더미 파일 생성: {file_path}")

    if not os.path.isfile(file_path):
        print(f"❌ 파일을 찾을 수 없습니다: {file_path}", file=sys.stderr)
        sys.exit(1)

    file_name = os.path.basename(file_path)
    file_size = os.path.getsize(file_path)
    content_type = _guess_content_type(file_path)

    print(f"\n{DIVIDER}")
    print("  S3 Presigned Upload 테스트")
    print(DIVIDER)
    print(f"  서버:      {args.base_url}")
    print(f"  파일:      {file_name} ({file_size} bytes, {content_type})")
    print(f"  참조타입:  {args.reference_type}")
    print(DIVIDER)

    try:
        # ── Step 1: 로그인 ────────────────────────────────────────────────
        print(f"\n▶ Step 1. Dev 로그인 ({args.identifier})...")
        access_token = login(args.base_url, args.identifier)
        print("   ✅ 로그인 성공 (accessToken 획득)")

        # ── Step 2: Presigned URL 발급 ────────────────────────────────────
        print("\n▶ Step 2. Presigned URL 발급 요청...")
        data = _issue_presigned_url(
            args.base_url, access_token,
            file_name, content_type, file_size,
            args.reference_type,
        )
        presigned_url = data["presignedUrl"]
        object_key = data["objectKey"]
        attachment_id = data["attachmentId"]
        print("   ✅ Presigned URL 발급 성공")
        print(f"   objectKey:    {object_key}")
        print(f"   attachmentId: {attachment_id}")

        # ── Step 3: S3 업로드 ─────────────────────────────────────────────
        print("\n▶ Step 3. S3 직접 업로드...")
        _upload_to_s3(presigned_url, file_path, content_type)
        print("   ✅ S3 업로드 성공 (HTTP 200)")

    except RuntimeError as e:
        print(f"\n❌ {e}", file=sys.stderr)
        sys.exit(1)
    finally:
        if cleanup_file:
            os.unlink(file_path)

    # ── 결과 출력 ─────────────────────────────────────────────────────────
    print(f"\n{DIVIDER}")
    print("  ✅ 업로드 완료")
    print(DIVIDER)
    print(f"  objectKey:    {object_key}")
    print(f"  attachmentId: {attachment_id}")
    print()
    print("  📌 게시물 content에 삽입할 형식:")
    print(f"     Markdown: ![이미지]({object_key})")
    print(f"     HTML:     <img src=\"{object_key}\" />")
    print(DIVIDER)


if __name__ == "__main__":
    main()
