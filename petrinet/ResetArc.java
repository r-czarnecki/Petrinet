package petrinet;

import java.util.Map;

class ResetArc<T> extends Arc<T> {
    public ResetArc(T place) {
        super(place, 0);
    }

    public void fire(Map<T, Integer> marking) {
        marking.remove(place);
    }
}