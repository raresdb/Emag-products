#Butilca Rares

# Arguments for the run command
INPUT_FOLDER=./inputs
NUMBER_OF_THREADS=6

build:
	javac Emag.java OrderManager.java ProductWorker.java

run:
	java Emag $(INPUT_FOLDER) $(NUMBER_OF_THREADS)

clean:
	rm Emag.class OrderManager.class ProductWorker.class

# Removes the output files
clean_out:
	rm order_products_out.txt orders_out.txt