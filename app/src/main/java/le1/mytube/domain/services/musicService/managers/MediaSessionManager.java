package le1.mytube.domain.services.musicService.managers;

import android.content.Context;
import android.content.Intent;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import le1.mytube.data.database.youTubeSong.YouTubeSong;
import le1.mytube.domain.listeners.AudioFocusCallback;

/**
 * Manager of {@link MediaSessionCompat}.
 * Every action related to {@link MediaSessionCompat} passes through this class
 */
public class MediaSessionManager {
    private static final String TAG = "MediaSessionManager";
    private static final long PLAYBACK_SPEED_NORMAL = 1;

    private final MediaSessionCompat mediaSession;
    private final PlaybackStateCompat.Builder playbackState = new PlaybackStateCompat.Builder();

    private MediaButtonManager mediaButtonManager;
    private PlayerManager playerManager;
    private AudioFocusManager audioFocusManager;
    private  QueueManager queueManager;

    /**
     * Build {@link #mediaSession} instance
     *
     * @param context  Application context
     * @param callback {@link MediaSessionCompat.Callback} of the {@link #mediaSession}
     */
    public MediaSessionManager(Context context, MediaSessionCompat.Callback callback) {
        mediaSession = new MediaSessionCompat(context.getApplicationContext(), TAG);
        mediaSession.setCallback(callback);
        mediaSession.setQueueTitle("Queue Title");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS | MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS);

        // set supported actions. If action is not specified here it won't do anything
        // when called through mediaSession.getController().getTransportControls()
        playbackState.setActions(PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID |
                PlaybackStateCompat.ACTION_PREPARE |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                PlaybackStateCompat.ACTION_REWIND |
                PlaybackStateCompat.ACTION_FAST_FORWARD |
                PlaybackStateCompat.ACTION_STOP
        );


        mediaButtonManager = new MediaButtonManager(context, mediaSession);
        playerManager = PlayerManager.INSTANCE;
        playerManager.buildPlayer(context);
        audioFocusManager = new AudioFocusManager(context);
        queueManager = new QueueManager(mediaSession);
    }

    /**
     * @see MediaButtonReceiver#handleIntent(MediaSessionCompat, Intent)
     */
    public MediaButtonManager getMediaButtonManager() {
        return mediaButtonManager;
    }

    public PlayerManager getPlayerManager(){
        return playerManager;
    }

    public AudioFocusManager getAudioFocusManager(AudioFocusCallback audioFocusCallback){
        audioFocusManager.setCallback(audioFocusCallback);
        return audioFocusManager;
    }

    public QueueManager getQueueManager(){
        return queueManager;
    }

    /**
     * The state must be one of the following:
     * {@link PlaybackStateCompat#STATE_NONE}
     * {@link PlaybackStateCompat#STATE_STOPPED}
     * {@link PlaybackStateCompat#STATE_PLAYING}
     * {@link PlaybackStateCompat#STATE_PAUSED}
     * {@link PlaybackStateCompat#STATE_FAST_FORWARDING}
     * {@link PlaybackStateCompat#STATE_REWINDING}
     * {@link PlaybackStateCompat#STATE_BUFFERING}
     * {@link PlaybackStateCompat#STATE_ERROR}
     * {@link PlaybackStateCompat#STATE_CONNECTING}
     * {@link PlaybackStateCompat#STATE_SKIPPING_TO_PREVIOUS}
     * {@link PlaybackStateCompat#STATE_SKIPPING_TO_NEXT}
     * {@link PlaybackStateCompat#STATE_SKIPPING_TO_QUEUE_ITEM}
     * <p>
     * for further info see {@link MediaSessionCompat#setPlaybackState(PlaybackStateCompat)}
     */
    public void setPlaybackState(int state, long playerCurrentPosition) {
        playbackState.setState(state, playerCurrentPosition, PLAYBACK_SPEED_NORMAL);
        mediaSession.setPlaybackState(playbackState.build());
    }

    public void setPlaybackStateErrorMessage(int errorCode, String errorMessage) {
        playbackState.setErrorMessage(errorCode, errorMessage);
        mediaSession.setPlaybackState(playbackState.build());
    }

    /**
     * @return Current playback state. see {@link #setPlaybackState(int, long)}
     */
    public int getPlaybackState() {
        return mediaSession.getController().getPlaybackState().getState();
    }

    public String getPlaybackStateErrorMessage() {
        if (mediaSession.getController().getPlaybackState().getErrorMessage() == null)
            return "error message missing";
        else return mediaSession.getController().getPlaybackState().getErrorMessage().toString();
    }


    /**
     * Set metadata of notification, wear etc
     *
     * @param youTubeSong the song from which retrieve metadata
     */
    public void setMetadata(YouTubeSong youTubeSong) {
        MediaMetadataCompat.Builder metadata = new MediaMetadataCompat.Builder();
        metadata.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, youTubeSong.getId());
        metadata.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, youTubeSong.getId());

        metadata.putString(MediaMetadataCompat.METADATA_KEY_TITLE, youTubeSong.getTitle());
        metadata.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, youTubeSong.getTitle());

        metadata.putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, youTubeSong.getAuthor());
        metadata.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, youTubeSong.getAuthor());
        metadata.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, youTubeSong.getTitle());

        if (youTubeSong.getImageUri() != null) {
            metadata.putString(MediaMetadataCompat.METADATA_KEY_ART_URI, youTubeSong.getImageUri().toString());
            metadata.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, youTubeSong.getImageUri().toString());

        }

        metadata.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, youTubeSong.getImageBitmap());
        metadata.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, youTubeSong.getImageBitmap());

        metadata.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, youTubeSong.getDuration());
        mediaSession.setMetadata(metadata.build());
    }

    /**
     * @return Metadata of current track
     */
    public MediaMetadataCompat getMetadata() {
        return mediaSession.getController().getMetadata();
    }


    /**
     * @return {@link #mediaSession}'s token
     */
    public MediaSessionCompat.Token getToken() {
        return mediaSession.getSessionToken();
    }


    /**
     * @see MediaSessionCompat#setActive(boolean)
     * should be called right after gaining audio focus
     */
    public void setActive() {
        mediaSession.setActive(true);
    }

    /**
     * @see MediaSessionCompat#setActive(boolean)
     * should be called right after losing audio focus
     */
    public void setInactive() {
        mediaSession.setActive(false);
    }

    /**
     * @see MediaSessionCompat#release()
     */
    public void destroy() {
        mediaSession.release();
    }
}
