#!/usr/bin/env bash
set -e

cd "$(dirname "$0")/.."

if ! git rev-parse --is-inside-work-tree &>/dev/null; then
    echo "git 저장소가 아닙니다. git init 후 다시 실행하세요." >&2
    exit 1
fi

git config core.hooksPath .githooks
chmod +x .githooks/*
chmod +x .claude/hooks/*.sh 2>/dev/null || true

echo "완료: .githooks를 Git hook 경로로 설정했습니다."
echo "설정된 hook:"
for hook in .githooks/*; do
    echo "  - $(basename "$hook")"
done
echo "pre-commit: .env 커밋 차단 + API 키(sk-ant-) 차단 + 설정 파일 리터럴 시크릿 차단"
