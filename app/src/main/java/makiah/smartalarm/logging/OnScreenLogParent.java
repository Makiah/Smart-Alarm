package makiah.smartalarm.logging;

import makiah.smartalarm.threading.TaskParent;

public interface OnScreenLogParent extends TaskParent
{
    void runOnUiThread(Runnable action);
}
