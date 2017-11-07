package makiah.smartalarm.startui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import makiah.smartalarm.R;
import makiah.smartalarm.alarmsetter.AlarmSetterActivity;
import makiah.smartalarm.cameraview.CameraViewActivity;

public class StartUIActivity extends Activity
{
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_ui_activity);
    }

    /**
     * Called by UI when this button tapped.
     * @param v the current UI state (a required parameter for this method)
     */
    public void startCameraDebug(View v)
    {
        Intent intent = new Intent(this, CameraViewActivity.class);
        startActivity(intent);
    }

    /**
     * Called by UI when the respective button is tapped.
     * @param v the current UI state (a required parameter for this method).
     */
    public void startAlarmSetter(View v)
    {
        Intent intent = new Intent(this, AlarmSetterActivity.class);
        startActivity(intent);
    }
}
