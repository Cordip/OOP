package org.simplenumbers;

public class App {
    public static void main(String[] args) {
        SimpleNumbers sn = new SimpleNumbers();
        //long [] arr = {2,3,5,4};
        long [] arr = Util.readIntegersFromFile("test_small.txt");
        sn.setArray(arr);
        long startTime = System.currentTimeMillis();
        System.out.println("1) Line:" + sn.checkArray());
        long endTime = System.currentTimeMillis();
        System.out.println("Time taken: " + (endTime - startTime) + " ms");

        ParallelSN psn = new ParallelSN(arr); 
        long startTimeP = System.currentTimeMillis();
        System.out.println("2) Thread pool" + psn.ParallelCheck());
        long endTimeP = System.currentTimeMillis();
        System.out.println("Time taken: " + (endTimeP - startTimeP) + " ms");
        
    }

}
