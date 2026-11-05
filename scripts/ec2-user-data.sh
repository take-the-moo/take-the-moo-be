#!/bin/bash
# EC2(Ubuntu 24.04) 첫 부팅 스크립트 — 인스턴스 시작 시 "고급 세부 정보 > 사용자 데이터"에 통째로 붙여넣는다.
# 하는 일: 서울 시간대, Docker + compose 플러그인, rsync 설치, 빌드용 스왑 2GB.
# 실행 로그: /var/log/cloud-init-output.log
set -euxo pipefail
export DEBIAN_FRONTEND=noninteractive

timedatectl set-timezone Asia/Seoul

apt-get update
apt-get install -y ca-certificates curl rsync

# Docker 공식 저장소 (Ubuntu 기본 docker.io는 compose 플러그인이 없다)
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  > /etc/apt/sources.list.d/docker.list
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
systemctl enable --now docker
usermod -aG docker ubuntu

# 이미지 빌드(Gradle + JDK 25)가 메모리를 많이 써서 스왑을 둔다
if [ ! -f /swapfile ]; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

echo "user-data done"
