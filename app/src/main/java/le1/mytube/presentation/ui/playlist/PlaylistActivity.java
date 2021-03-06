package le1.mytube.presentation.ui.playlist;

import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import le1.mytube.R;
import le1.mytube.data.database.playlist.Playlist;
import le1.mytube.data.database.youTubeSong.YouTubeSong;
import le1.mytube.presentation.adapters.PlaylistAdapter;


public class PlaylistActivity extends AppCompatActivity implements PlaylistContract.View, ListView.OnItemClickListener, ListView.OnItemLongClickListener{

    private BaseAdapter adapter;
    private PlaylistPresenter presenter;
    private ArrayList<YouTubeSong> songList = new ArrayList<>();
    private Playlist playlist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);

        playlist= new Playlist(0,getIntent().getStringExtra("TITLE"), "TODO"
                //getIntent().getStringExtra("PATH")
                , 0,0);

        Toolbar tb = findViewById(R.id.toolbarPlaylist);
        tb.setTitle(playlist.getName());
        tb.setTitleTextColor(Color.WHITE);
        setSupportActionBar(tb);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ListView listView = findViewById(R.id.playlistList);
        adapter = new PlaylistAdapter(this, songList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);


        presenter = ViewModelProviders.of(this).get(PlaylistPresenter.class);
        presenter.setContractView(this);
        presenter.loadSongsInPlaylist(getIntent().getStringExtra("TITLE"));

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        presenter.onListItemClick((YouTubeSong) adapter.getItem(position));
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
        presenter.onListLongItemClick(playlist,(YouTubeSong) adapter.getItem(position), position);
        return true;
    }

    @Override
    public void onSongLoaded(List<YouTubeSong> songsInPlaylist) {
        this.songList.addAll(songsInPlaylist);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onNoSongLoaded() {
        //TODO add nice view
        Toast.makeText(this, "Downloaded song will be saved here", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSongLoadingError() {
        Toast.makeText(this, "Something went wrong while loading songs", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showOfflineDeleteSongDialog(final YouTubeSong youTubeSong, final int position) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Remove \"" + youTubeSong.getTitle() + "\" ?");
        alertDialogBuilder.setMessage("You won't be able to listen to this song offline anymore. You can remove the song from the playlist without deleting it");

        alertDialogBuilder.setNeutralButton("Delete from playlist",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        presenter.removeSongFromPlaylist(playlist, youTubeSong);
                        songList.remove(position);
                        adapter.notifyDataSetChanged();
                    }
                });

        alertDialogBuilder.setPositiveButton("Delete",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        presenter.deleteSong(youTubeSong);
                        songList.remove(position);
                        adapter.notifyDataSetChanged();
                    }
                });


        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });


        alertDialogBuilder.show();
    }

    @Override
    public void showStandardDeleteSongDialog(final YouTubeSong youTubeSong, final int position) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Remove \"" + youTubeSong.getTitle() + "\" ?");
        alertDialogBuilder.setMessage("You won't be able to listen to this song offline anymore");
        alertDialogBuilder.setPositiveButton("Delete",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        presenter.deleteSong(youTubeSong);
                        songList.remove(position);
                        adapter.notifyDataSetChanged();
                    }
                });


        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });


        alertDialogBuilder.show();
    }
}
