package ai.javaclaw.channels.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Keeps the long-running {@code wacli sync} subprocess alive on a background thread. When it exits
 * on its own we wait a moment and start it again, and we give up after {@link #MAX_RESTART_RETRIES}
 * failures in quick succession.
 *
 * <p>Starting the process, the clock and the delay are all passed in, so this class is only about
 * staying alive and tests can drive the restarts without really waiting.
 *
 * <p>Several threads meet here -- Spring's startup and shutdown call {@link #start} and
 * {@link #stop}, the loop runs on its own "wacli-sync" thread, and any thread sending a message
 * asks {@link #isRunning} first -- so the fields they share are {@code volatile}.
 */
class WacliSync {

    static final int MAX_RESTART_RETRIES = 5;

    /** How long the subprocess must stay up before we treat its next crash as a fresh problem. */
    static final Duration HEALTHY_UPTIME = Duration.ofSeconds(60);

    @FunctionalInterface
    interface ProcessStarter {
        Process start() throws IOException;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(WacliSync.class);

    /** How long to wait before starting the subprocess again after it died. */
    private static final Duration RESTART_DELAY = Duration.ofSeconds(5);

    private static final int STOP_GRACE_SECONDS = 5;

    private final ProcessStarter processStarter;
    private final Clock clock;
    private final Duration restartDelay;

    /** True while the loop below is syncing. Read by senders, on their own threads. */
    private volatile boolean running;

    /** Tells the loop to quit instead of restarting. Set by {@link #stop}, read by the loop. */
    private volatile boolean stopRequested;

    /** The subprocess the loop is watching, so that {@link #stop} can kill it from its own thread. */
    private volatile Process syncProcess;

    WacliSync(ProcessStarter processStarter) {
        this(processStarter, Clock.systemUTC(), RESTART_DELAY);
    }

    WacliSync(ProcessStarter processStarter, Clock clock, Duration restartDelay) {
        this.processStarter = processStarter;
        this.clock = clock;
        this.restartDelay = restartDelay;
    }

    void start() {
        Thread thread = new Thread(this::runUntilStopped, "wacli-sync");
        thread.setDaemon(true);
        thread.start();
    }

    void stop() {
        stopRequested = true;
        running = false;

        Process process = syncProcess;
        if (process == null) {
            return;
        }
        process.destroy();
        try {
            if (!process.waitFor(STOP_GRACE_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    boolean isRunning() {
        return running;
    }

    /** The background thread's work: run the subprocess, and start it again whenever it dies. */
    void runUntilStopped() {
        running = true;
        try {
            int failures = 0;
            while (!stopRequested) {
                Instant startedAt = clock.instant();
                runSyncProcess();
                if (stopRequested) {
                    return;
                }

                failures = stayedUpLongEnough(startedAt) ? 1 : failures + 1;
                if (failures > MAX_RESTART_RETRIES) {
                    LOGGER.error("'wacli sync' failed {} times in quick succession, "
                            + "so the WhatsApp channel is stopping.", failures);
                    return;
                }
                waitBeforeRestart();
            }
        } finally {
            running = false;
        }
    }

    /** Starts {@code wacli sync} and waits here until it exits. */
    private void runSyncProcess() {
        Process process;
        try {
            process = processStarter.start();
        } catch (IOException e) {
            LOGGER.warn("Failed to start 'wacli sync'", e);
            return;
        }
        syncProcess = process;
        if (stopRequested) {
            process.destroy();
            return;
        }

        try {
            int exitCode = process.waitFor();
            if (!stopRequested) {
                LOGGER.warn("'wacli sync' exited unexpectedly with code {}", exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stopRequested = true;
            process.destroy();
        }
    }

    private boolean stayedUpLongEnough(Instant startedAt) {
        Duration uptime = Duration.between(startedAt, clock.instant());
        return uptime.compareTo(HEALTHY_UPTIME) >= 0;
    }

    private void waitBeforeRestart() {
        try {
            Thread.sleep(restartDelay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stopRequested = true;
        }
    }
}
