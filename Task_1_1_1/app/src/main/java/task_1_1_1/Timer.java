package task_1_1_1;

import java.time.Duration;
import java.time.Instant;
/**
 *class for calculate time (like timer) and print it.
 */
public class Timer {
    /**
     *needs allocation to work.
     */
    public Timer () {

    }
    /**
     *start of timer.
     */
    private Instant start;
    /**
     *time between start and end of timer.
     */
    private Duration timeElapsed;

    /**
     *function used to start timer.
     */
    public void startTimer() {
        this.start = Instant.now();
    }
    /**
     *function used to stop timer.
     */
    public void stopTimer() {
        Instant end = Instant.now();
        this.timeElapsed = Duration.between(this.start, end);
        this.start = null;
    }

    /**
     *prints timer in miliseconds.
     */
    public void printTimer() {
        System.out.println("Time taken: "+ this.timeElapsed.toMillis() +" milliseconds");
    }
}
