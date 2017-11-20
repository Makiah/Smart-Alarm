package makiah.smartalarm.logging;

import android.text.TextUtils;
import android.widget.TextView;

import java.util.ArrayList;

import makiah.smartalarm.threading.ParallelTask;

/**
 * The Advanced Console is an easy way to visualize a large number of tasks in parallel without having to rely
 * on superhuman vision.  It also supports sequential logging in the same window.
 *
 * This console uses a task to update its content so that it isn't erratic when displayed on the screen.
 *
 * Local so that resetting is not an issue.
 */
public abstract class OnScreenLog
{
    private final OnScreenLogParent parent;
    private final TextView log;

    private static final int MAX_SEQUENTIAL_LINES = 13;
    private ArrayList<String> sequentialConsoleData; //Lines being added and removed.
    private ArrayList<ProcessConsole> privateProcessConsoles;

    private ConsoleUpdater consoleUpdaterInstance = null;

    public OnScreenLog(OnScreenLogParent parent, TextView log)
    {
        this.parent = parent;
        this.log = log;

        //Initialize required components.
        sequentialConsoleData = new ArrayList<>();
        privateProcessConsoles = new ArrayList<>();

        startConsoleUpdater();
    }

    public void lines(String... newLines)
    {
        //Add new line at beginning of the lines.
        for (String line: newLines)
            sequentialConsoleData.add (0, line);
        //If there is more than 5 lines there, remove one.
        while (sequentialConsoleData.size () > MAX_SEQUENTIAL_LINES)
            sequentialConsoleData.remove (MAX_SEQUENTIAL_LINES);
    }

    public void appendToLastSequentialLine (String toAppend)
    {
        String result = sequentialConsoleData.get (0) + toAppend;
        sequentialConsoleData.remove (0);
        sequentialConsoleData.add (0, result);
    }

    /**
     * To get a private process console, create a new Log.ProcessConsole(<name here>) and then run write() to provide new content.
     */

    public ProcessConsole newProcessConsole(String name)
    {
        return new ProcessConsole(name, privateProcessConsoles);
    }

    /**
     * The task which updates the console at a fairly slow rate but your eye can't tell the difference.
     */
    private class ConsoleUpdater extends ParallelTask
    {
        public ConsoleUpdater() {
            super(parent, "Console Updater");
        }

        @Override
        protected void onDoTask () throws InterruptedException
        {
            while (true)
            {
                refreshConsole();
                flow.msPause(300);
            }
        }
    }

    /**
     * Creates a new console updater instance and runs it.
     */
    public void startConsoleUpdater ()
    {
        if (consoleUpdaterInstance == null)
        {
            consoleUpdaterInstance = new ConsoleUpdater();
            consoleUpdaterInstance.run();
        }
    }

    /**
     * Nullifies a new console instance and stops it.
     */
    public void stopConsoleUpdater ()
    {
        if (consoleUpdaterInstance != null)
        {
            consoleUpdaterInstance.stop ();
            consoleUpdaterInstance = null;
        }
    }

    /**
     * Rebuilds the whole console (call minimally, allow the task to take care of it.)
     */
    public void refreshConsole ()
    {
        parent.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String entireConsole = "";
                entireConsole = entireConsole.concat("—————— Sequential Data —————————\n");
                entireConsole = entireConsole.concat(TextUtils.join("\n", sequentialConsoleData));

                if (privateProcessConsoles.size() > 0)
                {
                    entireConsole = entireConsole.concat("\n—————— Process Console Data —————————\n");
                    for (ProcessConsole processConsole : privateProcessConsoles)
                    {
                        entireConsole = entireConsole.concat(TextUtils.join("\n", processConsole.processData));
                    }
                }

                log.setText(entireConsole);
            }
        });
    }
}
