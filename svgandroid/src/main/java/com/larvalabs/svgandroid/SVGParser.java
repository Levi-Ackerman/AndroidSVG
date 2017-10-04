/*Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing, software
 *distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *See the License for the specific language governing permissions and
 *limitations under the License.
 *
 *Changes Copyright 2011 Google Inc.
 */

package com.larvalabs.svgandroid;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.RectF;
import android.util.Log;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/*

   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */

/**
 * Entry point for parsing SVG files for Android.
 * Use one of the various static methods for parsing SVGs by resource, asset or input stream.
 * Optionally, a single color can be searched and replaced in the SVG while parsing.
 * You can also parse an svg path directly.
 *
 * @see #getSVGFromResource(Resources, int)
 * @see #getSVGFromAsset(AssetManager, String)
 * @see #getSVGFromString(String)
 * @see #getSVGFromInputStream(InputStream)
 * @see #parsePath(String)
 *
 * @author Larva Labs, LLC
 */
public class SVGParser {

	static final String TAG = "SVGAndroid";

	/**
	 * Parse SVG data from an input stream.
	 * @param svgData the input stream, with SVG XML data in UTF-8 character encoding.
	 * @return the parsed SVG.
	 * @throws SVGParseException if there is an error while parsing.
	 */
	public static SVG getSVGFromInputStream(InputStream svgData) throws SVGParseException {
		return SVGParser.parse(svgData, 0, 0, false);
	}

	/**
	 * Parse SVG data from a string.
	 * @param svgData the string containing SVG XML data.
	 * @return the parsed SVG.
	 * @throws SVGParseException if there is an error while parsing.
	 */
	public static SVG getSVGFromString(String svgData) throws SVGParseException {
		return SVGParser.parse(new ByteArrayInputStream(svgData.getBytes()), 0, 0, false);
	}

	/**
	 * Parse SVG data from an Android application resource.
	 * @param resources the Android context resources.
	 * @param resId the ID of the raw resource SVG.
	 * @return the parsed SVG.
	 * @throws SVGParseException if there is an error while parsing.
	 */
	public static SVG getSVGFromResource(Resources resources, int resId) throws SVGParseException {
		return SVGParser.parse(resources.openRawResource(resId), 0, 0, false);
	}

	/**
	 * Parse SVG data from an Android application asset.
	 * @param assetMngr the Android asset manager.
	 * @param svgPath the path to the SVG file in the application's assets.
	 * @return the parsed SVG.
	 * @throws SVGParseException if there is an error while parsing.
	 * @throws IOException if there was a problem reading the file.
	 */
	public static SVG getSVGFromAsset(AssetManager assetMngr, String svgPath) throws SVGParseException, IOException {
		InputStream inputStream = assetMngr.open(svgPath);
		SVG svg = getSVGFromInputStream(inputStream);
		inputStream.close();
		return svg;
	}

	/**
	 * Parse SVG data from an input stream, replacing a single color with another color.
	 * @param svgData the input stream, with SVG XML data in UTF-8 character encoding.
	 * @param searchColor the color in the SVG to replace.
	 * @param replaceColor the color with which to replace the search color.
	 * @return the parsed SVG.
	 * @throws SVGParseException if there is an error while parsing.
	 */
	public static SVG getSVGFromInputStream(InputStream svgData, int searchColor, int replaceColor) throws SVGParseException {
		return SVGParser.parse(svgData, searchColor, replaceColor, false);
	}

	/**
	 * Parse SVG data from a string.
	 * @param svgData the string containing SVG XML data.
	 * @param searchColor the color in the SVG to replace.
	 * @param replaceColor the color with which to replace the search color.
	 * @return the parsed SVG.
	 * @throws SVGParseException if there is an error while parsing.
	 */
	public static SVG getSVGFromString(String svgData, int searchColor, int replaceColor) throws SVGParseException {
		return SVGParser.parse(new ByteArrayInputStream(svgData.getBytes()), searchColor, replaceColor, false);
	}

	/**
	 * Parse SVG data from an Android application resource.
	 * @param resources the Android context
	 * @param resId the ID of the raw resource SVG.
	 * @param searchColor the color in the SVG to replace.
	 * @param replaceColor the color with which to replace the search color.
	 * @return the parsed SVG.
	 * @throws SVGParseException if there is an error while parsing.
	 */
	public static SVG getSVGFromResource(Resources resources, int resId, int searchColor, int replaceColor) throws SVGParseException {
		return SVGParser.parse(resources.openRawResource(resId), searchColor, replaceColor, false);
	}

