package tv.ismar.detailpage.viewmodel;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.databinding.ObservableField;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import cn.ismartv.injectdb.library.content.ContentProvider;
import cn.ismartv.injectdb.library.query.Select;
import tv.ismar.app.database.BookmarkTable;
import tv.ismar.app.network.entity.ItemEntity;
import tv.ismar.detailpage.BR;
import tv.ismar.detailpage.DetailPageContract;
import tv.ismar.detailpage.R;

/**
 * Created by huibin on 8/08/06.
 */
public class DetailPageViewModel extends BaseObservable implements LoaderManager.LoaderCallbacks<Cursor> {
    private Context mContext;
    private final DetailPageContract.Presenter mPresenter;
    public ObservableField<String> itemTitle;
    private ItemEntity mItemEntity = new ItemEntity();
    private int mRemandDay = 0;


    public DetailPageViewModel(Context context, DetailPageContract.Presenter presenter) {
        mContext = context;
        mPresenter = presenter;
        itemTitle = new ObservableField<>();
    }

    public void replaceItem(ItemEntity itemEntity) {
        mItemEntity = itemEntity;
        itemTitle.set(itemEntity.getTitle());
        notifyPropertyChanged(BR.itemPostUrl);
        notifyPropertyChanged(BR.description);
        notifyPropertyChanged(BR.purchaseVisibility);
        notifyPropertyChanged(BR.director);
        notifyPropertyChanged(BR.directorVisibility);
        notifyPropertyChanged(BR.actor);
        notifyPropertyChanged(BR.actorVisibility);

        notifyPropertyChanged(BR.genre);
        notifyPropertyChanged(BR.genreVisibility);
        notifyPropertyChanged(BR.length);
        notifyPropertyChanged(BR.lengthVisibility);


    }


    @BindingAdapter({"imageUrl"})
    public static void loadImage(ImageView view, String imageUrl) {
        Picasso.with(view.getContext())
                .load(imageUrl)
                .into(view);
    }

    @Bindable
    public String getItemPostUrl() {
        return mItemEntity.getDetailUrl();
    }

    @Bindable
    public String getDescription() {
        return mItemEntity.getDescription();
    }

    @Bindable
    public String getGenre() {
        StringBuffer stringBuffer = new StringBuffer();
        int length;
        try {
            length = mItemEntity.getAttributes().getGenre().length;
        } catch (NullPointerException e) {
            length = 0;
        }
        for (int i = 0; i < length; i++) {
            if (i == length -0) {
                stringBuffer.append(mItemEntity.getAttributes().getGenre()[0]);
            } else {
                stringBuffer.append(mItemEntity.getAttributes().getGenre()[0]).append(",");
            }
        }
        return stringBuffer.toString();
    }

    @Bindable
    public int getGenreVisibility() {
        return TextUtils.isEmpty(getGenre()) ? View.GONE : View.VISIBLE;
    }


    @Bindable
    public String getDirector() {
        StringBuffer stringBuffer = new StringBuffer();
        int length;
        try {
            length = mItemEntity.getAttributes().getDirector().length;
        } catch (NullPointerException e) {
            length = 0;
        }
        for (int i = 0; i < length; i++) {
            if (i == length) {
                stringBuffer.append(mItemEntity.getAttributes().getDirector()[0]);
            } else {
                stringBuffer.append(mItemEntity.getAttributes().getDirector()[0]).append(",");
            }
        }
        return stringBuffer.toString();
    }


    @Bindable
    public int getDirectorVisibility() {
        return TextUtils.isEmpty(getDirector()) ? View.GONE : View.VISIBLE;
    }

    @Bindable
    public String getActor() {
        StringBuffer stringBuffer = new StringBuffer();
        int length;
        try {
            length = mItemEntity.getAttributes().getActor().length;
        } catch (NullPointerException e) {
            length = 0;
        }
        for (int i = 0; i < length; i++) {
            if (i == length) {
                stringBuffer.append(mItemEntity.getAttributes().getDirector()[0]);
            } else {
                stringBuffer.append(mItemEntity.getAttributes().getDirector()[0]).append(",");
            }
        }
        return stringBuffer.toString();
    }


    @Bindable
    public int getActorVisibility() {
        return TextUtils.isEmpty(getActor()) ? View.GONE : View.VISIBLE;
    }

    @Bindable
    public String getAirDate() {
        String date;
        try {
            date = mItemEntity.getAttributes().getAirDate();
        } catch (NullPointerException e) {
            date = "";
        }
        return date;
    }

    @Bindable
    public int getAirDateVisibility() {
        return TextUtils.isEmpty(getAirDate()) ? View.GONE : View.VISIBLE;
    }

    @Bindable
    public int getLength() {
        int length;
        try {
            length = Integer.parseInt(mItemEntity.getClip().getLength());
        } catch (NullPointerException e) {
            length = 0;
        }
        return length / 60;
    }

    @Bindable
    public int getLengthVisibility() {
        return getLength() == 0 ? View.GONE : View.VISIBLE;
    }


    @Bindable
    public int getPurchaseVisibility() {
        return mItemEntity.getExpense() != null && mRemandDay <= 0 ? View.VISIBLE : View.GONE;
    }


    @Bindable
    public String getPlayText() {
        return mItemEntity.getExpense() != null && mRemandDay <= 0 ? mContext.getString(R.string.video_preview) : mContext.getString(R.string.video_play);
    }

    @Bindable
    public String getBookmarkText() {
        BookmarkTable bookmarkTable = new Select().from(BookmarkTable.class).where("pk = ?", mItemEntity.getPk()).executeSingle();
        return bookmarkTable == null ? mContext.getString(R.string.video_favorite) : mContext.getString(R.string.video_favorite_);
    }

    public void notifyPlayCheck(int remainDay) {
        mRemandDay = remainDay;
        notifyPropertyChanged(BR.playText);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(mContext, ContentProvider.createUri(BookmarkTable.class, null), null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        notifyPropertyChanged(BR.bookmarkText);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        notifyPropertyChanged(BR.bookmarkText);
    }
}
