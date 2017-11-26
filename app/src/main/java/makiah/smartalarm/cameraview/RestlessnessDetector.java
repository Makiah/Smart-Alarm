package makiah.smartalarm.cameraview;

import com.makiah.makiahsandroidlib.logging.LoggingBase;
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
public class RestlessnessDetector implements CameraBridgeViewBase.CvCameraViewListener2
{
    private LoggingBase logger;

    public RestlessnessDetector(LoggingBase logger)
    {
        this.logger = logger;
    }

    /**
     * This is where a lot of the OpenCV work comes into play.  Heavily aided by this
     * stackoverflow post: https://stackoverflow.com/questions/27035672/cv-extract-differences-between-two-images
     */
    private Mat analyzeImages(Mat mat1, Mat mat2)
    {
        Mat diffImage = Mat.zeros(mat1.rows(), mat1.cols(), CvType.CV_8UC1);

        // Get the slightest differences between the two images.
        Core.absdiff(mat1, mat2, diffImage);

        // Figure out what's actually different (absdiff is just a bit too sensitive)
        double threshold = 50.0;
        Mat thresholdDifferences = Mat.zeros(mat1.rows(), mat1.cols(), CvType.CV_8UC1);

        for (int j = 0; j < diffImage.rows(); j++)
        {
            for (int i = 0; i < diffImage.cols(); i++)
            {
                double[] pixel = diffImage.get(j, i);

                double difference = Math.sqrt(Math.pow(pixel[0], 2) + Math.pow(pixel[1], 2) + Math.pow(pixel[2], 2));

                if (difference > threshold)
                {
                    thresholdDifferences.put(j, i, 255, 255, 255);
                }
            }
        }

        return thresholdDifferences;
    }

    /**
     * Initialize all mats here...
     *
     * @param width -  the width of the frames that will be delivered
     * @param height - the height of the frames that will be delivered
     */
    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    /**
     * And deinit them here.
     */
    @Override
    public void onCameraViewStopped() {
    }

    // The last frame returned by the camera (for comparison).
    private Mat lastFrame = null;

    /**
     * Where all of the image processing goes.
     *
     * @param currentView the current view of the camera.
     * @return
     */
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame currentView)
    {
        if (lastFrame == null)
        {
            lastFrame = currentView.rgba().clone();
            return lastFrame;
        }

        Mat diff = analyzeImages(currentView.rgba(), lastFrame);
        lastFrame = currentView.rgba().clone();

        return diff;
    }
}
