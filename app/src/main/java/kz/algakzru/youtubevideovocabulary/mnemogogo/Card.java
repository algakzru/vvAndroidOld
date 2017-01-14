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
/*
 * Certain routines Copyright (c) Peter Bienstman <Peter.Bienstman@UGent.be>
 */

package kz.algakzru.youtubevideovocabulary.mnemogogo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Random;
import java.util.Vector;

public class Card
{
    // identifier in the database
    private Long id;
    private Long old_id;
    private String word = "";
    private String pinyin = "";
    private String translation = "";
    private int categoryId;
    private String questionType;
    public int serial;

    public int grade; 
    public int easiness;
    public int acq_reps;
    public int ret_reps;
    public int lapses;
    public int acq_reps_since_lapse;
    public int ret_reps_since_lapse;
    public long last_rep;
    public long next_rep;
    public boolean unseen, was_unseen, is_read;
    public boolean marked = false;
    public int inverse;
    public int category;

    private String question;
    private String answer;
    private Long videoId;
    private boolean overlay;
    private boolean skip = false;

    public static CardList cardlookup;
    public static int log_format;
    private static Random rand = new Random();

    private static int statLineLength = 62;
    private static char[] buffer = new char[statLineLength]; // NOT reentrant
    private static int pos;
    public static final int fourDigits = 12;
    private static final int eightDigits = 28;

    private static final int initial_interval[] = {0, 0, 1, 3, 4, 5};
    private static final String sound_prefix = "<sound src=\"";
    private static final int sound_prefix_offset = sound_prefix.length();

    Card() {
    }

    Card(Cursor cursor, int i) throws IOException
    {
        readCard(cursor, i);
    }

    public float feasiness() {
        return (float)easiness / 1000.0f;
    }

    public String categoryName() {
        return cardlookup.getCategory(category-1);
    }

    public boolean rememorise0() {
        return (lapses > 0 && grade == 0);
    }

    public boolean rememorise1() {
        return (lapses > 0 && grade == 1);
    }

    public boolean seenButNotMemorised0() {
        return (lapses == 0 && unseen == false && grade == 0);
    }

    public boolean seenButNotMemorised1() {
        return (lapses == 0 && unseen == false && grade == 1);
    }

    // Adapted directly from Peter Bienstman's Mnemosyne 1.x
    public boolean isDueForRetentionRep(long days_since_start, int days) {
        return (grade >= 2 && (days_since_start >= next_rep - days));
    }

    public boolean isDueForRetentionRep(long days_since_start) {
        return (grade >= 2 && days_since_start >= next_rep);
    }

    // Adapted directly from Peter Bienstman's Mnemosyne 1.x
    public boolean isDueForAcquisitionRep() {
        return (grade < 2);
    }

    public boolean qualifiesForLearnAhead(long days_since_start) {
        return (grade >= 2) && (days_since_start < next_rep);
    }

    public long sortKeyInterval() {
        return (next_rep - last_rep);
    }

    public long sortKey() {
        return next_rep;
    }

    public int repetitions() {
        return (acq_reps + ret_reps);
    }

    public int daysSinceLastRep(long days_since_start) {
        return (int)(days_since_start - last_rep);
    }

    public int daysUntilNextRep(long days_since_start) {
        return (int)(next_rep - days_since_start);
    }

    public String toString() {
        StringBuffer r = new StringBuffer(100);

        r.append("#");
        r.append(serial);
        r.append(" g=");
        r.append(grade); 
        r.append(" easy=");
        r.append(easiness);
        r.append(" acqs=");
        r.append(acq_reps);
        r.append(" rets=");
        r.append(ret_reps);
        r.append(" l=");
        r.append(lapses);
        r.append(" acqs_l=");
        r.append(acq_reps_since_lapse);
        r.append(" rets_l=");
        r.append(ret_reps_since_lapse);
        r.append(" last=");
        r.append(last_rep);
        r.append(" next=");
        r.append(next_rep);
        r.append(" unseen=");
        r.append(unseen);
        r.append(" inv=");
        r.append(inverse);
        r.append(" cat=");
        r.append(category);
        r.append(" skip=");
        r.append(isSkip());

        return r.toString();
    }

