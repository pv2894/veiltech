# VeilTech Demo (Android + FastAPI on AWS EC2)

## 1) Android Project

`android-app/` remains Kotlin-based with MVVM-style layers, Material 3 dark theme, BiometricPrompt gates, Retrofit, Coroutines, SHA-256 hashing before transit, secure JWT storage, and local AES-256 file encryption using Android Keystore.

## 2) Backend Project (Converted from Spring Boot to FastAPI)

```
backend/
  app/
    main.py
    config.py
    db.py
    models.py
    schemas.py
    security.py
    deps.py
    routers/
      auth.py
      users.py
      requests.py
      files.py
```

### Tech stack
- Python 3.11+
- FastAPI
- SQLAlchemy
- MySQL (PyMySQL)
- JWT (`python-jose`)
- bcrypt hashing (`passlib`)

### APIs
- `POST /register`
- `POST /login`
- `GET /users`
- `POST /request`
- `POST /accept`
- `GET /requests/{userId}`
- `POST /upload`
- `GET /file/{sessionId}`
- `GET /fileInfo/{sessionId}`
- `POST /verifyPin`
- `GET /health`

### Security logic implemented
- JWT auth required on all secured endpoints.
- Request lifecycle: `PENDING -> ACCEPTED/EXPIRED` with 5-minute expiry.
- File session stores `pin_hash` (client sends SHA-256) and locks after 3 wrong attempts.
- Upload validation enforces allowed file types and `actual <= masked` size.
- File storage path: `/var/veiltech/uploads/{sessionId}/`.

## 3) Database schema (MySQL)

Use this SQL on EC2 MySQL:

```sql
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    status ENUM('PENDING','ACCEPTED','EXPIRED') NOT NULL,
    expiry_time DATETIME NOT NULL,
    FOREIGN KEY (sender_id) REFERENCES users(id),
    FOREIGN KEY (receiver_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_id BIGINT NOT NULL,
    masked_file_path VARCHAR(500) NOT NULL,
    encrypted_file_path VARCHAR(500) NOT NULL,
    pin_hash VARCHAR(255) NOT NULL,
    expiry_time DATETIME NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (request_id) REFERENCES requests(id)
);
```

## 4) EC2 deployment guide (FastAPI)

1. Create Ubuntu EC2 instance.
2. Security group inbound: `22`, `8080` (and optionally `3306` for admin only).
   - If `8080` is already used by another app on the same EC2 instance, use another port for VeilTech (for example `8081`) and open that port in the security group.
3. Install Python + pip + venv:
   ```bash
   sudo apt update
   sudo apt install -y python3 python3-pip python3-venv mysql-server
   ```
4. Setup MySQL:
   ```bash
   sudo mysql -e "CREATE DATABASE veiltech;"
   sudo mysql -e "CREATE USER 'veiltech'@'localhost' IDENTIFIED BY 'veiltech123';"
   sudo mysql -e "GRANT ALL PRIVILEGES ON veiltech.* TO 'veiltech'@'localhost'; FLUSH PRIVILEGES;"
   ```
5. Prepare uploads folder:
   ```bash
   sudo mkdir -p /var/veiltech/uploads
   sudo chown -R ubuntu:ubuntu /var/veiltech
   ```
6. Backend setup:
   ```bash
   cd backend
   python3 -m venv .venv
   source .venv/bin/activate
   pip install -r requirements.txt
   export VEILTECH_DB_URL="mysql+pymysql://veiltech:veiltech123@localhost:3306/veiltech"
   export VEILTECH_JWT_SECRET="your-long-demo-secret"
   export VEILTECH_UPLOADS_ROOT="/var/veiltech/uploads"
   # default
   uvicorn app.main:app --host 0.0.0.0 --port 8080

   # if 8080 is already occupied by another app on the same EC2
   uvicorn app.main:app --host 0.0.0.0 --port 8081
   ```
7. In Android app, replace `http://YOUR_EC2_PUBLIC_IP:8080/` with actual EC2 public IP and matching backend port.
   - Example when reusing the server with another app: `http://YOUR_EC2_PUBLIC_IP:8081/`.

## 5) Testing guide

```bash
curl http://<EC2_IP>:8080/health
curl -X POST http://<EC2_IP>:8080/register -H 'Content-Type: application/json' -d '{"name":"A","phone":"111","passwordHash":"<sha256>"}'
curl -X POST http://<EC2_IP>:8080/login -H 'Content-Type: application/json' -d '{"phone":"111","passwordHash":"<sha256>"}'
```

Android manual checks:
- biometric required at launch and receiver open
- wrong PIN 3 times locks session
- request expires after 5 minutes
- reject actual file larger than masked file
- network/server-down toasts

## 6) College demo script

1. Explain architecture: Android secure client + FastAPI on EC2.
2. Show biometric gate at app launch.
3. Register and login (password hash sent, JWT returned).
4. Send connection request and poll receiver status.
5. Upload masked + AES-encrypted actual file with PIN and expiry.
6. Receiver authenticates biometrically, enters PIN, verifies unlock behavior.
7. Demonstrate 3 wrong PIN lock and expiry handling.
