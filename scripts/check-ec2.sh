#!/usr/bin/env bash
# Deoham 배포를 위한 EC2 서버 점검 스크립트
#
# 실행 방법 1) EC2에 들어가서:
#   bash check-ec2.sh
#
# 실행 방법 2) 로컬 맥에서 SSH로 한 번에:
#   ssh -i ~/path/to/deoham.pem ubuntu@54.180.33.194 'bash -s' < scripts/check-ec2.sh

set -u

OK="\033[0;32m[OK]\033[0m"
WARN="\033[0;33m[WARN]\033[0m"
FAIL="\033[0;31m[FAIL]\033[0m"

print_header() {
  echo ""
  echo "=== $1 ==="
}

echo "========================================="
echo "  Deoham EC2 서버 점검"
echo "  실행 시각: $(date)"
echo "========================================="

# 1) OS 정보
print_header "1. 시스템 정보"
uname -a
if [[ -f /etc/os-release ]]; then
  grep PRETTY_NAME /etc/os-release | cut -d= -f2 | tr -d '"'
fi

# 2) Docker 설치 및 권한
print_header "2. Docker"
if command -v docker &>/dev/null; then
  echo -e "$OK Docker 설치됨: $(docker --version)"
  if docker ps &>/dev/null; then
    echo -e "$OK ubuntu 유저로 docker 실행 가능 (sudo 불필요)"
  else
    echo -e "$FAIL ubuntu 유저가 docker 그룹에 없음. 다음 실행 후 재로그인 필요:"
    echo "    sudo usermod -aG docker ubuntu && exit"
  fi
else
  echo -e "$FAIL Docker 미설치. 설치 명령:"
  echo "    curl -fsSL https://get.docker.com | sh"
  echo "    sudo usermod -aG docker ubuntu"
fi

# 3) 디스크 공간 (이미지 + tar.gz 최소 2GB)
print_header "3. 디스크 공간"
df -h / | awk 'NR==1 || NR==2'
FREE_KB=$(df --output=avail / | tail -1)
FREE_GB=$((FREE_KB / 1024 / 1024))
if [[ "$FREE_GB" -lt 2 ]]; then
  echo -e "$FAIL 여유 공간 ${FREE_GB}GB - 최소 2GB 필요"
else
  echo -e "$OK 여유 공간 ${FREE_GB}GB"
fi

# 4) 메모리
print_header "4. 메모리"
free -h
TOTAL_MB=$(free -m | awk '/^Mem:/{print $2}')
if [[ "$TOTAL_MB" -lt 800 ]]; then
  echo -e "$WARN 총 메모리 ${TOTAL_MB}MB - Spring Boot에 부족할 수 있음 (1GB 이상 권장)"
else
  echo -e "$OK 총 메모리 ${TOTAL_MB}MB"
fi

# 5) 포트 8080
print_header "5. 포트 8080"
if command -v ss &>/dev/null; then
  if ss -ltn 2>/dev/null | grep -q ':8080 '; then
    echo -e "$WARN 포트 8080이 이미 사용 중:"
    ss -ltn | grep ':8080 '
  else
    echo -e "$OK 포트 8080 사용 가능"
  fi
fi

# 6) curl (헬스체크용)
print_header "6. curl"
if command -v curl &>/dev/null; then
  echo -e "$OK curl 설치됨: $(curl --version | head -1)"
else
  echo -e "$FAIL curl 미설치. 설치: sudo apt-get install -y curl"
fi

# 7) SSH authorized_keys
print_header "7. SSH 키"
if [[ -f ~/.ssh/authorized_keys ]]; then
  COUNT=$(grep -c '^ssh-' ~/.ssh/authorized_keys 2>/dev/null || echo 0)
  echo -e "$OK ~/.ssh/authorized_keys 존재 (등록된 키 ${COUNT}개)"
else
  echo -e "$FAIL ~/.ssh/authorized_keys 없음 - GitHub Actions에서 SSH 접속 불가"
fi

# 8) 외부 네트워크 (이미지 pull 등에 필요)
print_header "8. 외부 네트워크"
if curl -s --max-time 5 https://registry-1.docker.io/v2/ -o /dev/null; then
  echo -e "$OK Docker Hub 접근 가능"
else
  echo -e "$WARN Docker Hub 접근 불가 (보안그룹 아웃바운드 확인)"
fi

# 9) 현재 deoham 컨테이너 상태
print_header "9. 현재 deoham 컨테이너 상태"
if command -v docker &>/dev/null && docker ps &>/dev/null; then
  if docker ps -a --filter "name=deoham" --format "{{.Names}}" | grep -q '^deoham$'; then
    docker ps -a --filter "name=deoham" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    echo ""
    echo "--- 최근 로그 (마지막 20줄) ---"
    docker logs deoham --tail=20 2>&1 || true
    echo ""
    echo "--- 헬스체크 ---"
    if curl -sf http://localhost:8080/actuator/health; then
      echo ""
      echo -e "$OK 애플리케이션 정상 동작 중"
    else
      echo -e "$WARN /actuator/health 응답 없음"
    fi
  else
    echo "(deoham 컨테이너 없음 - 첫 배포 전이라면 정상)"
  fi
fi

# 10) Docker 이미지 목록
print_header "10. Docker 이미지"
if command -v docker &>/dev/null && docker ps &>/dev/null; then
  docker images --filter "reference=deoham-app" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"
fi

# 11) .env 파일
print_header "11. ~/deoham.env"
if [[ -f ~/deoham.env ]]; then
  ENV_LINES=$(wc -l < ~/deoham.env)
  echo -e "$OK ~/deoham.env 존재 (${ENV_LINES}줄)"
  echo "키 목록:"
  cut -d= -f1 ~/deoham.env | sed 's/^/  /'
else
  echo "(~/deoham.env 없음 - 첫 배포 전이라면 정상)"
fi

echo ""
echo "========================================="
echo "  점검 완료"
echo "========================================="