    public String toString(long days_since_start) {
        StringBuffer r = new StringBuffer(40);
        
        r.append("gr=");
        r.append(grade);
        r.append(" e=");
        r.append(feasiness());
        r.append(" r=");
        r.append(acq_reps + ret_reps);
        r.append(" l=");
        r.append(lapses);
        r.append(" ds=");
        r.append(days_since_start - last_rep);

        return r.toString();
    }

    private static char hexDigit(int d)
    {
        if (d < 10) {
            return (char)('0' + d);
        } else {
            return (char)('a' - 10 + d);
        }
    }

    public void addStat(long v, int d)
    {
        while (d >= 0) {
            buffer[pos++] = hexDigit((int)(v >> d & 0x0000000f));
            d -= 4;
        }
    }

    private static String hexDigitString(int d)
    {
        if (d < 10) {
            return String.valueOf((char) ('0' + d));
        } else {
            return String.valueOf((char) ('a' - 10 + d));
        }
    }

    public static String addStatString(long v, int d)
    {
        String str = "";
        while (d >= 0) {
            str += hexDigitString((int) (v >> d & 0x0000000f));
            d -= 4;
        }
        return str;
    }

    public void writeCard()
            throws IOException
    {
        CardDatabase cardDatabase = new CardDatabase(HexCsvAndroid.context);
        SQLiteDatabase db = cardDatabase.getReadableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("grade", hexDigitString(grade));
        cv.put("easiness", addStatString(easiness, fourDigits));
        cv.put("acq_reps", addStatString(acq_reps, fourDigits));
        cv.put("ret_reps", addStatString(ret_reps, fourDigits));
        cv.put("lapses", addStatString(lapses, fourDigits));
        cv.put("acq_reps_since_lapse", addStatString(acq_reps_since_lapse, fourDigits));
        cv.put("ret_reps_since_lapse", addStatString(ret_reps_since_lapse, fourDigits));
        cv.put("last_rep", addStatString(last_rep, eightDigits));
        cv.put("next_rep", addStatString(next_rep, eightDigits));
        cv.put("unseen", (unseen ? "1" : "0"));
        cv.put("was_unseen", (was_unseen ? "1" : "0"));
        cv.put("category", addStatString(category, fourDigits));
        cv.put("inverse", addStatString(inverse, fourDigits));
        String[] whereArgs = { getId().toString() };
        db.update("word", cv, "id = ?", whereArgs);
        cardDatabase.close();
    }

    public String getTodayNew(long days_since_start) {
        CardDatabase cardDatabase = new CardDatabase(HexCsvAndroid.context);
        SQLiteDatabase db = cardDatabase.getReadableDatabase();
        Cursor cur = db.query("word INNER JOIN category ON word.category_id = category.id", null, "category.is_skip = 0 AND word.was_unseen = 1 AND last_rep = '" + addStatString(days_since_start, eightDigits) + "'", null, null, null, null);
        int newWordsStudiedToday = cur.getCount();
        cur = db.query("word INNER JOIN category ON word.category_id = category.id", null, "category.is_skip = 0 AND word.unseen = 1", null, null, null, null);
        int leftNewWords = cur.getCount();
        cardDatabase.close();
        return Integer.toString(newWordsStudiedToday) + " / " + Integer.toString(leftNewWords);
    }

