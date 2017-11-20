package makiah.smartalarm.cameraview;

import android.widget.TextView;

import makiah.smartalarm.R;
import makiah.smartalarm.logging.OnScreenLog;

public class CameraViewLogger extends OnScreenLog
{
    public CameraViewLogger(CameraViewActivity activity)
    {
        super(activity, (TextView) activity.findViewById(R.id.debugtextview));
    }
}