package com.star.photogallery;


import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import java.util.List;

public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "PhotoGalleryFragment";

    private static final int DEFAULT_COLUMN_NUM = 3;
    private static final int ITEM_WIDTH = 100;

    private RecyclerView mPhotoRecyclerView;
    private GridLayoutManager mGridLayoutManager;
    private List<GalleryItem> mGalleryItems;

    private ThumbnailDownloader<PhotoHolder> mPhotoHolderThumbnailDownloader;

    private int mCurrentPage = 1;
    private int mFetchedPage = 0;
    private int mCurrentPosition = 0;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        new FetchItemsTask().execute(mCurrentPage);

        Handler responseHandler = new Handler();

        mPhotoHolderThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mPhotoHolderThumbnailDownloader.setThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
                    @Override
                    public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap thumbnail) {
                        Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                        photoHolder.bindDrawable(drawable);
                    }
                }
        );

        mPhotoHolderThumbnailDownloader.start();
        mPhotoHolderThumbnailDownloader.getLooper();

        Log.i(TAG, "Background thread started");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mPhotoRecyclerView = (RecyclerView)
                view.findViewById(R.id.fragment_photo_gallery_recycler_view);

        mGridLayoutManager = new GridLayoutManager(getActivity(), DEFAULT_COLUMN_NUM);

        mPhotoRecyclerView.setLayoutManager(mGridLayoutManager);

        mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {

                        int spanCount = convertPxToDp(mPhotoRecyclerView.getWidth()) / ITEM_WIDTH;
                        mGridLayoutManager.setSpanCount(spanCount);
                    }
                });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mPhotoRecyclerView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                @Override
                public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                    updateCurrentPage();
                }

            });
        } else {
            mPhotoRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    updateCurrentPage();
                }
            });
        }

        setupAdapter();
        
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mPhotoHolderThumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPhotoHolderThumbnailDownloader.quit();

        Log.i(TAG, "Background thread destroyed");
    }

    private int convertPxToDp(float sizeInPx) {
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();

        return (int) (sizeInPx / displayMetrics.density);
    }

    private void updateCurrentPage() {
        int firstVisibleItemPosition = mGridLayoutManager.findFirstVisibleItemPosition();
        int lastVisibleItemPosition = mGridLayoutManager.findLastVisibleItemPosition();

        if (lastVisibleItemPosition == (mGridLayoutManager.getItemCount() - 1) &&
                mCurrentPage == mFetchedPage ) {
            mCurrentPosition = firstVisibleItemPosition + 3;
            mCurrentPage++;
            new FetchItemsTask().execute(mCurrentPage);
        }
    }

    private void setupAdapter() {
        if (isAdded()) {
            if (mGalleryItems != null) {
                mPhotoRecyclerView.setAdapter(new PhotoAdapter(mGalleryItems));
            } else {
                mPhotoRecyclerView.setAdapter(null);
            }
            mPhotoRecyclerView.scrollToPosition(mCurrentPosition);
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {

        private ImageView mItemImageView;

        public PhotoHolder(View itemView) {
            super(itemView);

            mItemImageView = (ImageView)
                    itemView.findViewById(R.id.fragment_photo_gallery_image_view);
        }

        public void bindDrawable(Drawable drawable) {
            mItemImageView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, parent, false);

            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            Drawable placeHolder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                placeHolder = getResources().getDrawable(R.drawable.emma, null);
            } else {
                placeHolder = getResources().getDrawable(R.drawable.emma);
            }
            photoHolder.bindDrawable(placeHolder);
            mPhotoHolderThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getUrl());
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    private class FetchItemsTask extends AsyncTask<Integer, Void, List<GalleryItem>> {

        @Override
        protected List<GalleryItem> doInBackground(Integer... params) {
            return new FlickrFetchr().fetchItems(params[0]);
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            if (mGalleryItems == null) {
                mGalleryItems = items;
            } else {
                if (items != null) {
                    mGalleryItems.addAll(items);
                }
            }

            mFetchedPage++;

            setupAdapter();
        }
    }

}
