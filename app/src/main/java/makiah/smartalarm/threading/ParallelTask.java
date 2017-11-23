package makiah.smartalarm.threading;

import android.os.AsyncTask;
import android.util.Log;

import makiah.smartalarm.logging.OnScreenLog;
import makiah.smartalarm.logging.ProcessConsole;


// TODO Add old method of a ProcessConsole for every ParallelTask



/**
 * NiFTComplexTask is an easier method of working with AsyncTasks, which provides a convenient process console and a
 * bunch of other functionality to the table for an opmode which requires a bunch of advanced tooling.
 *
 * Warning: Moto Gs (and probably also ZTEs) can only run something like 5 tasks in parallel, and have to queue the
 * rest to run once the other tasks are completed.  KEEP THIS IN MIND, since if one task is inexplicably not running
 * this is probably why.
 */
public abstract class ParallelTask extends AsyncTask <Void, Void, Void>
{
    // Task properties.
    public final Flow flow;
    public final String taskName;

    // Log properties.
    private OnScreenLog screenLog;
    private ProcessConsole processConsole;

    /**
     * Creates a task with a given name and a console with that same name.
     */
    public ParallelTask (TaskParent parent)
    {
        this(parent, "Unnamed Task");
    }
    public ParallelTask (TaskParent parent, String taskName)
    {
        this.taskName = taskName;

        this.flow = new Flow(parent);
    }

    /**
     * Tells this task where to display output (on screen), to which the task responds by creating
     * a new process console.
     * @param screenLog
     */
    public void provideOnScreenLog(OnScreenLog screenLog)
    {
        this.processConsole = screenLog.newProcessConsole(taskName);
    }

    /**
     * Parallel Task instances should use this instead of trying to write to the process console on
     * their own.
     *
     * @param lines  Lines to log.
     */
    protected void logSequentialLines(String... lines)
    {
        if (screenLog != null)
            screenLog.lines(lines);
    }
    protected void logLinesToProcessConsole(String... lines)
    {
        if (processConsole != null)
            processConsole.write(lines);
    }

    /**
     * Runs the onDoTask() method while catching potential InterruptedExceptions, which indicate
     * that the user has requested a stop which was thrown in Flow.
     *
     * Runs the onQuitAndDestroyConsole() method on catching an InterruptedException, which
     * destroys the process console and ends the program.
     *
     * @param params can be safely ignored.
     */
    @Override
    protected final Void doInBackground (Void... params)
    {
        try
        {
            onDoTask ();
        }
        catch (InterruptedException e) //Upon stop requested by Flow
        {
            Log.i(taskName, "Stopped task!");
        }
        catch(Exception e)
        {
            // Yes, I know this is bad, but it prevents crashes (which are worse).
            Log.i(taskName, "Something weird happened!" + e.getMessage());
            Log.i(taskName, "Stack trace: " + getStackTrace(e));
        }
        finally
        {
            onQuitAndDestroyConsole ();
        }

        return null;
    }

    /**
     * Used to get stack trace info for weird errors.
     */
    private String getStackTrace(Exception e)
    {
        return "Class: " + e.getStackTrace()[0].getClassName() + ", Method: " + e.getStackTrace()[0].getMethodName() + ", Line: " + e.getStackTrace()[0].getLineNumber();
    }

    /**
     * When the stop() method is called, the doInBackground method halts and onCancelled is called, which causes console destruction and task end.
     */
    @Override
    protected final void onCancelled ()
    {
        onQuitAndDestroyConsole ();
    }

    /**
     * Inherit this method in child classes to actually accomplish something during your task.
     *
     * @throws InterruptedException
     */
    protected abstract void onDoTask () throws InterruptedException;

    /**
     * Used solely in this class, used to destroy the created process console and THEN
     * run the desired onCompletion method.
     */
    private void onQuitAndDestroyConsole ()
    {
        onQuitTask ();

        Log.i(taskName, "Quit " + taskName);

        if (processConsole == null)
            return;

        if (processConsole.isCurrentlyActive())
            processConsole.destroy();

        processConsole = null;
    }

    /**
     * Override this method if you want to do something when your task ends (regardless of whether it was cancelled or finished on its own).
     */
    protected void onQuitTask () {}

    /**
     * run() attempts to run the program in a try-catch block, and in the event of an
     * error, stops the attempt and returns an error to the user.
     */
    public final void run()
    {
        try
        {
            this.executeOnExecutor (AsyncTask.THREAD_POOL_EXECUTOR);

            if (processConsole == null)
                return;

            if (!processConsole.isCurrentlyActive())
                processConsole.revive();
        }
        catch (Exception e)
        {
            Log.i(taskName, "Uh oh! " + taskName + " can't run!" + e.getMessage ());
        }
    }
    /**
     * Stop attempts to cancel the given task, and reports an error if it cannot.
     */
    public final void stop()
    {
        try
        {
            this.cancel (true);

            if (processConsole == null)
                return;

            if (processConsole.isCurrentlyActive())
                processConsole.destroy();
        }
        catch (Exception e) // Dirty but prevents unwanted program crashes.
        {
            Log.i(taskName, "Uh oh! " + taskName + " can't stop!" + e.getMessage ());
        }
    }
}