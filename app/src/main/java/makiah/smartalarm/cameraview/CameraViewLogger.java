package makiah.smartalarm.cameraview;

import android.widget.TextView;

import com.makiah.makiahsandroidlib.logging.OnScreenLog;

import makiah.smartalarm.R;

public class CameraViewLogger extends OnScreenLog
{
    public CameraViewLogger(CameraViewActivity activity)
    {
        super(activity, (TextView) activity.findViewById(R.id.debugtextview));
    }
}