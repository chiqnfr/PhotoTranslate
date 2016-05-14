package corbatodd.phototranslate;

import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.widget.Toast;

/**
 * Created by CorbaTodd on 4/2/15.
 */
public class CaptureHandler extends Handler {

    private final MainActivity activity;
    private final DecodeThread decodeThread;
    private static State state;
    private final CameraManager cameraManager;

    private enum State {PREVIEW, PREVIEW_PAUSED, CONTINUOUS, CONTINUOUS_PAUSED, SUCCESS, DONE}

    CaptureHandler(MainActivity activity, CameraManager cameraManager, boolean isContinuousModeActive) {
        this.activity = activity;
        this.cameraManager = cameraManager;

        cameraManager.startPreview();

        decodeThread = new DecodeThread(activity);
        decodeThread.start();

        if (isContinuousModeActive) {
            state = State.CONTINUOUS;

            activity.setButtonVisibility(true);

            activity.setStatusViewForContinuous();

            restartOcrPreviewAndDecode();
        } else {
            state = State.SUCCESS;

            activity.setButtonVisibility(true);

            restartOcrPreview();
        }
    }

    @Override
    public void handleMessage(Message message) {

        switch (message.what) {
            case R.id.restart_preview:
                restartOcrPreview();
                break;
            case R.id.ocr_continuous_decode_failed:
                DecodeHandler.resetDecodeState();
                try {
                    activity.handleOcrContinuousDecode((OCRResultFail) message.obj);
                } catch (NullPointerException e) {

                }
                if (state == State.CONTINUOUS) {
                    restartOcrPreviewAndDecode();
                }
                break;
            case R.id.ocr_continuous_decode_succeeded:
                DecodeHandler.resetDecodeState();
                try {
                    activity.handleOcrContinuousDecode((OCRResult) message.obj);
                } catch (NullPointerException e) {

                }
                if (state == State.CONTINUOUS) {
                    restartOcrPreviewAndDecode();
                }
                break;
            case R.id.ocr_decode_succeeded:
                state = State.SUCCESS;
                activity.setShutterButtonClickable(true);
                activity.handleOcrDecode((OCRResult) message.obj);
                break;
            case R.id.ocr_decode_failed:
                state = State.PREVIEW;
                activity.setShutterButtonClickable(true);
                Toast toast = Toast.makeText(activity.getBaseContext(), "Cannot recognize text! Try again!", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM, 0, 0);
                toast.show();
                break;
        }
    }

    void stop() {

        state = State.CONTINUOUS_PAUSED;
        removeMessages(R.id.ocr_continuous_decode);
        removeMessages(R.id.ocr_decode);
        removeMessages(R.id.ocr_continuous_decode_failed);
        removeMessages(R.id.ocr_continuous_decode_succeeded);

    }

    void resetState() {
        if (state == State.CONTINUOUS_PAUSED) {
            state = State.CONTINUOUS;
            restartOcrPreviewAndDecode();
        }
    }

    void quitSynchronously() {
        state = State.DONE;
        if (cameraManager != null) {
            cameraManager.stopPreview();
        }
        try {
            decodeThread.join(500L);
        } catch (InterruptedException e) {

        } catch (RuntimeException e) {

        } catch (Exception e) {

        }

        removeMessages(R.id.ocr_continuous_decode);
        removeMessages(R.id.ocr_decode);

    }

    private void restartOcrPreview() {
        activity.setButtonVisibility(true);

        if (state == State.SUCCESS) {
            state = State.PREVIEW;
            activity.drawViewfinder();
        }
    }

    private void restartOcrPreviewAndDecode() {
        cameraManager.startPreview();

        cameraManager.requestOcrDecode(decodeThread.getHandler(), R.id.ocr_continuous_decode);
        activity.drawViewfinder();
    }

    private void ocrDecode() {
        state = State.PREVIEW_PAUSED;
        cameraManager.requestOcrDecode(decodeThread.getHandler(), R.id.ocr_decode);
    }

    void hardwareShutterButtonClick() {
        if (state == State.PREVIEW) {
            ocrDecode();
        }
    }

    void shutterButtonClick() {
        activity.setShutterButtonClickable(false);
        ocrDecode();
    }

}
