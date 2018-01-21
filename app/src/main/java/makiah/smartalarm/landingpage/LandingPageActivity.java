package makiah.smartalarm.landingpage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TimePicker;

import makiah.smartalarm.R;
import makiah.smartalarm.camerafeed.CameraFeedActivity;
import makiah.smartalarm.introquestions.IntroQuestionsActivity;

public class LandingPageActivity extends Activity
{
    /**
     * Starts the app, and checks whether intro complete.
     * @param savedInstanceState doesn't matter, this is where we started.
     */
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing_page);

        // Decide whether we need to navigate to the Intro Questions activity.
        SharedPreferences sharedPref = getSharedPreferences("appsettings", Context.MODE_PRIVATE);
        if (!(sharedPref.contains(getString(R.string.intro_complete)) && sharedPref.getBoolean(getString(R.string.intro_complete), false)))
        {
            Intent intent = new Intent(this, IntroQuestionsActivity.class);
            startActivity(intent);
            finish();
        }
    }

    public void onTimeChosen(View view)
    {
        TimePicker timePicker = (TimePicker)findViewById(R.id.timePicker);

        Intent intent = new Intent(this, CameraFeedActivity.class);
        intent.putExtra("HOUR", timePicker.getHour());
        intent.putExtra("MINUTE", timePicker.getMinute());
        intent.putExtra("AM", timePicker.getBaseline());

        startActivity(intent);
        finish();
    }
}
