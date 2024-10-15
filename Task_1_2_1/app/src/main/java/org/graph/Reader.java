package org.graph;

import java.nio.file.*;
import java.nio.charset.*;
import java.io.*;

public class Reader {
    private BufferedReader reader = null;

    public void openFile(Path file) {
        Charset charset = Charset.forName("US-ASCII");
        try (BufferedReader tmpReader = Files.newBufferedReader(file, charset)) {
            String line = null;
            reader = tmpReader;
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }
    }

    public String[] readFile () {
        String line;
        line = reader.readLine();
        return line.split(" ");
    }
}
