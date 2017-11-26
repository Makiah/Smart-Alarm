package makiah.smartalarm;

import android.widget.TextView;

import com.makiah.makiahsandroidlib.logging.OnScreenLog;

public class SmartAlarmLog extends OnScreenLog
{
    public SmartAlarmLog(SmartAlarmActivity activity)
    {
        super(activity, (TextView) activity.findViewById(R.id.debugtextview));
    }
}