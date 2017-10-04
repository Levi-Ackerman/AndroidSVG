package com.larvalabs.svgandroid;

import org.xml.sax.Attributes;

/**
 * Created by zhengxianlzx on 17-10-5.
 */
public class Properties {
    StyleSet styles = null;
    Attributes atts;

    public Properties(Attributes atts) {
        this.atts = atts;
        String styleAttr = SVGParser.getStringAttr("style", atts);
        if (styleAttr != null) {
            styles = new StyleSet(styleAttr);
        }
    }

    public String getAttr(String name) {
        String v = null;
        if (styles != null) {
            v = styles.getStyle(name);
        }
        if (v == null) {
            v = SVGParser.getStringAttr(name, atts);
        }
        return v;
    }

    public String getString(String name) {
        return getAttr(name);
    }

    public Integer getColorValue(String name) {
        String v = getAttr(name);
        if (v == null) {
            return null;
        } else if (v.startsWith("#") && (v.length() == 4 || v.length() == 7)) {
            try {
                int result = Integer.parseInt(v.substring(1), 16);
                return v.length() == 4 ? hex3Tohex6(result) : result;
            } catch (NumberFormatException nfe) {
                return null;
            }
        } else {
            return SVGColors.mapColor(v);
        }
    }

    // convert 0xRGB into 0xRRGGBB
    private int hex3Tohex6(int x) {
        return (x & 0xF00) << 8 | (x & 0xF00) << 12 |
                (x & 0xF0) << 4 | (x & 0xF0) << 8 |
                (x & 0xF) << 4 | (x & 0xF);
    }

    @SuppressWarnings("unused")
    public Float getFloat(String name, float defaultValue) {
        Float v = getFloat(name);
        if (v == null) {
            return defaultValue;
        } else {
            return v;
        }
    }

    public Float getFloat(String name) {
        String v = getAttr(name);
        if (v == null) {
            return null;
        } else {
            try {
                return Float.parseFloat(v);
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
    }
}
