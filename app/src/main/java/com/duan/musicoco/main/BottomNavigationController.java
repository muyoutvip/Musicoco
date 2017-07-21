package com.duan.musicoco.main;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.duan.musicoco.R;
import com.duan.musicoco.aidl.IPlayControl;
import com.duan.musicoco.aidl.Song;
import com.duan.musicoco.app.manager.BroadcastManager;
import com.duan.musicoco.db.DBMusicocoController;
import com.duan.musicoco.db.MainSheetHelper;
import com.duan.musicoco.db.Sheet;
import com.duan.musicoco.preference.PlayPreference;
import com.duan.musicoco.shared.ExceptionHandler;
import com.duan.musicoco.app.manager.MediaManager;
import com.duan.musicoco.app.SongInfo;
import com.duan.musicoco.app.interfaces.OnContentUpdate;
import com.duan.musicoco.app.interfaces.OnEmptyMediaLibrary;
import com.duan.musicoco.app.interfaces.OnPlayListVisibilityChange;
import com.duan.musicoco.app.interfaces.OnThemeChange;
import com.duan.musicoco.app.interfaces.OnUpdateStatusChanged;
import com.duan.musicoco.image.BitmapBuilder;
import com.duan.musicoco.play.PlayActivity;
import com.duan.musicoco.shared.PlayListAdapter;
import com.duan.musicoco.preference.AppPreference;
import com.duan.musicoco.preference.Theme;
import com.duan.musicoco.service.PlayController;
import com.duan.musicoco.service.PlayServiceCallback;
import com.duan.musicoco.shared.SongOperation;
import com.duan.musicoco.util.BitmapUtils;
import com.duan.musicoco.util.ColorUtils;
import com.duan.musicoco.shared.PeriodicTask;
import com.duan.musicoco.util.ToastUtils;
import com.duan.musicoco.util.Utils;
import com.duan.musicoco.view.PullDownLinearLayout;
import com.duan.musicoco.view.media.PlayView;

/**
 * Created by DuanJiaNing on 2017/6/27.
 */

