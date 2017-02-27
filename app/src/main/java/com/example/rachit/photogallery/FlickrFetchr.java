package com.example.rachit.photogallery;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rachit on 9/25/2016.
 */
public class FlickrFetchr {

    private static final String TAG="FlickrFetchr";
    private static final String API_KEY="7eb0700ab13ab5dc44a019c90c643e53";
    private static final String FETCH_RECENTS_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    public static int pageNumber=1;
    private static final Uri ENDPOINT = Uri
            .parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build();

    // int total=0, lengthOfFile=100;

    public byte[] getUrlBytes(String urlSpec) throws IOException{
        URL url= new URL(urlSpec);
        HttpURLConnection connection= (HttpURLConnection) url.openConnection();
        try{
            ByteArrayOutputStream out= new ByteArrayOutputStream();
            InputStream in= connection.getInputStream();

            if(connection.getResponseCode()!= HttpURLConnection.HTTP_OK){
                throw new IOException(connection.getResponseMessage()+": with "+urlSpec);
            }

           //lengthOfFile= connection.getContentLength();

            int bytesRead=0;
            byte[] buffer=new byte[1024];
            while((bytesRead=in.read(buffer))>0){
           //total+=bytesRead;
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        }finally{
            connection.disconnect();
        }
    }

    public String getUrlString(String urlSpec) throws IOException{
        return new String(getUrlBytes(urlSpec));
    }

    public List<GalleryItem> fetchRecentPhotos(){
        String url = buildUrl(FETCH_RECENTS_METHOD, null);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotos(String query) {
        String url = buildUrl(SEARCH_METHOD, query);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> downloadGalleryItems(String url){

        List<GalleryItem> galleryItems= new ArrayList<>();
        Log.i(TAG, "Page number is : "+pageNumber);
        try{
            /*String url= Uri.parse("https://api.flickr.com/services/rest/")
                    .buildUpon()
                    .appendQueryParameter("method", "flickr.photos.getRecent")
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    .appendQueryParameter("page", String.valueOf(pageNumber))
                    .appendQueryParameter("extras", "url_s")
                    .build().toString();*/
            String jsonString= getUrlString(url);
            Log.i(TAG, "Received Json : " + jsonString);
            JSONObject jsonObject= new JSONObject(jsonString);
            parseItems(galleryItems, jsonObject);
        } catch (IOException e) {
            Log.e(TAG, "Failed to fetch Items", e);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse Json", e);
        }
        return galleryItems;
    }

    private String buildUrl(String method, String query){
        Uri.Builder uriBuilder= ENDPOINT.buildUpon()
                .appendQueryParameter("method", method)
                .appendQueryParameter("page", String.valueOf(pageNumber));

        if(method.equals(SEARCH_METHOD)){
            uriBuilder.appendQueryParameter("text", query);
        }
        return uriBuilder.build().toString();
    }

    private void parseItems(List<GalleryItem> items, JSONObject jsonObject) throws IOException, JSONException{
        JSONObject photosJsonObject= jsonObject.getJSONObject("photos");
        JSONArray photoJsonArray= photosJsonObject.getJSONArray("photo");

        for(int i=0;i<photoJsonArray.length();i++){
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);

            GalleryItem galleryItem= new GalleryItem();
            galleryItem.setId(photoJsonObject.getString("id"));
            galleryItem.setCaption(photoJsonObject.getString("title"));

            if (!photoJsonObject.has("url_s")) {
                continue;
            }

            galleryItem.setUrl(photoJsonObject.getString("url_s"));
            galleryItem.setOwner(photoJsonObject.getString("owner"));
            items.add(galleryItem);
        }
    }
}
