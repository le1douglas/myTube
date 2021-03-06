package le1.mytube.domain.repos;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import le1.mytube.data.database.Database;
import le1.mytube.data.database.playlist.Playlist;
import le1.mytube.data.database.playlist.PlaylistDatabase;
import le1.mytube.data.database.playlist.PlaylistDatabaseImpl;
import le1.mytube.data.database.youTubeSong.YouTubeSong;
import le1.mytube.domain.listeners.OnExecuteTaskCallback;

public class Repo {

    private Database database;
    private PlaylistDatabase playlistDatabase;
    private Context context;
    private AutocompleteTask autocompleteTask;
    private OnExecuteTaskCallback onExecuteTaskCallback;

    public Repo(Context context) {
        this.context = context;
        database = Database.getDatabase(this.context);
        playlistDatabase = PlaylistDatabaseImpl.getDatabase(this.context);
    }

    //--------SONG DATABASE
    public void onDestroy() {
        //database.close();
        context = null;
    }


    public void addSongs(YouTubeSong... youTubeSongs) {
        database.youTubeSongDao().addSongs(youTubeSongs);
    }

    public void updateSong(YouTubeSong... youTubeSongs) {
        database.youTubeSongDao().updateSong(youTubeSongs);
    }

    public void deleteSong(YouTubeSong youTubeSong) {
        database.youTubeSongDao().deleteSong(youTubeSong);
    }

    public void deleteAllSongs() {
        database.youTubeSongDao().deleteAllSongs();
    }

    public YouTubeSong getSongById(String id) {
        return database.youTubeSongDao().getSongById(id);
    }

    public List<YouTubeSong> getAllSongs() {
        return database.youTubeSongDao().getAllSongs();
    }


    public List<Playlist> getAllPlaylists() {
        return playlistDatabase.getAllPlaylists();
    }

    public List<String> getAllPlaylistsName() {
        return playlistDatabase.getAllPlaylistsName();
    }

    public Playlist getPlaylistByName(String name) {
        return playlistDatabase.getPlaylistByName(name);
    }

    public int getAllPlaylistCount() {
        return playlistDatabase.getAllPlaylistCount();
    }

    public void addPlaylist(String... names) {
        playlistDatabase.addPlaylist(names);
    }

    public void deletePlaylist(String... names) {
        playlistDatabase.deletePlaylist(names);
    }

    public void deleteAllPlaylists() {
        playlistDatabase.deleteAllPlaylist();
    }

    public ArrayList<YouTubeSong> getSongsInPlaylist(String playlistName) {
        return playlistDatabase.getAllSongInPlaylist(playlistName);
    }

    
    //TASKS

    public void loadAutocompleteSuggestions(String query, OnExecuteTaskCallback onExecuteTaskCallback) {
        this.onExecuteTaskCallback = onExecuteTaskCallback;
        if (autocompleteTask != null) autocompleteTask.cancel(true);
        autocompleteTask = (AutocompleteTask) new AutocompleteTask().execute(query);

    }

    public void loadYouTubeSearchResult(String query, OnExecuteTaskCallback onExecuteTaskCallback) {
        this.onExecuteTaskCallback = onExecuteTaskCallback;
        new SearchTask().execute(query);
    }


    private class AutocompleteTask extends AsyncTask<String, Void, String> {
        URL url;

        @Override
        protected void onPreExecute() {
            onExecuteTaskCallback.onBeforeTask();
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            onExecuteTaskCallback.onDuringTask();
            Thread.currentThread().setName("le1.mytube.AutocompleteTask");
            try {
                if (!params[0].trim().equals("")) {
                    url = new URL("http://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q=" + Uri.encode(params[0]));
                    String JSON_string;
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                    InputStream inputStream = httpURLConnection.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder stringBuilder = new StringBuilder();
                    while ((JSON_string = bufferedReader.readLine()) != null) {
                        stringBuilder.append(JSON_string).append("\r\n");
                    }

                    bufferedReader.close();
                    inputStream.close();
                    httpURLConnection.disconnect();
                    return stringBuilder.toString().trim();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;

        }

        @Override
        protected void onPostExecute(String result) {
            onExecuteTaskCallback.onAfterTask(result);
        }


    }

    private class SearchTask extends AsyncTask<String, String, String> {

        final static int maxResults = 20;

        @Override
        protected void onPreExecute() {
            onExecuteTaskCallback.onBeforeTask();

            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            onExecuteTaskCallback.onDuringTask();
            Thread.currentThread().setName("le1.mytube.SearchTask");
            HttpURLConnection urlConnection;

            URL url;
            try {

                final String encodedURL = URLEncoder.encode(params[0], "UTF-8");
                url = new URL("https://www.googleapis.com/youtube/v3/search?part=snippet&maxResults=" + maxResults + "&q=" + encodedURL + "&type=video&key=AIzaSyCwH2GDglQOh4CKMPU8LIc8Gu9jxGUCj2w");


                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000 /* milliseconds */);
                urlConnection.setConnectTimeout(15000 /* milliseconds */);
                urlConnection.setDoOutput(true);
                urlConnection.connect();

                BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));


                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                    sb.append("\n");
                }
                br.close();
                return sb.toString();
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            } catch (ProtocolException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }


        }

        @Override
        protected void onPostExecute(String result) {
            onExecuteTaskCallback.onAfterTask(result);

        }
    }
}
