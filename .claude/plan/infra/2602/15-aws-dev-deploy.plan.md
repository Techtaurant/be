# AWS Dev Server Deployment with Terraform

## Business Goal
Spring Boot(Kotlin) Dev 서버를 AWS EC2에 Terraform으로 배포. Docker Compose로 앱 + PostgreSQL + Caddy(HTTPS) 실행. GitHub Actions CI/CD로 ECR push + SSH 배포 자동화.

## Scope
- **In Scope**: Terraform(VPC, SG, EC2, EIP, ECR), Docker Compose(App + PostgreSQL + Caddy), GitHub Actions CD 수정
- **Out of Scope**: Prod 환경, RDS, ElastiCache, Route53, 모니터링/알림

## Codebase Analysis Summary
- Spring Boot 3.5.7 + Kotlin, Java 17, PostgreSQL, Caffeine Cache (Redis 미사용)
- 기존 Dockerfile: multi-stage build (eclipse-temurin:17-jdk → 17-jre)
- 기존 docker-compose: app + PostgreSQL + Redis (infra/docker-compose.yml)
- 기존 CD: SSH + git pull + docker compose build (cd-dev.yml)
- 환경변수: DB, OAuth, JWT, CORS, Cookie, Swagger 등 다수

### Relevant Files
| File | Role | Action |
|------|------|--------|
| `terraform/environments/dev/main.tf` | Provider, backend 설정 | Create |
| `terraform/environments/dev/variables.tf` | 변수 선언 | Create |
| `terraform/environments/dev/terraform.tfvars.example` | 변수 예시 | Create |
| `terraform/environments/dev/outputs.tf` | 출력값 | Create |
| `terraform/environments/dev/vpc.tf` | VPC, Subnet, IGW | Create |
| `terraform/environments/dev/security-groups.tf` | Security Groups | Create |
| `terraform/environments/dev/ec2.tf` | EC2, EIP, Key Pair | Create |
| `terraform/environments/dev/ecr.tf` | ECR Repository | Create |
| `terraform/environments/dev/templates/user-data.sh` | EC2 초기화 | Create |
| `terraform/environments/dev/templates/docker-compose.yml` | Docker Compose | Create |
| `terraform/environments/dev/templates/Caddyfile` | Caddy 설정 | Create |
| `.github/workflows/cd-dev.yml` | CD 워크플로우 | Modify |
| `.gitignore` | tfstate, tfvars 제외 | Modify |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| UTF-8 인코딩 | FILE_WRITE_PRINCIPLES.md | 한글 포함 파일은 heredoc 사용 |
| 네이밍 | CODE_PRINCIPLES.md | 명확한 이름, 축약어 지양 |
| HCL 스타일 | Terraform 표준 | 2-space indent, snake_case |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| 컴퓨팅 | EC2 t4g.small | Dev 최저 비용, 프리티어 | ECS Fargate (비쌈), EB (Terraform 호환 나쁨) |
| DB | Docker PostgreSQL | Dev 데이터 손실 무관, RDS는 Prod 전용 | RDS ($15-20/mo 추가) |
| HTTPS | Caddy | 자동 Let's Encrypt, 설정 간단 | Nginx + Certbot (설정 복잡) |
| CI/CD | ECR + SSH | 서버 빌드 부담 없음, 깔끔한 분리 | git pull + build (서버 부담) |
| State | Local | 1인 개발, 설정 간단 | S3 Backend (팀 협업 시) |
| VPC | 단일 public subnet | Dev 서버, 심플 | Multi-AZ (과도) |

## Implementation Todos

### Todo 1: Terraform 기본 구조 + Provider 설정
- **Priority**: 1
- **Dependencies**: none
- **Goal**: Terraform 디렉터리 구조 생성 및 AWS provider 설정
- **Work**:
  - `terraform/environments/dev/main.tf` 생성: AWS provider (ap-northeast-2), local backend, required_providers
  - `terraform/environments/dev/variables.tf` 생성: project_name, environment, aws_region, ec2_instance_type, ec2_key_name, domain_name, db_name, db_username, db_password, app_env_vars 등
  - `terraform/environments/dev/terraform.tfvars.example` 생성: 변수 예시값
  - `terraform/environments/dev/outputs.tf` 생성: elastic_ip, ec2_instance_id, ecr_repository_url
  - `.gitignore`에 `*.tfstate*`, `*.tfvars`, `.terraform/` 추가
- **Convention Notes**: HCL 2-space indent, snake_case 변수명
- **Verification**: `terraform init` 성공
- **Exit Criteria**: terraform init이 오류 없이 완료
- **Status**: pending

