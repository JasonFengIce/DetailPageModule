package tv.ismar.player.view;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;

import tv.ismar.app.BaseActivity;
import tv.ismar.app.core.PageIntentInterface;
import tv.ismar.app.network.entity.ClipEntity;
import tv.ismar.app.network.entity.ItemEntity;
import tv.ismar.app.util.Utils;
import tv.ismar.player.AccessProxy;
import tv.ismar.player.PlayerPageContract;
import tv.ismar.player.R;
import tv.ismar.player.databinding.ActivityPlayerBinding;
import tv.ismar.player.presenter.PlayerPagePresenter;
import tv.ismar.player.viewmodel.PlayerPageViewModel;

public class PlayerActivity extends BaseActivity implements PlayerPageContract.View, SurfaceHolder.Callback {

    private final String TAG = "LH/PlayerActivity";

    private int itemPK = 0;
    private int subItemPk = 0;
    private int mediaPosition;
    private int mCurrentTeleplayIndex = 0;
    private ClipEntity mClipEntity;
    private String deviceToken = "__Ntksg9LjmpHH4Bx6wkjNKk8v6zzhQYu-erQaGzc7D0lUKTjwbH8GimsLJuRLEhaP";

    private PlayerPageViewModel mModel;
    private PlayerPageContract.Presenter mPresenter;
    private PlayerPagePresenter mPlayerPagePresenter;
    private ActivityPlayerBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String itemId = intent.getStringExtra(PageIntentInterface.EXTRA_ITEM_ID);
        String subItemId = intent.getStringExtra(PageIntentInterface.EXTRA_SUBITEM_ID);
        mediaPosition = intent.getIntExtra(PageIntentInterface.EXTRA_MEDIA_POSITION, 0);
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

        mPlayerPagePresenter = new PlayerPagePresenter(this);
        mModel = new PlayerPageViewModel(this, mPlayerPagePresenter);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_player);
        mBinding.setTasks(mModel);
        mBinding.setActionHandler(mPresenter);

        mPresenter.start();
        mPresenter.fetchItem(itemId, "", "");
        showProgressDialog(null);

    }

    @Override
    protected void onStop() {
        mPresenter.stop();
        super.onStop();
    }

    @Override
    public void loadItem(ItemEntity itemEntity) {
        ItemEntity.Clip clip = itemEntity.getClip();
        ItemEntity.SubItem[] subItems = itemEntity.getSubitems();
        if (subItemPk > 0 && subItems != null) {
            // 获取当前要播放的电视剧Clip
            for (int i = 0; i < subItems.length; i++) {
                int _subItemPk = subItems[i].getPk();
                if (subItemPk == _subItemPk) {
                    mCurrentTeleplayIndex = i;
                    clip = subItems[i].getClip();
                    break;
                }
            }
        }
        String accessToken = "";
        String sign = "";
        String code = "1";
        mPresenter.fetchMediaUrl(clip.getUrl(), deviceToken, accessToken, sign, code);

    }

    @Override
    public void loadClip(ClipEntity clipEntity) {
        Log.d(TAG, clipEntity.toString());
        String iqiyi = clipEntity.getIqiyi_4_0();
        mClipEntity = new ClipEntity();
        if (TextUtils.isEmpty(iqiyi)) {
            // 片源为视云
            String adaptive = clipEntity.getAdaptive();
            String normal = clipEntity.getNormal();
            String medium = clipEntity.getMedium();
            String high = clipEntity.getHigh();
            String ultra = clipEntity.getUltra();
            String blueray = clipEntity.getBlueray();
            String _4k = clipEntity.get_4k();
            if (!Utils.isEmptyText(adaptive)) {
                mClipEntity.setAdaptive(AccessProxy.AESDecrypt(adaptive, deviceToken));
            }
            if (!Utils.isEmptyText(normal)) {
                mClipEntity.setNormal(AccessProxy.AESDecrypt(normal, deviceToken));
            }
            if (!Utils.isEmptyText(medium)) {
                mClipEntity.setMedium(AccessProxy.AESDecrypt(medium, deviceToken));
            }
            if (!Utils.isEmptyText(high)) {
                mClipEntity.setHigh(AccessProxy.AESDecrypt(high, deviceToken));
            }
            if (!Utils.isEmptyText(ultra)) {
                mClipEntity.setUltra(AccessProxy.AESDecrypt(ultra, deviceToken));
            }
            if (!Utils.isEmptyText(blueray)) {
                mClipEntity.setBlueray(AccessProxy.AESDecrypt(blueray, deviceToken));
            }
            if (!Utils.isEmptyText(_4k)) {
                mClipEntity.set_4k(AccessProxy.AESDecrypt(_4k, deviceToken));
            }
        } else {
            // 片源为爱奇艺
            mClipEntity.setIqiyi_4_0(clipEntity.getIqiyi_4_0());
            mClipEntity.setIs_vip(clipEntity.is_vip());
        }
        Log.d(TAG, mClipEntity.toString());
        dismissProgressDialog();
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
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }
}