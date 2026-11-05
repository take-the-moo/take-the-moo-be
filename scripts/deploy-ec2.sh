#!/usr/bin/env bash
# 로컬 소스(application.yml 포함)를 EC2로 보내고 docker compose로 빌드·실행한다. 재배포도 같은 명령.
# 사용법:
#   scripts/deploy-ec2.sh ubuntu@<EC2 공인 IP> [-i ~/.ssh/키.pem]
#   LLM_MOCK=true scripts/deploy-ec2.sh ubuntu@<IP> -i ~/.ssh/키.pem   # 목 모드로 띄우기
# 전제: 인스턴스가 scripts/ec2-user-data.sh로 준비됐고(Docker·rsync), 보안 그룹에 22·80·443이 열려 있다. 자세한 건 docs/deploy.md
set -euo pipefail

cd "$(dirname "$0")/.."

TARGET="${1:?사용법: scripts/deploy-ec2.sh ubuntu@<EC2 IP> [-i 키.pem]}"
shift
SSH_OPTS=(-o StrictHostKeyChecking=accept-new "$@")
REMOTE_DIR="app"
HOST="${TARGET#*@}"

if [[ ! -f src/main/resources/application.yml ]]; then
  echo "src/main/resources/application.yml이 없습니다. 키가 든 설정 파일을 팀원에게 받아 두세요." >&2
  exit 1
fi

echo "==> 소스 전송: $TARGET:~/$REMOTE_DIR (application.yml 포함, .git·build 제외)"
rsync -az --delete \
  --exclude .git --exclude .gradle --exclude build --exclude .idea --exclude .claude \
  --exclude uploads --exclude '.env*' --exclude '.DS_Store' \
  -e "ssh ${SSH_OPTS[*]}" ./ "$TARGET:$REMOTE_DIR/"

# LLM_MOCK은 값이 있을 때만 넘긴다 (빈 문자열을 넘기면 llm.mock 바인딩이 깨진다)
REMOTE_ENV=""
if [[ -n "${LLM_MOCK:-}" ]]; then
  REMOTE_ENV="LLM_MOCK=$LLM_MOCK "
fi

# 로그인 JWT 서명 키: 서버의 ~/app/.env에 한 번 만들고 계속 쓴다 (rsync가 .env*를 건드리지 않는다)
# 없으면 서버가 켜질 때마다 임시 키를 만들어 재배포·재시작 때마다 모두 로그아웃된다
ssh "${SSH_OPTS[@]}" "$TARGET" "cd $REMOTE_DIR && touch .env && chmod 600 .env && (grep -q '^APP_AUTH_JWT_SECRET=' .env || echo \"APP_AUTH_JWT_SECRET=\$(openssl rand -base64 48 | tr -d '\\n')\" >> .env)"

# HTTPS: compose.prod.yml(Caddy)을 얹고, 도메인은 IP로 만든 sslip.io 주소를 쓴다 (도메인 구입 없이 Let's Encrypt 인증서)
# 이미 있으면 그대로 둔다 — 도메인을 사면 서버 .env의 DOMAIN만 바꾼다
DOMAIN_DEFAULT="$(echo "$HOST" | tr . -).sslip.io"
ssh "${SSH_OPTS[@]}" "$TARGET" "cd $REMOTE_DIR && (grep -q '^COMPOSE_FILE=' .env || echo 'COMPOSE_FILE=compose.yml:compose.prod.yml' >> .env) && (grep -q '^DOMAIN=' .env || echo 'DOMAIN=$DOMAIN_DEFAULT' >> .env)"
DOMAIN="$(ssh "${SSH_OPTS[@]}" "$TARGET" "grep '^DOMAIN=' $REMOTE_DIR/.env | cut -d= -f2")"

echo "==> 빌드·실행 (첫 빌드는 5분 정도 걸린다)"
ssh "${SSH_OPTS[@]}" "$TARGET" "cd $REMOTE_DIR && ${REMOTE_ENV}docker compose up -d --build --remove-orphans"

echo "==> 헬스체크: https://$DOMAIN/api/health (첫 배포는 인증서 발급에 몇십 초 더 걸린다)"
for _ in $(seq 1 36); do
  if curl -fsS --max-time 5 "https://$DOMAIN/api/health"; then
    echo
    echo "완료. API: https://$DOMAIN  ·  Swagger: https://$DOMAIN/swagger-ui.html"
    exit 0
  fi
  sleep 5
done

echo "3분 안에 응답이 없습니다. 로그: ssh ${SSH_OPTS[*]} $TARGET 'cd $REMOTE_DIR && docker compose logs --tail=100 api'" >&2
exit 1