### Todo 2: VPC + Network 구성
- **Priority**: 1
- **Dependencies**: none
- **Goal**: EC2가 배치될 VPC, Public Subnet, Internet Gateway 구성
- **Work**:
  - `terraform/environments/dev/vpc.tf` 생성
  - VPC: 10.0.0.0/16 CIDR
  - Public Subnet: 10.0.1.0/24 (ap-northeast-2a)
  - Internet Gateway + Route Table (0.0.0.0/0 → IGW)
  - Route Table Association
- **Convention Notes**: 리소스 이름에 project_name, environment 포함
- **Verification**: terraform plan에서 VPC 리소스 정상 출력
- **Exit Criteria**: VPC, Subnet, IGW, Route Table 리소스 정의 완료
- **Status**: pending

### Todo 3: Security Groups 구성
- **Priority**: 1
- **Dependencies**: none
- **Goal**: EC2 인바운드/아웃바운드 규칙 설정
- **Work**:
  - `terraform/environments/dev/security-groups.tf` 생성
  - EC2 SG: 인바운드 SSH(22), HTTP(80), HTTPS(443) / 아웃바운드 all
  - SSH 접근은 변수로 CIDR 제한 가능하게 (기본: 0.0.0.0/0)
- **Convention Notes**: SG 이름에 역할 명시 (e.g., dev-ec2-sg)
- **Verification**: terraform plan 정상
- **Exit Criteria**: Security Group 리소스 정의 완료
- **Status**: pending

### Todo 4: ECR Repository 생성
- **Priority**: 1
- **Dependencies**: none
- **Goal**: Docker 이미지 저장소 생성
- **Work**:
  - `terraform/environments/dev/ecr.tf` 생성
  - ECR Repository (techtaurant-main-server)
  - 이미지 태그 mutability: MUTABLE (dev 환경, latest 태그 덮어쓰기)
  - Lifecycle policy: 최근 5개만 유지 (비용 절약)
- **Convention Notes**: repository 이름 lowercase
- **Verification**: terraform plan 정상
- **Exit Criteria**: ECR 리소스 정의 완료
- **Status**: pending

### Todo 5: EC2 + Elastic IP + Templates 구성
- **Priority**: 2
- **Dependencies**: Todo 1, 2, 3
- **Goal**: EC2 인스턴스, Elastic IP, Docker Compose 템플릿 생성
- **Work**:
  - `terraform/environments/dev/ec2.tf` 생성
    - EC2 instance: t4g.small, Amazon Linux 2023 ARM64 AMI
    - Elastic IP + Association
    - IAM Role + Instance Profile (ECR pull 권한)
    - user_data로 초기화 스크립트 실행
  - `terraform/environments/dev/templates/user-data.sh` 생성
    - Docker + Docker Compose 설치
    - AWS CLI 설치 (ECR 로그인용)
    - docker-compose.yml, Caddyfile 배치
    - systemd 서비스 등록 (부팅 시 자동 시작)
  - `terraform/environments/dev/templates/docker-compose.yml` 생성
    - ECR에서 앱 이미지 pull
    - PostgreSQL 18 alpine
    - Caddy (reverse proxy + auto SSL)
    - 환경변수 templatefile로 주입
  - `terraform/environments/dev/templates/Caddyfile` 생성
    - dev-api.techtaurant.com → localhost:8080 reverse proxy
    - 자동 HTTPS (Let's Encrypt)
- **Convention Notes**: templatefile() 사용, 변수는 ${} 형태
- **Verification**: terraform plan에서 EC2, EIP 리소스 정상 출력
- **Exit Criteria**: EC2 + EIP + 모든 템플릿 파일 생성 완료
- **Status**: pending

### Todo 6: GitHub Actions CD 수정
- **Priority**: 2
- **Dependencies**: Todo 4
- **Goal**: ECR push + SSH deploy 방식으로 CD 워크플로우 변경
- **Work**:
  - `.github/workflows/cd-dev.yml` 수정
    - AWS credentials 설정 (OIDC 또는 access key)
    - ECR 로그인
    - Docker build (main-server/Dockerfile)
    - ECR push (latest 태그)
    - SSH로 EC2 접속
    - ECR 로그인 + docker compose pull + docker compose up -d
  - 필요한 GitHub Secrets 목록 문서화 (outputs.tf 주석 또는 README)
- **Convention Notes**: 기존 워크플로우 구조 유지, steps만 변경
- **Verification**: YAML lint 통과, 워크플로우 문법 정상
- **Exit Criteria**: cd-dev.yml이 ECR + SSH 배포 방식으로 변경 완료
- **Status**: pending

## Verification Strategy
- `terraform init` 성공
- `terraform validate` 성공
- `terraform plan` 정상 실행 (실제 apply는 사용자가 수행)
- cd-dev.yml YAML 문법 검증

## Progress Tracking
- Total Todos: 6
- Completed: 0
- Status: Planning complete

## Change Log
- 2026-02-15: Plan created
