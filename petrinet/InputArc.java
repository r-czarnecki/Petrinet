package petrinet;

import java.util.Map;

class InputArc<T> extends Arc<T> {
    public InputArc(T place, int weight) {
        super(place, weight);
    }

    public void fire(Map<T, Integer> marking) {
        addWeight(marking, -weight);
    }

    @Override
    public boolean isEnabled(Map<T, Integer> marking) {
        return weight == 0 || (marking.containsKey(place) && marking.get(place) >= weight);
    }
}