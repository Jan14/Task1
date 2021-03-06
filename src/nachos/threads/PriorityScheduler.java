//ytxh{binofudofxhgdofyh`nridojdptkh`o`ov`ofrhvdh
//PART OF THE NACHOS. DON'T CHANGE CODE OF THIS LINE
package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.LinkedList;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
	
	public static void selfTestRun( KThread t1, int t1p, KThread t2, int t2p) {

		boolean int_state;

		int_state = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority( t1, t1p );
		ThreadedKernel.scheduler.setPriority( t2, t2p );
		Machine.interrupt().restore( int_state );
		
		t1.setName("a").fork();
		t2.setName("b").fork();
		t1.join();
		t2.join();
	}
	
	public static void selfTestRun( KThread t1, int t1p, KThread t2, int t2p, KThread t3, int t3p ) {

		boolean int_state;

		int_state = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority( t1, t1p );
		ThreadedKernel.scheduler.setPriority( t2, t2p );
		ThreadedKernel.scheduler.setPriority( t3, t3p );
		Machine.interrupt().restore( int_state );
		
		t1.setName("a").fork();
		t2.setName("b").fork();
		t3.setName("c").fork();
		t1.join();
		t2.join();
		t3.join();
	}

	/**
	 * Tests whether this module is working.
	 */
	public static void selfTest() {

		KThread t1, t2, t3;
		final Lock lock;
		final Condition condition;

		/*
		 * Case 1: Tests priority scheduler without donation
		 *
		 * This runs t1 with priority 7, and t2 with priority 4.
		 *
		 */

		System.out.println( "Case 1:" );

		t1 = new KThread(new Runnable() {
				public void run() {
					System.out.println( KThread.currentThread().getName() + " started working" );
					for( int i = 0; i < 10; ++i ) {
						System.out.println( KThread.currentThread().getName() + " working " + i );	
						KThread.yield();
					}
					System.out.println( KThread.currentThread().getName() + " finished working" );
				}
			});

		t2 = new KThread(new Runnable() {
				public void run() {
					System.out.println( KThread.currentThread().getName() + " started working" );
					for( int i = 0; i < 10; ++i ) {
						System.out.println( KThread.currentThread().getName() + " working " + i );	
						KThread.yield();
					}
					System.out.println( KThread.currentThread().getName() + " finished working" );
				}

			});

		selfTestRun( t1, 7, t2, 4 );

		/*
		 * Case 2: Tests priority scheduler without donation, altering
		 * priorities of threads after they've started running
		 *
		 * This runs t1 with priority 7, and t2 with priority 4, but
		 * half-way through t1's process its priority is lowered to 2.
		 *
		 */

		System.out.println( "Case 2:" );

		t1 = new KThread(new Runnable() {
				public void run() {
					System.out.println( KThread.currentThread().getName() + " started working" );
					for( int i = 0; i < 10; ++i ) {
						System.out.println( KThread.currentThread().getName() + " working " + i );	
						KThread.yield();
						if( i == 4 ) {
							System.out.println( KThread.currentThread().getName() + " reached 1/2 way, changing priority" );
							boolean int_state = Machine.interrupt().disable();
							ThreadedKernel.scheduler.setPriority( 2 );
							Machine.interrupt().restore( int_state );
						}
					}
					System.out.println( KThread.currentThread().getName() + " finished working" );
				}
			});

		t2 = new KThread(new Runnable() {
				public void run() {
					System.out.println( KThread.currentThread().getName() + " started working" );
					for( int i = 0; i < 10; ++i ) {
						System.out.println( KThread.currentThread().getName() + " working " + i );	
						KThread.yield();
					}
					System.out.println( KThread.currentThread().getName() + " finished working" );
				}

			});

		selfTestRun( t1, 7, t2, 4 );

		/*
		 * Case 3: Tests priority donation
		 *
		 * This runs t1 with priority 7, t2 with priority 6 and t3 with
		 * priority 4. t1 will wait on a lock, and while t2 would normally
		 * then steal all available CPU, priority donation will ensure that
		 * t3 is given control in order to help unlock t1.
		 *
		 */

		System.out.println( "Case 3:" );

		lock = new Lock();
		condition = new Condition( lock );

		t1 = new KThread(new Runnable() {
			public void run() {
				lock.acquire();
				System.out.println( KThread.currentThread().getName() + " active" );
				lock.release();
			}
		});

		t2 = new KThread(new Runnable() {
				public void run() {
					for( int i = 0; i < 3; ++i ) {
						System.out.println( KThread.currentThread().getName() + " working " + i );	
						KThread.yield();
					}
					System.out.println( KThread.currentThread().getName() + " finished working" );
				}

			});

		t3 = new KThread(new Runnable() {
			public void run() {
				lock.acquire();

				boolean int_state = Machine.interrupt().disable();
				ThreadedKernel.scheduler.setPriority( 2 );
				Machine.interrupt().restore( int_state );

				KThread.yield();

				// t1.acquire() will now have to realise that t3 owns the lock it wants to obtain
				// so program execution will continue here.

				System.out.println( KThread.currentThread().getName() + " active ('a' wants its lock back so we are here)" );
				lock.release();
				KThread.yield();
				lock.acquire();
				System.out.println( KThread.currentThread().getName() + " active-again (should be after 'a' and 'b' done)" );
				lock.release();

			}
		});

		selfTestRun( t1, 6, t2, 4, t3, 7 );

	}

	
    public PriorityScheduler() {
    }

    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());

	return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());

	return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
	Lib.assertTrue(Machine.interrupt().disabled());

	Lib.assertTrue(priority >= priorityMinimum &&
		   priority <= priorityMaximum);

	getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
	boolean intStatus = Machine.interrupt().disable();

	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMaximum)
	    return false;

	setPriority(thread, priority+1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    public boolean decreasePriority() {
	boolean intStatus = Machine.interrupt().disable();

	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMinimum)
	    return false;

	setPriority(thread, priority-1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
	if (thread.schedulingState == null)
	    thread.schedulingState = new ThreadState(thread);

	return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
	PriorityQueue(boolean transferPriority) {
	    this.transferPriority = transferPriority;
	}

	public void waitForAccess(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).waitForAccess(this);
	}

	public void acquire(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).acquire(this);
	}

	public KThread nextThread() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    
	    // implement me
	    ThreadState next = pickNextThread();
	    if (next == null) {
	    	owner = null;
	    	return null;
	    }
	    if (owner != null) {
    		owner.donorQueue.remove(this);
    		owner.getEffectivePriority();
    	}
	    this.acquire(next.thread);
		return next.thread;
	}

	/**
	 * Return the next thread that <tt>nextThread()</tt> would return,
	 * without modifying the state of this queue.
	 *
	 * @return	the next thread that <tt>nextThread()</tt> would
	 *		return.
	 */
	protected ThreadState pickNextThread() {
	    // implement me
		if (waitingQueue.isEmpty())
			return null;
		return waitingQueue.peek();
	}

	public void print() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    
	    // implement me (if you want)
	    
	}

	/**
	 * <tt>true</tt> if this queue should transfer priority from waiting
	 * threads to the owning thread.
	 */
	public boolean transferPriority;
	
	//Task 1.5
	private ThreadState owner = null;
	private java.util.PriorityQueue<ThreadState> waitingQueue = new java.util.PriorityQueue<ThreadState>();
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState  implements Comparable {
	/**
	 * Allocate a new <tt>ThreadState</tt> object and associate it with the
	 * specified thread.
	 *
	 * @param	thread	the thread this state belongs to.
	 */
	public ThreadState(KThread thread) {
	    this.thread = thread;

	    setPriority(priorityDefault);
	}
	
	/**
	 * Update the priority of a thread and its owner.
	 * 
	 * @param 	A	the ThreadState to update
	 * @param	p	the value we want to assign to the effective priority of A
	 */
	public void update(ThreadState A, int p) {
		A.effectivePriority = p;
		if (A.waitingOn == null)
			return;
		if (A.waitingOn.owner == null)
			return;
		if (A.waitingOn.owner.effectivePriority > p)
			update(A.waitingOn.owner, p);
	}

	/**
	 * Return the priority of the associated thread.
	 *
	 * @return	the priority of the associated thread.
	 */
	public int getPriority() {
	    return priority;
	}

	/**
	 * Return the effective priority of the associated thread.
	 *
	 * @return	the effective priority of the associated thread.
	 */
	public int getEffectivePriority() {
	    // implement me
		effectivePriority = priority;
		for (PriorityQueue queue : donorQueue)
			if (queue.transferPriority)
				for (ThreadState t : queue.waitingQueue)
					if (t.effectivePriority > effectivePriority)
						effectivePriority = t.effectivePriority;
		PriorityQueue queue = (PriorityQueue) thread.joinQueue;
		if (queue.transferPriority)
			for (ThreadState t : queue.waitingQueue)
				if (t.effectivePriority > effectivePriority)
					effectivePriority = t.effectivePriority;
	    return effectivePriority;
	}

	/**
	 * Set the priority of the associated thread to the specified value.
	 *
	 * @param	priority	the new priority.
	 */
	public void setPriority(int priority) {
	    if (this.priority == priority)
		return;

	    this.priority = priority;

	    // implement me
		update(this, getEffectivePriority());
	}

	/**
	 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
	 * the associated thread) is invoked on the specified priority queue.
	 * The associated thread is therefore waiting for access to the
	 * resource guarded by <tt>waitQueue</tt>. This method is only called
	 * if the associated thread cannot immediately obtain access.
	 *
	 * @param	waitQueue	the queue that the associated thread is
	 *				now waiting on.
	 *
	 * @see	nachos.threads.ThreadQueue#waitForAccess
	 */
	public void waitForAccess(PriorityQueue waitQueue) {
	    // implement me
		waitQueue.waitingQueue.add(this);
		waitingOn = waitQueue;
		if (waitQueue.owner != null)
			waitQueue.owner.getEffectivePriority();
	}

	/**
	 * Called when the associated thread has acquired access to whatever is
	 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
	 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
	 * <tt>thread</tt> is the associated thread), or as a result of
	 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
	 *
	 * @see	nachos.threads.ThreadQueue#acquire
	 * @see	nachos.threads.ThreadQueue#nextThread
	 */
	public void acquire(PriorityQueue waitQueue) {
	    // implement me
		waitQueue.waitingQueue.remove(this);
		waitQueue.owner = this;
		donorQueue.add(waitQueue);
		getEffectivePriority();
	}
	
	//Task 1.5
	public int compareTo(Object t) {
		ThreadState state = (ThreadState) t;
		if (this.effectivePriority < state.effectivePriority)
			return 1;
		else if (this.effectivePriority > state.effectivePriority) 
			return -1;		
		return 0;
	}

	/** The thread with which this object is associated. */
	protected KThread thread;
	/** The priority of the associated thread. */
	protected int priority;
	
	//Task 1.5
	protected LinkedList<PriorityQueue> donorQueue = new LinkedList<PriorityQueue>();
	protected PriorityQueue waitingOn = null;
	protected int effectivePriority = 0;
    }
}
