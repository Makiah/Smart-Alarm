package makiah.smartalarm.cameraview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import com.makiah.makiahsandroidlib.logging.OnScreenLogParent;
import com.makiah.makiahsandroidlib.threading.Flow;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

import makiah.smartalarm.R;

import static org.opencv.core.CvType.CV_8UC3;

public class CameraViewActivity extends AppCompatActivity implements OnScreenLogParent, CameraBridgeViewBase.CvCameraViewListener
{
    private static final String TAG = "CameraViewActivity";

    private static final int FRAME_WIDTH_REQUEST = 176, FRAME_HEIGHT_REQUEST = 144;

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
    private CameraViewLogger onScreenLog;

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

    public CameraViewActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
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
        setContentView(R.layout.activity_camera_view);

        taskActive = true;

        Log.i(TAG, "called onCreate");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Create both a CameraBridgeViewBase and a JavaCameraView with flash so that I can modify the flash state while also being able to view the camera.
        javaCameraView = (JavaCameraViewWithFlash) findViewById(R.id.show_camera_activity_java_surface_view);

        // Required components which control app stuff.
        onScreenLog = new CameraViewLogger(this);

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
    private Mat current, previous, thresholdDifferences;
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
        current = new Mat(height, width, CvType.CV_8UC1);
        previous = new Mat(height, width, CvType.CV_8UC1);
        currentPictureState = PictureState.FIRST_FLASH;
    }

    /**
     * And deinit them here.
     */
    @Override
    public void onCameraViewStopped() {
        thresholdDifferences.release();
        previous.release();
    }

    /**
     * Where all of the image processing goes.
     */
    @Override
    public Mat onCameraFrame(Mat inputFrame)
    {
        try
        {
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
                    setFlashState(false);

                    inputFrame.copyTo(current);

                    onScreenLog.lines("Analyzing camera frame...");

                    // Figure out what's actually different (absdiff is just a bit too sensitive)
                    double threshold = 50.0;
                    thresholdDifferences = Mat.zeros(current.rows(), current.cols(), CvType.CV_8UC1);

                    for (int rowIndex = 0; rowIndex < current.rows(); rowIndex++)
                    {
                        for (int colIndex = 0; colIndex < current.cols(); colIndex++)
                        {
                            double[] currentPixel = current.get(rowIndex, colIndex);
                            double[] previousPixel = previous.get(rowIndex, colIndex);

                            try {
                                double totalDifference = 0;
                                for (int rgbIndex = 0; rgbIndex < 3; rgbIndex++)
                                    totalDifference += Math.pow((currentPixel[rgbIndex] - previousPixel[rgbIndex]), 2);
                                totalDifference = Math.sqrt(totalDifference);

                                if (totalDifference > threshold)
                                    thresholdDifferences.put(rowIndex, colIndex, 255, 255, 255);
                            } catch (Exception e)
                            {
                                onScreenLog.lines("Issue: " + e.getMessage());

                                return inputFrame;
                            }
                        }
                    }

                    inputFrame.copyTo(previous);

                    // TODO Find scheduling time
                    Thread.sleep(3000);

                    setFlashState(true);
                    Thread.sleep(1000);

                    // This display is on a noticeable delay, but that's the price I gotta pay for this to be synchronous.
                    return thresholdDifferences;
            }

            // Take a new picture!

        } catch (InterruptedException e)
        {
            onScreenLog.lines("Interrupted!");
        }

        return null; // Won't get called unless there's an exception.
    }
}