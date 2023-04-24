package petrinet;

import java.util.Map;

abstract class Arc<T> {
    protected T place;
    protected int weight;

    protected Arc(T place, int weight) {
        this.place = place;
        this.weight = weight;
    }

    public abstract void fire(Map<T, Integer> marking);

    public boolean isEnabled(Map<T, Integer> marking) {
        return true;
    }

    public T place() {
        return place;
    }

    protected void addWeight(Map<T, Integer> marking, int add) {
        if(add == 0)
            return;

        marking.putIfAbsent(place, 0);
        int newTokens = marking.get(place) + add;
        
        if(newTokens != 0)
            marking.put(place, newTokens);
        else
            marking.remove(place);
    }
}