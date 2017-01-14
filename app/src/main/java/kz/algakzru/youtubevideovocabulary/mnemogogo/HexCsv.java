/*
 * Copyright (C) 2009 Timothy Bourke
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package kz.algakzru.youtubevideovocabulary.mnemogogo;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

abstract class HexCsv
    implements CardList, CardDataSet
{
    private Card cards[];
    private RevQueue q;
    private int days_left;
    private Config config;
    private Progress progress;

    private boolean specify_encoding;
    private boolean allow_skip_categories;

    private int logFormat;

    public long days_since_start;
    public OutputStreamWriter logfile;
    public String categories[];
    public boolean skip_categories[];

    public int cards_to_load = 50;

    public static final String ascii = "US-ASCII";
    public static final String utf8 = "UTF-8";

    public static final String readingStatsText = "Loading statistics";
    public static final String writingStatsText = "Writing statistics";
    public static final String loadingCardsText = "Loading cards";

    public HexCsv(Progress prog,
                  boolean specify_encoding,
                  boolean allow_skip_categories)
        throws Exception, IOException
    {
        this.specify_encoding = specify_encoding;
        this.allow_skip_categories = allow_skip_categories;

        progress = prog;

        readConfig();
        logFormat = config.logFormat();

        days_left = daysLeft(config.lastDay());
        days_since_start = daysSinceStart(config.startDay());

        readCategories();
        readCards();

        try {
            readMarked();
        } catch (IOException e) { }
    }

    private void openLogFile(String filepath)
    {
        try {
            OutputStream outs = openAppend(filepath);
            if (specify_encoding) {
                try {
                    logfile = new OutputStreamWriter(outs, ascii);
                } catch (UnsupportedEncodingException e) {
                    logfile = new OutputStreamWriter(outs);
                }
            } else {
                logfile = new OutputStreamWriter(outs);
            }
        } catch (Exception e) {
            logfile = null;
        }
    }

    public String getCategory(int n)
    {
        if (0 <= n && n < categories.length) {
            return categories[n];
        } else {
            return null;
        }
    }

    public boolean skipCategory(int n)
    {
        if (0 <= n && n < categories.length) {
            return skip_categories[n];
        } else {
            return false;
        }
    }

    public void setSkipCategory(int n, boolean skip)
    {
        if (0 <= n && n < categories.length) {
            skip_categories[n] = skip;
        }
    }

    public int numCategories()
    {
        return categories.length;
    }

    public Card getCard()
    {
        return q.getCard();
    }

    public Card getCard(int serial)
    {
        if (0 <= serial && serial < cards.length) {
            return cards[serial];
        } else {
            return null;
        }
    }

    private void readConfig()
        throws IOException
    {
        config = new Config();
    }

    public int logFormat()
    {
        return logFormat;
    }

    public long nowInDays()
    {
        Date now = new Date(); // UTC (millisecs since 01/01/1970, 00:00:00 GMT)

        // hours since epoch in UTC
        long hours = now.getTime() / 3600000;

        // offset from UTC to local in hours
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        long tzoff = tz.getRawOffset() / 3600000;

        // e.g.
        // for day_starts_at = 3 (0300 local time)
        // and UTC +8
        // the next day should start at UTC 1900
        // (not at UTC 0000)
        // because 1900 + 8 - 3 = 0000

        return (hours + tzoff - config.dayStartsAt()) / 24;
    }

    private long daysSinceStart(long start_days)
    {
        long now_days = nowInDays();
        return now_days - start_days;
    }

    public int daysLeft()
    {
        return days_left;
    }

    private int daysLeft(long last_day)
    {
        if (last_day < 0) {
            return (int)-last_day;
        }
        return (int)(last_day - nowInDays());
    }

    private void readCards() throws IOException
    {
        // Number of cards
        int ncards = 10;
        CardDatabase cardDatabase = new CardDatabase(HexCsvAndroid.context);
        SQLiteDatabase db = cardDatabase.getReadableDatabase();
        Cursor cur = db.query("word", null, null, null, null, null, null);
        if (cur.getCount() > 0) {
            int i=0;
            ncards = cur.getCount();
            progress.startOperation(80 * 3, readingStatsText);

            cards = new Card[ncards];
            Card.cardlookup = this;
            Card.log_format = config.logFormat();

            while (cur.moveToNext()) {
                cards[i] = new Card(cur, i);

                if (i % 10 == 0) {
                    progress.updateOperation(10);
                }
                ++i;
            }
        }
        cardDatabase.close();

        progress.stopOperation();

        q = new RevQueue(ncards, days_since_start, config, progress, days_left);
        q.buildRevisionQueue(cards, false);
    }

    public void rebuildQueue()
    {
        q.buildRevisionQueue(cards, false);
    }
    
    public void learnAhead()
    {
        q.buildRevisionQueue(cards, true);
    }

    public boolean canLearnAhead()
    {
        return q.canLearnAhead(cards);
    }

    private void readMarked()
        throws IOException
    {
        /*InputStream is = openIn(path.append("MARKED").toString());
        InputStreamReader in = new InputStreamReader(is);;

        try {
            while (true) {
                int i = StatIO.readInt(in);
                if (i >= 0 && i < cards.length) {
                    cards[i].setMarked(true);
                }
            }
        } catch (NumberFormatException e) {}

        in.close();*/
    }

    private void writeMarked(StringBuffer path)
        throws IOException
    {
        OutputStream os = openOut(path.append("MARKED").toString());
        OutputStreamWriter out = new OutputStreamWriter(os);

        for (int i=0; i < cards.length; ++i) {
            if (cards[i].isMarked()) {
                out.write(Integer.toString(i));
                out.write('\n');
            }
        }

        out.flush();
        out.close();
    }

    public void writeCards(Progress progress)
            throws IOException
    {
        if (progress != null) {
            progress.startOperation(cards.length, writingStatsText);
        }
        for (int i=0; i < cards.length; ++i) {
            cards[i].writeCard();

            if (i % 10 == 0 && progress != null) {
                progress.updateOperation(10);
            }
        }
        if (progress != null) {
            progress.stopOperation();
        }
    }

    public void writeCards(StringBuffer path, Progress progress)
        throws IOException
    {
        int path_len = path.length();
        //writeCards(path, "STATS.CSV", progress);
        writeCards(progress);
        path.setLength(path_len);
        try {
            writeMarked(path);
        } catch (IOException e) {}
    }

    public void backupCards(Progress progress)
        throws IOException
    {
        //writeCards(path, "STATS.BKP", progress);
    }

    private void readCategorySkips()
    {
        int n = categories.length;

        skip_categories = new boolean[n];
        for(int i=0; i < n; ++i) {
            skip_categories[i] = false;
        }

        if (!allow_skip_categories) {
            return;
        }
    }

    public void writeCategorySkips(StringBuffer path)
    {
        if (!allow_skip_categories) {
            return;
        }

        try {
            OutputStream os = openOut(path.append("SKIPCATS").toString());
            OutputStreamWriter out;

            if (specify_encoding) {
                try {
                    out = new OutputStreamWriter(os, utf8);
                } catch (UnsupportedEncodingException e) {
                    out = new OutputStreamWriter(os);
                }
            } else {
                out = new OutputStreamWriter(os);
            }

            for (int i=0; i < categories.length; ++i) {
                if (skip_categories[i]) {
                    out.write(categories[i]);
                    out.write('\n');
                }
            }

            out.flush();
            out.close();
        } catch (Exception e) { }
    }

    private void readCategories()
        throws IOException
    {
        CardDatabase cardDatabase = new CardDatabase(HexCsvAndroid.context);
        SQLiteDatabase db = cardDatabase.getReadableDatabase();
        Cursor cur = db.query("category", null, null, null, null, null, null);
        if (cur.getCount() > 0) {
            categories = new String[cur.getCount()];
            skip_categories = new boolean[cur.getCount()];
            int i=0;
            while (cur.moveToNext()) {
                categories[i] = cur.getString(cur.getColumnIndex("category"));
                skip_categories[i] = (1 == cur.getInt(cur.getColumnIndex("is_skip")));
                ++i;
            }
        }
        cardDatabase.close();
    }

    public void setCardData(int serial, String question, String answer,
                boolean overlay)
    {
        cards[serial].setOverlay(overlay);
        cards[serial].setQuestion(question);
        cards[serial].setAnswer(answer);
    }

    public boolean cardDataNeeded(int serial)
    {
        return ((cards[serial].isDueForRetentionRep(days_since_start)
                 || cards[serial].isDueForAcquisitionRep()
                 || (q.isLearningAhead()
                     && cards[serial].qualifiesForLearnAhead(days_since_start)))
                && q.isScheduledSoon(serial, cards_to_load));
    }

    public void setProgress(Progress new_progress)
    {
        progress = new_progress;
    }

    private void readCardText()
        throws IOException
    {
        /*DataInputStream is = openDataIn(path.append("CARDS").toString());

        // the new object is not needed, rather just that its constructor
        // updates this object.
        new CardData(is, progress, this);
        is.close();*/
        new CardData(progress, this);
    }

    public void loadCardData()
        throws IOException
    {
        // clear any existing questions and answers
        for (int i=0; i < cards.length; ++i) {
            cards[i].setQuestion(null);
            cards[i].setAnswer(null);
        }

        // load them again
        progress.startOperation(cards.length, loadingCardsText);
        readCardText();
        progress.stopOperation();
    }

    public int numScheduled()
    {
        return q.numScheduled();
    }

    public int getTodayReview()
    {
        return q.getTodayReview();
    }

    public void addToFutureSchedule(Card card)
    {
        q.addToFutureSchedule(card);
    }

    public void removeFromFutureSchedule(Card card)
    {
        q.removeFromFutureSchedule(card);
    }

    public int[] getFutureSchedule()
    {
        return q.getFutureSchedule();
    }

    public String toString()
    {
        return q.toString();
    }

    public void dumpCards()
    {
        System.out.println("----Cards:");
        for (int i=0; i < cards.length; ++i) {
            System.out.print("  ");
            System.out.println(cards[i].toString());
        }
    }

    public void close()
    {
        if (logfile != null) {
            try {
                logfile.close();
            } catch (IOException e) { }
            logfile = null;
        }
    }

    public void reopen(String path)
    {
        if (logfile == null && config.logging()) {
//            pathbuf = new StringBuffer(path);
//            pathbuf.append("PRELOG");
//            openLogFile(pathbuf.toString());
        }
    }

    abstract protected OutputStream openAppend(String path)
        throws IOException;
    abstract protected OutputStream openOut(String path)
        throws IOException;
    abstract protected InputStream openIn(String path)
        throws IOException;
    abstract protected DataInputStream openDataIn(String path)
        throws IOException;
}