	/**
	 * Parse SVG data from an Android application asset.
	 * @param assetMngr the Android asset manager.
	 * @param svgPath the path to the SVG file in the application's assets.
	 * @param searchColor the color in the SVG to replace.
	 * @param replaceColor the color with which to replace the search color.
	 * @return the parsed SVG.
	 * @throws SVGParseException if there is an error while parsing.
	 * @throws IOException if there was a problem reading the file.
	 */
	public static SVG getSVGFromAsset(AssetManager assetMngr, String svgPath, int searchColor, int replaceColor) throws SVGParseException, IOException {
		InputStream inputStream = assetMngr.open(svgPath);
		SVG svg = getSVGFromInputStream(inputStream, searchColor, replaceColor);
		inputStream.close();
		return svg;
	}

	/**
	 * Parses a single SVG path and returns it as a <code>android.graphics.Path</code> object.
	 * An example path is <code>M250,150L150,350L350,350Z</code>, which draws a triangle.
	 *
	 * @param pathString the SVG path, see the specification <a href="http://www.w3.org/TR/SVG/paths.html">here</a>.
	 */
	public static Path parsePath(String pathString) {
		return doPath(pathString);
	}

	private static SVG parse(InputStream in, Integer searchColor, Integer replaceColor, boolean whiteMode) throws SVGParseException {
		// Util.debug("Parsing SVG...");
		SVGHandler svgHandler = null;
		try {
			// long start = System.currentTimeMillis();
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			final Picture picture = new Picture();
			svgHandler = new SVGHandler(picture);
			svgHandler.setColorSwap(searchColor, replaceColor);
			svgHandler.setWhiteMode(whiteMode);

			CopyInputStream cin = new CopyInputStream(in);
			
			IDHandler idHandler = new IDHandler();
			xr.setContentHandler(idHandler);
			xr.parse(new InputSource(cin.getCopy()));
			svgHandler.idXml = idHandler.idXml;
			
			xr.setContentHandler(svgHandler);
			xr.parse(new InputSource(cin.getCopy()));
			// Util.debug("Parsing complete in " + (System.currentTimeMillis() - start) + " millis.");
			SVG result = new SVG(picture, svgHandler.bounds);
			// Skip bounds if it was an empty pic
			if (!Float.isInfinite(svgHandler.limits.top)) {
				result.setLimits(svgHandler.limits);
			}
			return result;
		} catch (Exception e) {
			//for (String s : handler.parsed.toString().replace(">", ">\n").split("\n"))
			//	Log.d(TAG, "Parsed: " + s);
			throw new SVGParseException(e);
		}
	}

	static String escape(String s) {
		return s
			.replaceAll("\"", "&quot;")
			.replaceAll("'", "&apos")
			.replaceAll("<", "&lt;")
			.replaceAll(">", "&gt;")
			.replaceAll("&", "&amp;");
	}

	private static NumberParse parseNumbers(String s) {
		//Util.debug("Parsing numbers from: '" + s + "'");
		int n = s.length();
		int p = 0;
		ArrayList<Float> numbers = new ArrayList<Float>();
		boolean skipChar = false;
		for (int i = 1; i < n; i++) {
			if (skipChar) {
				skipChar = false;
				continue;
			}
			char c = s.charAt(i);
			switch (c) {
			// This ends the parsing, as we are on the next element
			case 'M':
			case 'm':
			case 'Z':
			case 'z':
			case 'L':
			case 'l':
			case 'H':
			case 'h':
			case 'V':
			case 'v':
			case 'C':
			case 'c':
			case 'S':
			case 's':
			case 'Q':
			case 'q':
			case 'T':
			case 't':
			case 'a':
			case 'A':
			case ')': {
				String str = s.substring(p, i);
				if (str.trim().length() > 0) {
					//Util.debug("  Last: " + str);
					Float f = Float.parseFloat(str);
					numbers.add(f);
				}
				p = i;
				return new NumberParse(numbers, p);
			}
			case '\n':
			case '\t':
			case ' ':
			case ',':
			case '-': {
				String str = s.substring(p, i);
				// Just keep moving if multiple whitespace
				if (str.trim().length() > 0) {
					//Util.debug("  Next: " + str);
					Float f = Float.parseFloat(str);
					numbers.add(f);
					if (c == '-') {
						p = i;
					} else {
						p = i + 1;
						skipChar = true;
					}
				} else {
					p++;
				}
				break;
			}
			}
		}
		String last = s.substring(p);
		if (last.length() > 0) {
			//Util.debug("  Last: " + last);
			try {
				numbers.add(Float.parseFloat(last));
			} catch (NumberFormatException nfe) {
				// Just white-space, forget it
			}
			p = s.length();
		}
		return new NumberParse(numbers, p);
	}

