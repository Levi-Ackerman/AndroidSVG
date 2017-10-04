package com.larvalabs.svgandroid;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by zhengxianlzx on 17-10-5.
 */
public class CopyInputStream {
    private InputStream _is;
    private ByteArrayOutputStream _copy;

    public CopyInputStream(InputStream is) {
        _is = is;

        try {
            copy();
        } catch (IOException ex) {
            System.out.println("IOException in CopyInputStream");
            System.out.println(ex.toString());
            // do nothing
        }
    }

    private int copy() throws IOException {
        _copy = new ByteArrayOutputStream();
        int read = 0;
        int chunk = 0;
        byte[] data = new byte[256];

        while (-1 != (chunk = _is.read(data))) {
            read += data.length;
            // System.out.println("chunk = " + chunk);
            // System.out.println("read = " + read);

            _copy.write(data, 0, chunk);
        }
        _copy.flush();

        return read;
    }

    public InputStream getCopy() {
        return new ByteArrayInputStream(_copy.toByteArray());
    }
}
