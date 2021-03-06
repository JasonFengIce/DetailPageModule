package tv.ismar.player.view;

/**
 * Created by Beaver on 2016/6/2.
 */

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import okhttp3.ResponseBody;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import tv.ismar.account.IsmartvActivator;
import tv.ismar.app.VodApplication;
import tv.ismar.app.network.SkyService;
import tv.ismar.app.network.entity.EventProperty;
import tv.ismar.app.util.DeviceUtils;
import tv.ismar.player.media.IsmartvMedia;

public class PlayerSync implements Runnable {

    private static final String TAG = "LH/PlayerSync";

    /**
     * 播放器打开
     */
    public static final String VIDEO_START = "video_start";
    /**
     * 开始播放缓冲结束
     */
    public static final String VIDEO_PLAY_LOAD = "video_play_load";
    /**
     * 切换码流
     */
    public static final String VIDEO_SWITCH_STREAM = "video_switch_stream";
    /**
     * 开始播放
     */
    public static final String VIDEO_PLAY_START = "video_play_start";
    /**
     * 播放暂停
     */
    public static final String VIDEO_PLAY_PAUSE = "video_play_pause";
    /**
     * 播放继续
     */
    public static final String VIDEO_PLAY_CONTINUE = "video_play_continue";
    /**
     * 播放快进/快退
     */
    public static final String VIDEO_PLAY_SEEK = "video_play_seek";
    /**
     * 播放快进/快退缓冲结束
     */
    public static final String VIDEO_PLAY_SEEK_BLOCKEND = "video_play_seek_blockend";
    /**
     * 播放缓冲结束
     */
    public static final String VIDEO_PLAY_BLOCKEND = "video_play_blockend";
    /**
     * 播放时网速
     */
    public static final String VIDEO_PLAY_SPEED = "video_play_speed";
    /**
     * 播放时下载速度慢
     */
    public static final String VIDEO_LOW_SPEED = "video_low_speed";
    /**
     * 播放器退出
     */
    public static final String VIDEO_EXIT = "video_exit";
    /**
     * 播放器异常
     */
    public static final String VIDEO_EXCEPT = "video_except";
    /**
     * 广告播放缓冲结束
     */
    public static final String AD_PLAY_LOAD = "ad_play_load";
    /**
     * 广告播放卡顿
     */
    public static final String AD_PLAY_BLOCKEND = "ad_play_blockend";
    /**
     * 广告播放结束
     */
    public static final String AD_PLAY_EXIT = "ad_play_exit";
    /**
     * 暂停广告播放
     */
    public static final String PAUSE_AD_PLAY = "pause_ad_play";
    /**
     * 暂停广告下载
     */
    public static final String PAUSE_AD_DOWNLOAD = "pause_ad_download";
    /**
     * 暂停广告异常
     */
    public static final String PAUSE_AD_EXCEPT = "pause_ad_except";

    private boolean isRunning = false;
    private static List<String> syncMessages = new ArrayList<>();

    public void setIsRunning(boolean run) {
        isRunning = run;
    }

    private HashMap<String, Object> getPublicParams(IsmartvMedia media, Integer quality, Integer speed, String sid, String playerFlag) {
        HashMap<String, Object> tempMap = new HashMap<>();
        tempMap.put(EventProperty.ITEM, media.getPk());
        if (media.getSubItemPk() > 0 && media.getPk() != media.getSubItemPk()) {
            tempMap.put(EventProperty.SUBITEM, media.getSubItemPk());
        }
        tempMap.put(EventProperty.TITLE, media.getTitle());
        tempMap.put(EventProperty.CLIP, media.getClipPk());
        tempMap.put(EventProperty.QUALITY, switchQuality(quality));
        tempMap.put(EventProperty.CHANNEL, media.getChannel());
        tempMap.put(EventProperty.SPEED, speed + "KByte/s");
        tempMap.put(EventProperty.SID, sid);
        tempMap.put(EventProperty.PLAYER_FLAG, playerFlag);
        return tempMap;
    }