	// Process a list of transforms
	// foo(n,n,n...) bar(n,n,n..._ ...)
	// delims are whitespace or ,'s

	static Matrix parseTransform(String s) {
		//Log.d(TAG, s);
		Matrix matrix = new Matrix();
		while (true) {
			parseTransformItem(s, matrix);
			// Log.i(TAG, "Transformed: (" + s + ") " + matrix);
			int rparen = s.indexOf(")");
			if (rparen > 0 && s.length() > rparen + 1) {
				s = s.substring(rparen + 1).replaceFirst("[\\s,]*", "");
			} else {
				break;
			}
		}
		//Log.d(TAG, matrix.toShortString());
		return matrix;
	}

	public static Matrix parseTransformItem(String s, Matrix matrix) {
		if (s.startsWith("matrix(")) {
			NumberParse np = parseNumbers(s.substring("matrix(".length()));
			if (np.size() == 6) {
				Matrix mat = new Matrix();
				mat.setValues(new float[] {
						// Row 1
						np.getNumber(0),
						np.getNumber(2),
						np.getNumber(4),
						// Row 2
						np.getNumber(1),
						np.getNumber(3),
						np.getNumber(5),
						// Row 3
						0,
						0,
						1,
				});
				matrix.preConcat(mat);
			}
		} else if (s.startsWith("translate(")) {
			NumberParse np = parseNumbers(s.substring("translate(".length()));
			if (np.size() > 0) {
				float tx = np.getNumber(0);
				float ty = 0;
				if (np.size() > 1) {
					ty = np.getNumber(1);
				}
				matrix.preTranslate(tx, ty);
			}
		} else if (s.startsWith("scale(")) {
			NumberParse np = parseNumbers(s.substring("scale(".length()));
			if (np.size() > 0) {
				float sx = np.getNumber(0);
				float sy = sx;
				if (np.size() > 1) {
					sy = np.getNumber(1);
				}
				matrix.preScale(sx, sy);
			}
		} else if (s.startsWith("skewX(")) {
			NumberParse np = parseNumbers(s.substring("skewX(".length()));
			if (np.size() > 0) {
				float angle = np.getNumber(0);
				matrix.preSkew((float) Math.tan(angle), 0);
			}
		} else if (s.startsWith("skewY(")) {
			NumberParse np = parseNumbers(s.substring("skewY(".length()));
			if (np.size() > 0) {
				float angle = np.getNumber(0);
				matrix.preSkew(0, (float) Math.tan(angle));
			}
		} else if (s.startsWith("rotate(")) {
			NumberParse np = parseNumbers(s.substring("rotate(".length()));
			if (np.size() > 0) {
				float angle = np.getNumber(0);
				float cx = 0;
				float cy = 0;
				if (np.size() > 2) {
					cx = np.getNumber(1);
					cy = np.getNumber(2);
				}
				matrix.preTranslate(cx, cy);
				matrix.preRotate(angle);
				matrix.preTranslate(-cx, -cy);
			}
		} else {
			Log.i(TAG, "Invalid transform (" + s + ")");
		}
		return matrix;
	}

