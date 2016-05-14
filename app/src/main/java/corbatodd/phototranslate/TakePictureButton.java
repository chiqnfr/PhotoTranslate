package corbatodd.phototranslate;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SoundEffectConstants;
import android.widget.ImageView;

/**
 * Created by CorbaTodd on 4/2/15.
 */

public class TakePictureButton extends ImageView {

    public interface OnShutterButtonListener {

        void onShutterButtonFocus(TakePictureButton b, boolean pressed);

        void onShutterButtonClick(TakePictureButton b);
    }

    private OnShutterButtonListener mListener;
    private boolean mOldPressed;

    public TakePictureButton(Context context) {
        super (context);
    }

    public TakePictureButton(Context context, AttributeSet attrs) {
        super (context, attrs);
    }

    public TakePictureButton(Context context, AttributeSet attrs, int defStyle) {
        super (context, attrs, defStyle);
    }

    public void setOnShutterButtonListener(OnShutterButtonListener listener) {
        mListener = listener;
    }

    @Override
    protected void drawableStateChanged() {
        super .drawableStateChanged();
        final boolean pressed = isPressed();
        if (pressed != mOldPressed) {
            if (!pressed) {
                post(new Runnable() {
                    public void run() {
                        callShutterButtonFocus(pressed);
                    }
                });
            } else {
                callShutterButtonFocus(pressed);
            }
            mOldPressed = pressed;
        }
    }

    private void callShutterButtonFocus(boolean pressed) {
        if (mListener != null) {
            mListener.onShutterButtonFocus(this , pressed);
        }
    }

    @Override
    public boolean performClick() {
        boolean result = super.performClick();
        playSoundEffect(SoundEffectConstants.CLICK);
        if (mListener != null) {
            mListener.onShutterButtonClick(this);
        }
        return result;
    }

}
