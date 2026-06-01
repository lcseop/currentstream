# EC2 배포 (DB / Backend 분리)

## 구조

| EC2 | 파일 | 실행 |
|-----|------|------|
| **DB** | `docker-compose.yml` | MySQL만 |
| **Backend** | `docker-compose.backend.yml` + `Dockerfile` + 소스 | Spring Boot만 |

Backend → DB 접속: `DB_HOST`에 **DB EC2 private IP** (보안 그룹 3306은 Backend SG만 허용)

---

## 1. DB EC2

```bash
cd current-stream-backend
cp .env.example .env
# 비밀번호 수정 (DB_PASSWORD 등)

docker compose up -d
docker compose ps
```

컨테이너가 DB·유저를 자동 생성합니다 (`MYSQL_DATABASE`, `DB_USERNAME`, `DB_PASSWORD`).  
수동 SQL은 보통 필요 없습니다.

로컬 확인:

```bash
docker exec -it currentstream-mysql mysql -u cs_user -p
```

---

## 2. Backend EC2

필요 파일: `Dockerfile`, `docker-compose.backend.yml`, `build.gradle`, `gradlew`, `gradle/`, `src/`, `secrets/firebase/serviceAccountKey.json`

```bash
cd current-stream-backend
cp .env.backend.example .env
# .env 에 DB_HOST=DB서버_private_IP, DB_PASSWORD=DB와 동일

docker compose -f docker-compose.backend.yml up -d --build
docker compose -f docker-compose.backend.yml logs -f backend
```

API: `http://<Backend공인IP>:8080`  
Android `API_BASE_URL`도 이 주소로 변경.

---

## 3. 보안 그룹

| 인스턴스 | 인바운드 |
|----------|----------|
| DB | TCP 3306 ← Backend EC2 보안 그룹 |
| Backend | TCP 8080 ← 테스트용(본인 IP) 또는 필요 범위 |

---

## 4. 로컬 개발 (PC)

MySQL을 PC에 두고 `./gradlew bootRun` → `application.yaml` 기본값 (`localhost:3306`) 사용.  
Docker 분리 compose는 EC2용입니다.
