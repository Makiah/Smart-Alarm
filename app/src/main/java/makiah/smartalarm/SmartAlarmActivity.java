package makiah.smartalarm;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import com.makiah.makiahsandroidlib.logging.OnScreenLogParent;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import makiah.smartalarm.alarmsetter.AlarmSetterActivity;
import makiah.smartalarm.opencvutilities.JavaCameraViewWithFlash;

public class SmartAlarmActivity extends AppCompatActivity implements OnScreenLogParent, CameraBridgeViewBase.CvCameraViewListener
{
    private static final String TAG = "SmartAlarmActivity";

    // The minimum camera pixel dimensions that will be requested by this app.
    private static final int FRAME_WIDTH_REQUEST = 176, FRAME_HEIGHT_REQUEST = 144;

    // The area of the camera which has to be obscured with noise to reset a sleep cycle.
    private static final double RESTLESSNESS_NOISE_SATURATION_THRESHOLD = .3;

    // Stuff for the subtasks.
    private boolean taskActive = true;
    public boolean isTaskActive()
    {
        return taskActive;
    }

    // Loads camera view of OpenCV for us to use. This lets us see using OpenCV
    private CameraBridgeViewBase cameraBridgeViewBase;
    private JavaCameraViewWithFlash javaCameraView;

    // This does a lot of the grunt work involved in the sleep processing.
    private SmartAlarmLog onScreenLog;

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
     * Used by the UI for when the user wants to set their alarm for some given time.
     * @param currentView
     */
    public void startAlarmSetter(View currentView)
    {
        Intent intent = new Intent(this, AlarmSetterActivity.class);
        startActivity(intent);
    }

    /**
     * Called upon the creation of the activity.
     *
     * @param savedInstanceState the state that the last launch was in.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_alarm);

        taskActive = true;

        Log.i(TAG, "called onCreate");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Create both a CameraBridgeViewBase and a JavaCameraView with flash so that I can modify the flash state while also being able to view the camera.
        javaCameraView = (JavaCameraViewWithFlash) findViewById(R.id.show_camera_activity_java_surface_view);

        // Required components which control app stuff.
        onScreenLog = new SmartAlarmLog(this);

        // Set camera view base properties and direct frames to the restlessness detector.
        cameraBridgeViewBase = javaCameraView;
        cameraBridgeViewBase.enableFpsMeter();
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setMinimumHeight(FRAME_HEIGHT_REQUEST);
        cameraBridgeViewBase.setMinimumWidth(FRAME_WIDTH_REQUEST);

        cameraBridgeViewBase.setCvCameraViewListener(this);
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

        taskActive = false;
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
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        taskActive = true;

        // Restart the on screen log.
        onScreenLog.run();
    }

    /**
     * When the user ends the activity.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (cameraBridgeViewBase != null)
            cameraBridgeViewBase.disableView();

        taskActive = false;
    }
    /// Custom methods I added for this app ///

    public void setFlashState(boolean state) {javaCameraView.setFlashState(state);}

    /**
     * A button on the UI triggers this function.
     */
    public void toggleCameraFlash()
    {
        javaCameraView.toggleFlashState();
    }
    /**
     * Called from UI instead (so View param included).
     *
     * @param currentView A required parameter for the UI element method.
     */
    public void toggleCameraFlash(View currentView)
    {
        toggleCameraFlash();
    }

    // The last frame returned by the camera (for comparison).
    private Mat previous, binaryDiff;
    private enum PictureState {FIRST_FLASH, FIRST_ANCHOR, TYPICAL_PROGRESSION}
    private PictureState currentPictureState = PictureState.FIRST_FLASH;

    /**
     * Initialize all mats here...
     *
     * @param width -  the width of the frames that will be delivered
     * @param height - the height of the frames that will be delivered
     */
    @Override
    public void onCameraViewStarted(int width, int height) {
        previous = new Mat(height, width, CvType.CV_8UC1);
        binaryDiff = new Mat();
        currentPictureState = PictureState.FIRST_FLASH;
    }

    /**
     * And deinit them here.
     */
    @Override
    public void onCameraViewStopped() {
        binaryDiff.release();
        previous.release();
    }

    private long lastUpdateTime = -1;

    /**
     * Where all of the image processing goes.
     */
    @Override
    public Mat onCameraFrame(Mat inputFrame)
    {
        try
        {
            Imgproc.cvtColor(inputFrame, inputFrame, Imgproc.COLOR_RGB2GRAY);

            switch(currentPictureState)
            {
                case FIRST_FLASH:
                    setFlashState(true);
                    Thread.sleep(1000);

                    // Return empty.
                    onScreenLog.lines("First flash complete.");
                    currentPictureState = PictureState.FIRST_ANCHOR;
                    return inputFrame;

                case FIRST_ANCHOR:
                    inputFrame.copyTo(previous);

                    // Return empty.
                    onScreenLog.lines("First anchor complete.");
                    currentPictureState = PictureState.TYPICAL_PROGRESSION;
                    return inputFrame;

                case TYPICAL_PROGRESSION:

                    if (lastUpdateTime != -1)
                        onScreenLog.lines("Took " + (System.currentTimeMillis() - lastUpdateTime) + " to update");
                    lastUpdateTime = System.currentTimeMillis();

                    setFlashState(false);

                    onScreenLog.lines("Analyzing camera frame...");

                    // Figure out what's actually different (absdiff is just a bit too sensitive)
                    Core.absdiff(inputFrame, previous, binaryDiff);
                    Imgproc.threshold(binaryDiff, binaryDiff, 100, 255, Imgproc.THRESH_BINARY);

                    // Remember the last image.
                    inputFrame.copyTo(previous);

                    // Determine whether the person is stirring (simple noise detection)
                    if ((double)(Core.countNonZero(binaryDiff)) / (binaryDiff.height() * binaryDiff.width()) > RESTLESSNESS_NOISE_SATURATION_THRESHOLD)
                    {
                        onScreenLog.lines("Would now reset alarm based on sleep cycle");

                        // TODO Find scheduling time
                        Thread.sleep(3000);
                    } else
                    {
                        // Wait a while longer (person's probably asleep).
                        Thread.sleep(15000);
                    }

                    // Start the flash again.
                    setFlashState(true);
                    Thread.sleep(500);

                    // This display is on a noticeable delay, but that's the price I gotta pay for this to be synchronous.
                    return binaryDiff;
            }

            // Take a new picture!

        } catch (InterruptedException e)
        {
            onScreenLog.lines("Interrupted!");
        }

        return null; // Won't get called unless there's an exception.
    }
}