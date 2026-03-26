package ai.javaclaw.speech;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@ConditionalOnProperty(name = "speech.provider", havingValue = "openai")
public class OpenAiSpeechToTextService implements SpeechToTextService {

    @Override
    public String transcribe(InputStream audioStream) {
        // TODO: integrate OpenAI Whisper API
        return "[transcribed via OpenAI]";
    }
}
