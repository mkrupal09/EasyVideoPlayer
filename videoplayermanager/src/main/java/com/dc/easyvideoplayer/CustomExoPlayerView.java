package com.dc.easyvideoplayer;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupWindow;

import androidx.core.view.GestureDetectorCompat;

import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;
import com.dc.easyvideoplayer.databinding.PopupSpeedOptionBinding;

/**
 * Custom player class for Double-Tapping listening
 */
public final class CustomExoPlayerView extends PlayerView {

    public interface OnPinchListener {
        public void onPinchZoom();

        public void onPinchZoomOut();
    }

    public static final String TAG = ".DoubleTapPlayerView";
    public static boolean DEBUG = !BuildConfig.BUILD_TYPE.equals("release");

    private boolean doubleTapActivated = true;

    private GestureDetectorCompat mDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private OnPinchListener onPinchListener;


    PlayerDoubleTapListener controls;

    // Variable to save current state
    private boolean isDoubleTap = false;

    private Handler mHandler = new Handler();
    private Runnable mRunnable = () -> {
        Log.d(TAG, "Runnable called");
        isDoubleTap = false;
        controls.onDoubleTapFinished();
    };

    /**
     * Default time window in which the double tap is active
     * Resets if another tap occurred within the time window by calling
     * {@link CustomExoPlayerView#keepInDoubleTapMode()}
     **/
    long doubleTapDelay = 650;

    public CustomExoPlayerView(Context context) {
        this(context, null);
    }