    public String getTodayReview(long days_since_start) {
        CardDatabase cardDatabase = new CardDatabase(HexCsvAndroid.context);
        SQLiteDatabase db = cardDatabase.getReadableDatabase();
        Cursor cur = db.query("word INNER JOIN category ON word.category_id = category.id", null, "category.is_skip = 0 AND word.was_unseen = 0 AND last_rep = '" + addStatString(days_since_start, eightDigits) + "'", null, null, null, null);
        int reviewWordsStudiedToday = cur.getCount();
        cardDatabase.close();
        return Integer.toString(reviewWordsStudiedToday);
    }

    public long hexLong()
    {
        long v = 0;

        while (pos < buffer.length
               && buffer[pos] != ',' && buffer[pos] != '\n')
        {
            v = v * 16 + Character.digit(buffer[pos], 16);
            ++pos;
        }
        ++pos;

        return v;
    }

    public long hexLong(String str)
    {
        long v = 0;

        for (int i=0; i < str.length(); i++) {
            v = v * 16 + Character.digit(str.charAt(i), 16);
        }

        return v;
    }

    private int readLine(InputStreamReader in)
        throws IOException
    {
        int total = 0;
        int last = 0;
        
        while (total < statLineLength) {
            last = in.read(buffer, total, statLineLength - total);
            if (last == -1) {
                break;
            }
            total += last;
        }
        
        return total;
    }
    
    public void readCard(Cursor cursor, int i)
            throws IOException
    {
        serial = i;
        setId(cursor.getLong(cursor.getColumnIndex("id")));
        setOldId(cursor.getLong(cursor.getColumnIndex("old_id")));
        setWord(cursor.getString(cursor.getColumnIndex("word")));
        setPinyin(cursor.getString(cursor.getColumnIndex("pinyin")));
        Log.d("myLogs", "id = " + getId().toString() + " word = " + cursor.getString(cursor.getColumnIndex("word")));
        grade = (int)hexLong( cursor.getString(cursor.getColumnIndex("grade")) );
        if (grade < 0 || grade > 5) {
            throw new IOException("invalid grade value ("
                    + Integer.toString(grade)
                    + ", at #"
                    + Integer.toString(i) + ")");
        }
        easiness = (int)hexLong( cursor.getString(cursor.getColumnIndex("easiness")) );
        if (easiness < 0) {
            throw new IOException("invalid easiness value ("
                    + Integer.toString(easiness)
                    + ", at #"
                    + Integer.toString(i) + ")");
        }
        acq_reps = (int)hexLong( cursor.getString(cursor.getColumnIndex("acq_reps")) );
        ret_reps = (int)hexLong( cursor.getString(cursor.getColumnIndex("ret_reps")) );
        lapses = (int)hexLong( cursor.getString(cursor.getColumnIndex("lapses")) );
        if (lapses < 0) {
            throw new IOException("invalid lapses value ("
                    + Integer.toString(lapses)
                    + ", at #"
                    + Integer.toString(i) + ")");
        }
        acq_reps_since_lapse = (int)hexLong( cursor.getString(cursor.getColumnIndex("acq_reps_since_lapse")) );
        ret_reps_since_lapse = (int)hexLong( cursor.getString(cursor.getColumnIndex("ret_reps_since_lapse")) );
        last_rep = hexLong( cursor.getString(cursor.getColumnIndex("last_rep")) );
        next_rep = hexLong( cursor.getString(cursor.getColumnIndex("next_rep")) );
        unseen = (hexLong( cursor.getString(cursor.getColumnIndex("unseen")) ) == 1);
        was_unseen = (hexLong( cursor.getString(cursor.getColumnIndex("was_unseen")) ) == 1);
        is_read = (hexLong( cursor.getString(cursor.getColumnIndex("is_read")) ) == 1);
        //category = (int)hexLong( cursor.getString(cursor.getColumnIndex("word.category")) );
        category = cursor.getInt(cursor.getColumnIndex("category_id"));

        inverse = (int)hexLong( cursor.getString(cursor.getColumnIndex("inverse")) );
    }

