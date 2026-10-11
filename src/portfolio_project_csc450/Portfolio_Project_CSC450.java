package portfolio_project_csc450;

public class Portfolio_Project_CSC450 {

	private static volatile boolean bExit = false;
	private static volatile boolean bDoneCountingUp = false;
	
	//custom exception class to test what we've learned
	//includes the four required constructors
	static class OperationsOutOfBoundsException extends Exception {
	
		private static final long serialVersionUID = 10211223L;
		
		public OperationsOutOfBoundsException(String message) {
			super(message);
		}
		
		public OperationsOutOfBoundsException(String message, Throwable cause) {
			super(message, cause);
		}
		
		public OperationsOutOfBoundsException() {
			super();
		}
		
		public OperationsOutOfBoundsException(Throwable cause) {
			super(cause);
		}
		
	}

	//counter object which holds the minimum and maximum values allowed
	//synchronizes all actions so that only one thread has access at a time
	//multiple counting up or counting down threads could be used
	//rather than just one of each with this design
	static class ConcurrentCounter  {
		private int iCounter = 0;
		private boolean bCanCountDown = false;
		private int iMaxCount = 0;
		private int iMinCount = 0;
		
		public synchronized int getCounter() {
			return iCounter;
		}
		
		public synchronized boolean getCanCountDown() {
			return bCanCountDown;
		}
		
		public synchronized int getMaxCount() {
			return iMaxCount;
		}
		
		public synchronized void setMaxCount(int iValue) {
			this.iMaxCount = iValue;
		}
		
		public synchronized int getMinCount() {
			return iMinCount;
		}
		
		public synchronized void setMinCount(int iValue) {
			this.iMinCount = iValue;
		}
		
		public synchronized boolean canIncrement() {
			return iCounter < iMaxCount;
		}

		public synchronized boolean canDecrement() {
			return iCounter > iMinCount;
		}
		
		public synchronized void incrementCounter() throws OperationsOutOfBoundsException {
			if (canIncrement()) {
				iCounter++;
				if (!canIncrement()) {
					bCanCountDown = true;
					notifyAll();
				}
			} 
			else {
				throw new OperationsOutOfBoundsException("You have attempted to increment the counter after the maximum has already been reached.  This operation is not permitted.");				
			}
		}
		
		public synchronized void incrementCounterAndPrint(int iThreadNum) throws OperationsOutOfBoundsException {			
			//System.out.println("Thread " + iThreadNum + " Executing");
			incrementCounter();
			printCounterInfo(true);
		}


		public synchronized void printCounterInfo(boolean countUp) {
			String countType = "up: ";
			if (!countUp) {
				countType = "down: ";
			}
			System.out.println("Counting " + countType + iCounter);
		}

		public synchronized void decrementCounter() throws OperationsOutOfBoundsException {
			while(!bCanCountDown) {
				try {
					wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
			if (canDecrement()) {
				iCounter--;
			}
			else {
				throw new OperationsOutOfBoundsException("You have attempted to decrement the counter after the minimum has already been reached.  This operation is not permitted.");				
			}
		}

		public synchronized void decrementCounterAndPrint() throws OperationsOutOfBoundsException {
			decrementCounter();
			printCounterInfo(false);
		}
		
	}	
	
	//thread for counting up, designed to allow multiples at the same time
	static class UpCounterThread implements Runnable {
		private final ConcurrentCounter myCounter;
		private int iThreadNum = 0;
		
		public UpCounterThread(ConcurrentCounter myCounter, int threadNum) {
			this.myCounter = myCounter;
			this.iThreadNum = threadNum;
		}

		@Override
		public void run() {
			System.out.println("Thread for counting up has started");
			while (!bDoneCountingUp) {
				if (!myCounter.canIncrement() || bDoneCountingUp) {
					bDoneCountingUp = true;
					break;
				}
				try {
				myCounter.incrementCounterAndPrint(iThreadNum);
				}
				catch (OperationsOutOfBoundsException e) {
					System.out.println(e.getMessage());
					bDoneCountingUp = true;
					break;
				}
			}
		}
	}
	
	//thread for couting down, designed to allow multiples at the same time
	static class DownCounterThread implements Runnable {
		private final ConcurrentCounter myCounter;
		private int iThreadNum = 0;
		
		public DownCounterThread(ConcurrentCounter myCounter, int threadNum) {
			this.myCounter = myCounter;
			this.iThreadNum = threadNum;
		}
		
		@Override
		public void run() {
			System.out.println("Thread for counting down has started");
			while (!bExit) {
				if ((myCounter.getCanCountDown() &&  !myCounter.canDecrement()) || bExit) {
					bExit = true;
					break;
				}
				try {
					myCounter.decrementCounterAndPrint();
				} catch (OperationsOutOfBoundsException e) {
					System.out.println(e.getMessage());
					bExit = true;
					break;
				}
			}
		}
	}

	public static void main(String[] args) {
		ConcurrentCounter myCounter = new ConcurrentCounter();

		//set a minimum and maximum count for more flexibility
		myCounter.setMaxCount(20);
		myCounter.setMinCount(0);

		//create two separate threads.  One responsible for counting up
		//and the other responsible for counting down
		Thread upCounter = new Thread(new UpCounterThread(myCounter, 1));
		Thread downCounter = new Thread(new DownCounterThread(myCounter, 1));

		//start both threads
		upCounter.start();
		downCounter.start();
		
		try {
			upCounter.join();
			downCounter.join();
		} catch (InterruptedException e) {
			System.err.println("The thread was interrupted unexpectedly");
			Thread.currentThread().interrupt();
		}
		
		
	}

}
