import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ProductWorker implements Runnable{
    AtomicInteger workersCount;
    AtomicInteger managersCount;
    AtomicInteger activeWorkers;
    static Scanner inFile = null;
    static FileWriter outFile = null;
    ConcurrentHashMap<String, AtomicInteger> productCounts;
    String folder;

    public ProductWorker(AtomicInteger workersCount, AtomicInteger activeWorkers,
                         String folder, AtomicInteger managersCount,
                         ConcurrentHashMap<String, AtomicInteger> productCounts) throws Exception {
        this.workersCount = workersCount;
        this.activeWorkers = activeWorkers;
        this.folder = folder;
        if(inFile == null) {
            this.inFile = new Scanner(new File(folder + "/order_products.txt"));
        }
        if(outFile == null) {
            this.outFile = new FileWriter("order_products_out.txt");
        }

        this.productCounts = productCounts;
        this.managersCount = managersCount;
    }

    public void run() {
        String orderId;
        String productId;
        String[] line;

        while(true) {
            // wait to add orders
            synchronized (ProductWorker.class) {
                try {
                    ProductWorker.class.wait();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

            activeWorkers.compareAndSet(0, workersCount.get());

            // if no more managers are running quit
            if(managersCount.get() == 0) {
                if(workersCount.decrementAndGet() == 0) {
                    inFile.close();

                    try {
                        outFile.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                break;
            }

            // reading the file
            while(true) {
                synchronized (inFile) {
                    if (!inFile.hasNextLine()) {
                        break;
                    }

                    line = inFile.nextLine().split(",");
                }

                orderId = line[0];
                productId = line[1];

                // update the order if it's the case
                if (productCounts.containsKey(orderId) &&
                        productCounts.get(orderId).decrementAndGet() >= 0) {
                    synchronized (outFile) {
                        try {
                            outFile.append(orderId + "," + productId + ",shipped\n");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            // close the files if done
            if(activeWorkers.decrementAndGet() == 0) {
                inFile.close();
                try {
                    inFile = new Scanner(new File(folder + "/order_products.txt"));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // notify that workers are done
                synchronized (OrderManager.class) {
                    OrderManager.class.notifyAll();
                }
            }
        }
    }
}