    // Adapted directly from Peter Bienstman's Mnemosyne 1.x
    private int calculateIntervalNoise(int interval) {
        int a;

        if (interval == 0) {
            return 0;

        } else if (interval == 1) {
            return rand.nextInt(2);

        } else if (interval <= 10) {
            return (rand.nextInt(3) - 1);

        } else if (interval <= 60) {
            return rand.nextInt(7) - 3;

        } else {
            a = interval / 20;
            return (rand.nextInt(2 * a + 1) - a);
        }
    }

    public void skipInverse()
    {
        if (cardlookup != null) {
            Card inverse_card = cardlookup.getCard(inverse);
            if (inverse_card != null) {
                inverse_card.setSkip();
            }
        }
    }

    private boolean hasSounds(String text)
    {
        return (text != null && text.startsWith(sound_prefix));
    }

    private String[] getSounds(String text)
    {
        if (!hasSounds(text)) {
            return (new String[0]);
        }

        Vector sounds = new Vector(3, 2);

        int pos = 0;
        while (text.startsWith(sound_prefix, pos)) {
            pos += sound_prefix_offset;

            int npos = text.indexOf('"', pos);
            if (npos != -1) {
                sounds.addElement(text.substring(pos, npos));
            }

            pos = text.indexOf('\n', pos);
            if (pos == -1) {
                break;
            }
            ++pos;
        }

        String[] r = new String[sounds.size()];
        sounds.copyInto(r);
        return r;
    }

