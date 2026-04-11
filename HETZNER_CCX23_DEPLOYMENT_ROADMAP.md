# Project Analysis & Hetzner CCX23 Deployment Roadmap

This document provides a comprehensive analysis of the **BTC Tech Mail Server (BNX-MAIL-BE)** and a detailed, step-by-step roadmap for deploying it on a **Hetzner CCX23** (Dedicated vCPU) instance.

---

## 1. Project Analysis

### 1.1 Technology Stack
- **Framework**: Spring Boot 3.2.3 (Java 17)
- **Database**: MySQL 8.x (Primary), H2 (Development/Test)
- **Security**: Spring Security + JWT (JSON Web Token)
- **Mail Protocols**: SMTP (outgoing), IMAP (incoming)
- **Utilities**: 
  - `Thumbnailator`: Image processing for previews.
  - `Jakarta Mail`: Core mail handling.
  - `ClamAV`: Virus scanning for mail attachments.

### 1.2 Infrastructure Requirements
- **Server**: Hetzner CCX23 (4 Dedicated vCPUs, 16GB RAM, 160GB NVMe)
- **Operating System**: Ubuntu 24.04 LTS (Recommended)
- **Local Services**:
  - **Postfix**: MTA (Mail Transfer Agent) for SMTP.
  - **Dovecot**: MDA (Mail Delivery Agent) for IMAP.
  - **MySQL Server**: Data storage for accounts, metadata, and contacts.
  - **ClamAV**: Scanning engine on port 3310.
  - **Nginx**: Reverse proxy for the API and SSL termination.

### 1.3 Directory Structure Requirements
- **Mail Storage**: `/var/mail/vmail` (owner: `vmail`)
- **Application Logs**: `/opt/mailapp/logs/`
- **Application Binary**: `/opt/mailapp/bin/`

---

## 2. Deployment Roadmap (Step-by-Step)

### Phase 1: Server Provisioning
1. **Create Instance**: Log in to Hetzner Cloud and create a new server.
   - **Type**: Dedicated vCPU (`ccx23`).
   - **OS**: Ubuntu 24.04.
   - **Location**: Choose nearest to your users.
2. **SSH Setup**: Add your SSH key for secure access.
3. **Internal Networking**: (Optional) Enable if you plan to use a separate DB server.

### Phase 2: Basic Security & OS Update
```bash
# Update System
sudo apt update && sudo apt upgrade -y

# Install Basic Tools
sudo apt install -y curl wget git UFW

# Setup Firewall
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw allow 25/tcp    # SMTP
sudo ufw allow 587/tcp   # Submission
sudo ufw allow 993/tcp   # IMAPS
sudo ufw enable
```

### Phase 3: Install Core Dependencies
#### 3.1 Java 17 (OpenJDK)
```bash
sudo apt install -y openjdk-17-jdk
java -version
```

#### 3.2 MySQL 8.x
```bash
sudo apt install -y mysql-server
sudo mysql_secure_installation
```

#### 3.3 ClamAV (Virus Scanner)
```bash
sudo apt install -y clamav clamav-daemon
# Enable network daemon for Spring Boot to connect (port 3310)
sudo systemctl stop clamav-daemon
sudo sed -i 's/^LocalSocket .*/# LocalSocket \/var\/run\/clamav\/clamd.ctl/' /etc/clamav/clamd.conf
echo "TCPSocket 3310" | sudo tee -a /etc/clamav/clamd.conf
echo "TCPAddr 127.0.0.1" | sudo tee -a /etc/clamav/clamd.conf
sudo systemctl start clamav-daemon
```

### Phase 4: Mail Stack Setup (Postfix & Dovecot)
> [!IMPORTANT]
> Configuring a production mail server is complex. Below are the essential installation steps.

1. **Install Postfix & Dovecot**:
   ```bash
   sudo apt install -y postfix dovecot-core dovecot-imapd
   ```
2. **Configure Virtual Mail Storage**:
   ```bash
   sudo groupadd -g 5000 vmail
   sudo useradd -g vmail -u 5000 vmail -d /var/mail/vmail -m
   sudo chown -R vmail:vmail /var/mail/vmail
   ```
3. **Update Domain**: Ensure your domain (`btctech.shop`) is set in `/etc/postfix/main.cf`.

### Phase 5: Database Setup
1. **Create Database & User**:
   ```bash
   sudo mysql -u root -p
   # Inside MySQL:
   CREATE DATABASE mail_app_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'mailuser'@'localhost' IDENTIFIED BY 'MailUser@2026Secure';
   GRANT ALL PRIVILEGES ON mail_app_db.* TO 'mailuser'@'localhost';
   FLUSH PRIVILEGES;
   EXIT;
   ```
2. **Import Schema**:
   Upload `db_schema.sql` to the server and run:
   ```bash
   mysql -u mailuser -p mail_app_db < db_schema.sql
   ```

### Phase 6: Application Deployment
1. **Build Locally**:
   ```bash
   mvn clean package -DskipTests
   ```
2. **Transfer to Server**:
   ```bash
   scp target/mailapp-1.0.0.jar root@YOUR_IP:/opt/mailapp/bin/
   ```
3. **Create Systemd Service**:
   Create `/etc/systemd/system/mailapp.service`:
   ```ini
   [Unit]
   Description=BTC Tech Mail Server
   After=mysql.service clamav-daemon.service

   [Service]
   User=root
   WorkingDirectory=/opt/mailapp/bin
   ExecStart=/usr/bin/java -jar mailapp-1.0.0.jar
   Restart=always
   StandardOutput=syslog
   StandardError=syslog
   SyslogIdentifier=mailapp

   [Install]
   WantedBy=multi-user.target
   ```
4. **Start Service**:
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable mailapp
   sudo systemctl start mailapp
   ```

### Phase 7: Reverse Proxy & SSL (Nginx)
1. **Install Nginx & Certbot**:
   ```bash
   sudo apt install -y nginx certbot python3-certbot-nginx
   ```
2. **Nginx Configuration**:
   Create `/etc/nginx/sites-available/btctech.shop`:
   ```nginx
   server {
       listen 80;
       server_name api.btctech.shop;

       location / {
           proxy_pass http://localhost:8080;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
           proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
       }
   }
   ```
3. **Enable & SSL**:
   ```bash
   sudo ln -s /etc/nginx/sites-available/btctech.shop /etc/nginx/sites-enabled/
   sudo nginx -t && sudo systemctl restart nginx
   sudo certbot --nginx -d api.btctech.shop
   ```

### Phase 8: DNS Configuration (CRITICAL)
For mail to work and not be marked as spam, you MUST configure your DNS provider:
- **A Record**: `api.btctech.shop` -> YOUR_IP
- **MX Record**: `@` -> `api.btctech.shop` (Priority 10)
- **SPF (TXT)**: `v=spf1 ip4:YOUR_IP -all`
- **DKIM (TXT)**: Generate via OpenDKIM and add to DNS.
- **DMARC (TXT)**: `v=DMARC1; p=none; rua=mailto:admin@btctech.shop`
- **PTR (Reverse DNS)**: Set in Hetzner Cloud Console for your IP to match `api.btctech.shop`.

---

## 3. Post-Deployment Checklist
- [ ] Check application logs: `tail -f /opt/mailapp/logs/application.log`
- [ ] Verify ClamAV connectivity on port 3310.
- [ ] Test API login using JWT.
- [ ] Send a test email and verify it reaches the destination.
- [ ] Check [Mail-Tester](https://www.mail-tester.com/) for deliverability score.
