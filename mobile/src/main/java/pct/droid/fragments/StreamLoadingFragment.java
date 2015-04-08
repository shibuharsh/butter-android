/*
 * This file is part of Popcorn Time.
 *
 * Popcorn Time is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Popcorn Time is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Popcorn Time. If not, see <http://www.gnu.org/licenses/>.
 */

package pct.droid.fragments;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.text.DecimalFormat;

import butterknife.ButterKnife;
import butterknife.InjectView;
import pct.droid.R;
import pct.droid.activities.BeamPlayerActivity;
import pct.droid.activities.VideoPlayerActivity;
import pct.droid.base.connectsdk.BeamManager;
import pct.droid.base.fragments.BaseStreamLoadingFragment;
import pct.droid.base.preferences.DefaultPlayer;
import pct.droid.base.providers.media.models.Media;
import pct.droid.base.torrent.DownloadStatus;
import pct.droid.base.torrent.StreamInfo;
import pct.droid.base.utils.PixelUtils;
import pct.droid.base.utils.ThreadUtils;
import pct.droid.base.utils.VersionUtils;
import timber.log.Timber;

public class StreamLoadingFragment extends BaseStreamLoadingFragment {

    private boolean mAttached = false;
    private Context mContext;

    View mRoot;
    @InjectView(R.id.progress_indicator)
    ProgressBar mProgressIndicator;
    @InjectView(R.id.primary_textview)
    TextView mPrimaryTextView;
    @InjectView(R.id.secondary_textview)
    TextView mSecondaryTextView;
    @InjectView(R.id.tertiary_textview)
    TextView mTertiaryTextView;
    @InjectView(R.id.background_imageview)
    ImageView mBackgroundImageView;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRoot = inflater.inflate(R.layout.fragment_streamloading, container, false);
        ButterKnife.inject(this, mRoot);

        if (VersionUtils.isLollipop()) {
            //postpone the transitions until after the view is layed out.
            getActivity().postponeEnterTransition();

            mRoot.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                public boolean onPreDraw() {
                    mRoot.getViewTreeObserver().removeOnPreDrawListener(this);
                    getActivity().startPostponedEnterTransition();
                    return true;
                }
            });
        }

        return mRoot;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mAttached = true;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mAttached = false;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mContext = getActivity();
        loadBackgroundImage();
    }

    private void loadBackgroundImage() {
        StreamInfo info = mCallback.getStreamInformation();
          /* attempt to load background image */
        if (null != info) {
            Media media = info.isShow() ? info.getShow() : info.getMedia();
            if(media != null) {
                String url = media.image;
                if (PixelUtils.isTablet(getActivity())) {
                    url = media.headerImage;
                }

                if (!TextUtils.isEmpty(url))
                    Picasso.with(getActivity()).load(url).error(R.color.bg).into(mBackgroundImageView);
            }
        }
    }

    private void updateStatus(final DownloadStatus status) {
        if (!mAttached) return;

        final DecimalFormat df = new DecimalFormat("#############0.00");
        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressIndicator.setIndeterminate(false);
                mProgressIndicator.setProgress(status.bufferProgress);
                mPrimaryTextView.setText(status.bufferProgress + "%");

                if (status.downloadSpeed / 1024 < 1000) {
                    mSecondaryTextView.setText(df.format(status.downloadSpeed / 1024) + " KB/s");
                } else {
                    mSecondaryTextView.setText(df.format(status.downloadSpeed / 1048576) + " MB/s");
                }
                mTertiaryTextView.setText(status.seeds + " " + getString(R.string.seeds));
            }
        });
    }


    @Override
    protected void updateView(State state, Object extra) {
        switch (state) {
            case UNINITIALISED:
                mTertiaryTextView.setText(null);
                mPrimaryTextView.setText(null);
                mSecondaryTextView.setText(null);
                mProgressIndicator.setIndeterminate(true);
                mProgressIndicator.setProgress(0);
                break;
            case ERROR:
                if (null != extra && extra instanceof String)
                    mPrimaryTextView.setText((String) extra);
                mSecondaryTextView.setText(null);
                mTertiaryTextView.setText(null);
                mProgressIndicator.setIndeterminate(true);
                mProgressIndicator.setProgress(0);
                break;
            case BUFFERING:
                mPrimaryTextView.setText(R.string.starting_buffering);
                mTertiaryTextView.setText(null);
                mSecondaryTextView.setText(null);
                mProgressIndicator.setIndeterminate(true);
                mProgressIndicator.setProgress(0);
                break;
            case STREAMING:
                mPrimaryTextView.setText(R.string.streaming_started);
                if (null != extra && extra instanceof DownloadStatus)
                    updateStatus((DownloadStatus) extra);
                break;
            case WAITING_SUBTITLES:
                mPrimaryTextView.setText(R.string.waiting_for_subtitles);
                mTertiaryTextView.setText(null);
                mSecondaryTextView.setText(null);
                mProgressIndicator.setIndeterminate(true);
                mProgressIndicator.setProgress(0);
                break;
            case WAITING_TORRENT:
                mPrimaryTextView.setText(R.string.waiting_torrent);
                mTertiaryTextView.setText(null);
                mSecondaryTextView.setText(null);
                mProgressIndicator.setIndeterminate(true);
                mProgressIndicator.setProgress(0);
                break;

        }
    }

    @Override
    protected void startPlayerActivity(String location, int resumePosition) {
        Timber.d("startVideoPlayer");
        mStreamInfo.setVideoLocation(location);
        if(BeamManager.getInstance(mContext).isConnected()) {
            BeamPlayerActivity.startActivity(mContext, mStreamInfo, resumePosition);
        } else {
            boolean playingExternal = DefaultPlayer.start(mStreamInfo.getMedia(), mStreamInfo.getSubtitleLanguage(), location);
            if (!playingExternal) {
                VideoPlayerActivity.startActivity(mContext, mStreamInfo, resumePosition);
            }
        }

        setRetainInstance(false);
        mCallback.playerStarted();
    }
}
