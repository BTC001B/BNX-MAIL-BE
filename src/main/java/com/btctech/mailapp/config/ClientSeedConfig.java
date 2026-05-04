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
            clientAppRepository.findByClientId("kinsword").ifPresentOrElse(
                client -> {
                    log.info("Updating test OAuth client redirect URI...");
                    client.setRedirectUri("https://www.kinsword.com");
                    clientAppRepository.save(client);
                },
                () -> {
                    log.info("Seeding test OAuth client...");
                    ClientApp client = ClientApp.builder()
                            .clientId("kinsword")
                            .clientSecret("secure-test-secret-2026")
                            .appName("KINSWORD")
                            .redirectUri("https://www.kinsword.com")
                            .build();
                    clientAppRepository.save(client);
                    log.info("Test OAuth client seeded: kinsword");
                }
            );
            
            clientAppRepository.findByClientId("cliks-app").ifPresentOrElse(
                client -> {
                    log.info("Updating cliks-app OAuth client redirect URI...");
                    client.setRedirectUri("https://cliks.beta-softnet.com");
                    clientAppRepository.save(client);
                },
                () -> {
                    log.info("Seeding cliks-app OAuth client...");
                    ClientApp client = ClientApp.builder()
                            .clientId("cliks-app")
                            .clientSecret("secure-cliks-secret-2026")
                            .appName("Cliks")
                            .redirectUri("https://cliks.beta-softnet.com")
                            .build();
                    clientAppRepository.save(client);
                    log.info("Test OAuth client seeded: cliks-app");
                }
            );
        };
    }
}
