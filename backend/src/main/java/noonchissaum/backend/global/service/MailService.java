package noonchissaum.backend.global.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.auth.password-reset.reset-url}")
    private String passwordResetUrl;

    public void sendPasswordResetMail(String toEmail, String token) {
        String resetLink = UriComponentsBuilder
                .fromUriString(passwordResetUrl)
                .queryParam("token", token)
                .build()
                .toUriString();

        String subject = "[IgLoo] 비밀번호 재설정 안내";
        String html = """
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #111;">
                    <h2 style="margin-bottom: 16px;">비밀번호 재설정 안내</h2>
                    <p>아래 버튼을 눌러 비밀번호를 재설정해주세요.</p>
                    <p style="margin: 24px 0;">
                        <a href="%s"
                           style="display: inline-block; padding: 10px 16px; background: #2563eb; color: #fff; text-decoration: none; border-radius: 6px;">
                            비밀번호 재설정
                        </a>
                    </p>
                    <p>버튼이 동작하지 않으면 아래 링크를 직접 열어주세요.</p>
                    <p><a href="%s">%s</a></p>
                    <p style="margin-top: 24px; color: #666;">링크는 15분 동안만 유효합니다.</p>
                </body>
                </html>
                """.formatted(resetLink, resetLink, resetLink);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MailException | MessagingException e) {
            log.error("비밀번호 재설정 메일 발송 실패. to={}, reason={}", toEmail, e.getMessage(), e);
            throw new ApiException(ErrorCode.PASSWORD_RESET_MAIL_SEND_FAILED);
        }
    }
}
