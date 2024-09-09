package task_1_1_1;

import java.time.Duration;
import java.time.Instant;

public class Timer {

    private Instant start;
    private Duration timeElapsed;

    public void startTimer() {
        this.start = Instant.now();
    }

    public void stopTimer() {
        Instant end = Instant.now();
        this.timeElapsed = Duration.between(this.start, end);
        this.start = null;
    }

    public void printTimer() {
        System.out.println("Time taken: "+ this.timeElapsed.toMillis() +" milliseconds");
    }
}
