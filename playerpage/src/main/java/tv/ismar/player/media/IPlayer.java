package tv.ismar.player.media;

import java.util.List;

import tv.ismar.app.network.entity.ClipEntity;

/**
 * Created by longhai on 16-9-12.
 */
public interface IPlayer {

    /**
     * 设置播放地址
     *
     * @param clipEntity 媒体播放地址
     */
    void setDataSource(ClipEntity clipEntity, OnDataSourceSetListener onDataSourceSetListener);

    /**
     * 播放器准备
     */
    void prepareAsync();

    void start();

    void pause();

    void seekTo(int position);

    void release();

    int getCurrentPosition();

    int getDuration();

    int getAdCountDownTime();

    boolean isPlaying();

    ClipEntity.Quality getCurrentQuality();

    List<ClipEntity.Quality> getQulities();

    void switchQuality(ClipEntity.Quality quality);

    /**
     * 是否处在可以播放状态
     */
    boolean isInPlaybackState();

    void setOnBufferChangedListener(IsmartvPlayer.OnBufferChangedListener onBufferChangedListener);

    void setOnVideoSizeChangedListener(IsmartvPlayer.OnVideoSizeChangedListener onVideoSizeChangedListener);

    void setOnStateChangedListener(OnStateChangedListener onStateChangedListener);

    public interface OnVideoSizeChangedListener {
        void onVideoSizeChanged(int videoWidth, int videoHeight);
    }

    public interface OnBufferChangedListener {
        void onBufferStart();

        void onBufferEnd();
    }

    public interface OnStateChangedListener {
        void onPrepared();

        void onAdStart();

        void onAdEnd();

        void onStarted();

        void onPaused();

        void onSeekComplete();

        void onCompleted();

        boolean onError(String message);
    }

    public interface OnDataSourceSetListener {

        void onSuccess();

        void onFailed(String message);

    }

}
