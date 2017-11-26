package makiah.smartalarm.cameraview;

import com.makiah.makiahsandroidlib.logging.LoggingBase;
import com.makiah.makiahsandroidlib.threading.Flow;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

/**
 * This class is where most of the code goes for detecting whether the person in question is
 * shifting in their sleep.
 *
 * Internals:
 *  1. This thread requests a new frame from the CameraViewActivity, then waits for the frame
 *      to show up in its thread.
 *  2. Upon receiving a new frame and recognizing a new frame has been requested, the camera view
 *      activity starts the phone's torch mode, takes the picture, sends the frame to the thread,
 *      then turns off torch mode.
 *  3. This thread processes the extent to which this frame differs from the last frame (this
 *      process doesn't need to occur super quickly), and decides whether it needs another picture
 *      to verify or if it's certain this showed movement (or that it didn't).
 */
public class RestlessnessDetector implements CameraBridgeViewBase.CvCameraViewListener
{
    private LoggingBase logger;

    public RestlessnessDetector(LoggingBase logger)
    {
        this.logger = logger;
    }


    // SUPER important that we don't initialize these more than we need to.

    /**
     * Initialize all mats here...
     *
     * @param width -  the width of the frames that will be delivered
     * @param height - the height of the frames that will be delivered
     */
    @Override
    public void onCameraViewStarted(int width, int height) {
        previous = new Mat(width, height, CvType.CV_64FC4);
        current = new Mat(width, height, CvType.CV_64FC4);
        difference = new Mat(width, height, CvType.CV_64FC4);
    }

    /**
     * And deinit them here.
     */
    @Override
    public void onCameraViewStopped() {
        current.release();
    }

    // The last frame returned by the camera (for comparison).
    private Mat current, previous, difference;
    private boolean first = true;

    /**
     * Where all of the image processing goes.
     */
    @Override
    public Mat onCameraFrame(Mat inputFrame)
    {
        inputFrame.copyTo(current);
        if (first) {//first is true at the first time
            inputFrame.copyTo(previous);
            first = false;
        }

        Core.absdiff(current, previous, difference);
        inputFrame.copyTo(previous);

        return difference;
    }
}
