package makiah.smartalarm.cameraview;

import com.makiah.makiahsandroidlib.threading.ParallelTask;

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
public class RestlessnessDetector extends ParallelTask implements CameraViewFrameReceiver
{
    private final CameraViewActivity cameraViewActivity;

    public RestlessnessDetector(final CameraViewActivity cameraViewActivity)
    {
        this(cameraViewActivity, null);
    }
    public RestlessnessDetector(final CameraViewActivity cameraViewActivity, final CameraViewLogger logger)
    {
        super(cameraViewActivity, "Restlessness Detector");

        this.cameraViewActivity = cameraViewActivity;
        this.cameraViewActivity.setCameraMode(CameraViewActivity.CameraMode.REQUEST);

        // Tell the base task that we've got a logger.
        provideOnScreenLog(logger);

        this.run();
    }

    // The callback for images from the primary camera view activity.
    private Mat newFrame = null;
    @Override
    public void provide(Mat frame)
    {
        newFrame = frame;
    }

    /**
     * This is where a lot of the OpenCV work comes into play.  Heavily aided by this
     * stackoverflow post: https://stackoverflow.com/questions/27035672/cv-extract-differences-between-two-images
     */
    private void analyzeImages(Mat mat1, Mat mat2)
    {
        Mat diffImage = Mat.zeros(mat1.rows(), mat1.cols(), CvType.CV_8UC1);

        // Get the slightest differences between the two images.
        Core.absdiff(mat1, mat2, diffImage);

        // Figure out what's actually different (absdiff is just a bit too sensitive)
        double threshold = 30.0;
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
    }

    @Override
    protected void onDoTask() throws InterruptedException
    {
        flow.msPause(2000);

        while (true)
        {
            // Turns on flash.
            cameraViewActivity.setFlashState(true);
            logSequentialLines("Turned flash on");

            // Ensures that flash has time to light room.
            flow.msPause(2000);

            // Asks for a new frame from the activity.
            cameraViewActivity.requestFrame(this);
            logSequentialLines("Requested frame");

            // Wait for the new frame to show up.
            while (newFrame == null)
                flow.yield();

            // Prevent infinite loop.
            Mat currentFrame = newFrame;
            newFrame = null;

            logSequentialLines("Got frame");

            // Turn off the camera flash.
            cameraViewActivity.setFlashState(false);
            logSequentialLines("Turned flash off");

            // TODO Analyze currentFrame

            // Wait for a grace period.
            flow.msPause(6000);
        }
    }
}
