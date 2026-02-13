# How to Run on Hostinger VPS

## Prerequisites
- **Java 17** installed on the VPS.
- **MySQL/MariaDB** running (which you have configured).
- **Postfix** and **Dovecot** installed and running (for mail services).

## Steps

1.  **Build the Application (Locally)**
    Run the following command in your project directory to create the JAR file:
    ```bash
    mvn clean package -DskipTests
    ```

2.  **Upload the JAR**
    Upload the generated JAR file from `target/MailApplication-Backend-0.0.1-SNAPSHOT.jar` to your VPS (e.g., to `/home/vps-user/mailapp/`).

3.  **Run the Application**
    SSH into your VPS and run the application:
    ```bash
    java -jar MailApplication-Backend-0.0.1-SNAPSHOT.jar
    ```

4.  **Run in Background (Production)**
    To keep it running after you disconnect, use `nohup`:
    ```bash
    nohup java -jar MailApplication-Backend-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
    ```
    - View logs: `tail -f app.log`

## Troubleshooting
- **Database Connection**: If the app fails to start, check `app.log`. If it's a connection error, ensure the VPS firewall allows connections to the database on port 3306 (though usually `localhost` is better if DB is on the same server).
- **Port 8080**: Ensure port 8080 is open in the Hostinger firewall to access the API.
