package sorter;

import org.checkerframework.dataflow.qual.AssertMethod;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import sorter.*;
import java.lang.System;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.InputStream;

import java.util.Arrays;
import java.util.Random;

public class AppTest {
    @Test public void easyValidCmdInput () {    
        String userInput = String.format("5%s-3%s10 7 512%s0", System.lineSeparator(), System.lineSeparator(), System.lineSeparator());
        ByteArrayInputStream bais = new ByteArrayInputStream(userInput.getBytes());
        InputStream inStream = System.in;

        System.setIn(bais);

        String expected = "-3 0 7 10 512 ";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(baos);
        PrintStream outStream = System.out;

        System.setOut(printStream);

        App.main(null);

        String[] lines = baos.toString().split(System.lineSeparator());
        String actual = lines[lines.length-1];

        System.setIn(inStream);
        System.setOut(outStream);

        assertEquals(expected,actual);
    }

    @Test public void easyValidCmdInputWithErrors() {
        String userInput = String.format("5a a A asjna ASJKFHA __ -- ..> 5 -3 10a a A asjna ASJKFHA __ -- ..> 10 7 512 0");
        ByteArrayInputStream bais = new ByteArrayInputStream(userInput.getBytes());
        InputStream inStream = System.in;

        System.setIn(bais);

        String expected = "-3 0 7 10 512 ";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(baos);
        PrintStream outStream = System.out;

        System.setOut(printStream);

        App.main(null);

        String[] lines = baos.toString().split(System.lineSeparator());
        String actual = lines[lines.length-1];

        System.setIn(inStream);
        System.setOut(outStream);

        assertEquals(expected,actual);
    }

    @RepeatedTest(10) public void randomValidCmdInput() {
        Random rnd = new Random();
        int length = rnd.nextInt(10000);
        int [] arr = new int[length];

        for (int i = 0; i < length; ++i) {
            arr[i] = rnd.nextInt(10000000);
        }
        int [] clone_arr = arr.clone();
        Arrays.sort(clone_arr);


        String userInput = String.format("%s %s", Integer.toString(length), Print.arrayToString(arr));
        ByteArrayInputStream bais = new ByteArrayInputStream(userInput.getBytes());
        InputStream inStream = System.in;

        System.setIn(bais);

        String expected = Print.arrayToString(clone_arr);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(baos);
        PrintStream outStream = System.out;

        System.setOut(printStream);

        App.main(null);

        String[] lines = baos.toString().split(System.lineSeparator());
        String actual = lines[lines.length-1];

        System.setIn(inStream);
        System.setOut(outStream);

        assertEquals(expected,actual);
    }
}
