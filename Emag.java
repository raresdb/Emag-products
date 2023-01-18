import java.lang.String;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Emag {
    public static void main(String[] args) throws Exception {
        String folder = args[0];
        int thrdCount = Integer.parseInt(args[1]);

        ConcurrentHashMap<String, AtomicInteger> productCounts = new ConcurrentHashMap();
        // not waiting
        AtomicInteger activeManagers = new AtomicInteger(thrdCount);
        AtomicInteger activeWorkers = new AtomicInteger(0);

        // threads still running
        AtomicInteger managersCount = new AtomicInteger(thrdCount);
        AtomicInteger workersCount = new AtomicInteger(thrdCount);

        ArrayList<Thread> threads = new ArrayList();

        for(int i = 0; i < 2 * thrdCount; i++) {
            if(i < thrdCount) {
                threads.add(new Thread(new ProductWorker(workersCount, activeWorkers, folder,
                        managersCount, productCounts)));
            } else {
                threads.add(new Thread(new OrderManager(folder, productCounts,
                        activeManagers, managersCount)));
            }

            threads.get(i).start();
        }

        for(Thread thrd : threads) {
            thrd.join();
        }
    }
}
