package makiah.smartalarm.landingpage;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import makiah.smartalarm.R;
import makiah.smartalarm.camerafeed.CameraFeedActivity;

public class LandingPageActivity extends Activity
{
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing_page);
    }

    /**
     * Starts the camera feed and the resulting analysis.
     * @param currentView
     */
    public void startAlarmSetter(View currentView)
    {
        Intent intent = new Intent(this, CameraFeedActivity.class);
        startActivity(intent);
    }
}
