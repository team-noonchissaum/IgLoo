package noonchissaum.backend.domain.report.handler;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.report.entity.ReportTargetType;
import noonchissaum.backend.domain.user.repository.UserRepository;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import noonchissaum.backend.global.handler.ReportTargetHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserHandler implements ReportTargetHandler {
    private final UserRepository userRepository;

    @Override
    public ReportTargetType getType(){
        return ReportTargetType.USER;
    }

    @Override
    public void validate(Long targetId) {
        if(!userRepository.existsById(targetId)) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
    }
}