    public CustomExoPlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomExoPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDetector = new GestureDetectorCompat(context, new DoubleTapGestureListener());
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureListener());
        init();

    }

    private float currentVolume;

    private void init() {
        ImageView ivMute = findViewById(R.id.ivMute);
        ivMute.setOnClickListener(v -> {
            if (getPlayer() != null) {
                if (getPlayer().getVolume() == 0.0f) {
                    ivMute.setImageResource(R.drawable.ic_unmute);
                    getPlayer().setVolume(currentVolume);
                } else {
                    currentVolume = getPlayer().getVolume();
                    ivMute.setImageResource(R.drawable.ic_mute);
                    getPlayer().setVolume(0.0f);
                }
            }
        });

        findViewById(R.id.exo_speed).setOnClickListener(this::showSpeedPopup);

        findViewById(R.id.exo_fullscreen_button).setOnClickListener(v -> {
            VideoPlayerConfig videoPlayerConfig = new VideoPlayerConfig.Builder().videoPath(url)
                    .orientation(VideoPlayerConfig.ORIENTATION_USER_ORIENTATION)
                    .setStartTime(getPlayer().getCurrentPosition())
                    .autoPlay(true)
                    .build();
            getContext().startActivity(VideoPlayerActivity.Companion.createIntent(getContext(), videoPlayerConfig));
        });

        setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS);

    }

    private void showSpeedPopup(View view) {
        PopupWindow popup = new PopupWindow(this);
        popup.setBackgroundDrawable(new BitmapDrawable());
        popup.setOutsideTouchable(true);
        PopupSpeedOptionBinding bindingX = PopupSpeedOptionBinding.inflate(LayoutInflater.from(getContext()), this, false);
        popup.setContentView(bindingX.getRoot());
        popup.showAtLocation(view, Gravity.TOP, (int) view.getX() - 100, (int) view.getY() - 50);
        popup.setOutsideTouchable(true);

        bindingX.onex.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                popup.dismiss();
                getPlayerEx().setPlaybackParameters(new PlaybackParameters(1.0f));
                /*invalidateSpeedTextView(1.0f)    */
            }
        });

        bindingX.onepointfivex.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                popup.dismiss();
                getPlayerEx().setPlaybackParameters(new PlaybackParameters(1.5f));
            }
        });

        bindingX.twopointzero.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                popup.dismiss();
                getPlayerEx().setPlaybackParameters(new PlaybackParameters(2.0f));
            }
        });

        bindingX.pointfive.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                popup.dismiss();
                getPlayerEx().setPlaybackParameters(new PlaybackParameters(0.5f));
            }
        });
    }

    /**
     * Sets whether the PlayerView recognizes double tap gestures or not
     */
    public CustomExoPlayerView activateDoubleTap(boolean active) {
        this.doubleTapActivated = active;
        return this;
    }

    /**
     * Sets {@link PlayerDoubleTapListener} for custom implementation
     */
    public CustomExoPlayerView setDoubleTapListener(PlayerDoubleTapListener layout) {
        if (layout != null) {
            controls = layout;
        }
        return this;
    }

    /**
     * Changes the time window a double tap is active, so a followed tap is calling
     * a gesture detector method instead of normal tap (see {@link PlayerView#onTouchEvent})
     */
    public CustomExoPlayerView setDoubleTapDelay(int milliSeconds) {
        this.doubleTapDelay = milliSeconds;
        return this;
    }

    public long getDoubleTapDelay() {
        return this.doubleTapDelay;
    }

    /**
     * Resets the timeout to keep in double tap mode.
     * <p>
     * Called once in {@link PlayerDoubleTapListener#onDoubleTapStarted} Needs to be called
     * from outside if the double tap is customized / overridden to detect ongoing taps
     */
    public void keepInDoubleTapMode() {
        isDoubleTap = true;
        mHandler.removeCallbacks(mRunnable);
        mHandler.postDelayed(mRunnable, doubleTapDelay);
    }

    /**
     * Cancels double tap mode instantly by calling {@link PlayerDoubleTapListener#onDoubleTapFinished()}
     */
    public void cancelInDoubleTapMode() {
        mHandler.removeCallbacks(mRunnable);
        isDoubleTap = false;
        controls.onDoubleTapFinished();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (doubleTapActivated) {
            mDetector.onTouchEvent(ev);
            scaleGestureDetector.onTouchEvent(ev);


            // Do not trigger original behavior when double tapping
            // otherwise the controller would show/hide - it would flack
            return true;
        }

        return super.onTouchEvent(ev);
    }

    private class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            /*Toast.makeText(getContext(),detector.,Toast.LENGTH_SHORT).show();*/
            if (detector.getScaleFactor() > 1.0) {
                onPinchListener.onPinchZoom();
            } else if (detector.getScaleFactor() < 1.0) {
                onPinchListener.onPinchZoomOut();
            }
            return super.onScale(detector);
        }
    }

    /**
     * Gesture Listener for double tapping
     * <p>
     * For more information which methods are called in certain situations look for
     * {@link GestureDetectorCompat#onTouchEvent}, especially for ACTION_DOWN and ACTION_UP
     */
    private class DoubleTapGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            // Used to override the other methods
            if (isDoubleTap) {
                controls.onDoubleTapProgressDown(e.getX(), e.getY());
            }
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (isDoubleTap) {
                if (DEBUG) Log.d(TAG, "onSingleTapUp: isDoubleTap = true");
                controls.onDoubleTapProgressUp(e.getX(), e.getY());
            }

            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            // Ignore this event if double tapping is still active
            // Return true needed because this method is also called if you tap e.g. three times
            // in a row, therefore the controller would appear since the original behavior is
            // to hide and show on single tap
            if (isDoubleTap) return true;
            if (DEBUG) Log.d(TAG, "onSingleTapConfirmed: isDoubleTap = false");

            return performClick();
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // First tap (ACTION_DOWN) of both taps
            if (DEBUG) Log.d(TAG, "onDoubleTap");

            if (!isDoubleTap) {
                isDoubleTap = true;
                keepInDoubleTapMode();
                controls.onDoubleTapStarted(e.getX(), e.getY());
            }
            return true;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            // Second tap (ACTION_UP) of both taps
            if (e.getActionMasked() == MotionEvent.ACTION_UP && isDoubleTap) {
                if (DEBUG) Log.d(TAG, "onDoubleTapEvent, ACTION_UP");
                controls.onDoubleTapProgressUp(e.getX(), e.getY());

                return true;
            }
            return super.onDoubleTapEvent(e);
        }
    }

    public void setOnPinchListener(OnPinchListener onPinchListener) {
        this.onPinchListener = onPinchListener;
    }

    public Player getPlayerEx() {
        getPlayer().setPlayWhenReady(true);
        return getPlayer();
    }

    public void playWhenReady() {
        getPlayer().setPlayWhenReady(true);
    }

    private String url;

    public void setUrl(String url) {
        this.url = url;
    }
}
