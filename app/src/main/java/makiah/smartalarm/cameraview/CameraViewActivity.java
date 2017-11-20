package makiah.smartalarm.cameraview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

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
import makiah.smartalarm.logging.OnScreenLogParent;

import static org.opencv.core.CvType.CV_8UC3;

public class CameraViewActivity extends AppCompatActivity implements CvCameraViewListener2, OnScreenLogParent
{
    private static final String TAG = "CameraViewActivity";

    // Stuff for the subtasks.
    private boolean taskActive = true;
    public boolean isTaskActive()
    {
        return taskActive;
    }

    // Where REQUEST means that it will wait for new requests
    public enum CameraMode { REQUEST, CONTINUOUS }
    private CameraMode currentCameraMode = CameraMode.CONTINUOUS;
    private ArrayList<CameraViewFrameReceiver> frameCallbacks = new ArrayList<>(); // It's possible that multiple might call.

    // This does a lot of the grunt work involved in the sleep processing.
    private CameraViewLogger onScreenLog;
    private RestlessnessDetector restlessnessDetector;

    // Loads camera view of OpenCV for us to use. This lets us see using OpenCV
    private CameraBridgeViewBase cameraBridgeViewBase;
    private JavaCameraViewWithFlash javaCameraView;

    // These variables are used (at the moment) to fix camera orientation from 270degree to 0degree
    private Mat cameraViewMat, mRgbaF, mRgbaT;

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

        cameraBridgeViewBase = javaCameraView;
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);

        // Required components which control app stuff.
        onScreenLog = new CameraViewLogger(this);
        restlessnessDetector = new RestlessnessDetector(this, onScreenLog);
    }

    @Override
    public void onPause()
    {
        super.onPause();

        if (cameraBridgeViewBase != null)
            cameraBridgeViewBase.disableView();

        taskActive = false;
    }

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

    public void onDestroy() {
        super.onDestroy();

        if (cameraBridgeViewBase != null)
            cameraBridgeViewBase.disableView();

        taskActive = false;
    }

    public void onCameraViewStarted(int width, int height)
    {
        cameraViewMat = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);

        // Start the restlessness detector.
        restlessnessDetector = new RestlessnessDetector(this);
    }

    public void onCameraViewStopped() {
        cameraViewMat.release();
    }

    /**
     * When the JavaCameraView sees a new frame (called very often).  This method has to
     * be modified in order to view single images.
     *
     * @param inputFrame the pixel array which the camera currently sees.
     * @return
     */
    public Mat onCameraFrame(CvCameraViewFrame inputFrame)
    {
        // Pass back the last frame if we're in the request mode and we don't want a new frame.
        if (currentCameraMode == CameraMode.REQUEST && frameCallbacks.size() == 0)
        {
            // In case not even a single picture has been taken.
            if (cameraViewMat == null)
                cameraViewMat = new Mat(320, 240, CV_8UC3, new Scalar(0, 0, 0));

            return cameraViewMat;
        }

        // TODO Auto-generated method stub
        cameraViewMat = inputFrame.rgba();

        // Rotate cameraViewMat 90 degrees
        Core.transpose(cameraViewMat, mRgbaT);
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0, 0, 0);
        Core.flip(mRgbaF, cameraViewMat, 1);

        if (currentCameraMode == CameraMode.REQUEST && frameCallbacks.size() > 0)
        {
            // Provide all callbacks the frame they requested.
            for (CameraViewFrameReceiver callback : frameCallbacks)
                callback.provide(cameraViewMat);

            // Remove all callbacks.
            frameCallbacks.clear();
        }

        return cameraViewMat;
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

    public void setCameraMode(CameraMode mode)
    {
        if (currentCameraMode == mode)
            return;

        currentCameraMode = mode;

        if (currentCameraMode == CameraMode.REQUEST)
            onScreenLog.lines("Changed camera mode to request");
        else if (currentCameraMode == CameraMode.CONTINUOUS)
            onScreenLog.lines("Changed camera mode to continuous");

        // Ensure that we clear the list if we changed the mode.
        if (currentCameraMode == CameraMode.REQUEST)
            frameCallbacks.clear();
    }

    public void requestFrame(CameraViewFrameReceiver callback)
    {
        frameCallbacks.add(callback);
    }
}