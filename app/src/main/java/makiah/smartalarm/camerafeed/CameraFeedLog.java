package makiah.smartalarm.camerafeed;

import android.widget.TextView;

import com.makiah.makiahsandroidlib.logging.OnScreenLog;

import makiah.smartalarm.R;

public class CameraFeedLog extends OnScreenLog
{
    public CameraFeedLog(CameraFeedActivity activity)
    {
        super(activity, null); //(TextView) activity.findViewById(R.id.debugtextview));
    }
}