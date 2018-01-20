package makiah.smartalarm.introquestions;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import makiah.smartalarm.R;

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
    public void respondToSelections()
    {
        // TODO respond to selections by changing activity content

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(getString(R.string.intro_complete), true);
        editor.commit();
    }
}
