package task_1_1_1;

import java.util.Scanner;

public class Receiver {

    private Scanner scannerIn = new Scanner(System.in);

    public int collectLengthForArrayFromCmd() {
        int length = -1;
        System.out.println("Write length of the array");
        while(length == -1) {
            if (scannerIn.hasNextInt()) {
                length = scannerIn.nextInt();
                System.out.println("inputted length " + length);
            } else {
                scannerIn.next();
                System.out.println("Writed not int");

            }
        }

        return length;
    }

    public int[] collectArrayFromCmd(int length) {
        int[] arr = new int[length];
        System.out.println("Write array of integers");
        for (int i = 0; i < length; ++i) {
            arr[i] = -1;
            while(arr[i] == -1) {
                if (scannerIn.hasNextInt()) {
                    arr[i] = scannerIn.nextInt();
                } else {
                    scannerIn.next();
                    System.out.println("Writed not int");
    
                }
            }
                
            
        }
        System.out.println("array collected");
        
        return arr;
    }
}
