package le1.mytube.services.musicService;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.exoplayer2.ui.SimpleExoPlayerView;

import le1.mytube.listeners.PlaybackStateListener;
import le1.mytube.mvpModel.database.song.YouTubeSong;

public interface ServiceRepo {

    void prepareStreaming(@NonNull YouTubeSong youTubeSong);

    void prepareLocal(@NonNull YouTubeSong youTubeSong);

    void play();

    void pause();

    void playOrPause();

    void stop();

    void duck();

    void seekTo(long position);

    void startService();

    void stopService();

    void addListener(PlaybackStateListener playbackStateCallback);

    void setView(SimpleExoPlayerView exoPlayerView);

    int getPlaybackState();

    @Nullable
    YouTubeSong getCurrentSong();

}