    /**
     * 播放器打开 video_start
     *
     * @param media   (媒体)       Item
     * @param quality (视频清晰度 normal   medium  high  ultra  adaptive) STRING
     * @param userId  (用户ID) STRING
     * @param speed   (网速, 单位KB/s) INTEGER
     * @return HashMap
     */
    public HashMap<String, Object> videoStart(IsmartvMedia media, Integer quality, String userId, Integer speed, String sid, String playerFlag) {
        if (media == null) {
            return null;
        }
        HashMap<String, Object> tempMap = getPublicParams(media, quality, speed, sid, playerFlag);
        tempMap.put("userid", userId);
        tempMap.put("source", media.getSource());
        tempMap.put("section", media.getSection());
        String eventName = VIDEO_START;
        addMessageList(eventName, tempMap);
        return tempMap;
    }

    /**
     * 开始播放缓冲结束 video_play_load
     *
     * @param media    (媒体)Item
     * @param quality  (视频清晰度)normal |  medium | high | ultra | adaptive | adaptive_norma l | adaptive_medium | adaptive_high | adaptive_ultra) STRING
     * @param duration (缓存时间,单位s)INTEGER
     * @param speed    (网速,单位KB/s)INTEGER
     * @param mediaIP  (媒体IP)STRING
     * @return HashMap
     */
    public HashMap<String, Object> videoPlayLoad(IsmartvMedia media, Integer quality,
                                                 long duration, Integer speed, String mediaIP, String sid, String playerUrl, String playerFlag) {
        if (media == null) {
            return null;
        }
        HashMap<String, Object> tempMap = getPublicParams(media, quality, speed, sid, playerFlag);
        tempMap.put(EventProperty.DURATION, duration / 1000);
        tempMap.put(EventProperty.MEDIAIP, mediaIP);
        tempMap.put("play_url", playerUrl);
        String eventName = VIDEO_PLAY_LOAD;
        addMessageList(eventName, tempMap);
        return tempMap;

    }


    /**
     * 开始播放 video_play_start
     *
     * @param media   (媒体)Item
     * @param quality (视频清晰度) normal |  medium | high | ultra | adaptive) STRING
     * @param speed   (网速, 单位KB/s) INTEGER
     * @return HashMap
     */

    public HashMap<String, Object> videoPlayStart(IsmartvMedia media, Integer quality, Integer speed, String sid, String playerFlag) {
        if (media == null) {
            return null;
        }
        HashMap<String, Object> tempMap = getPublicParams(media, quality, speed, sid, playerFlag);
        String eventName = VIDEO_PLAY_START;
        addMessageList(eventName, tempMap);
        return tempMap;

    }

    /**
     * 播放暂停 video_play_pause
     *
     * @param media    (媒体)Item
     * @param quality  (视频清晰度)normal |  medium | high | ultra | adaptive) STRING
     * @param position (位置，单位s) INTEGER
     * @param speed    (网速, 单位KB/s) INTEGER
     * @return HashMap
     */
    public HashMap<String, Object> videoPlayPause(IsmartvMedia media, Integer quality, Integer speed, Integer position, String sid, String playerFlag) {
        if (media == null) {
            return null;
        }
        HashMap<String, Object> tempMap = getPublicParams(media, quality, speed, sid, playerFlag);
        tempMap.put(EventProperty.POSITION, position / 1000);
        String eventName = VIDEO_PLAY_PAUSE;
        addMessageList(eventName, tempMap);
        return tempMap;

    }

    /**
     * 播放继续 video_play_continue
     *
     * @param media    (媒体)Item
     * @param quality  (视频清晰度:    normal |  medium | high | ultra | adaptive) STRING
     * @param position (位置，单位s)  INTEGER
     * @param speed    (网速, 单位KB/s) INTEGER
     * @return HashMap
     */

