package noonchissaum.backend.domain.chatbot.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.chatbot.dto.res.ChatActionRes;
import noonchissaum.backend.domain.chatbot.dto.res.ChatNextRes;
import noonchissaum.backend.domain.chatbot.dto.res.ChatNodeRes;
import noonchissaum.backend.domain.chatbot.dto.res.ChatOptionRes;
import noonchissaum.backend.domain.chatbot.dto.res.ScenarioSummaryRes;
import noonchissaum.backend.domain.chatbot.entity.ChatActionType;
import noonchissaum.backend.domain.chatbot.entity.ChatNode;
import noonchissaum.backend.domain.chatbot.entity.ChatOption;
import noonchissaum.backend.domain.chatbot.entity.ChatScenario;
import noonchissaum.backend.domain.chatbot.repository.ChatNodeRepository;
import noonchissaum.backend.domain.chatbot.repository.ChatOptionRepository;
import noonchissaum.backend.domain.chatbot.repository.ChatScenarioRepository;
import noonchissaum.backend.global.exception.CustomException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatScenarioRepository chatScenarioRepository;
    private final ChatNodeRepository chatNodeRepository;
    private final ChatOptionRepository chatOptionRepository;

    @Transactional(readOnly = true)
    public List<ScenarioSummaryRes> listScenarios() {
        return chatScenarioRepository.findAllByActiveTrueOrderByIdAsc()
                .stream()
                .map(s -> new ScenarioSummaryRes(s.getId(), s.getTitle(), s.getDescription()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChatNodeRes startScenario(Long scenarioId) {
        ChatScenario scenario = chatScenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_SCENARIO_NOT_FOUND));
        if (!scenario.isActive()) {
            throw new CustomException(ErrorCode.CHAT_SCENARIO_NOT_FOUND);
        }

        ChatNode rootNode = chatNodeRepository.findByScenarioIdAndRootTrue(scenarioId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NODE_NOT_FOUND));

        return toNodeRes(rootNode);
    }

    @Transactional(readOnly = true)
    public ChatNextRes next(Long nodeId, Long optionId) {
        ChatOption option = chatOptionRepository.findByIdAndNodeId(optionId, nodeId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_OPTION_NOT_FOUND));

        ChatNode nextNode = option.getNextNode();
        if (nextNode != null) {
            return new ChatNextRes("NODE", toNodeRes(nextNode), null);
        }

        ChatActionType actionType = option.getActionType();
        ChatActionRes action = new ChatActionRes(actionType, option.getActionTarget());
        return new ChatNextRes("ACTION", null, action);
    }

    private ChatNodeRes toNodeRes(ChatNode node) {
        List<ChatOptionRes> options = chatOptionRepository.findByNodeIdOrderBySortOrderAsc(node.getId())
                .stream()
                .map(this::toOptionRes)
                .collect(Collectors.toList());

        return new ChatNodeRes(
                node.getId(),
                node.getScenario().getId(),
                node.getText(),
                node.isTerminal(),
                options
        );
    }

    private ChatOptionRes toOptionRes(ChatOption option) {
        Long nextNodeId = option.getNextNode() != null ? option.getNextNode().getId() : null;
        return new ChatOptionRes(
                option.getId(),
                option.getLabel(),
                nextNodeId,
                option.getActionType(),
                option.getActionTarget()
        );
    }
}