	/**
	 * This is where the hard-to-parse paths are handled.
	 * Uppercase rules are absolute positions, lowercase are relative.
	 * Types of path rules:
	 * <p/>
	 * <ol>
	 * <li>M/m - (x y)+ - Move to (without drawing)
	 * <li>Z/z - (no params) - Close path (back to starting point)
	 * <li>L/l - (x y)+ - Line to
	 * <li>H/h - x+ - Horizontal ine to
	 * <li>V/v - y+ - Vertical line to
	 * <li>C/c - (x1 y1 x2 y2 x y)+ - Cubic bezier to
	 * <li>S/s - (x2 y2 x y)+ - Smooth cubic bezier to (shorthand that assumes the x2, y2 from previous C/S is the x1, y1 of this bezier)
	 * <li>Q/q - (x1 y1 x y)+ - Quadratic bezier to
	 * <li>T/t - (x y)+ - Smooth quadratic bezier to (assumes previous control point is "reflection" of last one w.r.t. to current point)
	 * </ol>
	 * <p/>
	 * Numbers are separate by whitespace, comma or nothing at all (!) if they are self-delimiting, (ie. begin with a - sign)
	 *
	 * @param s the path string from the XML
	 */
	static Path doPath(String s) {
		int n = s.length();
		ParserHelper ph = new ParserHelper(s, 0);
		ph.skipWhitespace();
		Path p = new Path();
		float lastX = 0;
		float lastY = 0;
		float lastX1 = 0;
		float lastY1 = 0;
		RectF r = new RectF();
		char cmd = 'x';
		while (ph.pos < n) {
			char next = s.charAt(ph.pos);
			if (!Character.isDigit(next) && !(next == '.') && !(next == '-')) {
				cmd = next;
				ph.advance();
			} else if (cmd == 'M') { // implied command
				cmd = 'L';
			} else if (cmd == 'm') { // implied command
				cmd = 'l';
			} else { // implied command
				// Log.d(TAG, "Implied command: " + cmd);
			}
			p.computeBounds(r, true);
			// Log.d(TAG, "  " + cmd + " " + r);
			// Util.debug("* Commands remaining: '" + path + "'.");
			boolean wasCurve = false;
			switch (cmd) {
			case 'M':
			case 'm': {
				float x = ph.nextFloat();
				float y = ph.nextFloat();
				if (cmd == 'm') {
					p.rMoveTo(x, y);
					lastX += x;
					lastY += y;
				} else {
					p.moveTo(x, y);
					lastX = x;
					lastY = y;
				}
				break;
			}
			case 'Z':
			case 'z': {
				p.close();
				break;
			}
			case 'L':
			case 'l': {
				float x = ph.nextFloat();
				float y = ph.nextFloat();
				if (cmd == 'l') {
					p.rLineTo(x, y);
					lastX += x;
					lastY += y;
				} else {
					p.lineTo(x, y);
					lastX = x;
					lastY = y;
				}
				break;
			}
			case 'H':
			case 'h': {
				float x = ph.nextFloat();
				if (cmd == 'h') {
					p.rLineTo(x, 0);
					lastX += x;
				} else {
					p.lineTo(x, lastY);
					lastX = x;
				}
				break;
			}
			case 'V':
			case 'v': {
				float y = ph.nextFloat();
				if (cmd == 'v') {
					p.rLineTo(0, y);
					lastY += y;
				} else {
					p.lineTo(lastX, y);
					lastY = y;
				}
				break;
			}
			case 'C':
			case 'c': {
				wasCurve = true;
				float x1 = ph.nextFloat();
				float y1 = ph.nextFloat();
				float x2 = ph.nextFloat();
				float y2 = ph.nextFloat();
				float x = ph.nextFloat();
				float y = ph.nextFloat();
				if (cmd == 'c') {
					x1 += lastX;
					x2 += lastX;
					x += lastX;
					y1 += lastY;
					y2 += lastY;
					y += lastY;
				}
				p.cubicTo(x1, y1, x2, y2, x, y);
				lastX1 = x2;
				lastY1 = y2;
				lastX = x;
				lastY = y;
				break;
			}
			case 'S':
			case 's': {
				wasCurve = true;
				float x2 = ph.nextFloat();
				float y2 = ph.nextFloat();
				float x = ph.nextFloat();
				float y = ph.nextFloat();
				if (cmd == 's') {
					x2 += lastX;
					x += lastX;
					y2 += lastY;
					y += lastY;
				}
				float x1 = 2 * lastX - lastX1;
				float y1 = 2 * lastY - lastY1;
				p.cubicTo(x1, y1, x2, y2, x, y);
				lastX1 = x2;
				lastY1 = y2;
				lastX = x;
				lastY = y;
				break;
			}
			case 'A':
			case 'a': {
				float rx = ph.nextFloat();
				float ry = ph.nextFloat();
				float theta = ph.nextFloat();
				int largeArc = (int) ph.nextFloat();
				int sweepArc = (int) ph.nextFloat();
				float x = ph.nextFloat();
				float y = ph.nextFloat();
				if (cmd == 'a') {
					x += lastX;
					y += lastY;
				}
				drawArc(p, lastX, lastY, x, y, rx, ry, theta, largeArc == 1, sweepArc == 1);
				lastX = x;
				lastY = y;
				break;
			}
			default:
				Log.d(TAG, "Invalid path command: " + cmd);
				ph.advance();
			}
			if (!wasCurve) {
				lastX1 = lastX;
				lastY1 = lastY;
			}
			ph.skipWhitespace();
		}
		return p;
	}

