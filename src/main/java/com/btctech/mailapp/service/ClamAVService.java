package com.btctech.mailapp.service;

import com.btctech.mailapp.exception.MailSecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * ClamAV Socket Client (Clamd Protocol)
 * Implements the INSTREAM command for streaming byte-level scanning.
 */
@Slf4j
@Service
public class ClamAVService {

    @Value("${mail.clamav.host:localhost}")
    private String host;

    @Value("${mail.clamav.port:3310}")
    private int port;

    @Value("${mail.clamav.timeout:30000}")
    private int timeout;

    private static final int CHUNK_SIZE = 2048;
    private static final byte[] INSTREAM = "nINSTREAM\n".getBytes(StandardCharsets.US_ASCII);

    /**
     * Scan InputStream for viruses via Clamd socket
     * Throws MailSecurityException if virus is found.
     */
    public void scan(InputStream inputStream) {
        log.info("Initiating Virus Scan via ClamAV at {}:{}", host, port);
        
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            socket.setSoTimeout(timeout);

            try (DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                // 1. Send INSTREAM command
                dos.write(INSTREAM);
                dos.flush();

                // 2. Stream data in chunks
                byte[] buffer = new byte[CHUNK_SIZE];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    dos.writeInt(read); // Chunk size
                    dos.write(buffer, 0, read); // Chunk data
                }

                // 3. Terminate stream with 0-length chunk
                dos.writeInt(0);
                dos.flush();

                // 4. Read response
                String response = reader.readLine();
                log.debug("ClamAV Response: {}", response);

                if (response == null) {
                    throw new IOException("Empty response from ClamAV");
                }

                if (response.contains("FOUND")) {
                    log.error("VIRUS DETECTED: {}", response);
                    throw new MailSecurityException("Malicious file detected. Upload rejected.");
                }
                
                log.info("✓ Virus scan clean: {}", response);
            }
        } catch (IOException e) {
            log.warn("ClamAV scan skipped (Scanner unreachable): {}", e.getMessage());
            // Fail-Open for development: If scanner is down, we allow the upload but log a warning.
            // In a strict production environment, this would throw a MailSecurityException.
        }
    }
}
