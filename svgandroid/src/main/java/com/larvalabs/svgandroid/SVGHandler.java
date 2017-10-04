package com.larvalabs.svgandroid;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.Log;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Stack;
import java.util.StringTokenizer;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Created by zhengxianlzx on 17-10-5.
 */
public class SVGHandler extends DefaultHandler {
    //public StringBuilder parsed = new StringBuilder();

    HashMap<String, String> idXml = new HashMap<String, String>();

    Picture picture;
    Canvas canvas;

    Paint strokePaint;
    boolean strokeSet = false;
    Stack<Paint> strokePaintStack = new Stack<Paint>();
    Stack<Boolean> strokeSetStack = new Stack<Boolean>();

    Paint fillPaint;
    boolean fillSet = false;
    Stack<Paint> fillPaintStack = new Stack<Paint>();
    Stack<Boolean> fillSetStack = new Stack<Boolean>();

    // Scratch rect (so we aren't constantly making new ones)
    RectF rect = new RectF();
    RectF bounds = null;
    RectF limits = new RectF(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);

    Integer searchColor = null;
    Integer replaceColor = null;

    boolean whiteMode = false;

    int pushed = 0;

    private boolean hidden = false;
    private int hiddenLevel = 0;
    private boolean boundsMode = false;

    HashMap<String, Shader> gradientMap = new HashMap<String, Shader>();
    HashMap<String, Gradient> gradientRefMap = new HashMap<String, Gradient>();
    Gradient gradient = null;
    SvgText text = null;

    private boolean inDefsElement = false;

    SVGHandler(Picture picture) {
        this.picture = picture;
        strokePaint = new Paint();
        strokePaint.setAntiAlias(true);
        strokePaint.setStyle(Paint.Style.STROKE);
        fillPaint = new Paint();
        fillPaint.setAntiAlias(true);
        fillPaint.setStyle(Paint.Style.FILL);
    }

    public void setColorSwap(Integer searchColor, Integer replaceColor) {
        this.searchColor = searchColor;
        this.replaceColor = replaceColor;
    }

    public void setWhiteMode(boolean whiteMode) {
        this.whiteMode = whiteMode;
    }

    @Override
    public void startDocument() {
        // Set up prior to parsing a doc
    }

    @Override
    public void endDocument() {
        // Clean up after parsing a doc
        /*
        String s = parsed.toString();
        if (s.endsWith("</svg>")) {
            s = s + "";
            Log.d(TAG, s);
        }
        */
    }

    boolean doFill(Properties atts, HashMap<String, Shader> gradients) {
        if ("none".equals(atts.getString("display"))) {
            return false;
        }
        if (whiteMode) {
            fillPaint.setShader(null);
            fillPaint.setColor(Color.WHITE);
            return true;
        }
        String fillString = atts.getString("fill");
        if (fillString != null) {
            if (fillString.startsWith("url(#")) {
                // It's a gradient fill, look it up in our map
                String id = fillString.substring("url(#".length(), fillString.length() - 1);
                Shader shader = gradients.get(id);
                if (shader != null) {
                    fillPaint.setShader(shader);
                    return true;
                } else {
                    Log.d(SVGParser.TAG, "Didn't find shader, using black: " + id);
                    fillPaint.setShader(null);
                    doColor(atts, Color.BLACK, true, fillPaint);
                    return true;
                }
            } else if (fillString.equalsIgnoreCase("none")) {
                fillPaint.setShader(null);
                fillPaint.setColor(Color.TRANSPARENT);
                return true;
            } else {
                fillPaint.setShader(null);
                Integer color = atts.getColorValue("fill");
                if (color != null) {
                    doColor(atts, color, true, fillPaint);
                    return true;
                } else {
                    Log.d(SVGParser.TAG, "Unrecognized fill color, using black: " + fillString);
                    doColor(atts, Color.BLACK, true, fillPaint);
                    return true;
                }
            }
        } else {
            if (fillSet) {
                // If fill is set, inherit from parent
                return fillPaint.getColor() != Color.TRANSPARENT;   // optimization
            } else {
                // Default is black fill
                fillPaint.setShader(null);
                fillPaint.setColor(Color.BLACK);
                return true;
            }
        }
    }

