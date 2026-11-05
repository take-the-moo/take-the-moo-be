#!/usr/bin/env bash
#
# PostToolUse(Edit|Write) 훅 — .java 파일이 바뀌면 바로 컴파일해 본다.
# 실패하면 exit 2 + stderr로 컴파일 오류를 Claude에게 돌려줘 즉시 고치게 한다.
# 입력: stdin JSON ({"tool_input": {"file_path": "..."}, ...})
#

set -u

input="$(cat)"

if command -v jq &>/dev/null; then
    file_path="$(printf '%s' "$input" | jq -r '.tool_input.file_path // empty')"
else
    file_path="$(printf '%s' "$input" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("tool_input",{}).get("file_path",""))' 2>/dev/null)"
fi

case "$file_path" in
    *.java) ;;
    *) exit 0 ;;
esac

cd "${CLAUDE_PROJECT_DIR:-$(dirname "$0")/../..}" || exit 0

if output="$(./gradlew compileJava -q 2>&1)"; then
    exit 0
fi

{
    echo "[compile-check] ./gradlew compileJava 실패 — ${file_path} 수정 후 컴파일 오류가 났습니다. 고친 뒤 계속하세요:"
    printf '%s\n' "$output" | grep -vE '^WARNING:' | head -40
} >&2
exit 2
