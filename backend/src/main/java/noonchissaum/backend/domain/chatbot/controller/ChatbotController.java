package noonchissaum.backend.domain.chatbot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.chatbot.dto.req.ChatNextReq;
import noonchissaum.backend.domain.chatbot.dto.res.ChatNextRes;
import noonchissaum.backend.domain.chatbot.dto.res.ChatNodeRes;
import noonchissaum.backend.domain.chatbot.dto.res.ScenarioSummaryRes;
import noonchissaum.backend.domain.chatbot.service.ChatbotService;
import noonchissaum.backend.global.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @GetMapping("/scenarios")
    public ResponseEntity<ApiResponse<List<ScenarioSummaryRes>>> listScenarios() {
        List<ScenarioSummaryRes> scenarios = chatbotService.listScenarios();
        return ResponseEntity.ok(ApiResponse.success("Chat scenarios retrieved", scenarios));
    }

    @GetMapping("/scenarios/{scenarioId}/start")
    public ResponseEntity<ApiResponse<ChatNodeRes>> startScenario(@PathVariable Long scenarioId) {
        ChatNodeRes node = chatbotService.startScenario(scenarioId);
        return ResponseEntity.ok(ApiResponse.success("Chat scenario started", node));
    }

    @PostMapping("/nodes/next")
    public ResponseEntity<ApiResponse<ChatNextRes>> next(@Valid @RequestBody ChatNextReq request) {
        ChatNextRes response = chatbotService.next(request.getNodeId(), request.getOptionId());
        return ResponseEntity.ok(ApiResponse.success("Chat next resolved", response));
    }
}