	/**
	 * Elliptical arc implementation based on the SVG specification notes
	 * Adapted from the Batik library (Apache-2 license) by SAU
	 */

	private static void drawArc(Path path, double x0, double y0, double x, double y, double rx,
			double ry, double angle, boolean largeArcFlag, boolean sweepFlag) {
		double dx2 = (x0 - x) / 2.0;
		double dy2 = (y0 - y) / 2.0;
		angle = Math.toRadians(angle % 360.0);
		double cosAngle = Math.cos(angle);
		double sinAngle = Math.sin(angle);

		double x1 = (cosAngle * dx2 + sinAngle * dy2);
		double y1 = (-sinAngle * dx2 + cosAngle * dy2);
		rx = Math.abs(rx);
		ry = Math.abs(ry);

		double Prx = rx * rx;
		double Pry = ry * ry;
		double Px1 = x1 * x1;
		double Py1 = y1 * y1;

		// check that radii are large enough
		double radiiCheck = Px1 / Prx + Py1 / Pry;
		if (radiiCheck > 1) {
			rx = Math.sqrt(radiiCheck) * rx;
			ry = Math.sqrt(radiiCheck) * ry;
			Prx = rx * rx;
			Pry = ry * ry;
		}

		// Step 2 : Compute (cx1, cy1)
		double sign = (largeArcFlag == sweepFlag) ? -1 : 1;
		double sq = ((Prx * Pry) - (Prx * Py1) - (Pry * Px1))
		/ ((Prx * Py1) + (Pry * Px1));
		sq = (sq < 0) ? 0 : sq;
		double coef = (sign * Math.sqrt(sq));
		double cx1 = coef * ((rx * y1) / ry);
		double cy1 = coef * -((ry * x1) / rx);

		double sx2 = (x0 + x) / 2.0;
		double sy2 = (y0 + y) / 2.0;
		double cx = sx2 + (cosAngle * cx1 - sinAngle * cy1);
		double cy = sy2 + (sinAngle * cx1 + cosAngle * cy1);

		// Step 4 : Compute the angleStart (angle1) and the angleExtent (dangle)
		double ux = (x1 - cx1) / rx;
		double uy = (y1 - cy1) / ry;
		double vx = (-x1 - cx1) / rx;
		double vy = (-y1 - cy1) / ry;
		double p, n;

		// Compute the angle start
		n = Math.sqrt((ux * ux) + (uy * uy));
		p = ux; // (1 * ux) + (0 * uy)
		sign = (uy < 0) ? -1.0 : 1.0;
		double angleStart = Math.toDegrees(sign * Math.acos(p / n));

		// Compute the angle extent
		n = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
		p = ux * vx + uy * vy;
		sign = (ux * vy - uy * vx < 0) ? -1.0 : 1.0;
		double angleExtent = Math.toDegrees(sign * Math.acos(p / n));
		if (!sweepFlag && angleExtent > 0) {
			angleExtent -= 360f;
		} else if (sweepFlag && angleExtent < 0) {
			angleExtent += 360f;
		}
		angleExtent %= 360f;
		angleStart %= 360f;

		RectF oval = new RectF((float) (cx - rx), (float) (cy - ry), (float) (cx + rx), (float) (cy + ry));
		path.addArc(oval, (float) angleStart, (float) angleExtent);
	}

	static NumberParse getNumberParseAttr(String name, Attributes attributes) {
		int n = attributes.getLength();
		for (int i = 0; i < n; i++) {
			if (attributes.getLocalName(i).equals(name)) {
				return parseNumbers(attributes.getValue(i));
			}
		}
		return null;
	}

	static String getStringAttr(String name, Attributes attributes) {
		int n = attributes.getLength();
		for (int i = 0; i < n; i++) {
			if (attributes.getLocalName(i).equals(name)) {
				return attributes.getValue(i);
			}
		}
		return null;
	}

	public static Float getFloatAttr(String name, Attributes attributes) {
		return getFloatAttr(name, attributes, null);
	}

	public static Float getFloatAttr(String name, Attributes attributes, Float defaultValue) {
		String v = getStringAttr(name, attributes);
		if (v == null) {
			return defaultValue;
		} else {
			if (v.endsWith("px")) {
				v = v.substring(0, v.length() - 2);
			}
			//            Log.d(TAG, "Float parsing '" + name + "=" + v + "'");
			return Float.parseFloat(v);
		}
	}

}
