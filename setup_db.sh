#!/bin/bash
sudo mysql -e "CREATE DATABASE IF NOT EXISTS mailserver; CREATE USER IF NOT EXISTS 'mailuser'@'localhost' IDENTIFIED BY 'StrongPassword'; GRANT ALL PRIVILEGES ON mailserver.* TO 'mailuser'@'localhost'; FLUSH PRIVILEGES;"
echo "Database setup complete."
