package kz.algakzru.youtubevideovocabulary;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.webkit.WebSettings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.google.android.youtube.player.YouTubeStandalonePlayer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import kz.algakzru.youtubevideovocabulary.mnemododo.CardStore;
import kz.algakzru.youtubevideovocabulary.mnemododo.LoadCardTask;
import kz.algakzru.youtubevideovocabulary.mnemododo.Pair;
import kz.algakzru.youtubevideovocabulary.mnemododo.TaskListener;
import kz.algakzru.youtubevideovocabulary.mnemogogo.Card;
import kz.algakzru.youtubevideovocabulary.mnemogogo.CardDatabase;
import kz.algakzru.youtubevideovocabulary.mnemogogo.HexCsvAndroid;
import kz.algakzru.youtubevideovocabulary.util.L;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;


public class StudyActivity extends YouTubeFailureRecoveryActivity implements OnClickListener {

    enum Mode { SHOW_QUESTION, SHOW_ANSWER, NO_CARDS, NO_NEW_CARDS }

//    private final int REQ_START_STANDALONE_PLAYER = 1;
//    private final int REQ_RESOLVE_SERVICE_MISSING = 2;
    private YouTubePlayer player;
    private List<String> videoIds = new ArrayList<String>();

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
        setContentView(R.layout.activity_study);
        actionBarTitle = MainActivity.setActionBar(this, true);

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

        YouTubePlayerView youTubeView = (YouTubePlayerView) findViewById(R.id.youtube_view);
        youTubeView.initialize(DeveloperKey.DEVELOPER_KEY, this);

        // Get settings and load cards if necessary
        loadPrefs((StudyActivity) getLastNonConfigurationInstance());
    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player, boolean wasRestored) {
        this.player = player;
        if (!wasRestored && !videoIds.isEmpty()) {
            player.cueVideos(videoIds);
        }
    }

    @Override
    protected YouTubePlayer.Provider getYouTubePlayerProvider() {
        return (YouTubePlayerView) findViewById(R.id.youtube_view);
    }

    public void loadPrefs(StudyActivity lastDodo)
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
                return StudyActivity.this;
            }

            public String getString(int resid)
            {
                return StudyActivity.this.getString(resid);
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
                return StudyActivity.this;
            }

            public String getString(int resid)
            {
                return StudyActivity.this.getString(resid);
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
        ViewFlipper vfHorizontal = (ViewFlipper) findViewById(R.id.vfHorizontal);

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
                    setContentFrontCard(cur_card, vfHorizontal);
                }
                break;

            case SHOW_ANSWER:
                setNumLeft(carddb.getTodayReview(), carddb.numScheduled());
                if (cur_card != null) {
                    setCategory(cur_card.categoryName());
                    setCategory("New words: " + cur_card.getTodayNew(carddb.cards.days_since_start));
                    setContentBackCard(cur_card, vfHorizontal);
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
        Cursor cur = db
                .query("word_video inner join video on word_video.video_id = video.video_id",
                        null, "word_video.word_id = " + currentCard.getOldId(), null, null, null, null);
        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                videoIds.add(cur.getString(cur.getColumnIndex("youtube_id")));
            }
        }
        cardDatabase.close();

        if (!videoIds.isEmpty()) {
            this.videoIds = videoIds;
            if (player != null) {
                player.loadVideos(videoIds);
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
                String url = "plecoapi://x-callback-url/s?q=" + cur_card.getWord();
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
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

    private void setContentFrontCard(Card currentCard, ViewFlipper vfHorizontal) {
        actionBarTitle.setText(R.string.title_activity_study);
        CardDatabase cardDatabase = new CardDatabase(this);
        SQLiteDatabase db = cardDatabase.getReadableDatabase();
        Cursor cur = db.query("word_video", null, "word_video.word_id = " + currentCard.getOldId(), null, null, null, null);
        String question = null;
        if (cur.getCount() > 0) {
            cur.moveToNext();
            question = cur.getString(cur.getColumnIndex("sentence"));
        }
        cardDatabase.close();

        String replace = "";
        for (int i = 0; i < currentCard.getWord().length(); i++) replace += "…";
        String htmlString = question.replaceAll("\n", "<br>");
        htmlString = htmlString.replaceAll(currentCard.getWord(), "<font color='#000000'>" + replace + "</font>");
        LinearLayout ll = (LinearLayout) vfHorizontal.getChildAt(0);
        ((TextView) ((RelativeLayout) ll.getChildAt(0)).getChildAt(0)).setText(Html.fromHtml(htmlString), TextView.BufferType.SPANNABLE);
    }

    private void setContentBackCard(Card currentCard, ViewFlipper vfHorizontal) {
        actionBarTitle.setText(currentCard.getWord());
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

                    vfHorizontal.setInAnimation(AnimationUtils.loadAnimation(this,
                            R.anim.bounce_interpolator));
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
                    if ((player != null) && (player.isPlaying())) {
                        player.pause();
                    }
                    vfHorizontal.setInAnimation(AnimationUtils.loadAnimation(this,
                            R.anim.bounce_interpolator));
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
