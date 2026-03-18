package ai.agentrunr.api;

import ai.agentrunr.chat.ChatChannel;
import ai.agentrunr.chat.api.WebChatController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebChatController.class)
class WebChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatChannel chatChannel;

    @Test
    void chatEndpointReturnsAgentResponse() throws Exception {
        when(chatChannel.chat(anyString())).thenReturn("Hello from the agent!");
        when(chatChannel.drainPendingMessages()).thenReturn(List.of());

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"Hi there\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("Hello from the agent!"));
    }

    @Test
    void chatEndpointRejectsEmptyMessage() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void healthEndpointReturnsOk() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
