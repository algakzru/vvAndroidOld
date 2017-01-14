/*
 * Copyright 2012 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kz.algakzru.youtubevideovocabulary;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewPropertyAnimator;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeApiServiceUtil;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.OnFullscreenListener;
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerFragment;
import com.youku.player.base.YoukuBasePlayerManager;
import com.youku.player.base.YoukuPlayer;
import com.youku.player.base.YoukuPlayerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import kz.algakzru.youtubevideovocabulary.mnemogogo.CardDatabase;
import kz.algakzru.youtubevideovocabulary.util.OverlayService;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * A sample Activity showing how to manage multiple YouTubeThumbnailViews in an adapter for display
 * in a List. When the list items are clicked, the video is played by using a YouTubePlayerFragment.
 * <p>
 * The demo supports custom fullscreen and transitioning between portrait and landscape without
 * rebuffering.
 */
@TargetApi(13)
public final class VideoListActivityYouku extends Activity implements OnFullscreenListener {

    /** The duration of the animation sliding up the video in portrait. */
    private static final int ANIMATION_DURATION_MILLIS = 300;
    /** The padding between the video list and the video in landscape orientation. */
    private static final int LANDSCAPE_VIDEO_PADDING_DP = 0;

    /** The request code when calling startActivityForResult to recover from an API service error. */
    private static final int RECOVERY_DIALOG_REQUEST = 1;

    private YoukuBasePlayerManager basePlayerManager;
    // 播放器控件
    private YoukuPlayerView mYoukuPlayerView;
    // YoukuPlayer实例，进行视频播放控制
    private YoukuPlayer youkuPlayer;

    private ListView listFragment;
    private LinearLayout videoFragment;

    private View blueText;
    private View videoBox;
    private TextView actionBarTitle;
    private Switch subtitlesSwitch;

    private boolean isFullscreen;
    private List<VideoId> videoIds;
    private int currentVideoPosition = -1;
    private String currentWord = "";
    private String wordToTranslate = "";
    private int currentPosition = -1;

