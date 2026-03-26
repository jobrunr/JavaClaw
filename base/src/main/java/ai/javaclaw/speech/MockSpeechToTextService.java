package ai.javaclaw.speech;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@ConditionalOnProperty(name = "speech.provider", havingValue = "mock", matchIfMissing = true)
public class MockSpeechToTextService implements SpeechToTextService {

    @Override
    public String transcribe(InputStream audioStream) {
        return "[voice message]";
    }
}
