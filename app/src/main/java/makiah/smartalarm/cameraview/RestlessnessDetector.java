package makiah.smartalarm.cameraview;

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
public class RestlessnessDetector implements CameraViewFrameReceiver
{
    // Singleton.
    public static RestlessnessDetector instance;

    public RestlessnessDetector()
    {
        instance = this;

        // Start the async task which will process pictures as they show up.
    }

    public void provide(Mat rgbaFrame)
    {

    }
}
