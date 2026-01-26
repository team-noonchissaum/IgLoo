package noonchissaum.backend.domain.user.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.report.entity.Report;
import noonchissaum.backend.domain.report.entity.ReportStatus;
import noonchissaum.backend.domain.user.dto.request.ReportProcessReq;
import noonchissaum.backend.domain.user.dto.response.ReportRes;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final OrderRepository orderRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    /* ================= 신고 관리 ================= */
    /**신고 목록 조회*/
    @Transactional(readOnly = true)
    public List<ReportRes> getReports(String status) {

        List<Report> reports = (status == null)
                ? reportRepository.findAll()
                : reportRepository.findByStatus(ReportStatus.valueOf(status));

        return reports.stream()
                .map(ReportRes::from)
                .toList();
    }
    /**신고 판단(처리)*/
    public void processReport(Long reportId, ReportProcessReq req) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고 없음"));

        report.process(ReportStatus.valueOf(req.getStatus()));
    }

    /* ================= 사용자 관리 ================= */
    /**유저 차단*/
    public void blockUser(Long userId) {
        User user = getUser(userId);
        user.block();
    }

    /**유저 차단 해제*/
    public void unblockUser(Long userId) {
        User user = getUser(userId);
        user.unblock();
    }

    /**유저 정보*/
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));
    }

    /* ================= 게시글 관리(복구) ================= */
    /**블락 처리된 게시글 복구*/
    public void restoreItem(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));

        item.restore();
    }

}
