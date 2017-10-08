package le1.mytube.services.musicService;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;
import le1.mytube.R;
import le1.mytube.listeners.PlaybackStateCallback;
import le1.mytube.mvpModel.database.song.YouTubeSong;

public class ServiceRepoImpl implements ServiceRepo {
    private static final String TAG = ("LE1_" + ServiceRepoImpl.class.getSimpleName());

    private MediaBrowserCompat mediaBrowser;
    private MusicService service;
    private Context context;
    private SimpleExoPlayer player;

    private YouTubeSong currentSong;
    private PlaybackStateCallback playbackStateCallback;
    private boolean postProgressUpdate = false;

    private class ConnectionCallback extends MediaBrowserCompat.ConnectionCallback {
        @Override
        public void onConnected() {
            super.onConnected();
            Log.d(TAG, "onConnected");
            init();
        }

        @Override
        public void onConnectionFailed() {
            super.onConnectionFailed();
            Log.e(TAG, "onConnectionFailed");
        }

        @Override
        public void onConnectionSuspended() {
            super.onConnectionSuspended();
            Log.e(TAG, "onConnectionSuspended");
        }
    }

    public ServiceRepoImpl(@NonNull Application application) {
        context = application;
        startService(application);
    }

    private void init() {
        service = new MusicService();
        player = ExoPlayerFactory.newSimpleInstance(context, new DefaultTrackSelector(new AdaptiveTrackSelection.Factory(new DefaultBandwidthMeter())));
        player.addListener(new PlayerListener());

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(this, 1000);
                if (postProgressUpdate)
                    playbackStateCallback.onPositionChanged(player.getCurrentPosition());
            }
        }, 1000);
    }


    private void startService(@NonNull Application application) {
        application.startService(new Intent(application, MusicService.class));
        mediaBrowser = new MediaBrowserCompat(application, new ComponentName(application, MusicService.class), new ConnectionCallback(), null);
        mediaBrowser.connect();
    }


    private void stopService() {
        player.release();
        context.stopService(new Intent(context.getApplicationContext(), MusicService.class));
        this.service = null;
        mediaBrowser.disconnect();
    }

    @Override
    public void prepareStreaming(@NonNull YouTubeSong youTubeSong) {
        Log.d(TAG, "prepareStreaming");
        currentSong = youTubeSong;
        service.setPlaybackState(PlaybackStateCompat.STATE_BUFFERING, player.getCurrentPosition());
        new YouTubeExtractor(context) {
            @Override
            protected void onExtractionComplete(SparseArray<YtFile> sparseArray, VideoMeta videoMeta) {
                Log.d(TAG, "onExtractionComplete");
                Uri audioUri = Uri.parse((sparseArray.get(140)).getUrl());
                //TODO dynamically get available i tags
                Uri videoUri = Uri.parse((sparseArray.get(134)).getUrl());
                String userAgent = Util.getUserAgent(context, context.getString(R.string.app_name));

                DefaultExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
                DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, userAgent);
                MediaSource audioSource = new ExtractorMediaSource(audioUri, dataSourceFactory, extractorsFactory, null, null);
                MediaSource videoSource = new ExtractorMediaSource(videoUri, dataSourceFactory, extractorsFactory, null, null);
                MediaSource compositeSource = new MergingMediaSource(audioSource, videoSource);
                player.prepare(compositeSource);
                service.setMetadata(currentSong, playbackStateCallback);
                play();
            }
        }.extract("http://youtube.com/watch?v=" + currentSong.getId(), false, false);
    }

    @Override
    public void prepareLocal(@NonNull YouTubeSong youTubeSong) {

    }


    @Override
    public void play() {
        Log.d(TAG, "play");
        if (service.requestAudioFocus()) {
            service.setPlaybackState(PlaybackStateCompat.STATE_PLAYING, player.getCurrentPosition());
            Log.d(TAG, "requestAudioFocus = true");
            player.setVolume(1.0f);
            player.setPlayWhenReady(true);
            service.setMediaSessionActive(true);
            service.setConnectedToNoisyReceiver(true);
            postProgressUpdate = true;
        } else {
            Toast.makeText(this.context, "audio focus not granted", Toast.LENGTH_SHORT).show();
            pause();

        }

    }

    @Override
    public void pause() {
        Log.d(TAG, "pause");
        service.setPlaybackState(PlaybackStateCompat.STATE_PAUSED, player.getCurrentPosition());
        player.setPlayWhenReady(false);
        service.setConnectedToNoisyReceiver(false);
        postProgressUpdate = false;
    }

    @Override
    public void playOrPause() {
        Log.d(TAG, "playOrPause");
        if (service.getPlaybackState() == PlaybackStateCompat.STATE_PLAYING)
            pause();
        else play();
    }

    @Override
    public void stop() {
        Log.d(TAG, "stop");
        player.setPlayWhenReady(false);
        service.setConnectedToNoisyReceiver(false);
        postProgressUpdate = false;
        currentSong = null;
        service.setPlaybackState(PlaybackStateCompat.STATE_STOPPED, player.getCurrentPosition());
        player.stop();
        service.setMediaSessionActive(false);
        service.abandonAudioFocus();
        playbackStateCallback.onStopped();
        stopService();
    }

    @Override
    public void duck() {
        Log.d(TAG, "duck");
        player.setVolume(0.5f);
    }

    @Override
    public void seekTo(long position) {
        Log.d(TAG, "seekTo");
        player.seekTo(position);
    }

    @Override
    public void setCallback(PlaybackStateCallback playbackStateCallback) {
        this.playbackStateCallback = playbackStateCallback;
    }

    @Override
    public void setView(SimpleExoPlayerView exoPlayerView) {
        exoPlayerView.setPlayer(player);

    }


    private void setMediaController(Activity activity) {
        try {
            MediaControllerCompat controller = new MediaControllerCompat(activity, mediaBrowser.getSessionToken());
            MediaControllerCompat.setMediaController(
                    activity, controller);
        } catch (RemoteException e) {
            e.printStackTrace();
        }


    }

    @Override
    public int getPlaybackState() {
        return service.getPlaybackState();
    }

    private class PlayerListener implements Player.EventListener {

        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {

        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
            if (isLoading) playbackStateCallback.onLoadingStarted(player);
            else playbackStateCallback.onLoadingFinished();
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            switch (playbackState) {
                case Player.STATE_READY:
                    if (playWhenReady) {
                        currentSong.setDuration(player.getDuration());
                        service.setMetadata(currentSong, playbackStateCallback);
                        playbackStateCallback.onPlaying();
                    } else {
                        playbackStateCallback.onPaused();
                    }
                    break;
                case Player.STATE_IDLE:
                    break;
                case Player.STATE_ENDED:
                    service.setPlaybackState(PlaybackStateCompat.STATE_NONE, player.getCurrentPosition());
                    playbackStateCallback.onStopped();
                    currentSong = null;
                    break;
            }
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {

        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            playbackStateCallback.onError(error.getMessage());
        }

        @Override
        public void onPositionDiscontinuity() {

        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

        }
    }
}