package noonchissaum.backend.domain.toss.dto.cancel;

public record  TossCancelReq (
        String cancelReason,
        Integer cancelAmount)
{
}
