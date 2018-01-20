package makiah.smartalarm.postsleepfeedback;

import android.app.Activity;
import android.os.Bundle;

import makiah.smartalarm.R;

public class PostSleepFeedbackActivity extends Activity
{
    /**
     * Just sets the layout of the post sleep feedback window, but also provides the sleep analysis
     * data in the savedInstanceState.
     * @param savedInstanceState The sleep analysis data (from the camera feed activity).
     */
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_sleep_feedback);
    }
}
