package multiplikator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import petrinet.PetriNet;
import petrinet.Transition;

public class Main {
    private static enum Place {
        A1, A2, B, Result, Loop
    }
    private static Collection<Transition<Place>> transitions = new ArrayList<>();
    private static Collection<Transition<Place>> end = new ArrayList<>();
    private static PetriNet<Place> net;
    private static volatile Map<Place, Integer> marking = new HashMap<>();

    private static HashMap<Place, Integer> addWeight1Arcs(Place[] places) {
        HashMap<Place, Integer> map = new HashMap<>();
        for(Place place : places)
            map.put(place, 1);
        return map;
    }

    private static Transition<Place> createWeight1Transition(Place[] in, Place[] res, Place[] inh, Place[] out) {
        Map<Place, Integer> input = addWeight1Arcs(in);
        Collection<Place> reset = new ArrayList<>();
        for(Place place : res)
            reset.add(place);

        Collection<Place> inhibitor = new ArrayList<>();
        for(Place place : inh)
            inhibitor.add(place);

        Map<Place, Integer> output = addWeight1Arcs(out);
        return new Transition<>(input, reset, inhibitor, output);
    }

    private static void setTransitions() {
        transitions.add(createWeight1Transition(new Place[]{Place.A1, Place.B, Place.Loop},
                                                new Place[]{},
                                                new Place[]{},
                                                new Place[]{Place.Loop, Place.B, Place.Result, Place.A2}));
        transitions.add(createWeight1Transition(new Place[]{Place.A2, Place.B},
                                                new Place[]{},
                                                new Place[]{Place.Loop},
                                                new Place[]{Place.A1, Place.B, Place.Result}));
        transitions.add(createWeight1Transition(new Place[]{Place.B},
                                                new Place[]{},
                                                new Place[]{Place.A2, Place.Loop},
                                                new Place[]{Place.Loop}));
        transitions.add(createWeight1Transition(new Place[]{Place.Loop, Place.B},
                                                new Place[]{},
                                                new Place[]{Place.A1},
                                                new Place[]{}));
        
        end.add(createWeight1Transition(new Place[]{},
                                        new Place[]{}, 
                                        new Place[]{Place.B}, 
                                        new Place[]{}));
    }

    private static class Multiplicate implements Runnable {
        private int fires;
        
        public Multiplicate() {
            this.fires = 0;
        }
        
        @Override
        public void run() {
            try {
                while(true) {
                    if(Thread.currentThread().isInterrupted())
                        throw new InterruptedException();
                    
                    net.fire(transitions);
                    fires++;
                }
            } catch(InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println(Thread.currentThread() + " fired " + fires + " times.");
            }
        }
    }
    public static void main(String[] args) {
        setTransitions();
        
        Scanner sc = new Scanner(System.in);
        int A = sc.nextInt();
        int B = sc.nextInt();
        sc.close();

        marking.put(Place.Loop, 1);
        if(A != 0)
            marking.put(Place.A1, A);

        if(B != 0)
            marking.put(Place.B, B);
        net = new PetriNet<>(marking, false);

        ArrayList<Thread> threads = new ArrayList<>();
        for(int i = 0; i < 4; i++) {
            Thread t = new Thread(new Multiplicate());
            t.start();
            threads.add(t);
        }

        try {
            net.fire(end);
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            for(Thread t : threads)
                t.interrupt();
            
            if(marking.containsKey(Place.Result))
                System.out.println(marking.get(Place.Result));
            else
                System.out.println(0);
        }
    }
}