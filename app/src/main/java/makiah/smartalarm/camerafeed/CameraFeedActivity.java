package makiah.smartalarm.camerafeed;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.Calendar;
import java.util.Date;

import dude.makiah.androidlib.logging.OnScreenLogParent;
import dude.makiah.androidlib.threading.ParallelTask;
import makiah.smartalarm.R;
import makiah.smartalarm.postsleepfeedback.PostSleepFeedbackActivity;

public class CameraFeedActivity extends Activity implements OnScreenLogParent, CameraBridgeViewBase.CvCameraViewListener
{
    private static final String TAG = "CameraFeedActivity";

    // How long a human sleep cycle lasts.
    private static final long SLEEP_CYCLE_SECONDS = 90 * 60; // 90 minutes for sleep cycle on average, will be tuned based on my own measurements.

    // Stuff for the subtasks.
    public boolean isTaskActive()
    {
        return true;
    }

    // region Methods required by Activity
    // Loads camera view of OpenCV for us to use. This lets us see using OpenCV
    private CameraBridgeViewBase cameraBridgeViewBase;
    private JavaCameraViewWithFlash javaCameraView;

    // For visualization of sleep disturbance.
    private GraphView sleepGraph;
    private LineGraphSeries<DataPoint> sleepGraphPoints;

    // To observe either the camera OR the disturbance graph.
    private Button viewToggleButton;
    private boolean observingGraph = true; // whether we're observing the graph vs the camera.

    // When we have to wake the user up.
    private long alarmTimeSeconds = 0;

