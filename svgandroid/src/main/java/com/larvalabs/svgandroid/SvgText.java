package com.larvalabs.svgandroid;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import org.xml.sax.Attributes;

/**
 * Created by zhengxianlzx on 17-10-5.
 */
public class SvgText {
    private final static int MIDDLE = 1;
    private final static int TOP = 2;
    private SVGHandler svgHandler;
    private Paint stroke = null, fill = null;
    private float x, y;
    private String svgText;
    private boolean inText;
    private int vAlign = 0;

    public SvgText(SVGHandler svgHandler, Attributes atts) {
        this.svgHandler = svgHandler;
        // Log.d(TAG, "text");
        x = SVGParser.getFloatAttr("x", atts, 0f);
        y = SVGParser.getFloatAttr("y", atts, 0f);
        svgText = null;
        inText = true;

        Properties props = new Properties(atts);
        if (svgHandler.doFill(props, svgHandler.gradientMap)) {
            fill = new Paint(svgHandler.fillPaint);
            svgHandler.doText(atts, fill);
        }
        if (svgHandler.doStroke(props)) {
            stroke = new Paint(svgHandler.strokePaint);
            svgHandler.doText(atts, stroke);
        }
        // quick hack
        String valign = SVGParser.getStringAttr("alignment-baseline", atts);
        if ("middle".equals(valign)) {
            vAlign = MIDDLE;
        } else if ("top".equals(valign)) {
            vAlign = TOP;
        }
    }

    // ignore tspan elements for now
    public void setText(char[] ch, int start, int len) {
        if (isInText()) {
            if (svgText == null) {
                svgText = new String(ch, start, len);
            } else {
                svgText += new String(ch, start, len);
            }

            // This is an experiment for vertical alignment
            if (vAlign > 0) {
                Paint paint = stroke == null ? fill : stroke;
                Rect bnds = new Rect();
                paint.getTextBounds(svgText, 0, svgText.length(), bnds);
                // Log.i(TAG, "Adjusting " + y + " by " + bnds);
                y += (vAlign == MIDDLE) ? -bnds.centerY() : bnds.height();
            }
        }
    }

    public boolean isInText() {
        return inText;
    }

    public void close() {
        inText = false;
    }

    public void render(Canvas canvas) {
        if (fill != null) {
            canvas.drawText(svgText, x, y, fill);
        }
        if (stroke != null) {
            canvas.drawText(svgText, x, y, stroke);
        }
        // Log.i(TAG, "Drawing: " + svgText + " " + x + "," + y);
    }
}
