package ing.software.dulcemayor.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Configures JavaMailSender with Gmail/STARTTLS defaults and normalizes App Passwords
 * containing spaces. Operates gracefully in log-only mode when credentials are absent.
 */
@Configuration
@Slf4j
public class MailConfig {

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);

        String cleanUsername = username != null ? username.trim() : "";
        String cleanPassword = password != null ? password.replace(" ", "").trim() : "";

        mailSender.setUsername(cleanUsername);
        mailSender.setPassword(cleanPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.trust", host);
        props.put("mail.debug", "false");

        if (!mailEnabled || cleanUsername.isEmpty() || cleanPassword.isEmpty()) {
            log.info("MailConfig initialized in simulator/preview mode (enabled={}, usernameConfigured={})",
                    mailEnabled, !cleanUsername.isEmpty());
        } else {
            log.info("MailConfig initialized for SMTP host={}:{} as {}", host, port, cleanUsername);
        }

        return mailSender;
    }
}
