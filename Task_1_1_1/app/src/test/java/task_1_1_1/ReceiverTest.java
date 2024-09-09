package task_1_1_1;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class ReceiverTest {
    InputStream sysInBackup = System.in; // backup System.in to restore it later
    ByteArrayInputStream in = new ByteArrayInputStream("My string".getBytes());
    System.setIn(in);

    // do your thing

    // optionally, reset System.in to its original
    System.setIn(sysInBackup);
}