    private String skipPreamble(String text)
    {
        if (text == null) {
            return null;
        }

        int pos = 0;
        while (text.startsWith(sound_prefix, pos)) {
            pos = text.indexOf('\n', pos);
            if (pos == -1) {
                return text;
            }

            ++pos;
        }

        return text.substring(pos);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOldId() { return this.old_id; }

    public void setOldId(Long old_id) { this.old_id = old_id; }

    private void ensureQuestionText()
    {
        if (question == null) {
            try {
                cardlookup.loadCardData();
            } catch (IOException e) {}
        }
    }

    public String getQuestion()
    {
        ensureQuestionText();
        return skipPreamble(question);
    }

    public String[] getQuestionSounds()
    {
        ensureQuestionText();
        return getSounds(question);
    }

    public boolean hasQuestionSounds()
    {
        ensureQuestionText();
        return hasSounds(question);
    }

    public void setQuestion(String question)
    {
        this.question = question;
    }

    public String getAnswer()
    {
        ensureQuestionText();
        return skipPreamble(answer);
    }

    public String[] getAnswerSounds()
    {
        ensureQuestionText();
        return getSounds(answer);
    }

    public boolean hasAnswerSounds()
    {
        ensureQuestionText();
        return hasSounds(answer);
    }

    public void setAnswer(String answer)
    {
        this.answer = answer;
    }

    public void setVideoId(Long videoId)
    {
        this.videoId = videoId;
    }

    public Long getVideoId()
    {
        return this.videoId;
    }

    public boolean isMarked()
    {
        return marked;
    }

    public void setMarked(boolean marked)
    {
        this.marked = marked;
    }

    public void toggleMarked()
    {
        marked = !marked;
    }

    public boolean getOverlay()
    {
        return overlay;
    }

    public void setOverlay(boolean overlay)
    {
        this.overlay = overlay;
    }

    public boolean isSkip()
    {
        return (skip || cardlookup.skipCategory(category-1));
    }

    public void setSkip()
    {
        skip = true;
    }

    // Adapted directly from Peter Bienstman's Mnemosyne 1.x (process_answer)
    public void gradeCard(long days_since_start, int new_grade)
        throws IOException
    {

        long scheduled_interval;
        long actual_interval;
        float new_interval = 0.0f;
        int noise;

        // Don't schedule inverse or identical questions on the same day.
        //skipInverse();

        // Calculate scheduled and actual interval, taking care of corner
        // case when learning ahead on the same day.
        
        scheduled_interval = next_rep - last_rep;
        actual_interval    = days_since_start - last_rep;

        if (actual_interval == 0) {
            actual_interval = 1; // Otherwise new interval can become zero.
        }

        if (acq_reps == 0 && ret_reps == 0) { // is_new()

            // The item is not graded yet, e.g. because it is imported.

            acq_reps = 1;
            acq_reps_since_lapse = 1;

            actual_interval = 0;

            new_interval = initial_interval[new_grade];

        } else if (grade < 2 && new_grade < 2) {
            // In the acquisition phase and staying there.
            acq_reps += 1;
            acq_reps_since_lapse += 1;
            new_interval = 0.0f;

        } else if (grade < 2 && new_grade >= 2 && new_grade <= 5) {
             // In the acquisition phase and moving to the retention phase.
             acq_reps += 1;
             acq_reps_since_lapse += 1;
             new_interval = 1.0f;

        } else if ((grade >= 2 && grade <= 5) && new_grade < 2) {
             // In the retention phase and dropping back to the acquisition phase.
             ret_reps += 1;
             lapses += 1;
             acq_reps_since_lapse = 0;
             ret_reps_since_lapse = 0;

             new_interval = 0.0f;

             // Move this item to the front of the list, to have precedence over
             // items which are still being learned for the first time.
             // THIS IS NOW DONE IN shiftforgottentonew()

        } else if ((grade >= 2 && grade <= 5)
                    && (new_grade >= 2 && new_grade <= 5)) {
            // In the retention phase and staying there.
            ret_reps += 1;
            ret_reps_since_lapse += 1;

            if (actual_interval >= scheduled_interval) {
                if (new_grade == 2) {
                    easiness -= 160;
                } else if (new_grade == 3) {
                    easiness -= 140;
                } else if (new_grade == 5) {
                    easiness += 100;
                }
                
                if (easiness < 1300) {
                    easiness = 1300;
                }
            }
                
            new_interval = 0.0f;
            
            if (ret_reps_since_lapse == 1) {
                new_interval = 6.0f;

            } else {
                if (new_grade == 2 || new_grade == 3) {
                    if (actual_interval <= scheduled_interval) {
                        new_interval = actual_interval * feasiness();
                    } else {
                        new_interval = scheduled_interval;
                    }

                } else if (new_grade == 4) {
                    new_interval = actual_interval * feasiness();

                } else if (new_grade == 5) {
                    if (actual_interval < scheduled_interval) {
                        new_interval = scheduled_interval; // Avoid spacing.
                    } else {
                        new_interval = actual_interval * feasiness();
                    }
                }
            }

            // Shouldn't happen, but build in a safeguard.
            if (new_interval == 0)
                new_interval = scheduled_interval;
        }

        // Add some randomness to interval.
        noise = calculateIntervalNoise((int)new_interval);

        // Update grade and interval.
        grade    = new_grade;
        if ((last_rep != days_since_start) && (was_unseen)) was_unseen = false;
        last_rep = days_since_start;
        next_rep = (int)(days_since_start + new_interval + noise);
        if (unseen) was_unseen = true;
        unseen   = false;
        is_read = !is_read;

        writeCard();
    }

    public void appendSerial(StringBuffer path)
    {
        int d = 12;
        while (d >= 0) {
            path.append(hexDigit(serial >> d & 0x000f));
            d -= 4;
        }
    }

    public void setWord(String word) { this.word = word; }

    public String getWord() { return this.word; }

    public void setPinyin(String pinyin) { this.pinyin = pinyin; }

    public String getPinyin() { return this.pinyin; }

    public int getCategoryId() { return this.categoryId; }

    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getQuestionType() {
        return this.questionType;
    }

    public void setQuestionType(String questionType) { this.questionType = questionType; }

    public void setTranslation(String translation) {
        this.translation = translation;
    }

    public String getTranslation() {
        return this.translation;
    }
}

