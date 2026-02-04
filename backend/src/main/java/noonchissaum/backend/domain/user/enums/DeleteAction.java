package noonchissaum.backend.domain.user.enums;

public enum DeleteAction {
    DELETED,           // 탈퇴 완료
    WARN_FIRST,        // 첫 시도 - 환전 권장
    CONFIRM_REQUIRED   // 두 번째 이상 - 포기 확인 (공통)
}