package makiah.smartalarm.camerafeed;

import dude.makiah.androidlib.logging.OnScreenLog;

public class CameraFeedLog extends OnScreenLog
{
    public CameraFeedLog(CameraFeedActivity activity)
    {
        super(activity, null); //(TextView) activity.findViewById(R.id.debugtextview));
    }
}