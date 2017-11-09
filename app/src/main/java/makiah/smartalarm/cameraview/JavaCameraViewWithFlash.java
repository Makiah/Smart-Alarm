package makiah.smartalarm.cameraview;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;

public class JavaCameraViewWithFlash extends JavaCameraView
{
    private boolean flashState = false;

    public JavaCameraViewWithFlash(Context context, int cameraId)
    {
        super(context, cameraId);
    }

    public JavaCameraViewWithFlash(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    /**
     * Will turn the camera flash on and off.
     * @param state the new state
     */
    public void setFlashState(boolean state) {
        Camera.Parameters params = mCamera.getParameters();
        flashState = state;
        if (state) {
            params.setFlashMode(params.FLASH_MODE_TORCH);
            mCamera.setParameters(params);
        } else {
            params.setFlashMode(params.FLASH_MODE_OFF);
            mCamera.setParameters(params);
        }
    }

    public void toggleFlashState()
    {
        setFlashState(!flashState);
    }
}