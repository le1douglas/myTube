package le1.mytube.presentation.customViews;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import le1.mytube.R;
import le1.mytube.data.database.youTubeSong.YouTubeSong;
import le1.mytube.domain.application.MyTubeApplication;
import le1.mytube.domain.listeners.PlaybackStateListener;
import le1.mytube.domain.repos.MusicControl;

/**
 * A {@link View} that mimics the player overlay of youtube.
 * This view does not contain th player, it's just an overlay
 */
public class PlayerOverlayView extends RelativeLayout implements PlaybackStateListener, SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "LE1_PlayerOverlayView";


    private TextView titleView;
    private TextView currentTimeView;
    private TextView totalTimeView;
    private ProgressBar loadingIcon;
    private SeekBar seekbar;
    private ImageButton playPauseButton;
    private Button retryButton;

    private MusicControl musicControl;
    private boolean isUiVisible;

    final Handler autoHideHandler = new Handler();
    Runnable autoHideRunnable = new Runnable() {
        @Override
        public void run() {
            hideUi();
        }

    };
    private static final int autoHideMs = 2500;

    private SharedPreferences sharedPref;
    private static final String SHARED_PREF_NAME = "sharedPref_PlayerOverlayView";
    private static final String SHARED_PREF_IS_UI_VISIBLE_KEY = "sharedPref_isUiVisible";


    public PlayerOverlayView(Context context) {
        this(context, null, 0);
    }

    public PlayerOverlayView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Inflates the {@link R.layout#player_overlay} layout used for the ui.
     * Starts an {@link Handler} that updates the {@link #seekbar} every second.
     */
    public PlayerOverlayView(final Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        sharedPref = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);

        musicControl = ((MyTubeApplication) context.getApplicationContext()).getMusicControl();

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view = layoutInflater.inflate(R.layout.player_overlay, this);

        titleView = view.findViewById(R.id.title);
        loadingIcon = view.findViewById(R.id.loading_icon);
        currentTimeView = view.findViewById(R.id.current_time);
        totalTimeView = view.findViewById(R.id.total_time);
        seekbar = view.findViewById(R.id.seek_bar);
        retryButton = view.findViewById(R.id.retry_button);
        playPauseButton = view.findViewById(R.id.play_pause);

        seekbar.setOnSeekBarChangeListener(this);

        playPauseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (musicControl.getPlaybackState() == PlaybackStateCompat.STATE_STOPPED) {
                    retryButton.callOnClick();
                } else {
                    musicControl.playOrPause();
                }
            }
        });

        retryButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                musicControl.prepareAndPlay(musicControl.getCurrentSong());
            }
        });

        final Handler handler = new Handler();
        handler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if (isAttachedToWindow()) {
                            handler.postDelayed(this, 1000);
                            seekbar.setProgress(musicControl.getCurrentPosition());
                            currentTimeView.setText(formatMilliseconds(musicControl.getCurrentPosition()));
                        }
                    }
                }, 1000);
    }

    /**
     * Update and show the ui to reflect a {@link PlaybackStateCompat} state.
     * Automatically calls {@link #hideUi()} after {@link #autoHideMs} milliseconds
     *
     * @param playbackState one of {@link PlaybackStateCompat#STATE_BUFFERING}
     *                      {@link PlaybackStateCompat#STATE_PLAYING}
     *                      {@link PlaybackStateCompat#STATE_PAUSED}
     *                      {@link PlaybackStateCompat#STATE_STOPPED}
     *                      {@link PlaybackStateCompat#STATE_ERROR}
     * @param youTubeSong   the current {@link YouTubeSong} that encapsulates all the
     *                      metadata used to build the ui
     */
    public void updateUi(int playbackState, YouTubeSong youTubeSong) {
        isUiVisible = true;

        if (youTubeSong != null) {
            titleView.setText(youTubeSong.getTitle());
            seekbar.setMax(youTubeSong.getDuration());
            seekbar.setProgress(musicControl.getCurrentPosition());
            currentTimeView.setText(formatMilliseconds(musicControl.getCurrentPosition()));
            totalTimeView.setText(formatMilliseconds(youTubeSong.getDuration()));

        }
        switch (playbackState) {
            case PlaybackStateCompat.STATE_BUFFERING:
                playPauseButton.setVisibility(GONE);
                loadingIcon.setVisibility(VISIBLE);
                titleView.setVisibility(VISIBLE);
                seekbar.setVisibility(VISIBLE);
                currentTimeView.setVisibility(VISIBLE);
                totalTimeView.setVisibility(VISIBLE);
                retryButton.setVisibility(GONE);

                autoHideHandler.removeCallbacks(autoHideRunnable);
                if (titleView.getText().toString().equals("")) titleView.setText("Loading");
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                playPauseButton.setVisibility(VISIBLE);
                loadingIcon.setVisibility(GONE);
                titleView.setVisibility(VISIBLE);
                seekbar.setVisibility(VISIBLE);
                currentTimeView.setVisibility(VISIBLE);
                totalTimeView.setVisibility(VISIBLE);
                retryButton.setVisibility(GONE);

                // If the handler it's already started, stop it and restart it,
                // so that the runnable it's called only after the last call to this method
                autoHideHandler.removeCallbacks(autoHideRunnable);
                autoHideHandler.postDelayed(autoHideRunnable, autoHideMs);

                playPauseButton.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.exo_controls_pause));
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                playPauseButton.setVisibility(VISIBLE);
                loadingIcon.setVisibility(GONE);
                titleView.setVisibility(VISIBLE);
                seekbar.setVisibility(VISIBLE);
                currentTimeView.setVisibility(VISIBLE);
                totalTimeView.setVisibility(VISIBLE);
                retryButton.setVisibility(GONE);

                autoHideHandler.removeCallbacks(autoHideRunnable);
                playPauseButton.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.exo_controls_play));

                break;
            case PlaybackStateCompat.STATE_STOPPED:
                playPauseButton.setVisibility(VISIBLE);
                loadingIcon.setVisibility(GONE);
                titleView.setVisibility(GONE);
                seekbar.setVisibility(GONE);
                currentTimeView.setVisibility(GONE);
                totalTimeView.setVisibility(GONE);
                retryButton.setVisibility(GONE);

                autoHideHandler.removeCallbacks(autoHideRunnable);
                playPauseButton.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.exo_controls_play));
                break;
            case PlaybackStateCompat.STATE_ERROR:
                playPauseButton.setVisibility(GONE);
                loadingIcon.setVisibility(GONE);
                titleView.setVisibility(VISIBLE);
                seekbar.setVisibility(GONE);
                currentTimeView.setVisibility(GONE);
                totalTimeView.setVisibility(GONE);
                retryButton.setVisibility(VISIBLE);

                autoHideHandler.removeCallbacks(autoHideRunnable);
                titleView.setText("Error");
                break;
            default:
                break;

        }
    }

    /**
     * Hides all the ui controls for a more immersive experience.
     * Ui can be made visible again with {@link #updateUi(int, YouTubeSong)}
     */
    public void hideUi() {
        isUiVisible = false;
        playPauseButton.setVisibility(GONE);
        loadingIcon.setVisibility(GONE);
        titleView.setVisibility(GONE);
        seekbar.setVisibility(GONE);
        currentTimeView.setVisibility(GONE);
        totalTimeView.setVisibility(GONE);
        retryButton.setVisibility(GONE);
    }


    /**
     * Restores the {@link #seekbar} and the correct view state
     * as soon as this view is visible.
     *
     * @see View#onAttachedToWindow()
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        musicControl.addListener(this);

        isUiVisible = sharedPref.getBoolean(SHARED_PREF_IS_UI_VISIBLE_KEY, true);
        if (musicControl.isConnected()) {
            if (isUiVisible)
                updateUi(musicControl.getPlaybackState(), musicControl.getCurrentSong());
            else hideUi();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        musicControl.removeListener(this);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(SHARED_PREF_IS_UI_VISIBLE_KEY, isUiVisible);
        editor.commit();
    }

    /**
     * Called every time the user touches the view and no other sub-view catches the
     * {@link MotionEvent} (for example, if a button is touched, this method is not called).
     * Toggles the visibility of the ui
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (musicControl.getPlaybackState() == PlaybackStateCompat.STATE_PLAYING) {
            if (isUiVisible) hideUi();
            else updateUi(musicControl.getPlaybackState(), musicControl.getCurrentSong());
        }
        return super.onTouchEvent(event);
    }

    /**
     * Converts milliseconds to a human readable string formatted as mm:ss
     *
     * @param time the number of milliseconds to convert
     * @return a human readable timestamp
     */
    private String formatMilliseconds(int time) {
        time = time / 1000;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return String.format(this.getResources().getConfiguration().getLocales().get(0),
                    "%02d:%02d", time / 60, time % 60);
        } else {
            //noinspection deprecation
            return String.format(this.getResources().getConfiguration().locale,
                    "%02d:%02d", time / 60, time % 60);
        }
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    /**
     * Calls {@link MusicControl#seekTo(int)} only when user releases the finger
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Log.d(TAG, "onStopTrackingTouch: " + seekbar.getProgress());
        musicControl.seekTo(seekbar.getProgress());
    }


    /**
     * @see PlaybackStateListener#onMetadataLoaded(YouTubeSong)
     */
    @Override
    public void onMetadataLoaded(YouTubeSong youTubeSong) {
        updateUi(musicControl.getPlaybackState(), youTubeSong);
    }

    /**
     * For every playback action,
     * calls {@link #updateUi(int, YouTubeSong)} with the appropriate {@link PlaybackStateCompat} state.
     */
    @Override
    public void onLoading() {
        updateUi(PlaybackStateCompat.STATE_BUFFERING, musicControl.getCurrentSong());
        Log.d(TAG, "onLoading:");
    }

    @Override
    public void onPaused() {
        updateUi(PlaybackStateCompat.STATE_PAUSED, musicControl.getCurrentSong());
        Log.d(TAG, "onPaused: ");
    }

    @Override
    public void onPlaying() {
        updateUi(PlaybackStateCompat.STATE_PLAYING, musicControl.getCurrentSong());
        Log.d(TAG, "onPlaying: ");
    }

    @Override
    public void onStopped() {
        updateUi(PlaybackStateCompat.STATE_STOPPED, musicControl.getCurrentSong());
        Log.d(TAG, "onStopped: ");
    }

    @Override
    public void onError(String error) {
        updateUi(PlaybackStateCompat.STATE_ERROR, musicControl.getCurrentSong());
        Log.d(TAG, "onError: ");
    }

}

