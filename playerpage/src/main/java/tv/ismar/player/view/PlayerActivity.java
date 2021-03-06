package tv.ismar.player.view;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tv.ismar.app.BaseActivity;
import tv.ismar.app.core.PageIntentInterface;
import tv.ismar.app.network.entity.ClipEntity;
import tv.ismar.app.network.entity.ItemEntity;
import tv.ismar.app.util.Utils;
import tv.ismar.app.widget.MessagePopWindow;
import tv.ismar.player.PlayerPageContract;
import tv.ismar.player.R;
import tv.ismar.player.databinding.ActivityPlayerBinding;
import tv.ismar.player.media.IPlayer;
import tv.ismar.player.media.IsmartvPlayer;
import tv.ismar.player.media.PlayerBuilder;
import tv.ismar.player.presenter.PlayerPagePresenter;
import tv.ismar.player.viewmodel.PlayerPageViewModel;

public class PlayerActivity extends BaseActivity implements PlayerPageContract.View,
        IPlayer.OnVideoSizeChangedListener, IPlayer.OnStateChangedListener, IPlayer.OnBufferChangedListener {

    private final String TAG = "LH/PlayerActivity";
    private static final int MSG_SEK_ACTION = 103;
    private static final int MSG_AD_COUNTDOWN = 104;

    private int itemPK = 0;
    private int subItemPk = 0;
    private int mediaHistoryPosition;
    private int mCurrentTeleplayIndex = 0;
    private int mCurrentQualityIndex = 0;
    private ItemEntity mItemEntity;

    // 播放器
    private IsmartvPlayer mIsmartvPlayer;
    private SurfaceView surfaceView;
    private FrameLayout player_container;
    private LinearLayout panel_layout;
    private SeekBar player_seekBar;
    private ListView player_menu;
    private boolean isShowSubMenu = false;
    private int groupMenuIndex = -1;
    private MenuAdapter mAdapter;
    private static final int SHORT_STEP = 1000;
    private boolean isSeeking = false;
    private ImageView player_logo_image;
    private ImageView ad_vip_btn;
    private TextView ad_count_text;

    private PlayerPageViewModel mModel;
    private PlayerPageContract.Presenter mPresenter;
    private PlayerPagePresenter mPlayerPagePresenter;
    private ActivityPlayerBinding mBinding;

    private Animation panelShowAnimation;
    private Animation panelHideAnimation;
    private Animation slideInRight;
    private Animation slideOutRight;
    private boolean mIsPlayingAd;
    private String[] keys = new String[]{"画面质量", "剧集", "客服中心", "从头播放"};
    private Map<String, List<String>> menuMaps = new HashMap<>();
    private List<String> menuDatas = new ArrayList<>();
    private GestureDetector mGestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String itemId = intent.getStringExtra(PageIntentInterface.EXTRA_ITEM_ID);
        String subItemId = intent.getStringExtra(PageIntentInterface.EXTRA_SUBITEM_ID);
        mediaHistoryPosition = intent.getIntExtra(PageIntentInterface.EXTRA_MEDIA_POSITION, 0);
        subItemId = subItemId == null ? "0" : subItemId;
        try {
            itemPK = Integer.valueOf(itemId);
            subItemPk = Integer.valueOf(subItemId);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if (itemPK <= 0) {
            finish();
            Log.i(TAG, "itemId can't be null.");
            return;
        }

        mPlayerPagePresenter = new PlayerPagePresenter(getApplicationContext(), this);
        mModel = new PlayerPageViewModel(this, mPlayerPagePresenter);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_player);
        mBinding.setTasks(mModel);
        mBinding.setActionHandler(mPresenter);

        surfaceView = findView(R.id.surfaceView);
        player_container = findView(R.id.player_container);
        panel_layout = findView(R.id.panel_layout);
        player_seekBar = findView(R.id.player_seekBar);
        player_seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
        player_menu = findView(R.id.player_menu);
        player_menu.setOnItemClickListener(onItemClickListener);
        player_logo_image = findView(R.id.player_logo_image);
        ad_vip_btn = findView(R.id.ad_vip_btn);
        ad_count_text = findView(R.id.ad_count_text);

        panelShowAnimation = AnimationUtils.loadAnimation(this,
                R.anim.fly_up);
        panelHideAnimation = AnimationUtils.loadAnimation(this,
                R.anim.fly_down);
        slideInRight = AnimationUtils.loadAnimation(this,
                R.anim.slide_in_right);
        slideOutRight = AnimationUtils.loadAnimation(this,
                android.R.anim.slide_out_right);

        mPresenter.start();
        mPresenter.fetchItem(itemId);
        showProgressDialog(null);

        mGestureDetector = new GestureDetector(this, onGestureListener);

    }

    @Override
    protected void onStop() {
        mPresenter.stop();
        if (mIsmartvPlayer != null) {
            mIsmartvPlayer.release();
        }
        hidePanel();
        super.onStop();
    }

    @Override
    public void loadItem(ItemEntity itemEntity) {
        mItemEntity = itemEntity;
        ItemEntity.Clip clip = itemEntity.getClip();
        ItemEntity.SubItem[] subItems = itemEntity.getSubitems();
        if (subItemPk > 0 && subItems != null) {
            // 获取当前要播放的电视剧Clip
            for (int i = 0; i < subItems.length; i++) {
                int _subItemPk = subItems[i].getPk();
                if (subItemPk == _subItemPk) {
                    mCurrentTeleplayIndex = i;
                    clip = subItems[i].getClip();
                    mItemEntity.setTitle(subItems[i].getTitle());
                    break;
                }
            }
        }
        String sign = "";
        String code = "1";
        mPresenter.fetchMediaUrl(clip.getUrl(), sign, code);

    }

    @Override
    public void loadClip(ClipEntity clipEntity) {
        Log.d(TAG, clipEntity.toString());
        String iqiyi = clipEntity.getIqiyi_4_0();
        byte playerMode;
        if (Utils.isEmptyText(iqiyi)) {
            // 片源为视云
            playerMode = PlayerBuilder.MODE_SMART_PLAYER;
        } else {
            // 片源为爱奇艺
            playerMode = PlayerBuilder.MODE_QIYI_PLAYER;
        }
        initPlayerData(playerMode);
        mIsmartvPlayer = PlayerBuilder.getInstance()
                .setActivity(PlayerActivity.this)
                .setPlayerMode(playerMode)
                .setItemEntity(mItemEntity)
                .setSurfaceView(surfaceView)
                .setContainer(player_container)
                .build();
        mIsmartvPlayer.setDataSource(clipEntity, new IPlayer.OnDataSourceSetListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "player init success.");
                mIsmartvPlayer.prepareAsync();
            }

            @Override
            public void onFailed(String message) {
                Log.i(TAG, "player init fail: " + message);
                Toast.makeText(PlayerActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
        mIsmartvPlayer.setOnBufferChangedListener(this);
        mIsmartvPlayer.setOnStateChangedListener(this);
        mIsmartvPlayer.setOnVideoSizeChangedListener(this);

    }

    private void initPlayerData(byte playerMode) {
        menuMaps.clear();
    }

    @Override
    public void setPresenter(PlayerPageContract.Presenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public void onHttpFailure(Throwable e) {
    }

    @Override
    public void onHttpInterceptor(Throwable e) {
    }

    @Override
    public void onBufferStart() {
        showProgressDialog(null);

    }

    @Override
    public void onBufferEnd() {
        dismissProgressDialog();

    }

    @Override
    public void onPrepared() {
        if (mIsmartvPlayer == null) {
            return;
        }
        mModel.setPanelData(mIsmartvPlayer, mItemEntity.getTitle());
        if (mediaHistoryPosition > 0) {
            mIsmartvPlayer.seekTo(mediaHistoryPosition);
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mIsmartvPlayer != null && !mIsmartvPlayer.isPlaying()) {
                    mIsmartvPlayer.start();
                }
            }
        }, 500);

    }

    @Override
    public void onAdStart() {
        mIsPlayingAd = true;
        switch (mIsmartvPlayer.getPlayerMode()) {
            case PlayerBuilder.MODE_SMART_PLAYER:
                ad_vip_btn.setVisibility(View.GONE);
                ad_count_text.setVisibility(View.VISIBLE);
                break;
            case PlayerBuilder.MODE_QIYI_PLAYER:
                ad_vip_btn.setVisibility(View.VISIBLE);
                ad_count_text.setVisibility(View.VISIBLE);
                ad_vip_btn.setFocusable(true);
                ad_vip_btn.requestFocus();
                break;
        }
        mHandler.removeMessages(MSG_AD_COUNTDOWN);
        mHandler.sendEmptyMessage(MSG_AD_COUNTDOWN);
    }

    @Override
    public void onAdEnd() {
        mIsPlayingAd = false;
        ad_vip_btn.setVisibility(View.GONE);
        ad_count_text.setVisibility(View.GONE);
        mHandler.removeMessages(MSG_AD_COUNTDOWN);
    }

    // 奇艺播放器在onPrepared时无法获取到影片时长
    @Override
    public void onStarted() {
        String logo = mItemEntity.getLogo();
        Log.i(TAG, "clipLength:" + mIsmartvPlayer.getDuration() + " logo:" + logo);
        if (!Utils.isEmptyText(logo) && mIsmartvPlayer.getPlayerMode() == PlayerBuilder.MODE_SMART_PLAYER) {
            Picasso.with(this).load(logo).into(player_logo_image, new Callback() {
                @Override
                public void onSuccess() {
                    player_logo_image.setVisibility(View.VISIBLE);
                }

                @Override
                public void onError() {
                }
            });

        }
        player_seekBar.setMax(mIsmartvPlayer.getDuration());
        mModel.updatePlayerPause();
        timerStart(0);
        showPanel();
        initMenuData();
        setMenuData();

    }

    @Override
    public void onPaused() {
        mModel.updatePlayerPause();
    }

    @Override
    public void onSeekComplete() {
        Log.i(TAG, "onSeekComplete");
        timerStart(1000);
        showPanel();
        isSeeking = false;
    }

    @Override
    public void onCompleted() {
        if (mItemEntity.getSubitems() != null && mCurrentTeleplayIndex < mItemEntity.getSubitems().length - 1) {
            String sign = "";
            String code = "1";
            mCurrentTeleplayIndex++;
            ItemEntity.SubItem subItem = mItemEntity.getSubitems()[mCurrentTeleplayIndex];
            mItemEntity.setTitle(subItem.getTitle());
            ItemEntity.Clip clip = subItem.getClip();
            if (clip != null) {
                mPresenter.fetchMediaUrl(clip.getUrl(), sign, code);
                return;
            }
        }
        finish();
    }

    @Override
    public boolean onError(String message) {
        return false;
    }

    @Override
    public void onVideoSizeChanged(int videoWidth, int videoHeight) {

    }

    private SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            mModel.updateTimer(progress, mIsmartvPlayer.getDuration());
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            timerStop();
            hidePanelHandler.removeCallbacks(hidePanelRunnable);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (!mItemEntity.getLiveVideo()) {
                int seekProgress = seekBar.getProgress();
                int maxSeek = mIsmartvPlayer.getDuration() - 3 * 1000;
                if (seekProgress >= maxSeek) {
                    seekProgress = maxSeek;
                }
                isSeeking = true;
                mIsmartvPlayer.seekTo(seekProgress);
            }
        }
    };

    private void timerStart(int delay) {
        if (mIsmartvPlayer == null) {
            Log.e(TAG, "checkTaskStart: mIsmartvPlayer is null.");
            return;
        }
        timerStop();
        if (delay > 0) {
            mTimerHandler.postDelayed(timerRunnable, delay);
        } else {
            mTimerHandler.post(timerRunnable);
        }
    }

    private void timerStop() {
        mTimerHandler.removeCallbacks(timerRunnable);
    }

    private Handler mTimerHandler = new Handler();

    private Runnable timerRunnable = new Runnable() {
        public void run() {
            if (!mItemEntity.getLiveVideo() && mIsmartvPlayer.isPlaying() && !isSeeking) {
                int currentPosition = mIsmartvPlayer.getCurrentPosition();
                player_seekBar.setProgress(currentPosition);
            }
            mTimerHandler.postDelayed(timerRunnable, 1000);
        }
    };

    private Handler hidePanelHandler = new Handler();

    private Runnable hidePanelRunnable = new Runnable() {
        @Override
        public void run() {
            hidePanel();
            hidePanelHandler.removeCallbacks(hidePanelRunnable);
        }
    };

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SEK_ACTION:
                    isSeeking = true;
                    mIsmartvPlayer.seekTo(mediaHistoryPosition);
                    offsets = 0;
                    offn = 1;
                    Log.d(TAG, "MSG_SEK_ACTION seek to " + mediaHistoryPosition);
                    break;
                case MSG_AD_COUNTDOWN:
                    int countDownTime = mIsmartvPlayer.getAdCountDownTime() / 1000;
                    String time = String.valueOf(countDownTime);
                    if (countDownTime < 10) {
                        time = "0" + time;
                    }
                    if (mIsmartvPlayer.getPlayerMode() == PlayerBuilder.MODE_SMART_PLAYER) {
                        ad_count_text.setText("广告倒计时" + time);
                    } else {
                        ad_count_text.setText("" + time);
                    }
                    sendEmptyMessageDelayed(MSG_AD_COUNTDOWN, 1000);
                    break;
            }
        }
    };

    private int offsets = 0; // 进度条变化
    private int offn = 1;

    private void fastForward(int step) {
        int clipLength = mIsmartvPlayer.getDuration();
        if (mediaHistoryPosition > clipLength) {
            player_seekBar.setProgress(clipLength - 3000);
            return;
        }
        if (clipLength > 1000000) {
            if (offsets != 1 && offsets % 5 != 0) {
                offsets += step;
            } else {
                if (offsets > 0) {
                    offn = offsets / 5;
                }
            }
            if (offn < 11) {
                mediaHistoryPosition += clipLength * offn * 0.01;
            } else {
                mediaHistoryPosition += clipLength * 0.1;
            }
        } else {
            Log.i(TAG, "clipLength  <= 1000000");
            mediaHistoryPosition += 10000;
        }

        if (mediaHistoryPosition > clipLength) {
            mediaHistoryPosition = clipLength - 3000;
        }
        player_seekBar.setProgress(mediaHistoryPosition);
    }

    private void fastBackward(int step) {
        int clipLength = mIsmartvPlayer.getDuration();
        if (mediaHistoryPosition < 0) {
            player_seekBar.setProgress(0);
            return;
        }
        if (clipLength > 1000000) {
            if (offsets != 1 && offsets % 5 != 0) {
                offsets += step;
            } else {
                if (offsets > 0) {
                    offn = offsets / 5;
                }
            }
            if (offn < 11) {
                mediaHistoryPosition -= clipLength * offn * 0.01;
            } else {
                mediaHistoryPosition -= clipLength * 0.1;
            }
        } else {
            mediaHistoryPosition -= 10000;
        }
        if (mediaHistoryPosition < 0)
            mediaHistoryPosition = 0;
        player_seekBar.setProgress(mediaHistoryPosition);
    }

    private void showPanel() {
        if (panel_layout == null || mIsmartvPlayer == null) {
            return;
        }
        if (mIsPlayingAd || !mIsmartvPlayer.isInPlaybackState())
            return;
        if (panel_layout.getVisibility() != View.VISIBLE) {
            panel_layout.startAnimation(panelShowAnimation);
            panel_layout.setVisibility(View.VISIBLE);
            hidePanelHandler.postDelayed(hidePanelRunnable, 3000);
        } else {
            hidePanelHandler.removeCallbacks(hidePanelRunnable);
            hidePanelHandler.postDelayed(hidePanelRunnable, 3000);
        }

    }

    private void hidePanel() {
        if (panel_layout.getVisibility() == View.VISIBLE) {
            panel_layout.startAnimation(panelHideAnimation);
            panel_layout.setVisibility(View.GONE);
        }
    }

    private void toggleMenuVisiblity() {
        if (player_menu == null || mIsmartvPlayer == null) {
            return;
        }
        if (mIsPlayingAd || !mIsmartvPlayer.isInPlaybackState())
            return;
        if (player_menu.getVisibility() == View.VISIBLE) {
            player_menu.startAnimation(slideOutRight);
            player_menu.setVisibility(View.GONE);
        } else {
            player_menu.startAnimation(slideInRight);
            player_menu.setVisibility(View.VISIBLE);
            player_menu.requestFocus();
        }
    }

    private void hideMenu() {
        if (player_menu.getVisibility() == View.VISIBLE) {
            player_menu.startAnimation(slideOutRight);
            player_menu.setVisibility(View.GONE);
        }
    }

    public void previousClick(View view) {
        if (!mItemEntity.getLiveVideo()) {
            timerStop();
            showPanel();
            hidePanelHandler.removeCallbacks(hidePanelRunnable);
            fastBackward(SHORT_STEP);
            if (mHandler.hasMessages(MSG_SEK_ACTION))
                mHandler.removeMessages(MSG_SEK_ACTION);
            mHandler.sendEmptyMessageDelayed(MSG_SEK_ACTION, 1000);
        }
    }

    public void forwardClick(View view) {
        if (!mItemEntity.getLiveVideo()) {
            timerStop();
            showPanel();
            hidePanelHandler.removeCallbacks(hidePanelRunnable);
            fastForward(SHORT_STEP);
            if (mHandler.hasMessages(MSG_SEK_ACTION))
                mHandler.removeMessages(MSG_SEK_ACTION);
            mHandler.sendEmptyMessageDelayed(MSG_SEK_ACTION, 1000);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i("LH/", "onKeyDown:" + keyCode);
        if (mItemEntity == null || mIsmartvPlayer == null) {
            return true;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                toggleMenuVisiblity();
                break;
            case KeyEvent.KEYCODE_BACK:
                if (player_menu.getVisibility() == View.VISIBLE) {
                    if (isShowSubMenu) {
                        setMenuData();
                    } else {
                        hideMenu();
                    }
                    return true;
                }
                finish();
                break;
        }
        boolean isKeyCodeSupported = keyCode != KeyEvent.KEYCODE_BACK &&
                keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
                keyCode != KeyEvent.KEYCODE_VOLUME_DOWN &&
                keyCode != KeyEvent.KEYCODE_VOLUME_MUTE &&
                keyCode != KeyEvent.KEYCODE_MENU &&
                keyCode != KeyEvent.KEYCODE_CALL &&
                keyCode != KeyEvent.KEYCODE_ENDCALL;
        if (mIsmartvPlayer.isInPlaybackState() && isKeyCodeSupported && player_menu.getVisibility() != View.VISIBLE) {
            if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                    keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                if (mIsmartvPlayer.isPlaying()) {
                    mIsmartvPlayer.pause();
                    showPanel();
                } else {
                    mIsmartvPlayer.start();
                    hidePanel();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                if (!mIsmartvPlayer.isPlaying()) {
                    mIsmartvPlayer.start();
                    hidePanel();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                    || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                if (mIsmartvPlayer.isPlaying()) {
                    mIsmartvPlayer.pause();
                    showPanel();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                    || keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
                previousClick(null);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                    || keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
                    || keyCode == KeyEvent.KEYCODE_FORWARD) {
                forwardClick(null);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                    || keyCode == KeyEvent.KEYCODE_ENTER
                    || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                if (mIsmartvPlayer.getDuration() > 0 && !mIsPlayingAd) {
                    showPanel();
                    if (mIsmartvPlayer.isPlaying()) {
                        mIsmartvPlayer.pause();
                    } else {
                        mIsmartvPlayer.start();
                    }
                    return true;
                }
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP) {
                finish();
            } else {
                toggleMenuVisiblity();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isProgressDialogShow()) {
            return true;
        }
        return mGestureDetector.onTouchEvent(event);
    }

    GestureDetector.OnGestureListener onGestureListener = new GestureDetector.OnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (player_menu.getVisibility() == View.VISIBLE) {
                return true;
            }
            showPanel();
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1.getX() - e2.getX() > 120 || e1.getX() - e2.getX() < -120) {
                toggleMenuVisiblity();
                return true;
            }
            if (player_menu.getVisibility() == View.VISIBLE) {
                return true;
            }
            return false;
        }
    };

    private void initMenuData() {
        if (menuMaps.isEmpty()) {
            List<ClipEntity.Quality> qualities = mIsmartvPlayer.getQulities();
            if (qualities != null && qualities.size() > 0) {
                List<String> qualityString = new ArrayList<>();
                int i = 0;
                for (ClipEntity.Quality quality : qualities) {
                    qualityString.add(ClipEntity.Quality.getString(quality));
                    // 初始化当前播放分辨率index
                    if (mIsmartvPlayer.getCurrentQuality() == quality) {
                        mCurrentQualityIndex = i;
                    }
                    i++;
                }
                menuMaps.put(keys[0], qualityString);
            }
            ItemEntity.SubItem[] subItems = mItemEntity.getSubitems();
            if (subItems != null) {
                ArrayList<String> teles = new ArrayList<>();
                for (ItemEntity.SubItem subItem : subItems) {
                    teles.add(subItem.getTitle());
                }
                menuMaps.put(keys[1], teles);
            }

            menuMaps.put(keys[2], null);
            if (!mItemEntity.getLiveVideo()) {
                menuMaps.put(keys[3], null);
            }

            mAdapter = new MenuAdapter();
            player_menu.setAdapter(mAdapter);
        }
    }

    private void setMenuData() {
        if (menuMaps.isEmpty()) {
            return;
        }
        menuDatas.clear();

        for (String key : keys) {
            if (menuMaps.containsKey(key)) {
                menuDatas.add(key);
            }
        }
        mAdapter.notifyDataSetChanged();
        player_menu.setSelection(0);
        groupMenuIndex = -1;
        isShowSubMenu = false;

    }

    private void setSubMenuData(int groupPosition) {
        if (menuMaps.isEmpty()) {
            return;
        }
        menuDatas.clear();
        menuDatas.addAll(menuMaps.get(keys[groupPosition]));
        mAdapter.notifyDataSetChanged();
        player_menu.setSelection(0);
        isShowSubMenu = true;
    }

    private class MenuAdapter extends BaseAdapter {

        private LayoutInflater mInflater;

        public MenuAdapter() {
            mInflater = LayoutInflater.from(PlayerActivity.this);
        }

        @Override
        public int getCount() {
            return menuDatas.size();
        }

        @Override
        public Object getItem(int position) {
            return menuDatas.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = mInflater.inflate(R.layout.adapter_player_menu, null);
            TextView checkBox = (TextView) convertView.findViewById(R.id.adapter_menu_checkBox);
            TextView textView = (TextView) convertView.findViewById(R.id.adapter_menu_text);
            textView.setText(menuDatas.get(position));
            checkBox.setVisibility(View.INVISIBLE);
            switch (groupMenuIndex) {
                case 0:
                    Log.i("LH/", "childPosition:" + position + " qualityIndex:" + mCurrentQualityIndex);
                    if (position == mCurrentQualityIndex) {
                        checkBox.setVisibility(View.VISIBLE);
                    }
                    break;
                case 1:
                    Log.i("LH/", "childPosition:" + position + " teleplayIndex:" + mCurrentTeleplayIndex);
                    if (position == mCurrentTeleplayIndex) {
                        checkBox.setVisibility(View.VISIBLE);
                    }
                    break;
            }
            convertView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    Log.i("LH/", "hasFocus:" + hasFocus);
                }
            });
            return convertView;
        }
    }

    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (isShowSubMenu) {
                // 菜单选择
                if (mIsmartvPlayer == null) {
                    return;
                }
                switch (groupMenuIndex) {
                    case 0:
                        List<ClipEntity.Quality> qualities = mIsmartvPlayer.getQulities();
                        if (!qualities.isEmpty()) {
                            ClipEntity.Quality quality = mIsmartvPlayer.getQulities().get(position);
                            if (mIsmartvPlayer.getCurrentQuality() != quality) {
                                mediaHistoryPosition = mIsmartvPlayer.getCurrentPosition();
                                mCurrentQualityIndex = position;
                                timerStop();
                                hideMenu();
                                mIsmartvPlayer.switchQuality(quality);
                                showProgressDialog(null);
                            }
                        }
                        break;
                    case 1:
                        if (mCurrentTeleplayIndex != position) {
                            mediaHistoryPosition = 0;
                            mCurrentTeleplayIndex = position;
                            timerStop();
                            hideMenu();
                            mIsmartvPlayer.release();
                            String sign = "";
                            String code = "1";
                            ItemEntity.SubItem subItem = mItemEntity.getSubitems()[mCurrentTeleplayIndex];
                            mItemEntity.setTitle(subItem.getTitle());
                            ItemEntity.Clip clip = subItem.getClip();
                            if (clip != null) {
                                mPresenter.fetchMediaUrl(clip.getUrl(), sign, code);
                            }
                            showProgressDialog(null);
                        }
                        break;
                }
            } else {
                String menu = menuDatas.get(position);
                for (int i = 0; i < keys.length; i++) {
                    if (menu.equals(keys[i])) {
                        groupMenuIndex = i;
                        break;
                    }
                }
                switch (groupMenuIndex) {
                    case 0:
                    case 1:
                        // 显示子菜单
                        setSubMenuData(groupMenuIndex);
                        break;
                    case 2:
                        // TODO
                        break;
                    case 3:
                        hideMenu();
                        timerStop();
                        showPanel();
                        hidePanelHandler.removeCallbacks(hidePanelRunnable);
                        player_seekBar.setProgress(0);
                        mediaHistoryPosition = 0;
                        isSeeking = true;
                        mIsmartvPlayer.seekTo(0);
                        break;
                }
            }
        }
    };

//    private void showExitPopup() {
//        final MessagePopWindow dialog = new MessagePopWindow(this);
//        dialog.setFirstMessage(getString(R.string.player_exit));
//        dialog.showAtLocation(contentView, Gravity.CENTER, 0, 0, new MessagePopWindow.ConfirmListener() {
//                    @Override
//                    public void confirmClick(View view) {
//                    }
//                },
//                new MessagePopWindow.CancelListener() {
//                    @Override
//                    public void cancelClick(View view) {
//
//                    }
//                });
//    }

}
