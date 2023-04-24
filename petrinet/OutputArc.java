package petrinet;

import java.util.Map;

class OutputArc<T> extends Arc<T> {
    public OutputArc(T place, int weight) {
        super(place, weight);
    }

    public void fire(Map<T, Integer> marking) {
        addWeight(marking, weight);
    }
}