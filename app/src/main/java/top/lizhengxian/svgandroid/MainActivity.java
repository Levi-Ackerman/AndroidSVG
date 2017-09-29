package top.lizhengxian.svgandroid;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.larvalabs.svgandroid.R;
import com.larvalabs.svgandroid.SVGParser;

/**
 * ************************************************************
 * Copyright (C) 2005 - 2017 UCWeb Inc. All Rights Reserved
 * Description  :  PACKAGE_NAME.MainActivity.java
 * <p>
 * Creation     : 9/29/17
 * Author       : zhengxian.lzx@alibaba-inc.com
 * History      : Creation, 2017 lizx, Create the file
 * *************************************************************
 */

public class MainActivity extends AppCompatActivity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ImageView imageView = (ImageView) findViewById(R.id.img);
        imageView.setImageDrawable(SVGParser.getSVGFromResource(getResources(),R.raw.viewgallery).createPictureDrawable());
    }
}