    public HashMap<String, Object> videoPlayContinue(IsmartvMedia media, Integer quality, Integer speed, Integer position, String sid, String playerFlag) {
        if (media == null) {
            return null;
        }
        HashMap<String, Object> tempMap = getPublicParams(media, quality, speed, sid, playerFlag);
        tempMap.put(EventProperty.POSITION, position / 1000);
        String eventName = VIDEO_PLAY_CONTINUE;
        addMessageList(eventName, tempMap);
        return tempMap;

    }

    /**
     * 播放快进/快退 video_play_seek
     *
     * @param media    (媒体)Item
     * @param quality  (视频清晰度:     normal |  medium | high | ultra | adaptive) STRING
     * @param position (目标位置,单位s) INTEGER
     * @param speed    (网速, 单位KB/s) INTEGER
     * @return HashMap
     */

    public HashMap<String, Object> videoPlaySeek(IsmartvMedia media, Integer quality, Integer speed, Integer position, String sid, String playerFlag) {
        if (media == null) {
            return null;
        }
        HashMap<String, Object> tempMap = getPublicParams(media, quality, speed, sid, playerFlag);
        tempMap.put(EventProperty.POSITION, position / 1000);
        String eventName = VIDEO_PLAY_SEEK;
        addMessageList(eventName, tempMap);
        return tempMap;

    }

    /**
     * 播放快进/快退缓冲结束 video_play_seek_blockend
     *
     * @param media    (媒体) Item
     * @param quality  (视频清晰度:      normal |  medium | high | ultra | adaptive | adaptive_norma l | adaptive_medium | adaptive_high | adaptive_ultra) STRING
     * @param position (缓冲位置，单位s)  INTEGER
     * @param speed    (网速, 单位KB/s) INTEGER
     * @param duration (缓存时间,单位s)  INTEGER
     * @param mediaIP  (媒体IP)STRING
     * @return HashMap
     */

    public HashMap<String, Object> videoPlaySeekBlockend(IsmartvMedia media, Integer quality, Integer speed, Integer position, long duration, String mediaIP, String sid, String playerFlag) {
        if (media == null) {
            return null;
        }
        HashMap<String, Object> tempMap = getPublicParams(media, quality, speed, sid, playerFlag);
        tempMap.put(EventProperty.DURATION, duration / 1000);
        tempMap.put(EventProperty.POSITION, position / 1000);
        tempMap.put(EventProperty.MEDIAIP, mediaIP);
        String eventName = VIDEO_PLAY_SEEK_BLOCKEND;
        addMessageList(eventName, tempMap);
        return tempMap;

    }

    /**
     * 播放缓冲结束 video_play_blockend
     *
     * @param media    (媒体) Item
     * @param quality  (视频清晰度:      normal |  medium | high | ultra | adaptive | adaptive_normal | adaptive_medium | adaptive_high | adaptive_ultra) STRING
     * @param speed    (网速, 单位KB/s) INTEGER
     * @param duration (缓存时间,单位s)  INTEGER
     * @param mediaIP  (媒体IP)STRING
     * @return HashMap
     */
    public HashMap<String, Object> videoPlayBlockend(IsmartvMedia media, Integer quality, Integer speed, long duration, String mediaIP, String sid, String playerFlag) {
        if (media == null) {
            return null;
        }
        HashMap<String, Object> tempMap = getPublicParams(media, quality, speed, sid, playerFlag);
        tempMap.put(EventProperty.DURATION, duration / 1000);
        tempMap.put(EventProperty.MEDIAIP, mediaIP);
        String eventName = VIDEO_PLAY_BLOCKEND;
        addMessageList(eventName, tempMap);
        return tempMap;

    }

    /**
     * 播放时网速 video_play_speed
     *
     * @param media   (媒体) INTEGER
     * @param quality (视频清晰度: normal |  medium | high | ultra | adaptive) STRING
     * @param speed   (网速, 单位KB/s) INTEGER
     * @param mediaIP (媒体IP) STRING
     * @return HashMap
     */


