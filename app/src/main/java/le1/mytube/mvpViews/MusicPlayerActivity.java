package le1.mytube.mvpViews;

import android.arch.lifecycle.LifecycleActivity;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.ViewModelProviders;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.SeekBar;

import com.google.android.exoplayer2.ui.SimpleExoPlayerView;

import java.util.List;

import le1.mytube.PlayerOverlayView;
import le1.mytube.R;
import le1.mytube.listeners.MusicPlayerCallback;
import le1.mytube.mvpModel.database.song.YouTubeSong;
import le1.mytube.mvpPresenters.MusicPlayerPresenter;


public class MusicPlayerActivity extends LifecycleActivity implements SeekBar.OnSeekBarChangeListener, MusicPlayerCallback, LifecycleOwner, AdapterView.OnItemSelectedListener {
    private static final String TAG = ("LE1_" + MusicPlayerActivity.class.getSimpleName());
    SimpleExoPlayerView playerView;
    PlayerOverlayView overlay;
    MusicPlayerPresenter presenter;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        makeFullScreenIfLandscape();
        setContentView(R.layout.activity_music_player);

        playerView = findViewById(R.id.exo_player);
        overlay = findViewById(R.id.overlay);



        presenter = ViewModelProviders.of(this).get(MusicPlayerPresenter.class);
        presenter.setListener(this);
        presenter.linkPlayerToView(playerView);
        getLifecycle().addObserver(presenter);


        overlay.setSpinnerOnItemSelected(this);
        overlay.setOnSeekBarChangeListener(this);
        overlay.setButtonOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.playOrPause();

            }
        });

        //activity just created, instead of rotated
        if (savedInstanceState == null) {
            presenter.startSongIfNecessary(getIntent(), getCallingActivity());
        }
    }

    private void makeFullScreenIfLandscape() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        presenter.seekTo(seekBar.getProgress());
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onUpdateSeekBar(int position) {
        overlay.setProgress(position);
    }

    @Override
    public void onInitializeUi(@NonNull List<YouTubeSong> youTubeSongs) {
        overlay.setTitle(youTubeSongs.get(0).getTitle());
        overlay.setMaxProgress(youTubeSongs.get(0).getDuration());
        overlay.setSpinnerContent(youTubeSongs);
    }

    @Override
    public void onCloseActivity() {
        this.finish();
    }



    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}