    /**
     * The callback for when OpenCV has finished initialization.
     */
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    cameraBridgeViewBase.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    /**
     * This task stops the camera thread and tells us when it's time for the user to wake up.
     */
    private ParallelTask alarmTask = new ParallelTask(this, "Wake Up Thread")
    {
        @Override
        protected void onDoTask() throws InterruptedException
        {
            long startTime = System.currentTimeMillis();
            while (alarmTimeSeconds - (System.currentTimeMillis() - startTime) / 1000.0 > 0)
                Thread.yield();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    timeToWakeUp();
                }
            });
        }
    };

    /**
     * Called upon the creation of the activity.
     *
     * @param savedInstanceState the state that the last launch was in.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_feed);

        // Get the data sent from the landing page, and determine how long this person will be sleeping.
        Bundle bundle = getIntent().getExtras();
        int chosenHour = bundle.getInt("HOUR"), chosenMinute = bundle.getInt("MINUTE");

        // Determine the current time.
        Date currentTime = Calendar.getInstance().getTime();
        Date chosenDate = new Date(currentTime.getYear(), currentTime.getMonth(), currentTime.getDate(), chosenHour, chosenMinute);

        // Decide whether this alarm will be for tomorrow.
        if (currentTime.after(chosenDate))
            chosenDate = new Date(currentTime.getYear(), currentTime.getMonth(), currentTime.getDate() + 1, chosenHour, chosenMinute);

        // Now decide how long this will take.
        alarmTimeSeconds = (long)((chosenDate.getTime() - currentTime.getTime()) / 1000.0);

        // Run the wake up thread.
        alarmTask.run();

        // Show chosen time (for 2:10 pm, it's 14/10)
        Toast.makeText(getBaseContext(), "" + chosenHour + "/" + chosenMinute + "/" + currentTime.getHours() + "/" + currentTime.getMinutes() + "/" + alarmTimeSeconds, Toast.LENGTH_LONG).show();

        Log.i(TAG, "called onCreate");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Create both a CameraBridgeViewBase and a JavaCameraView with flash so that I can modify the flash state while also being able to view the camera.
        javaCameraView = (JavaCameraViewWithFlash) findViewById(R.id.openCVCamFeed);

        // Set camera view base properties and direct frames to the restlessness detector.
        cameraBridgeViewBase = javaCameraView;
        cameraBridgeViewBase.enableFpsMeter();
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);
//        cameraBridgeViewBase.setMaxFrameSize(2000, 2000);

        // Initialize the graph view
        sleepGraph = (GraphView)findViewById(R.id.sleepGraph);
        sleepGraph.getViewport().setXAxisBoundsManual(true);
        sleepGraph.getViewport().setMinX(0);
        sleepGraph.getViewport().setMaxX(alarmTimeSeconds);
        sleepGraph.getViewport().setYAxisBoundsManual(true);
        sleepGraph.getViewport().setMinY(0);
        sleepGraph.getViewport().setMaxY(1);
        sleepGraphPoints = new LineGraphSeries<>();
        sleepGraph.addSeries(sleepGraphPoints);

        // Get other UI elements
        viewToggleButton = (Button)findViewById(R.id.toggleCameraButton);
    }

    /**
     * When the user navigates away from the activity.
     */
    @Override
    public void onPause()
    {
        super.onPause();

        if (cameraBridgeViewBase != null)
            cameraBridgeViewBase.disableView();
    }

    /**
     * When the user returns to this activity.
     */
    @Override
    public void onResume()
    {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    /**
     * When the user ends the activity.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (cameraBridgeViewBase != null)
            cameraBridgeViewBase.disableView();
    }

    //endregion

    private enum PictureState {FIRST_PICTURE, FIRST_FLASH_PICTURE, NORMAL_PROGRESSION}
    private PictureState pictureState = PictureState.FIRST_PICTURE;

    private Mat previous;

    /**
     * Initialize all mats here...
     *
     * @param width -  the width of the frames that will be delivered
     * @param height - the height of the frames that will be delivered
     */
    @Override
    public void onCameraViewStarted(int width, int height)
    {
        previous = Mat.zeros(new Size(width, height), CvType.CV_8UC1);
    }

    /**
     * And deinit them here.
     */
    @Override
    public void onCameraViewStopped()
    {
        previous.release();
    }

    private long analysisStartTime = 0;

    /**
     * Yields flow to other threads for a length of time.
     */
    private void pauseWhileYielding(long timeToPause)
    {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeToPause)
            Thread.yield();
    }

    /**
     * Where all of the image processing goes.
     */
    @Override
    public Mat onCameraFrame(Mat inputFrame)
    {
        try
        {
            // 4-channel to 3-channel (idk why we need alpha).
            Imgproc.cvtColor(inputFrame, inputFrame, Imgproc.COLOR_RGBA2GRAY);

            // region Setting up regular progression
            if (pictureState != PictureState.NORMAL_PROGRESSION)
            {
                if (pictureState == PictureState.FIRST_PICTURE)
                {
                    javaCameraView.setFlashState(true);
                    pauseWhileYielding(500);
                    pictureState = PictureState.FIRST_FLASH_PICTURE;
                    return inputFrame;
                }
                else if (pictureState == PictureState.FIRST_FLASH_PICTURE)
                {
                    inputFrame.copyTo(previous);
                    javaCameraView.setFlashState(false);
                    pauseWhileYielding(5000);
                    javaCameraView.setFlashState(true);
                    pictureState = PictureState.NORMAL_PROGRESSION;
                    analysisStartTime = System.currentTimeMillis();
                    return inputFrame;
                }
            }
            //endregion

            // Turn off flash during analysis.
            javaCameraView.setFlashState(false);

            // Determine how much this frame differs from the last.
            Mat diffMat = new Mat();
            Core.absdiff(inputFrame, previous, diffMat);
            inputFrame.copyTo(previous);
            Imgproc.threshold(diffMat, diffMat, 30, 255, Imgproc.THRESH_BINARY);

            // The extent to which this frame differs from the last.
            double saturation = Core.countNonZero(diffMat) / diffMat.size().area();

            // Append this data to the on-screen graph
            sleepGraphPoints.appendData(new DataPoint((System.currentTimeMillis() - analysisStartTime) / 1000.0, saturation), false, 5000);

            // Show on the input frame where differences were detected.
            inputFrame.setTo(new Scalar(255), diffMat);
            diffMat.release();

            // Wait for a bit
            pauseWhileYielding((long)(15000 * (1 - saturation) + 400));

            // Turn flash on and start new one.
            javaCameraView.setFlashState(true);
            pauseWhileYielding(500);
        }
        catch (Exception e)
        {}

        return inputFrame;
    }

    /**
     * Switches between camera and graph view.
     * @param view
     */
    public void toggleCurrentView(View view)
    {
        observingGraph = !observingGraph;

        if (observingGraph)
        {
            ((ViewGroup) findViewById(R.id.sleepGraphContainer)).setVisibility(View.VISIBLE);
            viewToggleButton.setText("Debug Camera View");
        }
        else
        {
            ((ViewGroup) findViewById(R.id.sleepGraphContainer)).setVisibility(View.GONE);
            viewToggleButton.setText("Observe Graph");
        }
    }

    /**
     * Pretty self-explanatory, called by thread.
     */
    public void timeToWakeUp()
    {
        ((Button)findViewById(R.id.wakeUpTime)).setVisibility(View.VISIBLE);

        // Stop the camera view.
        cameraBridgeViewBase.disableView();
    }

    /**
     * When the user accepts the "Time To Wake Up" prompt.
     */
    public void acceptedTimeToWakeUp(View view)
    {
        ((Button)findViewById(R.id.wakeUpTime)).setVisibility(View.GONE);
    }

    /**
     * Used by the UI for when the user wants to set their alarm for some given time.
     * @param currentView some weird parameter requested by the Button object.
     */
    public void postSleepFeedbackTime(View currentView)
    {
        Intent intent = new Intent(this, PostSleepFeedbackActivity.class);
        startActivity(intent);
    }
}