    public HashMap<String, Object> videoPlaySpeed(IsmartvMedia media, Integer quality, Integer speed, String mediaIP, String sid, String playerFlag) {
        if (media == null) {
            return null;
        }
        HashMap<String, Object> tempMap = getPublicParams(media, quality, speed, sid, playerFlag);
        tempMap.put(EventProperty.MEDIAIP, mediaIP);
        String eventName = VIDEO_PLAY_SPEED;
        addMessageList(eventName, tempMap);
        return tempMap;

    }

    /**
     * 播放时下载速度慢  video_low_speed
     *
     * @param media   (媒体) INTEGER
     * @param quality (视频清晰度: normal |  medium | high | ultra | adaptive) STRING
     * @param speed   (网速, 单位KB/s) INTEGER
     * @param mediaIP (媒体IP) STRING
     * @return HashMap
     */

    public HashMap<String, Object> videoLowSpeed(IsmartvMedia media, Integer quality, Integer speed, String mediaIP, String sid, String playerFlag) {
        if (media == null) {
            return null;
        }
        HashMap<String, Object> tempMap = getPublicParams(media, quality, speed, sid, playerFlag);
        tempMap.put(EventProperty.MEDIAIP, mediaIP);
        String eventName = VIDEO_LOW_SPEED;
        addMessageList(eventName, tempMap);
        return tempMap;

    }

    /**
     * 播放器退出 video_exit
     *
     * @param media   (媒体) INTEGER
     * @param quality (视频清晰度: normal |  medium | high | ultra | adaptive) STRING
     * @param speed   (网速, 单位KB/s) INTEGER
     * @param to      (去向：detail | end) STRING
     * @return HashMap
     */

    public HashMap<String, Object> videoExit(IsmartvMedia media, Integer quality, Integer speed, String to, Integer position, long duration, String sid, String playerFlag) {
        if (media == null) {
            return null;
        }
        HashMap<String, Object> tempMap = getPublicParams(media, quality, speed, sid, playerFlag);
        tempMap.put(EventProperty.TO, to);
        tempMap.put(EventProperty.POSITION, position / 1000);
        tempMap.put(EventProperty.DURATION, duration / 1000);
        tempMap.put(EventProperty.SECTION, media.getSection());
        tempMap.put(EventProperty.SOURCE, media.getSource());
        String eventName = VIDEO_EXIT;
        addMessageList(eventName, tempMap);
        return tempMap;

    }

    /**
     * 播放器异常 videoExcept
     *
     * @param code     (异常码servertimeout|servertimeout|noplayaddress|mediaexception|mediatimeout|filenotfound|nodetail|debuggingexception|noextras) STRING
     * @param content  (异常内容)                                                                                                                    STRING
     * @param media    (媒体) INTEGER
     * @param quality  (视频清晰度:     normal |  medium | high | ultra | adaptive | adaptive_normal | adaptive_medium | adaptive_high | adaptive_ultra) STRING
     * @param position (播放位置，单位s) INTEGER
     * @return HashMap
     */

    public HashMap<String, Object> videoExcept(String code, String content, IsmartvMedia media, Integer speed, String sid, Integer quality, Integer position, String playerFlag) {
        if (media == null) {
            return null;
        }
        HashMap<String, Object> tempMap = getPublicParams(media, quality, speed, sid, playerFlag);
        tempMap.put(EventProperty.CODE, code);
        tempMap.put(EventProperty.CONTENT, content);
        tempMap.put(EventProperty.POSITION, position / 1000);
        tempMap.put(EventProperty.QUALITY, switchQuality(quality));
        tempMap.put(EventProperty.PLAYER_FLAG, playerFlag);
        String eventName = VIDEO_EXCEPT;
        addMessageList(eventName, tempMap);
        return tempMap;
    }


    /**
     * 切换码流 video_switch_stream
     *
     * @param media   (媒体) INTEGER
     * @param quality (视频清晰度: normal |  medium | high | ultra | adaptive) STRING
     * @param mode    (切换模式：auto | manual) STRING
     * @param speed   (网速, 单位KB/s) INTEGER
     * @param userid  STRING
     * @param mediaip STRING
     * @return HashMap
     */

