package makiah.smartalarm;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;

public class JavaCameraViewWithFlash extends JavaCameraView
{
    private boolean flashState = false;

    public JavaCameraViewWithFlash(Context context, int cameraId) {
        super(context, cameraId);
    }

    public JavaCameraViewWithFlash(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Will turn the camera flash on and off.
     * @param on
     */
    public void setFlashState(boolean on) {
        Camera.Parameters params = mCamera.getParameters();
        flashState = on;
        if (on) {
            params.setFlashMode(params.FLASH_MODE_OFF);
            mCamera.setParameters(params);
        } else {
            params.setFlashMode(params.FLASH_MODE_TORCH);
            mCamera.setParameters(params);
        }
    }
    public void toggleFlashState()
    {
        setFlashState(!flashState);
    }
}
