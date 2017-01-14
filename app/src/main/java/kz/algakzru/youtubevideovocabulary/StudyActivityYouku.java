package kz.algakzru.youtubevideovocabulary;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.youku.player.base.YoukuBasePlayerManager;
import com.youku.player.base.YoukuPlayer;
import com.youku.player.base.YoukuPlayerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import kz.algakzru.youtubevideovocabulary.mnemododo.CardStore;
import kz.algakzru.youtubevideovocabulary.mnemododo.LoadCardTask;
import kz.algakzru.youtubevideovocabulary.mnemododo.Pair;
import kz.algakzru.youtubevideovocabulary.mnemododo.TaskListener;
import kz.algakzru.youtubevideovocabulary.mnemogogo.Card;
import kz.algakzru.youtubevideovocabulary.mnemogogo.CardDatabase;
import kz.algakzru.youtubevideovocabulary.mnemogogo.HexCsvAndroid;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;


public class StudyActivityYouku extends Activity implements OnClickListener {

    enum Mode { SHOW_QUESTION, SHOW_ANSWER, NO_CARDS, NO_NEW_CARDS }

    private YoukuBasePlayerManager basePlayerManager;
    // 播放器控件
    private YoukuPlayerView mYoukuPlayerView;
    //private YouTubePlayer player;
    // YoukuPlayer实例，进行视频播放控制
    private YoukuPlayer youkuPlayer;

    int isHanzi;

    RatingBar rbRating;
    Button btnRating;
    TextView actionBarTitle;
    LinearLayout llHandleNoCards;
    RelativeLayout llStudyCards;

    static final int DIALOG_ABOUT = 0;
    static final int DIALOG_STATS = 1;
    static final int DIALOG_SCHEDULE = 2;
    static final int DIALOG_CATEGORIES = 3;

    /* data (always recalculated) */
    boolean carddb_dirty = false;

    /* data (cache on temporary restart) */

    Mode mode = Mode.NO_CARDS;

    CardStore carddb = new CardStore();
    protected Card cur_card;
    protected LoadCardTask card_task = null;

    /* Configuration */