    private List<VideoEntry> WORD_LIST = new ArrayList<VideoEntry>();
    private PageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.video_list_demo_youku);
        actionBarTitle = MainActivity.setActionBar(this, false);
        actionBarTitle.setText(R.string.app_name);

        basePlayerManager = new YoukuBasePlayerManager(this) {

            @Override
            public void setPadHorizontalLayout() {
                // TODO Auto-generated method stub

            }

            @Override
            public void onInitializationSuccess(YoukuPlayer player) {
                // TODO Auto-generated method stub
                // 初始化成功后需要添加该行代码
                addPlugins();

                // 实例化YoukuPlayer实例
                youkuPlayer = player;
            }

            @Override
            public void onSmallscreenListener() {
                // TODO Auto-generated method stub

            }

            @Override
            public void onFullscreenListener() {
                // TODO Auto-generated method stub

            }
        };
        basePlayerManager.onCreate();

        listFragment = (ListView) findViewById(R.id.list_fragment);
        videoFragment = (LinearLayout) findViewById(R.id.video_fragment_container);

        listFragment.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        WORD_LIST.clear();
        CardDatabase cardDatabase = new CardDatabase(this);
        SQLiteDatabase db = cardDatabase.getReadableDatabase();
        String selection = cardDatabase.getSelected_categories();
        Cursor cur = db.query("word", null, selection, null, null, null, "grade DESC");
        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                if ("完整视频".equals(cur.getString(cur.getColumnIndex("word")))) {
                    WORD_LIST.add(0, new VideoEntry(cur.getString(cur
                            .getColumnIndex("word")), cur.getString(cur
                            .getColumnIndex("old_id"))));
                } else {
                    WORD_LIST.add(new VideoEntry(cur.getString(cur
                            .getColumnIndex("word")), cur.getString(cur
                            .getColumnIndex("old_id"))));
                }
            }
        }
        cardDatabase.close();

        adapter = new PageAdapter(this, WORD_LIST);
        listFragment.setAdapter(adapter);
        registerForContextMenu(listFragment);
        listFragment.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                currentPosition = position;
                currentWord = WORD_LIST.get(currentPosition).word;

                videoIds = getVideoIds(currentPosition);
                currentVideoPosition = new Random().nextInt((videoIds.size() - 1) + 1);
                if (currentVideoPosition > -1) {
                    playYoukuVideo();

                    // The videoBox is INVISIBLE if no video was previously selected, so we need to show it now.
                    if (videoBox.getVisibility() != View.VISIBLE) {
                        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                            // Initially translate off the screen so that it can be animated in from below.
                            videoBox.setTranslationY(videoBox.getHeight());
                        }
                        videoBox.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
        subtitlesSwitch = (Switch) findViewById(R.id.subtitlesSwitch);
        subtitlesSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (currentVideoPosition > -1) {
                    playYoukuVideo();
                }
            }
        });

        blueText = findViewById(R.id.blue_text);
        videoBox = findViewById(R.id.video_box);

        videoBox.setVisibility(View.INVISIBLE);

        layout();

        // 播放器控件
        mYoukuPlayerView = (YoukuPlayerView) this
                .findViewById(R.id.full_holder);
        //控制竖屏和全屏时候的布局参数。这两句必填。
        mYoukuPlayerView
                .setSmallScreenLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
        mYoukuPlayerView
                .setFullScreenLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT));
        // 初始化播放器相关数据
        mYoukuPlayerView.initialize(basePlayerManager);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            currentPosition = extras.getInt("position");
            currentWord = WORD_LIST.get(currentPosition).word;
            listFragment.setSelection(currentPosition);
            listFragment.setItemChecked( currentPosition, true );

            videoIds = getVideoIds(currentPosition);
            currentVideoPosition = extras.getInt("currentVideoPosition");
            if (currentVideoPosition > -1) {
                playYoukuVideo();

                // The videoBox is INVISIBLE if no video was previously selected, so we need to show it now.
                if (videoBox.getVisibility() != View.VISIBLE) {
                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                        // Initially translate off the screen so that it can be animated in from below.
                        videoBox.setTranslationY(videoBox.getHeight());
                    }
                    videoBox.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_select_wordlist:
                showDialogSelectWordlist();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        currentPosition = info.position;
        currentWord = WORD_LIST.get(currentPosition).word;
        if (listFragment.getCheckedItemPosition() != info.position) {
            videoIds = getVideoIds(currentPosition);
            currentVideoPosition = new Random().nextInt((videoIds.size() - 1) + 1);
        }
        menu.setHeaderTitle(currentWord);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_video_list, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_pleco:
//                String url = "plecoapi://x-callback-url/s?q=" + currentWord;
//                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//                startActivity(browserIntent);
                break;
            case R.id.menu_goldendict:
                Intent intent = new Intent("colordict.intent.action.SEARCH");
                intent.putExtra("EXTRA_QUERY", currentWord); //Search Query
                intent.putExtra("EXTRA_FULLSCREEN", true); //
                startActivity(intent);
                break;
        }
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            basePlayerManager.onConfigurationChanged(newConfig);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        basePlayerManager.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        basePlayerManager.onPause();
//        if (!currentWord.isEmpty())
//            startService(new Intent(VideoListActivityYouku.this, OverlayService.class).putExtra("word", currentWord).putExtra("position", currentPosition).putExtra("currentVideoPosition", currentVideoPosition));
    }

    @Override
    protected void onResume() {
        super.onResume();
        basePlayerManager.onResume();
//        stopService(new Intent(VideoListActivityYouku.this, OverlayService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        basePlayerManager.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        basePlayerManager.onStop();

    }

    @Override
    public void onFullscreen(boolean isFullscreen) {
        this.isFullscreen = isFullscreen;

        layout();
    }

    /**
     * Sets up the layout programatically for the three different states. Portrait, landscape or
     * fullscreen+landscape. This has to be done programmatically because we handle the orientation
     * changes ourselves in order to get fluent fullscreen transitions, so the xml layout resources
     * do not get reloaded.
     */
    private void layout() {
        boolean isPortrait =
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        listFragment.setVisibility(isFullscreen ? View.GONE : View.VISIBLE);
//        listFragment.setLabelVisibility(isPortrait);

        if (isFullscreen) {
            videoBox.setTranslationY(0); // Reset any translation that was applied in portrait.
            setLayoutSize(videoFragment, MATCH_PARENT, MATCH_PARENT);
            setLayoutSizeAndGravity(videoBox, MATCH_PARENT, MATCH_PARENT, Gravity.TOP | Gravity.LEFT);
        } else if (isPortrait) {
            setLayoutSize(listFragment, MATCH_PARENT, MATCH_PARENT);
            setLayoutSize(videoFragment, MATCH_PARENT, WRAP_CONTENT);
            setLayoutSizeAndGravity(videoBox, MATCH_PARENT, WRAP_CONTENT, Gravity.BOTTOM);
        } else {
            videoBox.setTranslationY(0); // Reset any translation that was applied in portrait.
            int screenWidth = dpToPx(getResources().getConfiguration().screenWidthDp);
            setLayoutSizeAndGravity(listFragment, screenWidth / 4, MATCH_PARENT, Gravity.RIGHT);
            int videoWidth = screenWidth - screenWidth / 4 - dpToPx(LANDSCAPE_VIDEO_PADDING_DP);
            setLayoutSize(videoFragment, videoWidth, MATCH_PARENT);
            setLayoutSizeAndGravity(videoBox, videoWidth, MATCH_PARENT,
                    Gravity.LEFT | Gravity.CENTER_VERTICAL);
            setLayoutSizeAndGravity(blueText, videoWidth, MATCH_PARENT,
                    Gravity.LEFT | Gravity.CENTER_VERTICAL);
        }
    }

    /**
     * Adapter for the video list. Manages a set of YouTubeThumbnailViews, including initializing each
     * of them only once and keeping track of the loader of each one. When the ListFragment gets
     * destroyed it releases all the loaders.
     */
    private class PageAdapter extends BaseAdapter {

        private final List<VideoEntry> entries;
        private final LayoutInflater inflater;

        private boolean labelsVisible;

        public PageAdapter(Context context, List<VideoEntry> entries) {
            this.entries = entries;

            inflater = LayoutInflater.from(context);

            labelsVisible = true;
        }

        @Override
        public int getCount() {
            return entries.size();
        }

        @Override
        public VideoEntry getItem(int position) {
            return entries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            VideoEntry entry = entries.get(position);

            // There are three cases here
            if (view == null) {
                // 1) The view has not yet been created - we need to initialize the YouTubeThumbnailView.
                view = inflater.inflate(R.layout.video_list_item, parent, false);
            }
            TextView label = ((TextView) view.findViewById(R.id.text));
            label.setText(entry.word);
            label.setVisibility(labelsVisible ? View.VISIBLE : View.VISIBLE);
            return view;
        }

    }

    private class VideoEntry {
        private final String word;
        private final String oldId;

        public VideoEntry(String word, String oldId) {
            this.word = word;
            this.oldId = oldId;
        }
    }

    private class VideoId {
        private String videoIdWithSubtitles;
        private String videoIdWithoutSubtitles;
        private String filename;

        public VideoId(String videoIdWithSubtitles, String filename, String videoIdWithoutSubtitles) {
            this.videoIdWithSubtitles = videoIdWithSubtitles;
            this.filename = filename;
            this.videoIdWithoutSubtitles = videoIdWithoutSubtitles;
        }

        public String getFilename() {
            return filename;
        }

        public String getVideoIdWithSubtitles() {
            return videoIdWithSubtitles;
        }

        public String getVideoIdWithoutSubtitles() {
            return videoIdWithoutSubtitles;
        }
    }

    private class class_category {
        String id, category;
        Boolean checked;

        class_category(String id, String category, Boolean checked) {
            this.id = id;
            this.category = category;
            this.checked = checked;
        }
    }

    public void showDialogSelectWordlist() {
        CardDatabase cardDatabase = new CardDatabase(this);
        SQLiteDatabase db = cardDatabase.getReadableDatabase();
//		List<String> checked_category_id = new ArrayList<String>();
//		Cursor cursor = db.query("current_category", null, null, null, null, null, null);
//		if (cursor.moveToFirst()) {
//			do {
//				checked_category_id.add(cursor.getString(cursor.getColumnIndex("category_id")));
//			} while (cursor.moveToNext());
//		}
//		cursor.close();
        final List<class_category> category = new ArrayList<class_category>();
        Cursor cursor = db.query("category", null, null, null, null, null, "category");
        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                Boolean checked = (0 == cursor.getInt(cursor.getColumnIndex("is_skip")));
//				if (checked_category_id.contains(cursor.getString(cursor
//						.getColumnIndex("id"))))
//					checked = true;

                category.add(new class_category(cursor.getString(cursor
                        .getColumnIndex("id")), cursor.getString(cursor
                        .getColumnIndex("category")), checked));
            }
        }
        cursor.close();
        String[] data = new String[category.size()];
        boolean[] chkd = new boolean[category.size()];
        for (int i = 0; i < category.size(); i++) {
            cursor = db.query("word", new String[] { "count(*) as count" },
                    "category_id='" + category.get(i).id + "'", null, null,
                    null, "word");
            if (cursor.moveToFirst()) {
                do {
                    data[i] = category.get(i).category + " ("
                            + cursor.getString(cursor.getColumnIndex("count"))
                            + ")";
                } while (cursor.moveToNext());
            }
            cursor.close();
            chkd[i] = category.get(i).checked;
        }
        cardDatabase.close();

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
        builderSingle.setTitle(R.string.menu_select_wordlist);
        builderSingle.setMultiChoiceItems(data, chkd,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which,
                                        boolean isChecked) {
                        category.get(which).checked = isChecked;
                    }
                });
        builderSingle.setNeutralButton(android.R.string.cancel, null);

        builderSingle.setPositiveButton(R.string.btn_select,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        CardDatabase cardDatabase = new CardDatabase(
                                getApplicationContext());
                        SQLiteDatabase db = cardDatabase.getWritableDatabase();
                        //db.delete("current_category", null, null);
                        for (int i = 0; i < category.size(); i++) {
                            ContentValues cv = new ContentValues();
                            cv.put("is_skip", !category.get(i).checked);
                            String[] whereArgs = { category.get(i).id.toString() };
                            db.update("category", cv, "id = ?", whereArgs);
                        }

//							if (category.get(i).checked) {
//								ContentValues cv = new ContentValues();
//								cv.put("category_id", category.get(i).id);
//								db.insert("current_category", null, cv);
//							}
                        db.execSQL("delete from word_to_study");
                        cardDatabase.close();
                        dialog.dismiss();
                    }
                });

        builderSingle.show();
    }


    // Utility methods for layouting.

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static void setLayoutSize(View view, int width, int height) {
        LayoutParams params = view.getLayoutParams();
        params.width = width;
        params.height = height;
        view.setLayoutParams(params);
    }

    private static void setLayoutSizeAndGravity(View view, int width, int height, int gravity) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        params.width = width;
        params.height = height;
        params.gravity = gravity;
        view.setLayoutParams(params);
    }

    private List<VideoId> getVideoIds(int position) {
        List<VideoId> videoIds = new ArrayList<VideoId>();

        CardDatabase cardDatabase = new CardDatabase(getApplicationContext());
        SQLiteDatabase db = cardDatabase.getReadableDatabase();
        Cursor cur = db.query("word_video inner join video on word_video.video_id = video.video_id",
                null, "word_video.word_id = " + WORD_LIST.get(position).oldId, null,
                null, null, null);
        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                String videoIdWithSubtitles = cur.getString(cur.getColumnIndex("youku_id"));
                String filename = cur.getString(cur.getColumnIndex("file_name"));
                String newFilename = filename.substring(0, filename.lastIndexOf("_")) + ".mp4";

                String videoIdWithoutSubtitles = "";
                Cursor cur2 = db.query("video", null, "file_name = '" + newFilename + "'", null, null, null, null);
                if (cur2.getCount() == 1) {
                    while (cur2.moveToNext()) {
                        videoIdWithoutSubtitles = cur2.getString(cur2.getColumnIndex("youku_id"));
                    }
                }
                videoIds.add(new VideoId(videoIdWithSubtitles, filename, videoIdWithoutSubtitles));
            }
        }
        cardDatabase.close();

        return videoIds;
    }

    private void playYoukuVideo() {
        if (youkuPlayer != null) {
            if (subtitlesSwitch.isChecked()) {
                youkuPlayer.playVideo(videoIds.get(currentVideoPosition).getVideoIdWithSubtitles());
            } else {
                if (videoIds.get(currentVideoPosition).getVideoIdWithoutSubtitles().isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Video Id for '" + videoIds.get(currentVideoPosition).getFilename() + "' is empty", Toast.LENGTH_LONG).show();
                } else {
                    youkuPlayer.playVideo(videoIds.get(currentVideoPosition).getVideoIdWithoutSubtitles());
                }
            }
        }
    }

}