    public HashMap<String, Object> videoSwitchStream(IsmartvMedia media, Integer quality, String mode, Integer speed, String userid, String mediaip, String sid, String playerFlag) {
        if (media == null) {
            return null;
        }
        HashMap<String, Object> tempMap = getPublicParams(media, quality, speed, sid, playerFlag);
        tempMap.put(EventProperty.MODE, mode);
        tempMap.put("userid", userid);
        tempMap.put(EventProperty.MEDIAIP, mediaip);
        tempMap.put(EventProperty.LOCATION, "detail");
        String eventName = VIDEO_SWITCH_STREAM;
        addMessageList(eventName, tempMap);
        return tempMap;

    }

    public void ad_play_load(IsmartvMedia media, long duration, String mediaip, int ad_id, String mediaflag) {
        if (media == null) {
            return;
        }
        HashMap<String, Object> tempMap = new HashMap<String, Object>();
        tempMap.put(EventProperty.SOURCE, media.getSource());
        tempMap.put(EventProperty.CHANNEL, media.getChannel());
        tempMap.put(EventProperty.SECTION, media.getSection());
        tempMap.put(EventProperty.DURATION, duration / 1000);
        tempMap.put(EventProperty.MEDIAIP, mediaip);
        tempMap.put(EventProperty.ITEM, media.getPk());
        tempMap.put(EventProperty.AD_ID, ad_id);
        tempMap.put(EventProperty.PLAYER_FLAG, mediaflag);
        String eventName = AD_PLAY_LOAD;
        addMessageList(eventName, tempMap);
    }

    public void ad_play_blockend(IsmartvMedia media, long duration, String mediaip, int ad_id, String mediaflag) {
        if (media == null) {
            return;
        }
        HashMap<String, Object> tempMap = new HashMap<String, Object>();
        tempMap.put(EventProperty.SOURCE, media.getSource());
        tempMap.put(EventProperty.CHANNEL, media.getChannel());
        tempMap.put(EventProperty.SECTION, media.getSection());
        tempMap.put(EventProperty.DURATION, duration / 1000);
        tempMap.put(EventProperty.MEDIAIP, mediaip);
        tempMap.put(EventProperty.ITEM, media.getPk());
        tempMap.put(EventProperty.AD_ID, ad_id);
        tempMap.put(EventProperty.PLAYER_FLAG, mediaflag);
        String eventName = AD_PLAY_BLOCKEND;
        addMessageList(eventName, tempMap);
    }

    public void ad_play_exit(IsmartvMedia media, long duration, String mediaip, int ad_id, String mediaflag) {
        if (media == null) {
            return;
        }
        HashMap<String, Object> tempMap = new HashMap<String, Object>();
        tempMap.put(EventProperty.SOURCE, media.getSource());
        tempMap.put(EventProperty.CHANNEL, media.getChannel());
        tempMap.put(EventProperty.SECTION, media.getSection());
        tempMap.put(EventProperty.DURATION, duration / 1000);
        tempMap.put(EventProperty.MEDIAIP, mediaip);
        tempMap.put(EventProperty.ITEM, media.getPk());
        tempMap.put(EventProperty.AD_ID, ad_id);
        tempMap.put(EventProperty.PLAYER_FLAG, mediaflag);
        String eventName = AD_PLAY_EXIT;
        addMessageList(eventName, tempMap);
    }

    public void pause_ad_play(String title, int media_id, String media_url, long duration, String mediaflag) {
        HashMap<String, Object> tempMap = new HashMap<String, Object>();
        tempMap.put(EventProperty.TITLE, title);
        tempMap.put(EventProperty.MEDIA_ID, media_id);
        tempMap.put(EventProperty.MEDIA_URL, media_url);
        tempMap.put(EventProperty.DURATION, duration / 1000);
        tempMap.put(EventProperty.PLAYER_FLAG, mediaflag);
        String eventName = PAUSE_AD_PLAY;
        addMessageList(eventName, tempMap);
    }

