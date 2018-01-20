package makiah.smartalarm.introquestions;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import makiah.smartalarm.R;
import makiah.smartalarm.camerafeed.CameraFeedActivity;
import makiah.smartalarm.landingpage.LandingPageActivity;

public class IntroQuestionsActivity extends Activity
{
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro_questions);
    }

    /**
     * Looks through what the user checked (if anything) and tells them what could arise if they
     * use this app (for epilepsy, I want to make sure they're forewarned).
     */
    public void respondToSelections(View view)
    {
        // Get responses
        CheckBox[] checkBoxes = new CheckBox[]{(CheckBox)findViewById(R.id.isEpileptic), (CheckBox)findViewById(R.id.isHardTimeSleeping), (CheckBox)findViewById(R.id.isSleepWalk)};

        String disclaimerText = "";
        // Interpret responses.
        if (checkBoxes[0].isChecked())
            disclaimerText += "You're epileptic?  It might be bad that Smart Alarm uses a flashing light overnight, please don't use this if you think it will.  \n";
        if (checkBoxes[1].isChecked())
            disclaimerText += "You have a hard time sleeping?  Smart Alarm might wake you up while it uses the flash camera overnight.";
        if (checkBoxes[2].isChecked())
            disclaimerText += "You sleep walk?  This might not be the best solution for you, Smart Alarm will interpret this as you waking up.";

        // They didn't check anything.
        if (disclaimerText.equals(""))
        {
            enterLandingPageSuccessfully();
            return;
        }

        // Display disclaimer.
        ViewGroup responsePanel = (ViewGroup) findViewById(R.id.responsePanel);
        responsePanel.setVisibility(View.VISIBLE);
        TextView textView = (TextView) findViewById(R.id.disclaimerText);
        textView.setText(disclaimerText);
    }

    /**
     * Exit to the home team right away.
     * @param view some param for the UI to be happy
     */
    public void deniedDisclaimer(View view)
    {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Just enter the next page.
     * @param view ...
     */
    public void acceptedDisclaimer(View view)
    {
        enterLandingPageSuccessfully();
    }

    /**
     * Begin landing page and set intro complete in persistent shared preferences to true.
     */
    private void enterLandingPageSuccessfully()
    {
        // Set intro completion to true.
        SharedPreferences sharedPref = getSharedPreferences("appsettings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(getString(R.string.intro_complete), true);
        editor.commit();

        // Transition to next activity.
        Intent intent = new Intent(this, LandingPageActivity.class);
        startActivity(intent);
        finish();
    }
}
