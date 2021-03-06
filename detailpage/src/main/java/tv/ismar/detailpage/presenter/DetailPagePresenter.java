package tv.ismar.detailpage.presenter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.text.ParseException;

import cn.ismartv.injectdb.library.query.Select;
import okhttp3.ResponseBody;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import tv.ismar.app.BaseActivity;
import tv.ismar.app.database.BookmarkTable;
import tv.ismar.app.network.SkyService;
import tv.ismar.app.network.entity.ItemEntity;
import tv.ismar.app.network.entity.PlayCheckEntity;
import tv.ismar.app.network.exception.OnlyWifiException;
import tv.ismar.app.util.Utils;
import tv.ismar.detailpage.DetailPageContract;
import tv.ismar.pay.PaymentActivity;

/**
 * Created by huibin on 8/19/16.
 */
public class DetailPagePresenter implements DetailPageContract.Presenter {
    private static final String TAG = DetailPagePresenter.class.getSimpleName();
    private final DetailPageContract.View mDetailView;
    private SkyService mSkyService;
    private Subscription apiItemSubsc;
    private Subscription bookmarksSubsc;
    private Subscription itemRelateSubsc;
    private Subscription removeBookmarksSubsc;
    private Subscription playCheckSubsc;

    private ItemEntity mItemEntity = new ItemEntity();
    private String mContentModel;


    public DetailPagePresenter(DetailPageContract.View detailView, String contentModel) {
        mContentModel = contentModel;
        mDetailView = detailView;
        mDetailView.setPresenter(this);
    }

    public String getContentModel() {
        return mContentModel;
    }

    @Override
    public void start() {
        mSkyService = ((BaseActivity)mDetailView.getContext()).mSkyService;
    }

    @Override
    public void fetchItem(String pk, String deviceToken, String accessToken) {
        if (apiItemSubsc != null && !apiItemSubsc.isUnsubscribed()) {
            apiItemSubsc.unsubscribe();
        }

        apiItemSubsc = mSkyService.apiItem(pk)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ItemEntity>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, e.getMessage());
                        if (e.getClass() == OnlyWifiException.class) {
                            mDetailView.onHttpInterceptor(e);
                        } else {
                            mDetailView.onHttpFailure(e);
                        }
                    }

                    @Override
                    public void onNext(ItemEntity itemEntity) {
                        mItemEntity = itemEntity;
                        mDetailView.loadItem(itemEntity);
                    }
                });
    }


    @Override
    public void createBookmarks(String pk, String deviceToken, String accessToken) {
        if (bookmarksSubsc != null && !bookmarksSubsc.isUnsubscribed()) {
            bookmarksSubsc.unsubscribe();
        }
        bookmarksSubsc = mSkyService.apiBookmarksCreate(pk)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ResponseBody>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(ResponseBody responseBody) {

                    }
                });
    }

    @Override
    public void removeBookmarks(String pk, String deviceToken, String accessToken) {
        if (removeBookmarksSubsc != null && !removeBookmarksSubsc.isUnsubscribed()) {
            removeBookmarksSubsc.unsubscribe();
        }
        removeBookmarksSubsc = mSkyService.apiBookmarksRemove(pk)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ResponseBody>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(ResponseBody responseBody) {

                    }
                });
    }

    @Override
    public void requestPlayCheck(String itemPk, String deviceToken, String accessToken) {
        if (playCheckSubsc != null && !playCheckSubsc.isUnsubscribed()) {
            playCheckSubsc.unsubscribe();
        }

        playCheckSubsc = mSkyService.apiPlayCheck(itemPk, null, null)
                .subscribeOn(Schedulers.io())
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
                        try {
                            PlayCheckEntity playCheckEntity = calculateRemainDay(responseBody.string());
                            mDetailView.notifyPlayCheck(playCheckEntity);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private PlayCheckEntity calculateRemainDay(String info) {
        PlayCheckEntity playCheckEntity;
        switch (info) {
            case "0":
                playCheckEntity = new PlayCheckEntity();
                playCheckEntity.setRemainDay(0);
                break;
            default:
                playCheckEntity = new Gson().fromJson(info, PlayCheckEntity.class);
                int remainDay;
                try {
                    remainDay = Utils.daysBetween(Utils.getTime(), playCheckEntity.getExpiry_date()) + 1;
                } catch (ParseException e) {
                    remainDay = 0;
                }
                playCheckEntity.setRemainDay(remainDay);
                break;
        }
        return playCheckEntity;
    }

    @Override
    public void fetchItemRelate(String pk, String deviceToken, String accessToken) {
        if (itemRelateSubsc != null && !itemRelateSubsc.isUnsubscribed()) {
            itemRelateSubsc.unsubscribe();
        }
        itemRelateSubsc = mSkyService.apiTvRelate(pk)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ItemEntity[]>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(ItemEntity[] itemEntities) {
                        mDetailView.loadItemRelate(itemEntities);
                    }
                });
    }


    @Override
    public void stop() {
        if (apiItemSubsc != null && !apiItemSubsc.isUnsubscribed()) {
            apiItemSubsc.unsubscribe();
        }
        if (bookmarksSubsc != null && !bookmarksSubsc.isUnsubscribed()) {
            bookmarksSubsc.unsubscribe();
        }
        if (itemRelateSubsc != null && !itemRelateSubsc.isUnsubscribed()) {
            itemRelateSubsc.unsubscribe();
        }
        if (removeBookmarksSubsc != null && !removeBookmarksSubsc.isUnsubscribed()) {
            removeBookmarksSubsc.unsubscribe();
        }
        if (playCheckSubsc != null && !playCheckSubsc.isUnsubscribed()) {
            playCheckSubsc.unsubscribe();
        }
    }

    @Override
    public void handleBookmark() {
        BookmarkTable bookmarkTable = new Select().from(BookmarkTable.class).where("pk = ?", mItemEntity.getPk()).executeSingle();
        if (bookmarkTable != null) {
            bookmarkTable.delete();
        } else {
            bookmarkTable = new BookmarkTable();
            bookmarkTable.pk = mItemEntity.getPk();
            bookmarkTable.title = mItemEntity.getTitle();
            bookmarkTable.save();
        }
    }

    @Override
    public void handlePlay() {

    }

    @Override
    public void handlePurchase() {
        Context context = mDetailView.getContext();
        Intent intent = new Intent();
        intent.putExtra("pk", mItemEntity.getPk());
        intent.putExtra("model", "item");

       intent.setClass(context, PaymentActivity.class);
        context.startActivity(intent);
    }
}
