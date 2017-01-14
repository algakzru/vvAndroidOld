/*
 * Copyright (C) 2010 Timothy Bourke
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

package kz.algakzru.youtubevideovocabulary.mnemododo;

import android.util.Log;

import kz.algakzru.youtubevideovocabulary.R;
import kz.algakzru.youtubevideovocabulary.mnemogogo.HexCsvAndroid;
import kz.algakzru.youtubevideovocabulary.mnemogogo.Progress;

import java.io.IOException;

public class CardStore
{
    public HexCsvAndroid cards = null;
    public String cards_path = null;

    protected long cards_timestamp = 0;
    protected int cards_to_load = 10;

    protected LoadStatsTask stats_task = null;
    protected boolean zombie_stats_task = false;

    public CardStore()
    {
    }

    public CardStore(int cards_to_load,
                     TaskListener<String> callback)
    {
        this.cards_to_load = cards_to_load;

        stats_task = new LoadStatsTask(callback);
        stats_task.execute();
    }

    public boolean active()
    {
        return (cards != null);
    }

    public void updateCallback(TaskListener<String> callback)
    {
        if (stats_task != null) {
            stats_task.updateCallback(callback);

            if (zombie_stats_task) {
                stats_task = null;
            }
        }
    }

    private class LoadStatsTask
            extends ProgressTask<String, String>
    {
        protected HexCsvAndroid loaddb;
        protected String error_msg;

        LoadStatsTask(TaskListener<String> callback)
        {
            super(callback, R.string.loading_card_dir);
        }

        public String doInBackground(String... path)
        {
            try {
                loaddb = new HexCsvAndroid(LoadStatsTask.this);
                loaddb.cards_to_load = cards_to_load;


                try {
                    loaddb.backupCards(null);
                } catch (IOException e) { }

            } catch (Exception e) {
                stopOperation();
                return getString(R.string.corrupt_card_dir)
                    + "\n\n(" + e.toString() + ")";

            } catch (OutOfMemoryError e) {
                stopOperation();
                return getString(R.string.not_enough_memory_to_load);
            }

            stopOperation();
            return null;
        }

        public void onPostExecute(String error_msg)
        {
            if (error_msg == null) {
                cards = loaddb;
                cards_timestamp = loaddb.nowInDays();
            } else {
                cards = null;
            }

            if (callback == null) {
                finished = true;
                cached_result = error_msg;
                zombie_stats_task = true;

            } else {
                callback.onFinished(error_msg);
                stats_task = null;
            }
        }
    }

    boolean needsReload()
    {
        return (cards != null && cards_timestamp != cards.nowInDays());
    }

    void resume()
    {
        if (cards != null) {
            cards.reopen(cards_path);
        }
    }

    public void close()
    {
        if (cards != null) {
            cards.close();
        }
    }
    
    public boolean loadingCards()
    {
        return (stats_task != null);
    }

    public boolean needLoadCards(String settings_cards_path)
    {
        return (cards_path == null && settings_cards_path != null)
               || (cards_path != null
                   && settings_cards_path != null
                   && !cards_path.equals(settings_cards_path));
    }

    public void saveCards()
        throws IOException
    {
        if (cards != null) {
            cards.writeCards(new StringBuffer(cards_path), null);
        }
    }

    void writeCategorySkips()
    {
        cards.writeCategorySkips(new StringBuffer(cards_path));
    }

    void onPause()
    {
        if (stats_task != null) {
            stats_task.pause();
        }
    }

    public int numScheduled() {
        if (cards == null) {
            return 0;
        } else {
            return cards.numScheduled();
        }
    }

    public int getTodayReview() {
        if (cards == null) {
            return 0;
        } else {
            return cards.getTodayReview();
        }
    }

    public boolean canLearnAhead() {
        if (cards == null) {
            return false;
        } else {
            return cards.canLearnAhead();
        }
    }

    public void setProgress(Progress progress)
    {
        if (cards != null) {
            cards.setProgress(progress);
        }
    }
}

