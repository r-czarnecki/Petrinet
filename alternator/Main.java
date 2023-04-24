package alternator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import petrinet.PetriNet;
import petrinet.Transition;

public class Main {
    private static enum Place {
        A, B, C, CriticalSection
    }
    private static Map<Place, Collection<Transition<Place>>> begin = new HashMap<>();
    private static Map<Place, Collection<Transition<Place>>> end = new HashMap<>();
    private static PetriNet<Place> net;
    private static Map<Place, Integer> marking = new HashMap<>();
    private static Place[] places = {Place.A, Place.B, Place.C};

    private static class PrintThread implements Runnable {
        Place name;

        public PrintThread(Place name) {
            this.name = name;
        }

        @Override
        public void run() {
            try {
                while(true) {
                    if(Thread.currentThread().isInterrupted())
                        throw new InterruptedException();

                    net.fire(begin.get(name));
                
                    System.out.println(name);
                    System.out.println('.');

                    net.fire(end.get(name));

                }
            } catch(InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void setTransitions() {
        setBeginTransitions();
        setEndTransitions();
    }

    private static void setBeginTransitions() {
        for(Place place : places) {
            Map<Place, Integer> input = Collections.singletonMap(place, 1);
            Collection<Place> reset = new ArrayList<>();

            for(Place otherPlace : places) {
                if(otherPlace.equals(place))
                    continue;

                reset.add(otherPlace);
            }

            Collection<Place> inhibitor = Collections.singleton(Place.CriticalSection);
            Map<Place, Integer> output = Collections.singletonMap(Place.CriticalSection, 1);
            Transition<Place> transition = new Transition<>(input, reset, inhibitor, output);

            begin.put(place, Collections.singletonList(transition));
        }
    }

    private static void setEndTransitions() {
        for(Place place : places) {
            Map<Place, Integer> input = Collections.singletonMap(Place.CriticalSection, 1);
            Collection<Place> reset = Collections.emptyList();
            Collection<Place> inhibitor = Collections.singletonList(place);
            Map<Place, Integer> output = new HashMap<>();

            for(Place otherPlace : places) {
                if(otherPlace.equals(place))
                    continue;

                output.put(otherPlace, 1);
            }

            Transition<Place> transition = new Transition<>(input, reset, inhibitor, output);
            end.put(place, Collections.singletonList(transition));
        }
    }

    private static void setInitialMarking() {
        for(Place place : places)
            marking.put(place, 1);
        net = new PetriNet<>(marking, true);
    }

    private static boolean checkMarkings() {
        Collection<Transition<Place>> allTransitions = new ArrayList<>();
        for(Place place : places) {
            allTransitions.addAll(begin.get(place));
            allTransitions.addAll(end.get(place));
        }

        Set<Map<Place, Integer>> markings = net.reachable(allTransitions);

        for(Map<Place, Integer> map : markings) {
            if(map.containsKey(Place.CriticalSection) && map.get(Place.CriticalSection) >= 2)
                return false;
        }

        return true;
    }

    public static void main(String[] args) {
        setTransitions();
        setInitialMarking();
        
        if(!checkMarkings()) {
            System.out.println("Safety error");
            System.exit(1);
        }

        ArrayList<Thread> threads = new ArrayList<>();
        int i = 0;
        for(Place place : places) {
            threads.add(new Thread(new PrintThread(place)));
            threads.get(i).start();
            i++;
        }

        try {
            Thread.sleep(30000);
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            for(Thread t : threads)
                t.interrupt();
        }
    }

    public static void print(String s) {
        System.out.println(s);
    }
}