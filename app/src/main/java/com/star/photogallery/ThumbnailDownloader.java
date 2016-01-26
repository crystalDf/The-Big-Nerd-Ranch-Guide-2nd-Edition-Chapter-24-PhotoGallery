package com.star.photogallery;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class ThumbnailDownloader<T> extends HandlerThread {

    private static final String TAG = "ThumbnailDownloader";

    private static final int MESSAGE_DOWNLOAD = 0;

    private Handler mRequestHandler;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();

    public ThumbnailDownloader() {
        super(TAG);
    }

    public void queueThumbnail(T target, String url) {
        Log.i(TAG, "Got a URL " + url);
    }
}