    int cards_to_load = 50;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.youku_activity_study);
        actionBarTitle = MainActivity.setActionBar(this, true);

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

                // 进行播放
                //goPlay();
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

        HexCsvAndroid.context = this;

        llHandleNoCards = (LinearLayout) findViewById(R.id.llHandleNoCards);
        llStudyCards = (RelativeLayout) findViewById(R.id.llStudyCards);

        final List<String> stars = new ArrayList<String>();
        stars.add(getResources().getString(R.string.btnResponse0));
        stars.add(getResources().getString(R.string.btnResponse1));
        stars.add(getResources().getString(R.string.btnResponse2));
        stars.add(getResources().getString(R.string.btnResponse3));
        stars.add(getResources().getString(R.string.btnResponse4));
        stars.add(getResources().getString(R.string.btnResponse5));
        rbRating = (RatingBar) findViewById(R.id.rbRating);
        rbRating.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating,
                                        boolean b) {
                if (rating > 0) {
                    btnRating.setEnabled(true);
                    btnRating.setText(stars.get(((int) rating-1)));
//					btnRating.setText(SM2.getInterval(currentCard, Math.round(rating)));
                }
            }
        });
        btnRating = (Button) findViewById(R.id.btnRating);
        btnRating.setEnabled(false);
        btnRating.setOnClickListener(this);
        ((Button) findViewById(R.id.btnShowAnswer)).setOnClickListener(this);
        ((Button) findViewById(R.id.btnShowVideo)).setOnClickListener(this);


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

        // Get settings and load cards if necessary
        loadPrefs((StudyActivityYouku) getLastNonConfigurationInstance());
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        basePlayerManager.onResume();
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
    protected void onNewIntent(Intent intent) {
        // TODO Auto-generated method stub
        super.onNewIntent(intent);

        // 进行播放
        goPlay();
    }

    private void goPlay() {
        // 播放在线视频
            youkuPlayer.playVideo("XMTMxNDIxMDIzNg==");
    }

    public void loadPrefs(StudyActivityYouku lastDodo)
    {
        if (!carddb.loadingCards()) {
            loadCards();
        }
    }

    public void loadCards()
    {
        loadCardDB();
    }

    protected void loadCardDB()
    {
        carddb = new CardStore(cards_to_load, makeCardStoreListener());
    }

    protected TaskListener<String> makeCardStoreListener ()
    {
        return new TaskListener<String> () {
            public Context getContext ()
            {
                return StudyActivityYouku.this;
            }

            public String getString(int resid)
            {
                return StudyActivityYouku.this.getString(resid);
            }

            public void onFinished(String error_msg)
            {
                if (error_msg == null) {
                    carddb_dirty = false;
                    nextQuestion();
                } else {
                    setMode(Mode.NO_CARDS);
                    showFatal(error_msg, false);
                }
            }
        };
    }

    protected TaskListener<Pair<Boolean, String>> makeLoadCardListener()
    {
        return new TaskListener<Pair<Boolean, String>> () {
            public Context getContext ()
            {
                return StudyActivityYouku.this;
            }

            public String getString(int resid)
            {
                return StudyActivityYouku.this.getString(resid);
            }

            public void onFinished(Pair<Boolean, String> result)
            {
                card_task = null;
            }
        };
    }

    protected void showFatal(String msg, final boolean exit)
    {
        new AlertDialog.Builder(this).setTitle(getString(R.string.fatal_error))
                .setMessage(msg).setNegativeButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton)
                    {
                        if (exit) {
                            finish();
                        }
                    }
                }).show();
    }

    protected boolean nextQuestion()
    {
        cur_card = carddb.cards.getCard();


        if (cur_card == null) {
            setMode(Mode.NO_NEW_CARDS);
            return false;
        }

        try {
            CardDatabase cardDatabase = new CardDatabase(this);
            SQLiteDatabase db = cardDatabase.getReadableDatabase();
            String movieName = "", fileName = "";
            Cursor cur = db.query("word_video INNER JOIN video ON word_video.video_id = video.video_id", null, "word_video.word_id = " + cur_card.getOldId(), null, null, null, "RANDOM() LIMIT 1");
            if (cur.getCount() == 1) {
                while (cur.moveToNext()) {
                    cur_card.setVideoId(cur.getLong(cur.getColumnIndex("video_id")));
                    movieName = cur.getString(cur.getColumnIndex("movie_name"));
                    fileName = cur.getString(cur.getColumnIndex("file_name"));
                }
            }
            fileName = fileName.split("_")[0] + "_" + fileName.split("_")[1] + ".mp4";
            cur = db.query("video", null, "movie_name = \"" + movieName + "\" AND file_name = \"" + fileName + "\"", null, null, null, null);
            if (cur.getCount() == 1) {
                while (cur.moveToNext()) {
                    cur_card.setVideoId(cur.getLong(cur.getColumnIndex("video_id")));
                }
            }
            cardDatabase.close();
            setMode(Mode.SHOW_QUESTION);

        } catch (Exception e) {
            showFatal(e.toString(), false);
            return false;
        }

        return true;
    }

    public void setMode(Mode m)
    {
        StringBuffer html = null;

        if (m == Mode.NO_CARDS || m == Mode.NO_NEW_CARDS) {
            //show_panel.setVisibility(View.GONE);
            //grading_panel.setVisibility(View.GONE);
        } else {
            //show_panel.setVisibility(m == Mode.SHOW_QUESTION ? View.INVISIBLE : View.GONE);
            //grading_panel.setVisibility(m == Mode.SHOW_ANSWER ? View.INVISIBLE : View.GONE);
        }

        mode = m;
        //hidden_view = null;

        switch (m) {
            case SHOW_QUESTION:
                setNumLeft(carddb.getTodayReview(), carddb.numScheduled());
                if (cur_card != null) {
                    setCategory(cur_card.categoryName());
                    setCategory("New words: " + cur_card.getTodayNew(carddb.cards.days_since_start));
                    setContentFrontCard(cur_card);
                }
                break;

            case SHOW_ANSWER:
                setNumLeft(carddb.getTodayReview(), carddb.numScheduled());
                if (cur_card != null) {
                    setCategory(cur_card.categoryName());
                    setCategory("New words: " + cur_card.getTodayNew(carddb.cards.days_since_start));
                    setContentBackCard(cur_card);
                }
                break;

            case NO_CARDS:
                //html = new StringBuffer(html_pre);
                html.append("<body>");

                html.append("<div style=\"padding: 1ex;\"><p>");
                html.append(getString(R.string.no_cards_main));
                html.append("</p><ol>");

                html.append("<li style=\"padding-bottom: 2ex;\">");
                html.append(getString(R.string.no_cards_step1));
                html.append("</li>");

                html.append("<li>");
                html.append(getString(R.string.no_cards_step2));
                html.append("</li></ol></div>");

                //html.append(html_post);

                setCategory(getString(R.string.no_cards_title));
                //webview.loadDataWithBaseURL("", html.toString(), "text/html", "UTF-8", "");
                break;

            case NO_NEW_CARDS:
                actionBarTitle.setText(R.string.no_new_cards_title);
                ((TextView) findViewById(R.id.tvFrontCard)).setText(getString(R.string.no_cards_left));
                findViewById(R.id.btnShowAnswer).setVisibility(View.GONE);
                if (carddb.canLearnAhead()) {
//                    Toast.makeText(this, "canLearnAhead", Toast.LENGTH_LONG).show();
//                    html.append("<input type=\"button\" value=\"");
//                    html.append(getString(R.string.learn_ahead));
//                    html.append("\" style=\"width: 100%; margin-top: 1em;\"");
//                    html.append(" onclick=\"Mnemododo.learnAhead();\" />");
                }

                setCategory("");
                ((TextView) findViewById(R.id.cards_left)).setText("");
                //setNumLeft(carddb.numScheduled());
                //webview.loadDataWithBaseURL("", html.toString(), "text/html", "UTF-8", "");
                break;
        }
    }

    public void setNumLeft(int today_review, int cards_left)
    {
        TextView cardsl_title = (TextView) findViewById(R.id.cards_left);
        //cardsl_title.setText("Review words: " + cur_card.getTodayReview(carddb.cards.days_since_start) + " / " + Integer.toString(cards_left));
        cardsl_title.setText("Review words: " + Integer.toString(cards_left) + " / " + Integer.toString(today_review));

        if (carddb.active()) {
            int daysLeft = carddb.cards.daysLeft();

            if (daysLeft < 0) {
                cardsl_title.setBackgroundColor(android.graphics.Color.RED);
                cardsl_title.setTextColor(android.graphics.Color.BLACK);
            } else if (daysLeft == 0) {
                cardsl_title.setBackgroundColor(android.graphics.Color.YELLOW);
                cardsl_title.setTextColor(android.graphics.Color.BLACK);
            } else {
                TextView cat_title = (TextView) findViewById(R.id.category);
                cardsl_title.setBackgroundColor(
                        cat_title.getDrawingCacheBackgroundColor());
                cardsl_title.setTextColor(cat_title.getCurrentTextColor());
            }
        }
    }

    public void setCategory(String category)
    {
        TextView cat_title = (TextView) findViewById(R.id.category);
        cat_title.setText(category);
    }

    private void setVideoIds(Card currentCard) {
        List<String> videoIds = new ArrayList<String>();
        if (currentCard == null) return;
        CardDatabase cardDatabase = new CardDatabase(this);
        SQLiteDatabase db = cardDatabase.getReadableDatabase();
        Cursor cur = db.query("video", null, "video_id = " + currentCard.getVideoId(), null, null, null, null);
        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                videoIds.add(cur.getString(cur.getColumnIndex("youku_id")));
            }
        }
        cardDatabase.close();

        if (!videoIds.isEmpty()) {
            if (youkuPlayer != null) {
                //youkuPlayer.loadVideos(videoIds);
                youkuPlayer.playVideo(videoIds.get(0));
            }
        }
        else Toast.makeText(this, "videoIds.isEmpty()", Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_study, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_statistics:
                showDialogStatistics();
                break;
            case R.id.menu_schedule:
                showDialogSchedule();
                break;
            case R.id.menu_pleco:
//                String url = "plecoapi://x-callback-url/s?q=" + cur_card.getWord();
//                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//                startActivity(browserIntent);
                Intent intent = new Intent("colordict.intent.action.SEARCH");
                intent.putExtra("EXTRA_QUERY", cur_card.getWord()); //Search Query
                intent.putExtra("EXTRA_FULLSCREEN", true); //
                startActivity(intent);

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    protected boolean doGrade(int grade)
    {
        if (cur_card == null) {
            return false;
        }

        try {
            if (carddb.active()) {

                //android.widget.Toast.makeText(this, "grade=" + Integer.toString(grade), android.widget.Toast.LENGTH_SHORT).show();

                carddb.cards.removeFromFutureSchedule(cur_card);
                cur_card.gradeCard(carddb.cards.days_since_start, grade);
                carddb.cards.addToFutureSchedule(cur_card);
                carddb_dirty = true;
                return true;
            } else {
                return false;
            }

        } catch (IOException e) {
            showFatal(e.toString(), false);
            return false;
        }
    }

    public void handleNoCards() {
        llStudyCards.setVisibility(View.GONE);
        llHandleNoCards.setVisibility(View.VISIBLE);
        actionBarTitle.setText(R.string.NoMoreCards);
    }

    private void setContentBackCard(Card currentCard) {
        actionBarTitle.setText(currentCard.getWord());
        CardDatabase cardDatabase = new CardDatabase(this);
        SQLiteDatabase db = cardDatabase.getReadableDatabase();
        Cursor cur = db.query("video", null, "video_id = " + currentCard.getVideoId(), null, null, null, null);
        String time = null, movieName = null;
        if (cur.getCount() > 0) {
            cur.moveToNext();
            time = cur.getString(cur.getColumnIndex("file_name"));
            movieName = cur.getString(cur.getColumnIndex("movie_name"));
        }
        String start = time.split("_")[0]; start = start.substring(0, 2) + ":" + start.substring(2, 4) + ":" + start.substring(4, 6) + "." + start.substring(6);
        String finish = time.split("_")[1]; finish = finish.substring(0, 2) + ":" + finish.substring(2, 4) + ":" + finish.substring(4, 6) + "." + finish.substring(6, 9);
        String hanzi = "", pinyin = "";
        cur = db.query("subtitles_" + movieName, null, "time(start) >= time('" + start + "') AND time(finish) <= time('" + finish + "')", null, null, null, null);
        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                hanzi += cur.getString(cur.getColumnIndex("hanzi"));
                pinyin += cur.getString(cur.getColumnIndex("pinyin"));
            }
        }
        cardDatabase.close();

        String replace = "";
        for (int i = 0; i < currentCard.getWord().length(); i++) replace += "…";
        final String htmlStringWord = "<font color='#cc0000'>" + currentCard.getWord() + "</font>";
        final String htmlStringHanzi = hanzi.replaceAll("\n", "").replaceAll(currentCard.getWord(), "<font color='#cc0000'>" + currentCard.getWord() + "</font>");
        final String htmlStringPinyin = pinyin.replaceAll(currentCard.getPinyin(), "<font color='#cc0000'>" + currentCard.getPinyin() + "</font>");
        isHanzi = 1;
        ((TextView) findViewById(R.id.tvFrontCard)).setText(Html.fromHtml(htmlStringWord), TextView.BufferType.SPANNABLE);
        ((RelativeLayout) findViewById(R.id.rlFrontCard)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (isHanzi) {
                    case 0:
                        ((TextView) findViewById(R.id.tvFrontCard)).setText(Html.fromHtml(htmlStringWord), TextView.BufferType.SPANNABLE);
                        isHanzi++;
                        break;
                    case 1:
                        ((TextView) findViewById(R.id.tvFrontCard)).setText(Html.fromHtml(htmlStringHanzi), TextView.BufferType.SPANNABLE);
                        isHanzi++;
                        break;
                    case 2:
                        ((TextView) findViewById(R.id.tvFrontCard)).setText(Html.fromHtml(htmlStringPinyin), TextView.BufferType.SPANNABLE);
                        isHanzi=0;
                        break;
                }
            }
        });
    }

    private void setContentFrontCard(Card currentCard) {
        actionBarTitle.setText(currentCard.getPinyin());
        setVideoIds(currentCard);
    }

    public void onClick(View v)
    {
        ViewFlipper vfHorizontal = (ViewFlipper) findViewById(R.id.vfHorizontal);
        int click_id = v.getId();

        switch (mode) {

            case SHOW_QUESTION:
                if (click_id == R.id.btnShowAnswer) {
                    setMode(Mode.SHOW_ANSWER);

                    if (youkuPlayer != null) {
                        youkuPlayer.playVideo("stop");
                    }
                    //vfHorizontal.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce_interpolator));
                    vfHorizontal.setInAnimation(this, R.anim.slide_in_right);
                    vfHorizontal.setOutAnimation(this, R.anim.slide_out_left);
                    vfHorizontal.showNext();
                } else if (click_id == R.id.category) {
                    showDialog(DIALOG_STATS);
                }
                break;

            case SHOW_ANSWER:
                if (click_id == R.id.btnRating) {
                    doGrade(Math.round(rbRating.getRating())-1);
                    btnRating.setEnabled(false);
                    btnRating.setText(R.string.btn_next_word);
                    rbRating.setRating(0);
                    nextQuestion();
                    if (youkuPlayer != null) {
                        youkuPlayer.playVideo("stop");
                    }
                    vfHorizontal.setInAnimation(AnimationUtils.loadAnimation(this,
                            R.anim.bounce_interpolator));
                    vfHorizontal.showNext();
                }

                if (click_id == R.id.btnShowVideo) {
                    setMode(Mode.SHOW_QUESTION);
                    vfHorizontal.setInAnimation(this, android.R.anim.slide_in_left);
                    vfHorizontal.setOutAnimation(this, android.R.anim.slide_out_right);
                    vfHorizontal.showNext();
                }

                if (click_id == R.id.category) {
                    showDialog(DIALOG_STATS);
                }
                break;

            default:
                break;
        }

        if (click_id == R.id.cards_left) {
            showDialog(DIALOG_SCHEDULE);
        }

    }

    private void showDialogStatistics() {
        if (cur_card == null || !carddb.active()) {
            return;
        }

        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.stats, null);

        TextView text;

        text = (TextView) layout.findViewById(R.id.grade);
        text.setText(Integer.toString(cur_card.grade));

        text = (TextView) layout.findViewById(R.id.easiness);
        text.setText(Float.toString(cur_card.feasiness()));

        text = (TextView) layout.findViewById(R.id.repetitions);
        text.setText(Integer.toString(cur_card.repetitions()));

        text = (TextView) layout.findViewById(R.id.lapses);
        text.setText(Integer.toString(cur_card.lapses));

        text = (TextView) layout
                .findViewById(R.id.days_since_last_repetition);
        text.setText(Integer.toString(cur_card
                .daysSinceLastRep(carddb.cards.days_since_start)));

        text = (TextView) layout
                .findViewById(R.id.days_until_next_repetition);
        text.setText(Integer.toString(cur_card
                .daysUntilNextRep(carddb.cards.days_since_start)));

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setView(layout);
        dialog.setTitle(getString(R.string.card_statistics));
        dialog.setNeutralButton(android.R.string.ok, null);
        dialog.show();
    }

    private void showDialogSchedule() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(getString(R.string.schedule));
        int daysLeft = carddb.cards.daysLeft();
        if (daysLeft < 0) {
            dialog.setMessage(R.string.update_overdue_text);

        } else if (daysLeft == 0) {
            dialog.setMessage(R.string.update_today_text);

        } else {
            int[] indays = carddb.cards.getFutureSchedule();
            if (indays != null) {
                TableLayout table = (TableLayout) getLayoutInflater().inflate(R.layout.schedule, null);
                dialog.setView(table);
                table.setPadding(10, 0, 10, 10);

                for (int i = 0; i < indays.length; ++i) {
                    TableRow row = new TableRow(this);
                    TableRow.LayoutParams layoutParams = new TableRow.LayoutParams();
                    layoutParams.height = WRAP_CONTENT;
                    layoutParams.width = 0;
                    layoutParams.weight = 1;


                    TextView label = new TextView(this);
                    label.setText(getString(R.string.in_text)
                            + " "
                            + Integer.toString(i + 1)
                            + " "
                            + getString(i == 0 ? R.string.day_text
                            : R.string.days_text) + ":");
                    label.setTypeface(null, Typeface.BOLD);
                    label.setPadding(0, 0, 10, 2);
                    //label.setGravity(Gravity.RIGHT);
                    //label.setLayoutParams(layoutParams);

                    TextView value = new TextView(this);
                    value.setText(Integer.toString(indays[i]));
                    value.setGravity(Gravity.RIGHT);
                    //value.setLayoutParams(layoutParams);

                    row.addView(label);
                    row.addView(value);
                    table.addView(row);
                }
            }
        }
        dialog.setNeutralButton(android.R.string.ok, null);
        dialog.show();
        //dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

}
