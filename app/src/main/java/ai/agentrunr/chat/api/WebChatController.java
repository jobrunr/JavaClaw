package ai.agentrunr.chat.api;

import ai.agentrunr.chat.ChatChannel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST API controller for the web chat GUI.
 */
@RestController
@RequestMapping("/api")
public class WebChatController {

    private final ChatChannel chatChannel;

    public WebChatController(ChatChannel chatChannel) {
        this.chatChannel = chatChannel;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        String message = request.message();
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(new ChatResponse("Message cannot be empty", List.of()));
        }
        String response = chatChannel.chat(message.trim());
        List<String> backgroundMessages = chatChannel.drainPendingMessages();
        return ResponseEntity.ok(new ChatResponse(response, backgroundMessages));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "agentrunr"));
    }

    @GetMapping("/messages/pending")
    public ResponseEntity<List<String>> pendingMessages() {
        return ResponseEntity.ok(chatChannel.drainPendingMessages());
    }

    public record ChatRequest(String message) {}

    public record ChatResponse(String response, List<String> backgroundMessages) {}
}
