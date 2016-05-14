package corbatodd.phototranslate;

/**
 * Created by CorbaTodd on 5/24/15.
 */
public class OCRResultFail {

    private final long timeRequired;
    private final long timestamp;

    OCRResultFail(long timeRequired) {
        this.timeRequired = timeRequired;
        this.timestamp = System.currentTimeMillis();
    }

    //public long getTimeRequired() {
    //    return timeRequired;
    //}

    //public long getTimestamp() {
    //    return timestamp;
    //}

    @Override
    public String toString() {
        return timeRequired + " " + timestamp;
    }

}
