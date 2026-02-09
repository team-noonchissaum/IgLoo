package noonchissaum.backend.domain.inquiry.repositroy;

import noonchissaum.backend.domain.inquiry.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    /**
     * 닉네임으로 차단 해제 요청 삭제
     */
    void deleteByNickname(String nickname);
}