public class BottomNavigationController implements
        OnPlayListVisibilityChange,
        View.OnClickListener,
        PlayServiceCallback,
        OnContentUpdate,
        OnEmptyMediaLibrary,
        OnThemeChange {

    private final Activity activity;
    private IPlayControl mControl;
    private final static String TAG = "BottomNavigationController";

    public final static String CURRENT_POSITION = "current_position";
    private int currentPosition;

    private View mContainer;
    private PullDownLinearLayout mListContainer;
    private View mProgress;
    private ImageView mAlbum;
    private TextView mName;
    private TextView mArts;
    private PlayView mPlay;
    private ImageButton mShowList;

    private final PeriodicTask task;

    private long mDuration;

    private final MediaManager mediaManager;
    private final AppPreference appPreference;
    private final PlayPreference playPreference;
    private BitmapBuilder builder;

    private final Dialog mDialog;
    private ListView mList;
    private View mLine;

    private ImageButton mPlayMode;
    private ImageButton mLocation;
    private TextView mSheet;

    private PlayListAdapter adapter;

    private boolean hasInitData = false;
    private DBMusicocoController dbController;

    BottomNavigationController(Activity activity, MediaManager mediaManager,
                               AppPreference appPreference, PlayPreference playPreference) {
        this.activity = activity;
        this.mediaManager = mediaManager;
        this.appPreference = appPreference;
        this.playPreference = playPreference;
        this.builder = new BitmapBuilder(activity);
        this.mDialog = new Dialog(activity, R.style.BottomDialog);

        task = new PeriodicTask(new PeriodicTask.Task() {
            @Override
            public void execute() {
                mContainer.post(new Runnable() {
                    @Override
                    public void run() {
                        updateProgress();
                    }
                });
            }
        }, 800);
    }

    public void initView() {
        mAlbum = (ImageView) activity.findViewById(R.id.list_album);
        mName = (TextView) activity.findViewById(R.id.list_name);
        mArts = (TextView) activity.findViewById(R.id.list_arts);
        mPlay = (PlayView) activity.findViewById(R.id.list_play);
        mShowList = (ImageButton) activity.findViewById(R.id.list_list);
        mContainer = activity.findViewById(R.id.list_bottom_nav_container);
        mProgress = activity.findViewById(R.id.list_progress);

        mContainer.setOnClickListener(this);
        mShowList.setOnClickListener(this);
        mPlay.setOnClickListener(this);

        initPlayListDialog();

    }

    private void initPlayListDialog() {
        View contentView = activity.getLayoutInflater().inflate(R.layout.main_play_list, null);
        mList = (ListView) contentView.findViewById(R.id.main_play_list);

        mListContainer = (PullDownLinearLayout) contentView.findViewById(R.id.main_play_list_container);
        mListContainer.isListViewExist(true);

        mLocation = (ImageButton) contentView.findViewById(R.id.main_play_location);
        mPlayMode = (ImageButton) contentView.findViewById(R.id.main_play_mode);
        mSheet = (TextView) contentView.findViewById(R.id.main_play_sheet);
        mLine = contentView.findViewById(R.id.main_play_line);


        Drawable drawable = activity.getResources().getDrawable(R.drawable.ic_location_searching_black_24dp);
        mLocation.setImageDrawable(drawable);

        mLocation.setOnClickListener(this);
        mPlayMode.setOnClickListener(this);

        mDialog.setContentView(contentView);
        ViewGroup.LayoutParams layoutParams = contentView.getLayoutParams();
        DisplayMetrics metrics = Utils.getMetrics(activity);
        layoutParams.width = metrics.widthPixels;
        layoutParams.height = metrics.heightPixels * 5 / 9;
        contentView.setLayoutParams(layoutParams);
        mDialog.getWindow().setGravity(Gravity.BOTTOM);
        mDialog.getWindow().setWindowAnimations(R.style.BottomDialog_Animation);
        mDialog.setCanceledOnTouchOutside(true);

    }

    @Override
    public void dataIsReady(IPlayControl control) {
        Log.d(TAG, "dataIsReady: ");
    }

    public void initData(IPlayControl control, DBMusicocoController dbMusicocoController) {
        this.mControl = control;
        this.dbController = dbMusicocoController;

        mContainer.setEnabled(true);
        mPlay.setEnabled(true);
        mShowList.setEnabled(true);

        adapter = new PlayListAdapter(
                activity,
                mControl,
                dbMusicocoController,
                new SongOperation(activity, mControl, dbMusicocoController));
        mList.setAdapter(adapter);

        Theme theme = appPreference.getTheme();
        themeChange(theme, null);

        hasInitData = true;

    }

    public boolean hasInitData() {
        return hasInitData;
    }

    @Override
    public void songChanged(Song song, int index, boolean isNext) {
        if (song == null) {
            return;
        }

        SongInfo info = mediaManager.getSongInfo(song);
        mDuration = (int) info.getDuration();

        update(null, null);
    }

    private void updateProgress() {
        int progress;
        final float phoneWidth = Utils.getMetrics(activity).widthPixels;

        try {
            progress = mControl.getProgress();
            int width = (int) (phoneWidth * (progress / (float) mDuration));
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mProgress.getLayoutParams();
            params.width = width;
            mProgress.setLayoutParams(params);
            mProgress.invalidate();

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void updateSongInfo(SongInfo info) {
        String name = info.getTitle();
        String arts = info.getArtist();
        builder.reset();
        Bitmap b = builder.setPath(info.getAlbum_path())
                .resize(mAlbum.getHeight())
                .build()
                .getBitmap();

        if (b == null) {
            b = BitmapUtils.getDefaultAlbumPicture(activity,
                    mAlbum.getWidth(),
                    mAlbum.getHeight());
        }

        mName.setText(name);
        mArts.setText(arts);
        mAlbum.setImageBitmap(b);

    }

    @Override
    public void update(@Nullable Object obj, OnUpdateStatusChanged completed) {
        if (checkNull()) {
            return;
        }

        updateCurrentSheet();
        updatePlayMode();

        try {

            if (mControl.status() == PlayController.STATUS_PLAYING) {
                mPlay.setPlayStatus(true);
            } else {
                mPlay.setPlayStatus(false);
            }

            Song song = mControl.currentSong();
            SongInfo info = mediaManager.getSongInfo(song);
            mDuration = (int) info.getDuration();
            updateProgress();
            updateSongInfo(info);

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void updateCurrentSheet() {
        try {
            int id = mControl.getPlayListId();
            if (id < 0) {
                String name = MainSheetHelper.getMainSheetName(activity, id);
                mSheet.setText(name);
            } else {
                Sheet sheet = dbController.getSheet(id);
                String name = "歌单：" + sheet.name + " (" + sheet.count + "首)";
                mSheet.setText(name);
            }

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private boolean checkNull() {
        if (mControl == null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void startPlay(Song song, int index, int status) {
        SongInfo info = mediaManager.getSongInfo(song);
        mDuration = (int) info.getDuration();

        task.start();
        mPlay.setChecked(true);

        BroadcastManager.sendMyBroadcast(activity, BroadcastManager.FILTER_MY_SHEET_CHANGED);
    }

    @Override
    public void stopPlay(Song song, int index, int status) {
        task.stop();
        mPlay.setChecked(false);

        BroadcastManager.sendMyBroadcast(activity, BroadcastManager.FILTER_MY_SHEET_CHANGED);
    }

    @Override
    public void onPlayListChange(Song current, int index, int id) {
        adapter.update(null, null);
        update(null, null);
        playPreference.updateSheet(id);

        BroadcastManager.sendMyBroadcast(activity, BroadcastManager.FILTER_MY_SHEET_CHANGED);
        Log.i(TAG, "onPlayListChange: main bottom");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.list_bottom_nav_container:
                activity.startActivity(new Intent(activity, PlayActivity.class));
                break;
            case R.id.list_play: {
                boolean play = mPlay.isChecked();
                try {
                    if (play) {
                        mControl.resume();
                    } else mControl.pause();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            }
            case R.id.list_list:
                if (mDialog.isShowing()) {
                    hide();
                } else {
                    show();
                }
                break;
            case R.id.main_play_mode:
                try {
                    int mode = mControl.getPlayMode();
                    mode = ((mode - 21) + 1) % 3 + 21;
                    mControl.setPlayMode(mode);

                    mode = updatePlayMode();
                    StringBuilder builder = new StringBuilder();
                    switch (mode) {
                        case PlayController.MODE_LIST_LOOP:
                            builder.append(activity.getString(R.string.play_mode_list_loop));
                            break;

                        case PlayController.MODE_SINGLE_LOOP:
                            builder.append(activity.getString(R.string.play_mode_single_loop));
                            break;

                        case PlayController.MODE_RANDOM:
                            builder.append(activity.getString(R.string.play_mode_random));
                            break;
                    }
                    ToastUtils.showShortToast(activity, builder.toString());
                } catch (RemoteException e) {
                    e.printStackTrace();
                    new ExceptionHandler().handleRemoteException(activity,
                            activity.getString(R.string.exception_remote), null
                    );
                }
                break;
            case R.id.main_play_location:
                try {
                    int index = mControl.currentSongIndex();
                    mList.smoothScrollToPosition(index);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    new ExceptionHandler().handleRemoteException(activity,
                            activity.getString(R.string.exception_remote), null
                    );
                }
                break;
        }
    }

    @Override
    public void themeChange(Theme theme, int[] colors) {

        int[] cs = new int[4];
        if (theme == Theme.DARK) {
            cs = ColorUtils.get4DarkThemeColors(activity);
        } else if (theme == Theme.WHITE) {
            cs = ColorUtils.get4WhiteThemeColors(activity);
        } else return;

        int mainBC = cs[0];
        int mainTC = cs[1];
        int vicBC = cs[2];
        int vicTC = cs[3];

        if (adapter != null) {
            adapter.themeChange(theme, new int[]{mainTC, vicTC});
        }

        mContainer.setBackgroundColor(mainBC);

        mName.setTextColor(mainTC);
        mArts.setTextColor(vicTC);

        mPlay.setPauseLineColor(mainTC);
        mPlay.setSolidColor(mainTC);
        mPlay.setTriangleColor(mainTC);
        mProgress.setBackgroundColor(mainTC);
        mList.setBackgroundColor(mainBC);
        mLine.setBackgroundColor(vicTC);
        mSheet.setTextColor(mainTC);

        mListContainer.setBackgroundColor(mainBC);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mShowList.getDrawable().setTint(mainTC);

            mPlayMode.getDrawable().setTint(vicTC);
            mLocation.getDrawable().setTint(vicTC);

        }
    }

    private int updatePlayMode() {

        Drawable drawable = null;
        StringBuilder builder = new StringBuilder();
        int mode = PlayController.MODE_LIST_LOOP;

        try {
            mode = mControl.getPlayMode();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        switch (mode) {
            case PlayController.MODE_SINGLE_LOOP:
                drawable = activity.getResources().getDrawable(R.drawable.single_loop);
                builder.append(activity.getString(R.string.play_mode_single_loop));
                break;

            case PlayController.MODE_RANDOM:
                drawable = activity.getResources().getDrawable(R.drawable.random);
                builder.append(activity.getString(R.string.play_mode_random));
                break;

            case PlayController.MODE_LIST_LOOP:
            default:
                drawable = activity.getResources().getDrawable(R.drawable.list_loop);
                builder.append(activity.getString(R.string.play_mode_list_loop));
                break;

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawable.setTint(adapter.getColorVic());
        }
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        mPlayMode.setImageDrawable(drawable);

        return mode;
    }

    @Override
    public void show() {

        if (mDialog.isShowing()) {
            return;
        } else {
            mDialog.show();
            mList.setSelection(currentPosition);
        }
    }

    @Override
    public void hide() {
        if (mDialog.isShowing()) {
            mDialog.dismiss();
            currentPosition = mList.getFirstVisiblePosition();
        }

    }

    @Override
    public void emptyMediaLibrary() {
        mContainer.setEnabled(false);
        mPlay.setEnabled(false);
        mShowList.setEnabled(false);
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(int currentPosition) {
        this.currentPosition = currentPosition;
    }

}