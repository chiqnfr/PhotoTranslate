package corbatodd.phototranslate;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.CountDownLatch;

/**
 * Created by CorbaTodd on 4/2/15.
 */
public class DecodeThread extends Thread {

    private final MainActivity activity;
    private Handler handler;
    private final CountDownLatch handlerInitLatch;

    DecodeThread(MainActivity activity) {
        this.activity = activity;
        handlerInitLatch = new CountDownLatch(1);
    }

    Handler getHandler() {
        try {
            handlerInitLatch.await();
        } catch (InterruptedException ie) {
        }
        return handler;
    }

    @Override
    public void run() {
        Looper.prepare();
        handler = new DecodeHandler(activity);
        handlerInitLatch.countDown();
        Looper.loop();
    }

}
