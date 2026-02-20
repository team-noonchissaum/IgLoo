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
import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.front.base-url:http://localhost:3000}")
    private String frontBaseUrl;

    @Value("${app.front.paths.password-reset:/reset-password}")
    private String passwordResetPath;

    @Value("${app.front.paths.auction-detail:/auctions/{auctionId}}")
    private String auctionDetailPath;

    /** 비밀번호 재설정 메일 발송 */
    public void sendPasswordResetMail(String toEmail, String token) {
        String resetLink = UriComponentsBuilder
                .fromUriString(frontBaseUrl)
                .path(passwordResetPath)
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
            sendHtmlMail(toEmail, subject, html);
        } catch (MailException | MessagingException e) {
            log.error("비밀번호 재설정 메일 발송 실패. to={}, reason={}", toEmail, e.getMessage(), e);
            throw new ApiException(ErrorCode.PASSWORD_RESET_MAIL_SEND_FAILED);
        }
    }

    /** 관심 카테고리 신규 경매 알림 메일 발송 */
    public void sendAuctionReadyNoticeMail(String toEmail, String categoryName, String itemTitle, BigDecimal price, Long auctionId) {
        String auctionLink = UriComponentsBuilder
                .fromUriString(frontBaseUrl)
                .path(auctionDetailPath)
                .buildAndExpand(auctionId)
                .toUriString();

        String subject = "[IgLoo] 관심 카테고리 신규 경매 알림";
        String html = """
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #111;">
                    <h2 style="margin-bottom: 16px;">관심 카테고리에 새 경매가 시작되었어요</h2>
                    <p><strong>카테고리:</strong> %s</p>
                    <p><strong>상품명:</strong> %s</p>
                    <p><strong>시작가:</strong> %s원</p>
                    <p style="margin-top: 16px;">
                        <a href="%s"
                           style="display: inline-block; padding: 10px 16px; background: #2563eb; color: #fff; text-decoration: none; border-radius: 6px;">
                            경매 보러가기
                        </a>
                    </p>
                    <p>버튼이 동작하지 않으면 아래 링크를 직접 열어주세요.</p>
                    <p><a href="%s">%s</a></p>
                </body>
                </html>
                """.formatted(categoryName, itemTitle, price.toPlainString(), auctionLink, auctionLink, auctionLink);

        try {
            sendHtmlMail(toEmail, subject, html);
        } catch (MailException | MessagingException e) {
            log.warn("관심 카테고리 알림 메일 발송 실패. to={}, reason={}", toEmail, e.getMessage());
        }
    }

    /** HTML 메일 공통 전송 메서드 */
    private void sendHtmlMail(String toEmail, String subject, String html) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(message);
    }
}
