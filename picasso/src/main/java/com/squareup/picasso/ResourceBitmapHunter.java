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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;

import static com.squareup.picasso.Picasso.LoadedFrom.DISK;

class ResourceBitmapHunter extends BitmapHunter {
  private final int resourceId;
  private final Context context;

  ResourceBitmapHunter(Context context, Picasso picasso, Dispatcher dispatcher, Cache cache,
      Request request) {
    super(picasso, dispatcher, cache, request);
    this.context = context;
    this.resourceId = request.resourceId;
  }

  @Override Bitmap decode(Uri uri, PicassoBitmapOptions options, int retryCount)
      throws IOException {
    return decodeResource(context.getResources(), resourceId, options);
  }

  @Override Picasso.LoadedFrom getLoadedFrom() {
    return DISK;
  }

  @Override String getName() {
    return Integer.toString(resourceId);
  }

  private Bitmap decodeResource(Resources resources, int resourceId,
      PicassoBitmapOptions bitmapOptions) {

      String res = resources.getResourceName(resourceId).split("/")[1];
      if (bitmapOptions == null)
          bitmapOptions = new PicassoBitmapOptions();

      if (bitmapOptions != null) {
        boolean checkSampleSize = bitmapOptions.inJustDecodeBounds;

        bitmapOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(resources, resourceId, bitmapOptions);
        bitmapOptions.inJustDecodeBounds = false;

        Bitmap inBitmap = cache.getUnusedBmp(bitmapOptions.outWidth, bitmapOptions.outHeight);

        if (inBitmap == null) {
//            Log.d("decodingUdinicResources", "res["+res+"] inBitmap was NOT found. " + getBmpDetails(inBitmap));
            if (checkSampleSize)
                calculateInSampleSize(bitmapOptions);
        } else {
//            Log.d("decodingUdinicResources", "res["+res+"] inBitmap was FOUND. " + getBmpDetails(inBitmap));
            bitmapOptions.inBitmap = inBitmap;
            bitmapOptions.inSampleSize = 1;
        }
      }

      bitmapOptions.inMutable = true;
      Bitmap ret =  BitmapFactory.decodeResource(resources, resourceId, bitmapOptions);
      if (bitmapOptions.inBitmap != null && ret.equals(bitmapOptions.inBitmap)) {
        Log.d("decodingUdinicResources", "res["+res+"] After decode: inBitmap USED " + getBmpDetails(ret));
        cache.reusedBitmap(ret);
      } else if (bitmapOptions.inBitmap != null && !ret.equals(bitmapOptions.inBitmap)) {
        Log.d("decodingUdinicResources", "res["+res+"] After decode: inBitmap FOUND but NOT used " + getBmpDetails(ret));
      } else {
        Log.d("decodingUdinicResources", "res["+res+"] After decode: inBitmap NOT USED " + getBmpDetails(ret));
      }
      return ret;
  }
    private Bitmap decodeResource_old(Resources resources, int resourceId,
      PicassoBitmapOptions bitmapOptions) {

      if (bitmapOptions != null) {
        if (bitmapOptions.inBitmap != null || bitmapOptions.inJustDecodeBounds) {

            boolean checkSampleSize = bitmapOptions.inJustDecodeBounds;

            Bitmap inBitmap = options.inBitmap;
            options.inBitmap = null;

            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(resources, resourceId, bitmapOptions);
            options.inJustDecodeBounds = false;

            options.inBitmap = inBitmap;
            if (options.inBitmap != null) {

                // If the sizes don't fit - don't do this
                if (options.outWidth != options.inBitmap.getWidth() ||
                        options.outHeight != options.inBitmap.getHeight()) {

                    Log.d("decodingUdinicResources", "inBitmap is NO GO. " + getDetails(inBitmap, options));

                    options.inBitmap = null;
                    if (checkSampleSize)
                        calculateInSampleSize(bitmapOptions);

                } else {
                    options.inSampleSize = 1; // Needed for the inBitmap to work
                    Log.d("decodingUdinicResources", "inBitmap is a GO. " + getDetails(inBitmap, options));
                }
            }
        }
      }

      Bitmap ret =  BitmapFactory.decodeResource(resources, resourceId, bitmapOptions);
      Log.d("decodingUdinicResources", "After decode: " + getBmpDetails(ret));
      return ret;
  }
}
