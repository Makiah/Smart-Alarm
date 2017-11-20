package makiah.smartalarm.threading;

public class Flow
{
    private final TaskParent parent;

    public Flow(TaskParent parent)
    {
        this.parent = parent;
    }

    /**
     * All rest/stop methods should run off of this method: it throws an exception when
     * the thread should stop.
     *
     * @throws InterruptedException — indicates that the thread should be stopped.
     */
    public void yield() throws InterruptedException
    {
        if (parent == null || !parent.isTaskActive())
            throw new InterruptedException("Stop requested!");

        Thread.yield();
    }

    /**
     * Pauses for some set length of time by calling yield() repeatedly.
     *
     * @param ms — the milliseconds to wait.
     * @throws InterruptedException — indicates that yield() said to stop execution.
     */
    public void msPause(long ms) throws InterruptedException
    {
        long startTime = System.currentTimeMillis ();

        while (System.currentTimeMillis () - startTime <= ms)
            yield();
    }
}
