package ai.javaclaw.channels.whatsapp;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WacliSyncTest {

    /** Short enough that the waits between restarts do not slow the tests down. */
    private static final Duration RESTART_DELAY = Duration.ofMillis(1);

    /** Comfortably more restarts than the retry limit allows, to show the counter really resets. */
    private static final int RESTARTS_PAST_THE_LIMIT = 2 * WacliSync.MAX_RESTART_RETRIES;

    /** Stays at zero unless a test moves it, so by default every run looks like an instant crash. */
    private final TestClock clock = new TestClock();

    /** How often the sync under test asked for a new subprocess. */
    private final AtomicInteger starts = new AtomicInteger();

    /** A field, so the lambdas below can stop the very sync they are starting a process for. */
    private WacliSync sync;

    /** A subprocess that has already exited with an error. */
    private static Process crashedProcess() throws InterruptedException {
        Process process = mock(Process.class);
        when(process.waitFor()).thenReturn(1);
        return process;
    }

    @Test
    void restartsSyncWhenItExitsAndGivesUpAfterTooManyFailures() throws Exception {
        Process process = crashedProcess();
        sync = new WacliSync(() -> {
            starts.incrementAndGet();
            return process;
        }, clock, RESTART_DELAY);

        sync.runUntilStopped();

        assertThat(starts).hasValue(1 + WacliSync.MAX_RESTART_RETRIES);
        assertThat(sync.isRunning()).isFalse();
    }

    @Test
    void keepsRestartingAsLongAsSyncStaysUpLongEnough() throws Exception {
        Process process = crashedProcess();
        sync = new WacliSync(() -> {
            clock.advance(WacliSync.HEALTHY_UPTIME);
            if (starts.incrementAndGet() == RESTARTS_PAST_THE_LIMIT) {
                sync.stop();
            }
            return process;
        }, clock, RESTART_DELAY);

        sync.runUntilStopped();

        assertThat(starts).hasValue(RESTARTS_PAST_THE_LIMIT);
    }

    @Test
    void stopWhileSyncIsRunningEndsTheLoop() throws Exception {
        Process process = mock(Process.class);
        when(process.waitFor()).thenAnswer(invocation -> {
            assertThat(sync.isRunning()).isTrue();
            sync.stop();
            return 1;
        });
        sync = new WacliSync(() -> {
            starts.incrementAndGet();
            return process;
        }, clock, RESTART_DELAY);

        sync.runUntilStopped();

        assertThat(starts).hasValue(1);
        assertThat(sync.isRunning()).isFalse();
    }

    @Test
    void killsASyncThatStartedAfterWeAskedToStop() throws Exception {
        Process process = mock(Process.class);
        sync = new WacliSync(() -> {
            sync.stop();
            return process;
        }, clock, RESTART_DELAY);

        sync.runUntilStopped();

        verify(process).destroy();
        verify(process, never()).waitFor();
    }

    @Test
    void killsSyncWhenInterruptedWhileWaitingForItToExit() throws Exception {
        Process process = mock(Process.class);
        when(process.waitFor()).thenThrow(new InterruptedException("stopping"));
        sync = new WacliSync(() -> process, clock, RESTART_DELAY);

        sync.runUntilStopped();

        verify(process).destroy();
        assertThat(Thread.interrupted()).isTrue();
    }

    /** A clock the test moves by hand, to say how long a subprocess stayed up. */
    private static class TestClock extends Clock {

        private Instant now = Instant.EPOCH;

        void advance(Duration amount) {
            now = now.plus(amount);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
