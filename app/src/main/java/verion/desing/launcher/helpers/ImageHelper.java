package verion.desing.launcher.helpers;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

import javax.inject.Singleton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import dagger.Module;
import dagger.Provides;

@Module
public class ImageHelper {
    private Target<Drawable> target;
    private int tries = 0;

    @Provides
    @Singleton
    ImageHelper provideImageHelper() {
        return new ImageHelper();
    }

    public void setBackgroundFromURL(View view, String url) {
        if (target == null) {
            target =
                    Glide.with(view)
                            .load(url)
                            .into(new Target<Drawable>() {

                                @Override
                                public void onStart() {

                                }

                                @Override
                                public void onStop() {

                                }

                                @Override
                                public void onDestroy() {

                                }

                                @Override
                                public void onLoadStarted(@Nullable Drawable placeholder) {

                                }

                                @Override
                                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                    if (tries < 2) {
                                        setBackgroundFromURL(view, url);
                                        tries = 0;
                                    }
                                    tries++;
                                }

                                @Override
                                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {

                                }

                                @Override
                                public void getSize(@NonNull SizeReadyCallback cb) {
                                    cb.onSizeReady(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);

                                }

                                @Override
                                public void removeCallback(@NonNull SizeReadyCallback cb) {

                                }

                                @Nullable
                                @Override
                                public Request getRequest() {
                                    return null;
                                }

                                @Override
                                public void setRequest(@Nullable Request request) {

                                }
                            });
        }

        Glide.with(view)
                .load(url)
                .into(target);


    }


    public void loadRoundCorner(String url, ImageView view) {
        Context context = view.getContext();
        Glide.with(context).load(url)
                .apply(new RequestOptions()
                        .transforms(new CenterCrop(), new RoundedCorners(context, 0, 0))
                        .dontAnimate().diskCacheStrategy(DiskCacheStrategy.NONE)).into(view);
    }

    public void loadRoundCorner(String url, ImageButton view) {

        Context context = view.getContext();

        Glide.with(context).load(url)
                .apply(new RequestOptions()
//                        .transforms(new CenterCrop(), new RoundedCorners(context, 0, 0))
                        .dontAnimate().diskCacheStrategy(DiskCacheStrategy.NONE)).into(view);
    }

    public void loadRoundCornerAdapter(String url, ImageButton view) {

        Context context = view.getContext();

        Glide.with(context).load(url)
                .apply(new RequestOptions()
                        .transforms(new CenterCrop(), new RoundedCorners(context, 4, 4))
                        .dontAnimate().diskCacheStrategy(DiskCacheStrategy.NONE)).into(view);
    }

    public void loadRoundCornerFragment(String url, ImageButton view, Fragment f) {

        Context context = view.getContext();
        Glide.with(f).load(url)
                .apply(new RequestOptions()
                        //.transforms(new CenterCrop(), new RoundedCorners(context, 0, 0))
                        .dontAnimate().diskCacheStrategy(DiskCacheStrategy.NONE)).into(view);
    }

    public void loadRoundCornerFragmentDrawable(int url, ImageButton view, Fragment f) {

        Context context = view.getContext();
        Glide.with(f).load(url)
                .apply(new RequestOptions()
                        //.transforms(new CenterCrop(), new RoundedCorners(context, 0, 0))
                        .dontAnimate().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)).into(view);
    }

    public void loadSquareFragment(String url, ImageView view, Fragment f) {
        Glide.with(f).load(url)
                .apply(new RequestOptions()
                        .dontAnimate().diskCacheStrategy(DiskCacheStrategy.NONE)).into(view);
    }


    public void loadRoundCornerCache(String url, ImageButton view) {


        Context context = view.getContext();

        Glide.with(context).load(url)
                .apply(new RequestOptions()
                        .transforms(new CenterCrop(), new RoundedCorners(context, 0, 0))
                        .dontAnimate().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)).into(view);
    }

    public void loadSquare(String url, ImageView view) {
        Context context = view.getContext();
        Glide.with(context).load(url)
                .apply(new RequestOptions()
                        .dontAnimate().diskCacheStrategy(DiskCacheStrategy.NONE)).into(view);
    }

    public void loadSquare(int id, ImageView view) {
        Context context = view.getContext();
        Glide.with(context).load(id)
                .apply(new RequestOptions()
                        .dontAnimate().diskCacheStrategy(DiskCacheStrategy.NONE)).into(view);
    }

    public void loadRoundCornerCacheResize(String url, ImageButton view) {

        Context context = view.getContext();
        Glide.with(context).load(url)
                .apply(new RequestOptions()
                        .override(100, 100) // resizes the image to these dimensions (in pixel). resize does not respect aspect ratio
                        .transforms(new CenterCrop(), new RoundedCorners(context, 0, 0))
                        .dontAnimate().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)).into(view);
    }

}
