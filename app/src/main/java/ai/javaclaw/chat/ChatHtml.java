package ai.javaclaw.chat;

import org.springframework.web.util.HtmlUtils;

import java.util.List;

/**
 * Chat message bubble HTML fragment helpers.
 */
public class ChatHtml {

    private ChatHtml() {}

    public static String agentBubble(String text) {
        return "<article class=\"ar-msg ar-msg--agent\">" +
                "<div class=\"ar-msg__avatar\">JC</div>" +
                "<div class=\"ar-msg__bubble\">" + HtmlUtils.htmlEscape(text) + "</div>" +
                "</article>";
    }

    public static String userBubble(String text) {
        return "<article class=\"ar-msg ar-msg--user\">" +
                "<div class=\"ar-msg__bubble\">" + HtmlUtils.htmlEscape(text) + "</div>" +
                "</article>";
    }

    public static String typingDots() {
        return "<div class=\"ar-typing\">" +
                "<div class=\"ar-msg__avatar\">JC</div>" +
                "<div class=\"ar-typing__dots\"><span></span><span></span><span></span></div>" +
                "</div>";
    }

    public static String conversationSelector(List<String> ids, String selectedId) {
        StringBuilder sb = new StringBuilder();
        sb.append("<select id=\"channel-select\" class=\"select\" name=\"conversationId\" "
                + "ws-send hx-trigger=\"change\" "
                + "hx-vals='{\"type\": \"channelChanged\"}'>"); 
        for (String id : ids) {
            sb.append("<option value=\"").append(HtmlUtils.htmlEscape(id)).append("\"");
            if (id.equals(selectedId)) sb.append(" selected");
            sb.append(">").append(HtmlUtils.htmlEscape(labelFor(id))).append("</option>");
        }
        sb.append("</select>");
        return sb.toString();
    }

    private static String labelFor(String conversationId) {
        if ("web".equals(conversationId)) return "Web Chat";
        if (conversationId.startsWith("telegram-")) return "Telegram (" + conversationId.substring("telegram-".length()) + ")";
        return conversationId;
    }
}
