package makiah.smartalarm.logging;

import java.util.ArrayList;

public class ProcessConsole
{
    public final String processName;
    public String[] processData;

    private boolean currentlyActive;

    private ArrayList<ProcessConsole> parentList;

    public ProcessConsole (String processName, ArrayList<ProcessConsole> parentList)
    {
        this.processName = processName;
        processData = new String[0];

        this.parentList = parentList;
        this.parentList.add (this);
        currentlyActive = true;
    }

    public boolean isCurrentlyActive()
    {
        return currentlyActive;
    }

    public void write(String... data)
    {
        this.processData = data;
    }

    public void destroy ()
    {
        if (!currentlyActive)
            return;
        currentlyActive = false;

        parentList.remove (this);
    }
    public void revive ()
    {
        if (currentlyActive)
            return;
        currentlyActive = true;

        parentList.add (this);
    }
}