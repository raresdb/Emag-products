import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class OrderManager implements Runnable {
    AtomicInteger managersCount;
    AtomicInteger activeManagers;
    static Scanner inFile = null;
    static FileWriter outFile = null;
    ConcurrentHashMap<String, AtomicInteger> productCounts;

    OrderManager(String folder, ConcurrentHashMap productCounts,
        AtomicInteger activeManagers, AtomicInteger managersCount) throws Exception {

        if(inFile == null) {
            this.inFile = new Scanner(new File(folder + "/orders.txt"));
        }
        if(outFile == null) {
            this.outFile = new FileWriter("orders_out.txt");
        }

        this.productCounts = productCounts;
        this.activeManagers = activeManagers;
        this.managersCount = managersCount;
    }

    public void  run() {
        String orderId;
        int productCount;
        String[] line;

        while(true) {
            // reading from file
            synchronized (inFile) {
                // finish if 0 orders left
                if(!inFile.hasNextLine()) {
                    // close the files at the end
                    if(managersCount.decrementAndGet() == 0) {
                        inFile.close();

                        try {
                            outFile.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    // notify that the managers are done
                    if(activeManagers.decrementAndGet() == 0) {
                        synchronized (ProductWorker.class) {
                            ProductWorker.class.notifyAll();
                        }
                    }

                    break;
                }

                // actual reading :)
                line = inFile.nextLine().split(",");
            }

            orderId = line[0];
            productCount = Integer.parseInt(line[1]);

            // ignore empty orders
            if(productCount == 0)
                continue;

            productCounts.put(orderId, new AtomicInteger(productCount));

            // notify that the managers are done
            if(activeManagers.decrementAndGet() == 0) {
                synchronized (ProductWorker.class) {
                    ProductWorker.class.notifyAll();
                }
            }

            // wait for the products to be seeked
            synchronized (OrderManager.class) {
                try {
                    OrderManager.class.wait();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

            activeManagers.compareAndSet(0, managersCount.get());

            // write the result
            synchronized (outFile) {
                try {
                    outFile.append(orderId + "," + productCount);

                    if(productCounts.get(orderId).get() <= 0) {
                        outFile.append(",shipped");
                    }

                    outFile.append("\n");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
