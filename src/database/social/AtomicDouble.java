package database.social;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public @Data class AtomicDouble {
    private volatile double value;

    public AtomicDouble(int initialValue) {
        this.value = initialValue;
    }

    public synchronized double getAndIncrement(double delta){
        double currVal = value;
        value+=delta;
        return currVal;
    }

    public synchronized double incrementAndGet(double delta){
        value+=delta;
        return value;
    }

    public synchronized void addDelta(double delta){
        value+=delta;
    }

    public synchronized double getValue(){
        return value;
    }
    
}
