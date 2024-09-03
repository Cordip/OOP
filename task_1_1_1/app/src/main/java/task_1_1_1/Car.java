package main.java.task_1_1_1;

public class Car {
    public int speed;

    static int numOfWheels; // final == const

    public Car() { // contructor
    }

    public Car(int speed) { // contructor
        this.speed = speed;
    }
    
    public void SetSpeed (int speed) {
        this.speed = speed;
    }

    public static void SetNumOfWheels (int numOfWheels) {
        numOfWheels = num;
    }

    public static void DefaultCar (Car car) {
        car.speed = 20;
        numOfWheels = 5;
    }
}
