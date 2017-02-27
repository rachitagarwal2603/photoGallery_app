package com.example.rachit.photogallery;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.ProgressBar;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rachit on 9/25/2016.
 */
public class PhotoGalleryFragment extends VisibleFragment {

    public static final String TAG = "PhotoGalleryFragment";
    private RecyclerView mRecyclerView;
    private ProgressBar mProgressView, mProgressViewEnd;
    private List<GalleryItem> items = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    static boolean loading = true;
    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        new FetchItemsTask().execute();
        updateItems();

//        Intent i= PollService.newIntent(getActivity());
//        getActivity().startService(i);
//        PollService.setServiceAlarm(getActivity(), true);

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadlistener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder target, Bitmap bitmap) {
                Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                target.bindDrawable(drawable);
            }
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        //Log.i(TAG, "Background Thread Started");

        boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
        PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mProgressView = (ProgressBar) v.findViewById(R.id.progress_bar);
        mProgressViewEnd = (ProgressBar) v.findViewById(R.id.progress_bar_end);
        mRecyclerView = (RecyclerView) v.findViewById(R.id.fragment_photo_gallery_recycler_view);
        final GridLayoutManager mLayoutManager = new GridLayoutManager(getActivity(), 3);
        mRecyclerView.setLayoutManager(mLayoutManager);

        final int[] pastVisiblesItems = new int[1];
        final int[] visibleItemCount = new int[1];
        final int[] totalItemCount = new int[1];
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) //check for scroll down
                {
                    visibleItemCount[0] = mLayoutManager.getChildCount();
                    totalItemCount[0] = mLayoutManager.getItemCount();
                    pastVisiblesItems[0] = mLayoutManager.findFirstVisibleItemPosition();

                    if (loading) {
                        if ((visibleItemCount[0] + pastVisiblesItems[0]) >= totalItemCount[0]) {
                            loading = false;
                            //Log.v(TAG, "Last Item Wow !");

                            //Do pagination.. i.e. fetch new data
                            //LinearLayout.LayoutParams params= new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 560);
                            //view.setLayoutParams(params);
                            showProgress(true, true);
                            //mThumbnailDownloader.clearQueue();
                            FlickrFetchr.pageNumber++;
                            new FetchItemsTask().execute();
                        }
                    }
                }
            }
        });
        showProgress(true, false);
        setAdapter();
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        //Log.i(TAG, "Background Thread destroyed");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);

        final MenuItem searchItem= menu.findItem(R.id.menu_item_search);
        final SearchView searchView= (SearchView) searchItem.getActionView();
        searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();                    // Close keyboard on pressing IME_ACTION_SEARCH option
                Log.d("pppp", "QueryTextSubmit : "+ query);
                FlickrFetchr.pageNumber=1;
                QueryPreferences.setStoredQuery(getActivity(), query);
                updateItems();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d("pppp", "QueryTextChange: "+ newText);
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("pppp", "On search Click listener is called");
                String query= QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query, false);
            }
        });

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if (PollService.isServiceAlarmOn(getActivity())) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                updateItems();
                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
                getActivity().invalidateOptionsMenu(); //This update toolbar options menu
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateItems() {
        String query= QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(query).execute();
    }

    private void setAdapter() {
        PhotoAdapter mAdapter= new PhotoAdapter(items);
        if (isAdded() && FlickrFetchr.pageNumber==1)
            mRecyclerView.setAdapter(mAdapter);
        else if(isAdded()) {
            mAdapter.notifyItemInserted(items.size());
            mAdapter.notifyDataSetChanged();
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private ImageView mImageView;
        private GalleryItem mGalleryItem;

        public PhotoHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.fragment_photo_gallery_imageview);
            itemView.setOnClickListener(this);
        }

        public void bindDrawable(Drawable drawable) {
            mImageView.setImageDrawable(drawable);
        }

        public void bindGalleryItem(GalleryItem galleryItem) {
            mGalleryItem = galleryItem;
        }

        @Override
        public void onClick(View view) {
            Intent i = PhotoPageActivity.newIntent(getActivity(), mGalleryItem.getPhotoPageUri());
            startActivity(i);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter {

        private List<GalleryItem> mGalleryItems;
        public PhotoAdapter(List<GalleryItem> mGalleryItems) {
            this.mGalleryItems = mGalleryItems;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View v = inflater.inflate(R.layout.gallery_item, parent, false);
            return new PhotoHolder(v);

        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            PhotoHolder holder1 = (PhotoHolder) holder;
            GalleryItem galleryItem = mGalleryItems.get(position);
            holder1.bindGalleryItem(galleryItem);
            Drawable placeHolder = ResourcesCompat.getDrawable(getResources(), R.drawable.bill_up_close, null);
            holder1.bindDrawable(placeHolder);
            mThumbnailDownloader.queueThumbnail(holder1, galleryItem.getUrl());
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    public class FetchItemsTask extends AsyncTask<Void, Integer, List<GalleryItem>> {
        private String mQuery;

        public FetchItemsTask(String query){
            mQuery= query;
        }

        public FetchItemsTask(){  }

        @Override
        protected List<GalleryItem> doInBackground(Void... voids) {
            if(mQuery==null)
                return new FlickrFetchr().fetchRecentPhotos();
            else
                return new FlickrFetchr().searchPhotos(mQuery);
        }

        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
            showProgress(false, false);
            showProgress(false, true);
            loading = true;
            if (FlickrFetchr.pageNumber == 1) {
                items = galleryItems;
                setAdapter();
            } else {
                items.addAll(galleryItems);
                setAdapter();
            }
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show, boolean end) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (!end) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            /*mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });*/

                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                mProgressView.animate().setDuration(shortAnimTime).alpha(
                        show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                    }
                });
            } else {
                // The ViewPropertyAnimator APIs are not available, so simply show
                // and hide the relevant UI components.
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                //mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
                mProgressViewEnd.setVisibility(show ? View.VISIBLE : View.GONE);
                mProgressViewEnd.animate().setDuration(shortAnimTime).alpha(
                        show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mProgressViewEnd.setVisibility(show ? View.VISIBLE : View.GONE);
                    }
                });
            } else {
                mProgressViewEnd.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        }
    }
}
