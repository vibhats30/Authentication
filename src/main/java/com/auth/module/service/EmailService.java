package com.auth.module.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Service for sending emails
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${app.mail.from:noreply@authentication-module.com}")
    private String fromEmail;

    @Value("${app.mail.from-name:Authentication Module}")
    private String fromName;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * Send verification email to user
     *
     * @param toEmail recipient email
     * @param userName recipient name
     * @param verificationToken verification token
     */
    public void sendVerificationEmail(String toEmail, String userName, String verificationToken) {
        try {
            logger.info("Sending verification email to: {}", toEmail);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Verify Your Email Address");

            // Create context for template
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("verificationLink", frontendUrl + "/verify-email?token=" + verificationToken);
            context.setVariable("frontendUrl", frontendUrl);

            // Process template
            String htmlContent = templateEngine.process("email/verification-email", context);
            helper.setText(htmlContent, true);

            // Send email
            mailSender.send(message);
            logger.info("Verification email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            logger.error("Failed to send verification email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send verification email", e);
        } catch (Exception e) {
            logger.error("Unexpected error sending verification email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    /**
     * Send welcome email after successful verification
     *
     * @param toEmail recipient email
     * @param userName recipient name
     */
    public void sendWelcomeEmail(String toEmail, String userName) {
        try {
            logger.info("Sending welcome email to: {}", toEmail);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to Authentication Module!");

            // Create context for template
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("frontendUrl", frontendUrl);

            // Process template
            String htmlContent = templateEngine.process("email/welcome-email", context);
            helper.setText(htmlContent, true);

            // Send email
            mailSender.send(message);
            logger.info("Welcome email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            logger.error("Failed to send welcome email to: {}", toEmail, e);
            // Don't throw exception for welcome email - it's not critical
        } catch (Exception e) {
            logger.error("Unexpected error sending welcome email to: {}", toEmail, e);
        }
    }

    /**
     * Send account deletion warning email
     *
     * @param toEmail recipient email
     * @param userName recipient name
     * @param hoursRemaining hours until account deletion
     */
    public void sendAccountDeletionWarning(String toEmail, String userName, int hoursRemaining) {
        try {
            logger.info("Sending account deletion warning to: {}", toEmail);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Action Required: Verify Your Email Address");

            // Create context for template
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("hoursRemaining", hoursRemaining);
            context.setVariable("frontendUrl", frontendUrl);

            // Process template
            String htmlContent = templateEngine.process("email/deletion-warning", context);
            helper.setText(htmlContent, true);

            // Send email
            mailSender.send(message);
            logger.info("Account deletion warning sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            logger.error("Failed to send deletion warning to: {}", toEmail, e);
        } catch (Exception e) {
            logger.error("Unexpected error sending deletion warning to: {}", toEmail, e);
        }
    }
}
