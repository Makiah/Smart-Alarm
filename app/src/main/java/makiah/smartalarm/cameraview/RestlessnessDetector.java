package makiah.smartalarm.cameraview;

import org.opencv.core.Mat;

import makiah.smartalarm.threading.ParallelTask;

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
    private final CameraViewLogger logger;

    public RestlessnessDetector(final CameraViewActivity cameraViewActivity)
    {
        this(cameraViewActivity, null);
    }
    public RestlessnessDetector(final CameraViewActivity cameraViewActivity, final CameraViewLogger logger)
    {
        super(cameraViewActivity, "Restlessness Detector");

        this.cameraViewActivity = cameraViewActivity;
        this.cameraViewActivity.setCameraMode(CameraViewActivity.CameraMode.REQUEST);

        this.logger = logger;

        this.run();
    }

    private Mat newFrame = null;
    @Override
    public void provide(Mat frame)
    {
        newFrame = frame;
    }

    @Override
    protected void onDoTask() throws InterruptedException
    {
        flow.msPause(2000);

        while (true)
        {
            // Turns on flash.
            cameraViewActivity.setFlashState(true);
            logger.lines("Turned flash on");

            // Ensures that flash has time to light room.
            flow.msPause(2000);

            // Asks for a new frame from the activity.
            cameraViewActivity.requestFrame(this);
            logger.lines("Requested frame");

            // Wait for the new frame to show up.
            while (newFrame == null)
                flow.yield();

            // Prevent infinite loop.
            Mat currentFrame = newFrame;
            newFrame = null;

            logger.lines("Got frame");

            // Turn off the camera flash.
            cameraViewActivity.setFlashState(false);
            logger.lines("Turned flash off");

            // TODO Analyze currentFrame

            // Wait for a grace period.
            flow.msPause(6000);
        }
    }
}