    public void pause_ad_download(String title, int media_id, String media_url, String mediaflag) {
        HashMap<String, Object> tempMap = new HashMap<String, Object>();
        tempMap.put(EventProperty.TITLE, title);
        tempMap.put(EventProperty.MEDIA_ID, media_id);
        tempMap.put(EventProperty.MEDIA_URL, media_url);
        tempMap.put(EventProperty.PLAYER_FLAG, mediaflag);
        String eventName = PAUSE_AD_DOWNLOAD;
        addMessageList(eventName, tempMap);
    }

    public void pause_ad_except(Integer errcode, String errorContent) {
        HashMap<String, Object> tempMap = new HashMap<String, Object>();
        tempMap.put(EventProperty.CODE, errcode);
        tempMap.put(EventProperty.CONTENT, errorContent);
        String eventName = PAUSE_AD_EXCEPT;
        addMessageList(eventName, tempMap);
    }

    private String switchQuality(Integer currQuality) {
        String quality = "";
        switch (currQuality) {
            case 0:
                quality = "low";
                break;
            case 1:
                quality = "adaptive";
                break;
            case 2:
                quality = "normal";
                break;
            case 3:
                quality = "medium";
                break;
            case 4:
                quality = "high";
                break;
            case 5:
                quality = "ultra";
                break;
            case 6:
                quality = "blueray";
                break;
            case 7:
                quality = "4k";
                break;
        }
        return quality;
    }

    private void addMessageList(String eventName, HashMap<String, Object> propertiesMap) {
        JSONObject logJson = new JSONObject();
        try {
            JSONObject propertiesJson = new JSONObject();
            propertiesJson.put("time", System.currentTimeMillis() / 1000);
            if (propertiesMap != null) {
                Set<String> set = propertiesMap.keySet();
                for (String key : set) {
                    propertiesJson.put(key, propertiesMap.get(key));
                }
            }
            logJson.put("event", eventName);
            logJson.put("properties", propertiesJson);

            syncMessages.add(logJson.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                Thread.sleep(10 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.i(TAG, "Log send thread interrupted.");
            }

            if (TextUtils.isEmpty(IsmartvActivator.getInstance().getLogDomain())) {
                Log.i(TAG, "Log domain null.");
                return;
            }
            String logUrl = IsmartvActivator.getInstance().getLogDomain() + "/log";
            if (!syncMessages.isEmpty()) {
                try {
                    JSONArray jsonArray = new JSONArray();
                    for (String s : syncMessages) {
                        jsonArray.put(new JSONObject(s));
                    }
                    String jsonContent = Base64
                            .encodeToString(jsonArray.toString().getBytes("UTF-8"), Base64.URL_SAFE);
                    String content = "sn=" + IsmartvActivator.getInstance().getSnToken()
                            + "&modelname=" + DeviceUtils.getModelName()
                            + "&data=" + URLEncoder.encode(jsonContent, "UTF-8")
                            + "&deviceToken=" + IsmartvActivator.getInstance().getDeviceToken()
                            + "&acessToken=" + IsmartvActivator.getInstance().getAuthToken();

                    SkyService.ServiceManager.getService().sendPlayerLog(
                            logUrl, IsmartvActivator.getInstance().getSnToken(),
                            DeviceUtils.getModelName(), URLEncoder.encode(jsonContent, "UTF-8")
                    ).subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<ResponseBody>() {
                                @Override
                                public void onCompleted() {

                                }

                                @Override
                                public void onError(Throwable e) {

                                }

                                @Override
                                public void onNext(ResponseBody responseBody) {
                                    if (responseBody != null) {
                                        syncMessages.clear();
                                    }
                                    if (VodApplication.DEBUG) {
                                        try {
                                            Log.i(TAG, "Log send result: " + responseBody.string());
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            });
                } catch (JSONException e) {
                    e.printStackTrace();
                    if (VodApplication.DEBUG) {
                        Log.i(TAG, "Log send json data error.");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (VodApplication.DEBUG) {
                        Log.i(TAG, "Log send post data error.");
                    }
                }
            }
        }

    }
}
