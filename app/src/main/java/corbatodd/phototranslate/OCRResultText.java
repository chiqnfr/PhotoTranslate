package corbatodd.phototranslate;

import java.util.List;

import android.graphics.Point;
import android.graphics.Rect;

/**
 * Created by CorbaTodd on 4/2/15.
 */
public class OCRResultText {

    private final String text;

    private final int[] wordConfidences;
    private final int meanConfidence;
    private final Point bitmapDimensions;
    private final List<Rect> regionBoundingBoxes;
    private final List<Rect> textlineBoundingBoxes;
    private final List<Rect> stripBoundingBoxes;
    private final List<Rect> wordBoundingBoxes;

    public OCRResultText(String text, int[] wordConfidences, int meanConfidence, Point bitmapDimensions, List<Rect> regionBoundingBoxes, List<Rect> textlineBoundingBoxes, List<Rect> stripBoundingBoxes, List<Rect> wordBoundingBoxes, List<Rect> characterBoundingBoxes) {
        this.text = text;
        this.wordConfidences = wordConfidences;
        this.meanConfidence = meanConfidence;
        this.bitmapDimensions = bitmapDimensions;
        this.regionBoundingBoxes = regionBoundingBoxes;
        this.textlineBoundingBoxes = textlineBoundingBoxes;
        this.stripBoundingBoxes = stripBoundingBoxes;
        this.wordBoundingBoxes = wordBoundingBoxes;
    }

    public String getText() {
        return text;
    }

    public Point getBitmapDimensions() {
        return bitmapDimensions;
    }

    public int[] getWordConfidences() {
        return wordConfidences;
    }


    public List<Rect> getRegionBoundingBoxes() {
        return regionBoundingBoxes;
    }

    public List<Rect> getTextlineBoundingBoxes() {
        return textlineBoundingBoxes;
    }

    public List<Rect> getStripBoundingBoxes() {
        return stripBoundingBoxes;
    }

    public List<Rect> getWordBoundingBoxes() {
        return wordBoundingBoxes;
    }


    @Override
    public String toString() {
        return text + " " + meanConfidence;
    }

}
