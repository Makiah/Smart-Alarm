package makiah.smartalarm.cameraview;

import org.opencv.core.Mat;

public interface CameraViewFrameReceiver
{
    void provide(Mat frame);
}