    // XXX not done yet
    boolean doText(Attributes atts, Paint paint) {
        if ("none".equals(atts.getValue("display"))) {
            return false;
        }
        if (atts.getValue("font-size") != null) {
            paint.setTextSize(SVGParser.getFloatAttr("font-size", atts, 10f));
        }
        Typeface typeface = setTypeFace(atts);
        if (typeface != null) {
            paint.setTypeface(typeface);
        }
        Paint.Align align = getTextAlign(atts);
        if (align != null) {
            paint.setTextAlign(getTextAlign(atts));
        }
        return true;
    }

    boolean doStroke(Properties atts) {
        if (whiteMode) {
            // Never stroke in white mode
            return false;
        }
        if ("none".equals(atts.getString("display"))) {
            return false;
        }

        // Check for other stroke attributes
        Float width = atts.getFloat("stroke-width");
        if (width != null) {
            strokePaint.setStrokeWidth(width);
        }

        String linecap = atts.getString("stroke-linecap");
        if ("round".equals(linecap)) {
            strokePaint.setStrokeCap(Paint.Cap.ROUND);
        } else if ("square".equals(linecap)) {
            strokePaint.setStrokeCap(Paint.Cap.SQUARE);
        } else if ("butt".equals(linecap)) {
            strokePaint.setStrokeCap(Paint.Cap.BUTT);
        }

        String linejoin = atts.getString("stroke-linejoin");
        if ("miter".equals(linejoin)) {
            strokePaint.setStrokeJoin(Paint.Join.MITER);
        } else if ("round".equals(linejoin)) {
            strokePaint.setStrokeJoin(Paint.Join.ROUND);
        } else if ("bevel".equals(linejoin)) {
            strokePaint.setStrokeJoin(Paint.Join.BEVEL);
        }

        pathStyleHelper(atts.getString("stroke-dasharray"), atts.getString("stroke-dashoffset"));

        String strokeString = atts.getAttr("stroke");
        if (strokeString != null) {
            if (strokeString.equalsIgnoreCase("none")) {
                strokePaint.setColor(Color.TRANSPARENT);
                return false;
            } else {
                Integer color = atts.getColorValue("stroke");
                if (color != null) {
                    doColor(atts, color, false, strokePaint);
                    return true;
                } else {
                    Log.d(SVGParser.TAG, "Unrecognized stroke color, using none: " + strokeString);
                    strokePaint.setColor(Color.TRANSPARENT);
                    return false;
                }
            }
        } else {
            if (strokeSet) {
                // Inherit from parent
                return strokePaint.getColor() != Color.TRANSPARENT;   // optimization
            } else {
                // Default is none
                strokePaint.setColor(Color.TRANSPARENT);
                return false;
            }
        }
    }

    private Gradient doGradient(boolean isLinear, Attributes atts) {
        Gradient gradient = new Gradient();
        gradient.id = SVGParser.getStringAttr("id", atts);
        gradient.isLinear = isLinear;
        if (isLinear) {
            gradient.x1 = SVGParser.getFloatAttr("x1", atts, 0f);
            gradient.x2 = SVGParser.getFloatAttr("x2", atts, 0f);
            gradient.y1 = SVGParser.getFloatAttr("y1", atts, 0f);
            gradient.y2 = SVGParser.getFloatAttr("y2", atts, 0f);
        } else {
            gradient.x = SVGParser.getFloatAttr("cx", atts, 0f);
            gradient.y = SVGParser.getFloatAttr("cy", atts, 0f);
            gradient.radius = SVGParser.getFloatAttr("r", atts, 0f);
        }
        String transform = SVGParser.getStringAttr("gradientTransform", atts);
        if (transform != null) {
            gradient.matrix = SVGParser.parseTransform(transform);
        }
        String xlink = SVGParser.getStringAttr("href", atts);
        if (xlink != null) {
            if (xlink.startsWith("#")) {
                xlink = xlink.substring(1);
            }
            gradient.xlink = xlink;
        }
        return gradient;
    }

