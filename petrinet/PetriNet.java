package petrinet;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PetriNet<T> {
    private Map<T, Integer> marking;
    private boolean fair;
    private Semaphore protection;
    private Semaphore protection2;
    private Semaphore wait;
    private AtomicInteger waitingThreads;
    private AtomicBoolean isWorking;
    private CyclicBarrier barrier;
    private Instant bestTime;
    private Thread bestThread;
    private Random random;

    public PetriNet(Map<T, Integer> initial, boolean fair) {
        this.marking = initial;
        this.fair = fair;
        initialiseFiring();
        this.random = new Random();
    }

    public Set<Map<T, Integer>> reachable(Collection<Transition<T>> transitions) {
        Set<Map<T, Integer>> result = new HashSet<>();

        try {
            protection.acquire();
            Map<T, Integer> copy = new TreeMap<>(marking);
            protection.release();
            
            DFS(transitions, copy, result);
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Reachable: Thread interrupted");
        }     

        return result;
    }

    public Transition<T> fire(Collection<Transition<T>> transitions) throws InterruptedException {
        try {
            protection.acquire();
            waitingThreads.getAndIncrement();
            Instant time = Clock.systemDefaultZone().instant();
            boolean needToWait = isWorking.get();
            ArrayList<Transition<T>> enabledTransitions = new ArrayList<>();
            if(!needToWait) {
                enabledTransitions = getEnabled(transitions);
                if(enabledTransitions.size() != 0)
                    isWorking.set(true);
                else
                    needToWait = true;
            }
                
            protection.release();
            
            if(needToWait) {
                while(true) {
                    wait.acquire();
                    enabledTransitions = getEnabled(transitions);
                    if(fair && enabledTransitions.size() != 0) {
                        protection2.acquire();
                        if(time.isBefore(bestTime)) {
                            isWorking.set(true);
                            bestTime = time;
                            bestThread = Thread.currentThread();
                        }
                        protection2.release();
                    }
                    else if(!fair && enabledTransitions.size() != 0) {
                        isWorking.set(true);
                        bestThread = Thread.currentThread();
                    }
                    
                    barrier.await();
                    if(Thread.currentThread().equals(bestThread))
                    break;
                }
            }
            
            Transition<T> chosenTransition = fireRandom(enabledTransitions);
            
            
            protection.acquire();
            isWorking.set(false);
            barrier = new CyclicBarrier(waitingThreads.get());
            bestThread = null;
            bestTime = Clock.systemDefaultZone().instant().plusSeconds(10);
            wait.release(waitingThreads.get() - 1);
            barrier.await();
            waitingThreads.decrementAndGet();
            protection.release();
            
            return chosenTransition;
        } catch(InterruptedException e) {
            initialiseFiring();
            throw new InterruptedException();
        } catch(BrokenBarrierException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedException();
        }
    }

    private void initialiseFiring() {
        this.protection = new Semaphore(1);
        this.protection2 = new Semaphore(1);
        this.wait = new Semaphore(0);
        this.waitingThreads = new AtomicInteger(0);
        this.isWorking = new AtomicBoolean(false);
        this.barrier = new CyclicBarrier(1);
    }

    private void DFS(Collection<Transition<T>> transitions, Map<T, Integer> marking, Set<Map<T, Integer>> currentSet) throws InterruptedException {
        if(currentSet.contains(marking))
            return;
        
        currentSet.add(marking);

        for(Transition<T> t : transitions) 
            if(t.isEnabled(marking))
                DFS(transitions, t.fire(marking), currentSet);
    }

    private ArrayList<Transition<T>> getEnabled(Collection<Transition<T>> transitions) throws InterruptedException {
        ArrayList<Transition<T>> result = new ArrayList<>();
        for(Transition<T> t : transitions)
            if(t.isEnabled(marking))
                result.add(t);
        
        return result;
    }

    private Transition<T> fireRandom(ArrayList<Transition<T>> transitions) throws InterruptedException {
        int pos = random.nextInt(transitions.size());
        Transition<T> chosenTransition = transitions.get(pos);
        Map<T, Integer> result = chosenTransition.fire(marking);
        marking.clear();
        marking.putAll(result);
        return chosenTransition;
    }

    public Map<T, Integer> marking() {
        return marking;
    }
}