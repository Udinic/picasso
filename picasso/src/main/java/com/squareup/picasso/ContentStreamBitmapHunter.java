/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.picasso;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

import static com.squareup.picasso.Picasso.LoadedFrom.DISK;

class ContentStreamBitmapHunter extends BitmapHunter {

  final Context context;

  ContentStreamBitmapHunter(Context context, Picasso picasso, Dispatcher dispatcher, Cache cache,
      Request request) {
    super(picasso, dispatcher, cache, request);
    this.context = context;
  }

  @Override Bitmap decode(Uri uri, PicassoBitmapOptions options, int retryCount)
      throws IOException {
    return decodeContentStream(uri, options);
  }

  @Override Picasso.LoadedFrom getLoadedFrom() {
    return DISK;
  }

//    private Bitmap decodeContentStream_new(Uri path, PicassoBitmapOptions bitmapOptions) throws IOException {
//        ContentResolver contentResolver = context.getContentResolver();
//
//        if (bitmapOptions != null) {
//            if (bitmapOptions.inBitmap != null || bitmapOptions.inJustDecodeBounds)
//            {
//
//                boolean checkSampleSize = bitmapOptions.inJustDecodeBounds;
//
//                options.inJustDecodeBounds = true;
//                InputStream is = null;
//                try {
//                    is = contentResolver.openInputStream(path);
//                    BitmapFactory.decodeStream(is, null, options);
//                } finally {
//                    Utils.closeQuietly(is);
//                }
//                options.inJustDecodeBounds = false;
//
//                if (options.inBitmap != null)
//                {
//
//                    // If the sizes don't fit - don't do this
//                    if (options.outWidth != options.inBitmap.getWidth() ||
//                            options.outHeight != options.inBitmap.getHeight()) {
//
//                        Log.d("decodingUdinicContent", "inBitmap is NO GO. " + getDetails(inBitmap, options));
//
//                        options.inBitmap = null;
//                        if (checkSampleSize)
//                            calculateInSampleSize(bitmapOptions);
//
//                    } else {
//                        options.inSampleSize = 1; // Needed for the inBitmap to work
//                        Log.d("decodingUdinicContent", "inBitmap is a GO. " + getDetails(inBitmap, options));
//                    }
//                }
//            }
//
//        }
//
//        InputStream is = null;
//        Bitmap ret = null;
//        try {
//            is = contentResolver.openInputStream(path);
//            ret = BitmapFactory.decodeStream(is, null, options);
//            Log.d("decodingUdinicContent", "After decode: " + getBmpDetails(ret));
//        } finally {
//            Utils.closeQuietly(is);
//        }
//
//        return ret;
//    }

    private Bitmap decodeContentStream(Uri path, PicassoBitmapOptions bitmapOptions) throws IOException {
        ContentResolver contentResolver = context.getContentResolver();

        if (bitmapOptions != null) {
            if (bitmapOptions.inBitmap != null || bitmapOptions.inJustDecodeBounds) {

                boolean checkSampleSize = bitmapOptions.inJustDecodeBounds;

                Bitmap inBitmap = options.inBitmap;
                options.inBitmap = null;

                options.inJustDecodeBounds = true;
                InputStream is = null;
                try {
                    is = contentResolver.openInputStream(path);
                    BitmapFactory.decodeStream(is, null, options);
                } finally {
                    Utils.closeQuietly(is);
                }
                options.inJustDecodeBounds = false;

                options.inBitmap = inBitmap;
                if (options.inBitmap != null) {

                    // If the sizes don't fit - don't do this
                    if (options.outWidth != options.inBitmap.getWidth() ||
                            options.outHeight != options.inBitmap.getHeight()) {

                        Log.d("decodingUdinicContent", "inBitmap is NO GO. " + getDetails(inBitmap, options));

                        options.inBitmap = null;
                        if (checkSampleSize)
                            calculateInSampleSize(bitmapOptions);

                    } else {
                        options.inSampleSize = 1; // Needed for the inBitmap to work
                        Log.d("decodingUdinicContent", "inBitmap is a GO. " + getDetails(inBitmap, options));
                    }
                }
            }

        }

        InputStream is = null;
        Bitmap ret = null;
        try {
            is = contentResolver.openInputStream(path);
            ret = BitmapFactory.decodeStream(is, null, options);
            Log.d("decodingUdinicContent", "After decode: " + getBmpDetails(ret));
        } finally {
            Utils.closeQuietly(is);
        }

        return ret;
    }


    private Bitmap decodeContentStream_old(Uri path, PicassoBitmapOptions options) throws IOException {
    ContentResolver contentResolver = context.getContentResolver();
    if (options != null && options.inJustDecodeBounds) {

        Bitmap inBitmap = options.inBitmap;
        options.inBitmap = null;

        InputStream is = null;
      try {
        is = contentResolver.openInputStream(path);
        BitmapFactory.decodeStream(is, null, options);
      } finally {
        Utils.closeQuietly(is);
      }
      calculateInSampleSize(options);

        options.inBitmap = inBitmap;

    }
    InputStream is = contentResolver.openInputStream(path);
    try {

        if (options != null && options.inBitmap != null)
            Log.d("udini2", "inBitmap[" + options.inBitmap.hashCode() + "] = h[" +
                    options.inBitmap.getHeight() + "] w[" + options.inBitmap.getWidth() + "]");

        Bitmap ret = BitmapFactory.decodeStream(is, null, options);
        Log.d("udini2", "RetBMP["+ret.hashCode()+"] h["+ret.getHeight()+"] w["+ret.getWidth()+"] isMutable["+ret.isMutable()+"] options = " + options);
        return ret;
    } finally {
      Utils.closeQuietly(is);
    }
  }
}
