import java.util.ArrayList;
import java.util.List;

interface Observer {
    void update(int value);
}

interface Subject {
    void attach(Observer o);
    void notifyObservers();
}

class Data implements Subject {
    private List<Observer> observers = new ArrayList<>();
    private int value;

    public void attach(Observer o) {
        observers.add(o);
    }

    public void setValue(int value) {
        this.value = value;
        notifyObservers();
    }

    public void notifyObservers() {
        for (Observer o : observers) {
            o.update(value);
        }
    }
}

class Display1 implements Observer {
    public void update(int value) {
        System.out.println("Display1 current value: " + value);
    }
}

class Display2 implements Observer {
    public void update(int value) {
        System.out.println("Display2 current value: " + value);
    }
}

class Display3 implements Observer {
    private int count = 0;
    private int sum = 0;

    public void update(int value) {
        count++;
        sum =sum + value;
        System.out.println("Display3 current value: " + count + ", Average ("+ sum+"/"+count+"): " + (sum / count));
    }
}

public class Main {
    public static void main(String[] args) {
        Data data = new Data();
        Display1 d1 = new Display1();
        Display2 d2 = new Display2();
        Display3 d3 = new Display3();

        data.attach(d1);
        data.attach(d2);
        data.attach(d3);

        data.setValue(10);
        data.setValue(20);
        data.setValue(30);
    }
}
/*
if i want to create another update function here, do i need to create a separate subject class and observer list for it?

It depends on what you want to achieve:

Option 1: Different data source → Yes, create a new subject class
If you want to track a completely different data source (e.g., temperature vs. humidity), create a new subject class like Data2 with its own observer list.

Option 2: Different notifications for same data → No, use the existing setup
If you want different observers to react differently to the same data, you already have this - each observer's update() method can do different things.

Option 3: Multiple types of updates on same subject → Extend the interface
If you want the same subject to send different types of notifications (e.g., onValueChanged(), onError(), onReset()), you could:

Add more methods to the Observer interface
Pass different parameters to distinguish update types
Use different observer interfaces for different event types */