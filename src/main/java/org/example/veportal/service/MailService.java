package org.example.veportal.service;

import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.example.veportal.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final AppProperties.Mail mailProperties;
    private final JavaMailSenderImpl mailSender;
    private final boolean configured;

    public MailService(AppProperties appProperties) {
        this.mailProperties = appProperties.mail();
        boolean hasCredentials = mailProperties.host() != null && !mailProperties.host().isBlank()
                && mailProperties.username() != null && !mailProperties.username().isBlank()
                && mailProperties.password() != null && !mailProperties.password().isBlank();
        this.configured = hasCredentials;
        this.mailSender = new JavaMailSenderImpl();
        this.mailSender.setHost(mailProperties.host());
        this.mailSender.setPort(mailProperties.port());
        if (hasCredentials) {
            this.mailSender.setUsername(mailProperties.username());
            this.mailSender.setPassword(mailProperties.password());
        }
        Properties props = this.mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        if (!configured) {
            log.warn("SMTP is not configured (set VEPORTAL_MAIL_USERNAME / VEPORTAL_MAIL_PASSWORD). "
                    + "Credential emails will be logged instead of sent.");
        }
    }

    public boolean sendPasswordReset(String recipient, String name, String token) {
        String subject = "Reset your VE Faculty Portal password";
        String link = mailProperties.portalUrl() + "/reset-password?token=" + token;
        String body = """
                Hi %s,

                A password reset was requested for your VE Faculty Portal account.
                Reset your password here: %s

                This link expires in 30 minutes and can be used only once.
                If you did not request this, you can safely ignore this message.

                Regards,
                VE Faculty Portal
                """.formatted(name, link);
        if (!configured) {
            log.info("[MAILPREVIEW] To: {} Subject: {}\n{}", recipient, subject, body);
            return true;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(mailProperties.from());
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(body);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", recipient, e.getMessage());
            return false;
        }
    }
}
