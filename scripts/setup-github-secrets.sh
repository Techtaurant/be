#!/usr/bin/env bash
# GitHub Actions 시크릿을 .env.secrets 파일에서 읽어 자동으로 등록한다.
# 사용법: ./scripts/setup-github-secrets.sh
set -euo pipefail

ENV_FILE="${1:-.env}"
REPO=$(git remote get-url origin | sed 's|https://github.com/||' | sed 's|git@github.com:||' | sed 's|\.git$||')

# 의존성 확인
if ! command -v gh &>/dev/null; then
  echo "gh CLI가 설치되어 있지 않습니다. https://cli.github.com 참고"
  exit 1
fi
if ! gh auth status &>/dev/null; then
  echo "gh CLI 로그인이 필요합니다: gh auth login"
  exit 1
fi
if [[ ! -f "$ENV_FILE" ]]; then
  echo ".env.secrets 파일이 없습니다. .env.secrets.example을 복사해서 값을 채워주세요."
  echo "  cp .env.secrets.example .env.secrets"
  exit 1
fi

echo "저장소: $REPO"
echo "시크릿 파일: $ENV_FILE"
echo ""

# KEY에 해당하는 값을 .env.secrets에서 읽음
get_val() {
  local key="$1"
  local raw
  raw=$(grep "^${key}=" "$ENV_FILE" 2>/dev/null | head -1 | cut -d'=' -f2- || true)
  # 따옴표 제거
  raw="${raw%\"}"
  raw="${raw#\"}"
  raw="${raw%\'}"
  raw="${raw#\'}"
  echo "$raw"
}

set_secret() {
  local key="$1"
  local value
  value=$(get_val "$key")
  if [[ -z "$value" ]]; then
    echo "  [SKIP] $key (값이 비어있음)"
    return
  fi
  echo "  [SET]  $key"
  printf '%s' "$value" | gh secret set "$key" --repo "$REPO" --body -
}

set_secret_from_file() {
  local key="$1"
  local path
  path=$(get_val "$2")
  if [[ -z "$path" ]]; then
    echo "  [SKIP] $key (경로가 비어있음)"
    return
  fi
  local expanded_path="${path/#\~/$HOME}"
  if [[ ! -f "$expanded_path" ]]; then
    echo "  [SKIP] $key (파일 없음: $expanded_path)"
    return
  fi
  echo "  [SET]  $key (from $expanded_path)"
  gh secret set "$key" --repo "$REPO" < "$expanded_path"
}

echo "=== AWS 시크릿 ==="
set_secret "AWS_ACCESS_KEY_ID"
set_secret "AWS_SECRET_ACCESS_KEY"

echo ""
echo "=== SSH 시크릿 ==="
set_secret           "DEV_SSH_HOST"
set_secret           "DEV_SSH_USER"
set_secret           "DEV_SSH_PORT"
set_secret_from_file "DEV_SSH_PRIVATE_KEY" "DEV_SSH_PRIVATE_KEY_PATH"

echo ""
echo "완료. 현재 등록된 시크릿 목록:"
gh secret list --repo "$REPO"
