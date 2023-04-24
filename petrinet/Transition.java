package petrinet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

public class Transition<T> {
    private ArrayList<Arc<T>> arcs;
    private Semaphore protection;

    public Transition(Map<T, Integer> input, Collection<T> reset, Collection<T> inhibitor, Map<T, Integer> output) {
        this();

        for(T place : input.keySet()) 
            arcs.add(new InputArc<>(place, input.get(place)));
        
        for(T place : reset)
            arcs.add(new ResetArc<>(place));
        
        for(T place : inhibitor)
            arcs.add(new InhibitorArc<>(place));

        for(T place : output.keySet()) 
            arcs.add(new OutputArc<>(place, output.get(place)));
    }
    
    public boolean isEnabled(Map<T, Integer> marking) throws InterruptedException {
        protection.acquire();
        
        for(Arc<T> arc : arcs)
        if(!arc.isEnabled(marking)) {
            protection.release();
            return false;
        }
        
        protection.release();
        return true;
    }

    public Map<T, Integer> fire(Map<T, Integer> initial) throws InterruptedException {
        protection.acquire();
        Map<T, Integer> result = new TreeMap<>(initial);
        protection.release();

        for(Arc<T> arc : arcs)
            arc.fire(result);

        return result;
    }

    private Transition() {
        arcs = new ArrayList<>();
        protection = new Semaphore(1);
    }
}