/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.graphics.Bitmap;
import android.util.Log;

import java.util.*;

/** A memory cache which uses a least-recently used eviction policy. */
public class LruCache implements Cache {
  final LinkedHashMap<String, Bitmap> map;
  final LinkedHashMap<Bitmap, Integer> bitmapsUsage;
  private final int maxSize;

  private int size;
  private int putCount;
  private int evictionCount;
  private int hitCount;
  private int missCount;

  /** Create a cache using an appropriate portion of the available RAM as the maximum size. */
  public LruCache(Context context) {
    this(Utils.calculateMemoryCacheSize(context));
  }

  /** Create a cache with a given maximum size in bytes. */
  public LruCache(int maxSize) {
    if (maxSize <= 0) {
      throw new IllegalArgumentException("Max size must be positive.");
    }
    this.maxSize = maxSize;
    this.map = new LinkedHashMap<String, Bitmap>(0, 0.75f, true);
    this.bitmapsUsage = new LinkedHashMap<Bitmap, Integer>();
  }

  @Override public Bitmap get(String key) {
    if (key == null) {
      throw new NullPointerException("key == null");
    }

    Bitmap mapValue;
    synchronized (this) {
      mapValue = map.get(key);

      if (mapValue != null) {
          increaseRefCount(mapValue);
          hitCount++;
          return mapValue;
      }
      missCount++;
    }

    return null;
  }

    @Override public synchronized void reusedBitmap(Bitmap bmp) {
//        bitmapsUsage.put(bmp, 1);
        // This bmp will be added later with the new key it belongs to
        for (Map.Entry<String, Bitmap> entry : map.entrySet()) {
            if (entry.getValue().equals(bmp)) {
                Log.d("decodingUdinicResources", "Removed key["+entry.getKey().hashCode()+"] for bmp["+entry.getValue().hashCode()+"]");
                map.remove(entry.getKey());
                size -= Utils.getBitmapBytes(entry.getValue());
                break;
            }
        }
    }

    @Override public synchronized void increaseRefCount(Bitmap bmp) {
        Integer currRefCount = bitmapsUsage.get(bmp);
        Integer newVal = currRefCount == null ? 1 : currRefCount+1;
        Log.v("decodingUdinicResources", "increaseRefCount[" + bmp.hashCode() + "] [" + currRefCount + "]->[" + newVal + "]");
        bitmapsUsage.put(bmp, newVal);
    }
    @Override public synchronized void decreaseRefCount(Bitmap bmp) {
        Integer currRefCount = bitmapsUsage.get(bmp);
        Integer newVal = currRefCount == null || currRefCount == 0?0: currRefCount-1;
        Log.v("decodingUdinicResources", "decreaseRefCount[" + bmp.hashCode() + "] [" + currRefCount + "]->[" + newVal + "]");
        bitmapsUsage.put(bmp, newVal);
    }

  @Override public void set(String key, Bitmap bitmap) {
    if (key == null || bitmap == null) {
      throw new NullPointerException("key == null || bitmap == null");
    }

    Bitmap previous;
    synchronized (this) {
      putCount++;
      size += Utils.getBitmapBytes(bitmap);
        Log.d("decodingUdinicResources", "Added key["+key.hashCode()+"] for bmp["+bitmap.hashCode()+"]");

        previous = map.put(key, bitmap);
//        increaseRefCount(bitmap);

//TODO do something about previous, maybe remove from ref count?

        if (previous != null) {
        size -= Utils.getBitmapBytes(previous);
      }
    }

    trimToSize(maxSize);
  }

  private void trimToSize(int maxSize) {
    while (true) {
      String key;
      Bitmap value;
      synchronized (this) {
        if (size < 0 || (map.isEmpty() && size != 0)) {
          throw new IllegalStateException(
              getClass().getName() + ".sizeOf() is reporting inconsistent results!");
        }

        if (size <= maxSize || map.isEmpty()) {
          break;
        }

        Map.Entry<String, Bitmap> toEvict = map.entrySet().iterator().next();
        key = toEvict.getKey();
        value = toEvict.getValue();
        map.remove(key);
        bitmapsUsage.remove(value);
        size -= Utils.getBitmapBytes(value);
        evictionCount++;
      }
    }
  }

    private void printBitmapRefCount() {
        Set<Map.Entry<Bitmap, Integer>> entries = bitmapsUsage.entrySet();
        StringBuilder sd = new StringBuilder("");
        for (Map.Entry<Bitmap, Integer> entry : entries) {
            sd.append("bmp["+entry.getKey().hashCode()+"] refs["+entry.getValue()+"]\n");
        }
        Log.d("LruCache", "printBitmapRefCount\n" + sd.toString());
    }

    @Override
    public Bitmap getUnusedBmp(int w, int h) {
        Set<Map.Entry<Bitmap, Integer>> entries = bitmapsUsage.entrySet();
        printBitmapRefCount();

        for (Map.Entry<Bitmap, Integer> entry : entries) {
            if (entry.getValue().intValue() == 0 &&
                entry.getKey().getWidth() == w && entry.getKey().getHeight() == h)
                return entry.getKey();
        }

        return null;
    }

//    @Override
//    public Bitmap getLeastUsedBitmap() {
//        if (map.size() < 5)
//            return null;
//
//        Map.Entry<String, Bitmap> victim = map.entrySet().iterator().next();
//        Bitmap bmp = victim.getValue();
//        map.remove(victim.getKey());
//        size -= Utils.getBitmapBytes(bmp);
//        evictionCount++;
//
//        return bmp;
//    }

  /** Clear the cache. */
  public final void evictAll() {
    trimToSize(-1); // -1 will evict 0-sized elements
  }

  /** Returns the sum of the sizes of the entries in this cache. */
  public final synchronized int size() {
    return size;
  }

  /** Returns the maximum sum of the sizes of the entries in this cache. */
  public final synchronized int maxSize() {
    return maxSize;
  }


    public final synchronized void clear() {
    evictAll();
  }

  /** Returns the number of times {@link #get} returned a value. */
  public final synchronized int hitCount() {
    return hitCount;
  }

  /** Returns the number of times {@link #get} returned {@code null}. */
  public final synchronized int missCount() {
    return missCount;
  }

  /** Returns the number of times {@link #set(String, Bitmap)} was called. */
  public final synchronized int putCount() {
    return putCount;
  }

  /** Returns the number of values that have been evicted. */
  public final synchronized int evictionCount() {
    return evictionCount;
  }
}