    private void doColor(Properties atts, Integer color, boolean fillMode, Paint paint) {
        int c = (0xFFFFFF & color) | 0xFF000000;
        if (searchColor != null && searchColor.intValue() == c) {
            c = replaceColor;
        }
        paint.setColor(c);
        Float opacity = atts.getFloat("opacity");
        if (opacity == null) {
            opacity = atts.getFloat(fillMode ? "fill-opacity" : "stroke-opacity");
        }
        if (opacity == null) {
            paint.setAlpha(255);
        } else {
            paint.setAlpha((int) (255 * opacity));
        }
    }

    /**
     * set the path style (if any)
     * stroke-dasharray="n1,n2,..."
     * stroke-dashoffset=n
     */

    private void pathStyleHelper(String style, String offset) {
        if (style == null) {
            return;
        }

        if (style.equals("none")) {
            strokePaint.setPathEffect(null);
            return;
        }

        StringTokenizer st = new StringTokenizer(style, " ,");
        int count = st.countTokens();
        float[] intervals = new float[(count & 1) == 1 ? count * 2 : count];
        float max = 0;
        float current = 1f;
        int i = 0;
        while (st.hasMoreTokens()) {
            intervals[i++] = current = toFloat(st.nextToken(), current);
            max += current;
        }

        // in svg speak, we double the intervals on an odd count
        for (int start = 0; i < intervals.length; i++, start++) {
            max += intervals[i] = intervals[start];
        }

        float off = 0f;
        if (offset != null) {
            try {
                off = Float.parseFloat(offset) % max;
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        strokePaint.setPathEffect(new DashPathEffect(intervals, off));
    }

    private static float toFloat(String s, float dflt) {
        float result = dflt;
        try {
            result = Float.parseFloat(s);
        } catch (NumberFormatException e) {
            // ignore
        }
        return result;
    }

    private void doLimits(float x, float y) {
        if (x < limits.left) {
            limits.left = x;
        }
        if (x > limits.right) {
            limits.right = x;
        }
        if (y < limits.top) {
            limits.top = y;
        }
        if (y > limits.bottom) {
            limits.bottom = y;
        }
    }

    private void doLimits(float x, float y, float width, float height) {
        doLimits(x, y);
        doLimits(x + width, y + height);
    }

    private void doLimits(Path path) {
        path.computeBounds(rect, false);
        doLimits(rect.left, rect.top);
        doLimits(rect.right, rect.bottom);
    }

    private final static Matrix IDENTITY_MATRIX = new Matrix();

    // XXX could be more selective using save(flags)
    private void pushTransform(Attributes atts) {
        final String transform = SVGParser.getStringAttr("transform", atts);
        final Matrix matrix = transform == null ? IDENTITY_MATRIX : SVGParser.parseTransform(transform);
        pushed++;
        canvas.save(); //Canvas.MATRIX_SAVE_FLAG);

        /*final Matrix m = canvas.getMatrix();
        m.postConcat(matrix);
        canvas.setMatrix(m);*/

        canvas.concat(matrix);
        //Log.d(TAG, "matrix push: " + canvas.getMatrix());
    }

    private void popTransform() {
        canvas.restore();
        //Log.d(TAG, "matrix pop: " + canvas.getMatrix());
        pushed--;
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) {
        //appendElementString(parsed, namespaceURI, localName, qName, atts);

        // Log.d(TAG, localName + showAttributes(atts));
        // Reset paint opacity
        strokePaint.setAlpha(255);
        fillPaint.setAlpha(255);
        // Ignore everything but rectangles in bounds mode
        if (boundsMode) {
            if (localName.equals("rect")) {
                Float x = SVGParser.getFloatAttr("x", atts);
                if (x == null) {
                    x = 0f;
                }
                Float y = SVGParser.getFloatAttr("y", atts);
                if (y == null) {
                    y = 0f;
                }
                Float width = SVGParser.getFloatAttr("width", atts);
                Float height = SVGParser.getFloatAttr("height", atts);
                bounds = new RectF(x, y, x + width, y + height);
            }
            return;
        }

        if (inDefsElement) {
            return;
        }

        if (localName.equals("svg")) {
            float width = SVGParser.getFloatAttr("width", atts);
            float height = SVGParser.getFloatAttr("height", atts);
            float[] nums = parseViewBox(SVGParser.getStringAttr("viewBox", atts));
            float scaleWidth = (nums[2] - nums[0]) / width;
            float scaleHeight = (nums[3] - nums[1]) / height;
            canvas = picture.beginRecording((int) (width * scaleWidth), (int) (height * scaleHeight));
        } else if (localName.equals("defs")) {
            inDefsElement = true;
        } else if (localName.equals("linearGradient")) {
            gradient = doGradient(true, atts);
        } else if (localName.equals("radialGradient")) {
            gradient = doGradient(false, atts);
        } else if (localName.equals("stop")) {
            if (gradient != null) {
                float offset = SVGParser.getFloatAttr("offset", atts);
                String styles = SVGParser.getStringAttr("style", atts);
                StyleSet styleSet = new StyleSet(styles);
                String colorStyle = styleSet.getStyle("stop-color");
                int color = Color.BLACK;
                if (colorStyle != null) {
                    if (colorStyle.startsWith("#")) {
                        color = Integer.parseInt(colorStyle.substring(1), 16);
                    } else {
                        color = Integer.parseInt(colorStyle, 16);
                    }
                }
                String opacityStyle = styleSet.getStyle("stop-opacity");
                if (opacityStyle != null) {
                    float alpha = Float.parseFloat(opacityStyle);
                    int alphaInt = Math.round(255 * alpha);
                    color |= (alphaInt << 24);
                } else {
                    color |= 0xFF000000;
                }
                gradient.positions.add(offset);
                gradient.colors.add(color);
            }
        } else if (localName.equals("use")) {
            String href = atts.getValue("xlink:href");
            String attTransform = atts.getValue("transform");
            String attX = atts.getValue("x");
            String attY = atts.getValue("y");

            StringBuilder sb = new StringBuilder();
            sb.append("<g");
            sb.append(" xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink' version='1.1'");
            if (attTransform != null || attX != null || attY != null) {
                sb.append(" transform='");
                if (attTransform != null) {
                    sb.append(SVGParser.escape(attTransform));
                }
                if (attX != null || attY != null) {
                    sb.append("translate(");
                    sb.append(attX != null ? SVGParser.escape(attX) : "0");
                    sb.append(",");
                    sb.append(attY != null ? SVGParser.escape(attY) : "0");
                    sb.append(")");
                }
                sb.append("'");
            }

            for (int i = 0; i < atts.getLength(); i++) {
                String attrQName = atts.getQName(i);
                if (!"x".equals(attrQName) && !"y".equals(attrQName) &&
                        !"width".equals(attrQName) && !"height".equals(attrQName) &&
                        !"xlink:href".equals(attrQName) && !"transform".equals(attrQName)) {

                    sb.append(" ");
                    sb.append(attrQName);
                    sb.append("='");
                    sb.append(SVGParser.escape(atts.getValue(i)));
                    sb.append("'");
                }
            }

            sb.append(">");

            sb.append(idXml.get(href.substring(1)));

            sb.append("</g>");

            // Log.d(TAG, sb.toString());

            InputSource is = new InputSource(new StringReader(sb.toString()));
            try {
                SAXParserFactory spf = SAXParserFactory.newInstance();
                SAXParser sp = spf.newSAXParser();
                XMLReader xr = sp.getXMLReader();
                xr.setContentHandler(this);
                xr.parse(is);
            } catch (Exception e) {
                Log.d(SVGParser.TAG, sb.toString());
                e.printStackTrace();
            }
        } else if (localName.equals("g")) {
            // Check to see if this is the "bounds" layer
            if ("bounds".equalsIgnoreCase(SVGParser.getStringAttr("id", atts))) {
                boundsMode = true;
            }
            if (hidden) {
                hiddenLevel++;
                //Util.debug("Hidden up: " + hiddenLevel);
            }
            // Go in to hidden mode if display is "none"
            if ("none".equals(SVGParser.getStringAttr("display", atts))) {
                if (!hidden) {
                    hidden = true;
                    hiddenLevel = 1;
                    //Util.debug("Hidden up: " + hiddenLevel);
                }
            }
            pushTransform(atts); // sau
            Properties props = new Properties(atts);

            fillPaintStack.push(new Paint(fillPaint));
            strokePaintStack.push(new Paint(strokePaint));
            fillSetStack.push(fillSet);
            strokeSetStack.push(strokeSet);

            doText(atts, fillPaint);
            doText(atts, strokePaint);
            doFill(props, gradientMap);
            doStroke(props);

            fillSet |= (props.getString("fill") != null);
            strokeSet |= (props.getString("stroke") != null);
        } else if (!hidden && localName.equals("rect")) {
            Float x = SVGParser.getFloatAttr("x", atts);
            if (x == null) {
                x = 0f;
            }
            Float y = SVGParser.getFloatAttr("y", atts);
            if (y == null) {
                y = 0f;
            }
            Float width = SVGParser.getFloatAttr("width", atts);
            Float height = SVGParser.getFloatAttr("height", atts);
            Float rx = SVGParser.getFloatAttr("rx", atts, 0f);
            Float ry = SVGParser.getFloatAttr("ry", atts, 0f);
            pushTransform(atts);
            Properties props = new Properties(atts);
            if (doFill(props, gradientMap)) {
                doLimits(x, y, width, height);
                if (rx <= 0f && ry <= 0f) {
                    canvas.drawRect(x, y, x + width, y + height, fillPaint);
                } else {
                    rect.set(x, y, x + width, y + height);
                    canvas.drawRoundRect(rect, rx, ry, fillPaint);
                }
            }
            if (doStroke(props)) {
                if (rx <= 0f && ry <= 0f) {
                    canvas.drawRect(x, y, x + width, y + height, strokePaint);
                } else {
                    rect.set(x, y, x + width, y + height);
                    canvas.drawRoundRect(rect, rx, ry, strokePaint);
                }
            }
            popTransform();
        } else if (!hidden && localName.equals("line")) {
            Float x1 = SVGParser.getFloatAttr("x1", atts);
            Float x2 = SVGParser.getFloatAttr("x2", atts);
            Float y1 = SVGParser.getFloatAttr("y1", atts);
            Float y2 = SVGParser.getFloatAttr("y2", atts);
            Properties props = new Properties(atts);
            if (doStroke(props)) {
                pushTransform(atts);
                doLimits(x1, y1);
                doLimits(x2, y2);
                canvas.drawLine(x1, y1, x2, y2, strokePaint);
                popTransform();
            }
        } else if (!hidden && localName.equals("circle")) {
            Float centerX = SVGParser.getFloatAttr("cx", atts);
            Float centerY = SVGParser.getFloatAttr("cy", atts);
            Float radius = SVGParser.getFloatAttr("r", atts);
            if (centerX != null && centerY != null && radius != null) {
                pushTransform(atts);
                Properties props = new Properties(atts);
                if (doFill(props, gradientMap)) {
                    doLimits(centerX - radius, centerY - radius);
                    doLimits(centerX + radius, centerY + radius);
                    canvas.drawCircle(centerX, centerY, radius, fillPaint);
                }
                if (doStroke(props)) {
                    canvas.drawCircle(centerX, centerY, radius, strokePaint);
                }
                popTransform();
            }
        } else if (!hidden && localName.equals("ellipse")) {
            Float centerX = SVGParser.getFloatAttr("cx", atts);
            Float centerY = SVGParser.getFloatAttr("cy", atts);
            Float radiusX = SVGParser.getFloatAttr("rx", atts);
            Float radiusY = SVGParser.getFloatAttr("ry", atts);
            if (centerX != null && centerY != null && radiusX != null && radiusY != null) {
                pushTransform(atts);
                Properties props = new Properties(atts);
                rect.set(centerX - radiusX, centerY - radiusY, centerX + radiusX, centerY + radiusY);
                if (doFill(props, gradientMap)) {
                    doLimits(centerX - radiusX, centerY - radiusY);
                    doLimits(centerX + radiusX, centerY + radiusY);
                    canvas.drawOval(rect, fillPaint);
                }
                if (doStroke(props)) {
                    canvas.drawOval(rect, strokePaint);
                }
                popTransform();
            }
        } else if (!hidden && (localName.equals("polygon") || localName.equals("polyline"))) {
            NumberParse numbers = SVGParser.getNumberParseAttr("points", atts);
            if (numbers != null) {
                Path p = new Path();
                if (numbers.size() > 1) {
                    pushTransform(atts);
                    Properties props = new Properties(atts);
                    p.moveTo(numbers.getNumber(0), numbers.getNumber(1));
                    for (int i = 2; i < numbers.size(); i += 2) {
                        float x = numbers.getNumber(i);
                        float y = numbers.getNumber(i + 1);
                        p.lineTo(x, y);
                    }
                    // Don't close a polyline
                    if (localName.equals("polygon")) {
                        p.close();
                    }
                    if (doFill(props, gradientMap)) {
                        doLimits(p);

                        // showBounds("fill", p);
                        canvas.drawPath(p, fillPaint);
                    }
                    if (doStroke(props)) {
                        // showBounds("stroke", p);
                        canvas.drawPath(p, strokePaint);
                    }
                    popTransform();
                }
            }
        } else if (!hidden && localName.equals("path")) {
            Path p = SVGParser.doPath(SVGParser.getStringAttr("d", atts));
            pushTransform(atts);
            Properties props = new Properties(atts);
            if (doFill(props, gradientMap)) {
                // showBounds("gradient", p);
                doLimits(p);
                // showBounds("gradient", p);
                canvas.drawPath(p, fillPaint);
            }
            if (doStroke(props)) {
                // showBounds("paint", p);
                canvas.drawPath(p, strokePaint);
            }
            popTransform();
        } else if (!hidden && localName.equals("text")) {
            pushTransform(atts);
            text = new SvgText(this, atts);
        } else if (!hidden) {
            Log.d(SVGParser.TAG, "UNRECOGNIZED SVG COMMAND: " + localName);
        }
    }

    private float[] parseViewBox(String viewBoxStr) {
        String[] viewBox = viewBoxStr.split("\\s+");
        float[] nums = new float[4];
        if (viewBox.length == 4) {
            for (int i = 0; i < 4; i++) {
                nums[i] = Float.parseFloat(viewBox[i]);
            }
        } else {
            throw new RuntimeException("Invalid ViewBox");
        }
        return nums;
    }

    @SuppressWarnings("unused")
    private void showBounds(String text, Path p) {
        RectF b = new RectF();
        p.computeBounds(b, true);
        Log.d(SVGParser.TAG, text + " bounds: " + b.left + "," + b.bottom + " to " + b.right + "," + b.top);
    }

    @SuppressWarnings("unused")
    private String showAttributes(Attributes a) {
        String result = "";
        for (int i = 0; i < a.getLength(); i++) {
            result += " " + a.getLocalName(i) + "='" + a.getValue(i) + "'";
        }
        return result;
    }

    @Override
    public void characters(char ch[], int start, int length) {
        // Log.i(TAG, new String(ch) + " " + start + "/" + length);
        if (text != null) {
            text.setText(ch, start, length);
        }
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName) {
        /*parsed.append("</");
        parsed.append(localName);
        parsed.append(">");*/

        if (inDefsElement) {
            if (localName.equals("defs")) {
                inDefsElement = false;
            }
            return;
        }

        if (localName.equals("svg")) {
            picture.endRecording();
        } else if (localName.equals("text")) {
            if (text != null) {
                text.render(canvas);
                text.close();
            }
            popTransform();
        } else if (localName.equals("linearGradient")) {
            if (gradient.id != null) {
                if (gradient.xlink != null) {
                    Gradient parent = gradientRefMap.get(gradient.xlink);
                    if (parent != null) {
                        gradient = parent.createChild(gradient);
                    }
                }
                int[] colors = new int[gradient.colors.size()];
                for (int i = 0; i < colors.length; i++) {
                    colors[i] = gradient.colors.get(i);
                }
                float[] positions = new float[gradient.positions.size()];
                for (int i = 0; i < positions.length; i++) {
                    positions[i] = gradient.positions.get(i);
                }
                if (colors.length == 0) {
                    Log.d("BAD", "BAD");
                }
                LinearGradient g = new LinearGradient(gradient.x1, gradient.y1, gradient.x2, gradient.y2, colors, positions, Shader.TileMode.CLAMP);
                if (gradient.matrix != null) {
                    g.setLocalMatrix(gradient.matrix);
                }
                gradientMap.put(gradient.id, g);
                gradientRefMap.put(gradient.id, gradient);
            }
        } else if (localName.equals("radialGradient")) {
            if (gradient.id != null) {
                int[] colors = new int[gradient.colors.size()];
                for (int i = 0; i < colors.length; i++) {
                    colors[i] = gradient.colors.get(i);
                }
                float[] positions = new float[gradient.positions.size()];
                for (int i = 0; i < positions.length; i++) {
                    positions[i] = gradient.positions.get(i);
                }
                if (gradient.xlink != null) {
                    Gradient parent = gradientRefMap.get(gradient.xlink);
                    if (parent != null) {
                        gradient = parent.createChild(gradient);
                    }
                }
                RadialGradient g = new RadialGradient(gradient.x, gradient.y, gradient.radius, colors, positions, Shader.TileMode.CLAMP);
                if (gradient.matrix != null) {
                    g.setLocalMatrix(gradient.matrix);
                }
                gradientMap.put(gradient.id, g);
                gradientRefMap.put(gradient.id, gradient);
            }
        } else if (localName.equals("g")) {
            if (boundsMode) {
                boundsMode = false;
            }
            // Break out of hidden mode
            if (hidden) {
                hiddenLevel--;
                //Util.debug("Hidden down: " + hiddenLevel);
                if (hiddenLevel == 0) {
                    hidden = false;
                }
            }
            // Clear gradient map
            gradientMap.clear();
            popTransform(); // SAU
            fillPaint = fillPaintStack.pop();
            fillSet = fillSetStack.pop();
            strokePaint = strokePaintStack.pop();
            strokeSet = strokeSetStack.pop();
        }
    }

    // class to hold text properties

    private Paint.Align getTextAlign(Attributes atts) {
        String align = SVGParser.getStringAttr("text-anchor", atts);
        if (align == null) {
            return null;
        }
        if ("middle".equals(align)) {
            return Paint.Align.CENTER;
        } else if ("end".equals(align)) {
            return Paint.Align.RIGHT;
        } else {
            return Paint.Align.LEFT;
        }
    }

    private Typeface setTypeFace(Attributes atts) {
        String face = SVGParser.getStringAttr("font-family", atts);
        String style = SVGParser.getStringAttr("font-style", atts);
        String weight = SVGParser.getStringAttr("font-weight", atts);

        if (face == null && style == null && weight == null) {
            return null;
        }
        int styleParam = Typeface.NORMAL;
        if ("italic".equals(style)) {
            styleParam |= Typeface.ITALIC;
        }
        if ("bold".equals(weight)) {
            styleParam |= Typeface.BOLD;
        }
        Typeface result = Typeface.create(face, styleParam);
        // Log.d(TAG, "typeface=" + result + " " + styleParam);
        return result;
    }
}
