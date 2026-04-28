package com.btctech.mailapp.config;

import com.btctech.mailapp.entity.ClientApp;
import com.btctech.mailapp.repository.ClientAppRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ClientSeedConfig {

    private final ClientAppRepository clientAppRepository;

    @Bean
    public CommandLineRunner seedClients() {
        return args -> {
            clientAppRepository.findByClientId("bnx-test-app").ifPresentOrElse(
                client -> {
                    log.info("Updating test OAuth client redirect URI...");
                    client.setRedirectUri("http://localhost:3000");
                    clientAppRepository.save(client);
                },
                () -> {
                    log.info("Seeding test OAuth client...");
                    ClientApp client = ClientApp.builder()
                            .clientId("bnx-test-app")
                            .clientSecret("secure-test-secret-2026")
                            .appName("BNX Test Application")
                            .redirectUri("http://localhost:3000")
                            .build();
                    clientAppRepository.save(client);
                    log.info("Test OAuth client seeded: bnx-test-app");
                }
            );
        };
    }
}
