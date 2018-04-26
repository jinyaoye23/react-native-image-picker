package com.mg.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;

import java.io.File;
import java.io.IOException;

import id.zelory.compressor.Compressor;


class Compression {

    public File compressImage(final Context activity, final ReadableMap options, final String originalImagePath) throws IOException {
        Integer maxWidth = options.hasKey("width") ? options.getInt("width") : null;
        Integer maxHeight = options.hasKey("height") ? options.getInt("height") : null;
        Integer quality = options.hasKey("compressQuality") ? options.getInt("compressQuality") : null;


        if (maxWidth == null || maxWidth <= 0) {
            maxWidth = 300;
        }
        if (maxHeight == null || maxHeight <= 0) {
            maxHeight = 300;
        }
        if (quality == null || quality >= 100 || quality <= 0) {
            return new File(originalImagePath);
        }
        String path = PickerModule.img_savepath.substring(0, PickerModule.img_savepath.length() - 1);
        Compressor compressor = new Compressor(activity)
                .setCompressFormat(Bitmap.CompressFormat.JPEG)
                .setDestinationDirectoryPath(path);
//                .setDestinationDirectoryPath(Environment.getExternalStoragePublicDirectory(
//                        Environment.DIRECTORY_PICTURES).getAbsolutePath());


        Integer myquality = 80;
        if (quality < myquality) {
            myquality = quality;
        }
        compressor.setQuality(myquality);

        if (maxWidth != null) {
            compressor.setMaxWidth(maxWidth);
            Log.d("aaa", maxWidth + "---maxWidth2");

        }

        if (maxHeight != null) {
            compressor.setMaxHeight(maxHeight);
            Log.d("aaa", maxHeight + "---maxHeight2");

        }

        File image = new File(originalImagePath);

        String[] paths = image.getName().split("\\.(?=[^\\.]+$)");
        String compressedFileName = paths[0] + "-compressed";

        if (paths.length > 1)
            compressedFileName += "." + paths[1];

        return compressor
                .compressToFile(image, compressedFileName);
    }

    synchronized void compressVideo(final Activity activity, final ReadableMap options,
                                    final String originalVideo, final String compressedVideo, final Promise promise) {
        // todo: video compression
        // failed attempt 1: ffmpeg => slow and licensing issues
        promise.resolve(originalVideo);
    }
}
