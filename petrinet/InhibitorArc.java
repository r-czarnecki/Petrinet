package petrinet;

import java.util.Map;

class InhibitorArc<T> extends Arc<T> {
    public InhibitorArc(T place) {
        super(place, 0);
    }

    public void fire(Map<T, Integer> marking) {}

    @Override
    public boolean isEnabled(Map<T, Integer> marking) {
        return !marking.containsKey(place);
    }
}