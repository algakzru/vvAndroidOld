package kz.algakzru.youtubevideovocabulary.mnemogogo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


public class CardDatabase extends SQLiteOpenHelper {

	SQLiteDatabase copieddb;
	Context context;
	static int DB_VERSION = 20150807; // DB version
	public static String DB_PATH, DB_NAME = "DatabaseVocabulary.db", DB_NAME2 = "videovocabulary.db";
	private static final String CARDS_TABLE_NAME = "word";

	public CardDatabase(Context _context) {
		super(_context, DB_NAME2, null, DB_VERSION);

		context = _context;
		DB_PATH = context.getDatabasePath(DB_NAME).toString();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Toast.makeText(context, "--- Database has been created ---",
				Toast.LENGTH_LONG).show();

		createDatabase(db);
		updateDatabase(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Toast.makeText(
				context,
				"--- Database has been updated from " + oldVersion + " to "
						+ newVersion + " version --- ", Toast.LENGTH_LONG)
				.show();
		Log.d(CardDatabase.class.getName(),
				"--- Database has been updated from " + oldVersion + " to "
						+ newVersion + " version --- ");
		if (oldVersion < DB_VERSION)
			createDatabase(db);
		db.beginTransaction();
		try {
			updateDatabase(db);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	void createDatabase(SQLiteDatabase db) {
		db.execSQL("DROP TABLE IF EXISTS word");
		db.execSQL("create table if not exists word ("
				+ "id INTEGER primary key autoincrement,"
                + "old_id INTEGER not null,"
                + "word TEXT not null,"
				+ "pinyin TEXT not null,"
				+ "category_id INTEGER not null,"
				+ "question_type TEXT not null,"
				+ "grade INTEGER not null,"
                + "easiness TEXT not null,"
				+ "acq_reps TEXT not null,"
                + "ret_reps TEXT not null,"
                + "lapses TEXT not null,"
                + "acq_reps_since_lapse TEXT not null,"
                + "ret_reps_since_lapse TEXT not null,"
                + "last_rep TEXT not null,"
                + "next_rep TEXT not null,"
                + "unseen INTEGER not null,"
				+ "was_unseen INTEGER not null,"
				+ "is_read INTEGER not null,"
				+ "category TEXT not null,"
                + "inverse TEXT not null" + ");");

		db.execSQL("DROP TABLE IF EXISTS category");
		db.execSQL("create table if not exists category ("
				+ "id INTEGER primary key autoincrement,"
				+ "category TEXT not null,"
				+ "is_skip INTEGER not null" + ");");

		db.execSQL("DROP TABLE IF EXISTS current_category");
		db.execSQL("create table if not exists current_category ("
				+ "category_id INTEGER not null" + ");");
		
		db.execSQL("DROP TABLE IF EXISTS word_to_study");
		db.execSQL("create table if not exists word_to_study ("
                + "word_id INTEGER not null,"
                + "last INTEGER not null" + ");");

		Log.d(CardDatabase.class.getName(), "--- Database has been created ---");
	}

	void updateDatabase(SQLiteDatabase db) {
		try {
			copyDataBase(DB_NAME);
		} catch (IOException ioe) {
			throw new Error("Unable to create database");
		}

		SQLiteDatabase copieddb = SQLiteDatabase.openDatabase(DB_PATH, null, SQLiteDatabase.OPEN_READONLY);
		db.execSQL("DROP TABLE IF EXISTS questions_speaking");
		db.execSQL("create table if not exists questions_speaking ("
				+ "type TEXT not null," + "topic TEXT not null,"
				+ "time INTEGER not null" + ");");
		Cursor cursor = copieddb.query("questions_speaking", null, null, null, null, null, null);
		if (cursor.moveToFirst()) {
			do {
				int type = cursor.getInt(cursor.getColumnIndex("type"));
				String topic = cursor.getString(cursor.getColumnIndex("topic"));
				int timeLimit = cursor.getInt(cursor.getColumnIndex("time"));
				ContentValues cv = new ContentValues();
				if (type == 1)
					cv.put("type", "初级");
				if (type == 2)
					cv.put("type", "中级");
				if (type == 3)
					cv.put("type", "高级");
				if (type == 4)
					cv.put("type", "IELTS");
				cv.put("topic", topic);
				cv.put("time", timeLimit);
				db.insert("questions_speaking", null, cv);
			} while (cursor.moveToNext());
		}
		cursor.close();

		db.execSQL("DROP TABLE IF EXISTS video");
		db.execSQL("create table if not exists video ("
				+ "video_id INTEGER not null,"
				+ "movie_name TEXT not null," + "file_name TEXT not null,"
				+ "youtube_id TEXT not null," + "youku_id TEXT not null," + "youku_thumbnail TEXT not null" + ");");

		cursor = copieddb.query("video", null, null, null, null, null, null);
		if (cursor.getCount() > 0) {
			while (cursor.moveToNext()) {
				ContentValues cv = new ContentValues();
				cv.put("video_id",
						cursor.getString(cursor.getColumnIndex("video_id")));
				cv.put("movie_name",
						cursor.getString(cursor.getColumnIndex("movie_name")));
				cv.put("file_name",
						cursor.getString(cursor.getColumnIndex("file_name")));
				cv.put("youtube_id",
						cursor.getString(cursor.getColumnIndex("youtube_id")));
				cv.put("youku_id",
						cursor.getString(cursor.getColumnIndex("youku_id")));
				cv.put("youku_thumbnail",
						cursor.getString(cursor.getColumnIndex("youku_thumbnail")));
				db.insert("video", null, cv);
			}
		}
		cursor.close();

		db.execSQL("DROP TABLE IF EXISTS word_video");
		db.execSQL("create table if not exists word_video ("
				+ "word_id INTEGER not null,"
                + "video_id INTEGER not null,"
                + "sentence TEXT not null" + ");");

		cursor = copieddb.query("word_video INNER JOIN video ON word_video.video_id = video.video_id where video.movie_name LIKE '地三鲜' or video.movie_name LIKE '锦绣华南'", null, null, null, null, null, null);
		if (cursor.getCount() > 0) {
			while (cursor.moveToNext()) {
				ContentValues cv = new ContentValues();
				cv.put("word_id", cursor.getString(cursor.getColumnIndex("word_id")));
				cv.put("video_id", cursor.getString(cursor.getColumnIndex("video_id")));
                cv.put("sentence", cursor.getString(cursor.getColumnIndex("sentence")));
                db.insert("word_video", null, cv);
			}
		}
		cursor.close();

		db.execSQL("DROP TABLE IF EXISTS subtitles_锦绣华南");
		db.execSQL("create table if not exists subtitles_锦绣华南 ("
				+ "start TEXT not null,"
				+ "finish TEXT not null,"
                + "hanzi TEXT not null,"
                + "pinyin TEXT not null" + ");");

        cursor = copieddb.query("subtitles_锦绣华南", null, null, null, null, null, null);
		if (cursor.getCount() > 0) {
			while (cursor.moveToNext()) {
				ContentValues cv = new ContentValues();
				cv.put("start", cursor.getString(cursor.getColumnIndex("start")));
				cv.put("finish", cursor.getString(cursor.getColumnIndex("finish")));
				cv.put("hanzi", cursor.getString(cursor.getColumnIndex("hanzi")));
                cv.put("pinyin", cursor.getString(cursor.getColumnIndex("pinyin")));
                db.insert("subtitles_锦绣华南", null, cv);
			}
		}
		cursor.close();

//		setHSKTranslations();
		copyCategory(copieddb, db, "锦绣华南（HSK6）", "高级");
		copyCategory(copieddb, db, "锦绣华南（HSK5）", "中级");
		copyCategory(copieddb, db, "锦绣华南（熟语）", "高级");
		copyCategory(copieddb, db, "地三鲜", "高级");
	}

	private void copyCategory(SQLiteDatabase copieddb, SQLiteDatabase db, String category, String question_type) {
		ContentValues cv = new ContentValues();
		cv.put("category", category);
		cv.put("is_skip", false);
		Long categoryId = db.insert("category", null, cv);
		if (category.contains("HSK4")) {
			cv = new ContentValues();
			cv.put("category_id", categoryId);
			db.insert("current_category", null, cv);
		}

		Cursor cursor = copieddb.query("word INNER JOIN word_video ON word_video.word_id = word.id", null, "category_id = '"
				+ getCatergoryID(copieddb, category) + "'", null, "word_video.word_id", null,
				"COUNT(word_video.video_id) DESC");
		if (cursor.moveToFirst()) {
			do {
				int old_id = cursor.getInt(cursor.getColumnIndex("id"));
				String word = cursor.getString(cursor.getColumnIndex("word"));
				String category_id = getCatergoryID(db, category);
				String word_id = "";
				Cursor cursorExists = db.query("word", null, "word = '" + word
						+ "' and category_id = '" + category_id + "'", null,
						null, null, null);
				if (cursorExists.moveToFirst()) {
					do {
						word_id = cursorExists.getString(cursorExists
								.getColumnIndex("id"));
					} while (cursorExists.moveToNext());
				}
				cursorExists.close();
				String word_id_copied = cursor.getString(cursor.getColumnIndex("id"));
				if (word_id == "") {
					// Log.d(tag, word + " not exists");
					cv = new ContentValues();
					cv.put("word", word);
                    cv.put("old_id", old_id);
                    cv.put("pinyin", cursor.getString(cursor.getColumnIndex("pronunciation")));
					cv.put("category_id", category_id);
					cv.put("question_type", question_type);
					cv.put("grade", 0);
					cv.put("easiness", "09c4");
					cv.put("acq_reps", "0000");
                    cv.put("ret_reps", "0000");
                    cv.put("lapses", "0000");
                    cv.put("acq_reps_since_lapse", "0000");
                    cv.put("ret_reps_since_lapse", "0000");
                    cv.put("last_rep", "00000000");
                    cv.put("next_rep", "00000000");
                    cv.put("unseen", 1);
					cv.put("was_unseen", 0);
					cv.put("is_read", 1);
					cv.put("category", Card.addStatString(Long.valueOf(category_id), Card.fourDigits));
                    cv.put("inverse", "ffff");
                    db.insert("word", null, cv);
				} else
					Log.d(CardDatabase.class.getName(), word_id_copied + " " + word + " exists");
			} while (cursor.moveToNext());
		}
		cursor.close();

	}

	@Override
	public synchronized void close() {
		if (copieddb != null)
			copieddb.close();
		super.close();
	}

	private void copyDataBase(String fileFrom) throws IOException {
		String fileTo = context.getDatabasePath(fileFrom).toString();
		InputStream assestDB = context.getAssets().open(fileFrom);
		OutputStream appDB = new FileOutputStream(fileTo, false);
		byte[] buffer = new byte[1024];
		int length;
		while ((length = assestDB.read(buffer)) > 0) {
			appDB.write(buffer, 0, length);
		}
		appDB.flush();
		appDB.close();
		assestDB.close();
		Log.d(CardDatabase.class.getName(), fileFrom + " has been copied to " + fileTo);
	}

	public String getCatergoryID(SQLiteDatabase db, String category) {
		String category_id = "";
		String selection = "category = '" + category + "'";
		Cursor cursor = db.query("category", null, selection, null, null, null,
				null);
		if (cursor.moveToFirst()) {
			do {
				category_id = cursor.getString(cursor.getColumnIndex("id"));
			} while (cursor.moveToNext());
		}
		cursor.close();
		return category_id;
	}

	/*
	public String getCatergoryByID(SQLiteDatabase db, String category_id) {
		String value = "";
		String selection = "id = '" + category_id + "'";
		Cursor cursor = db.query("category", null, selection, null, null, null,
				null);
		if (cursor.moveToFirst()) {
			do {
				value = cursor.getString(cursor.getColumnIndex("category"));
			} while (cursor.moveToNext());
		}
		cursor.close();
		return value;
	}

	public static ContentValues contentValuesFromCard(Card card) {
		ContentValues values = new ContentValues();
		values.put("word", card.getWord());
		values.put("translation", card.getTranslation());
		values.put("category_id", card.getCategoryId());
		values.put("old_id", String.valueOf(card.getOldId()));
		values.put("ef", card.getEFactor());
		values.put("count", card.getCount());
		values.put("interval", card.getInterval());
        values.put("previous", card.getPreviousTimeSeconds());
        values.put("last", card.getLastTimeSeconds());
		return values;
	}

	public Cursor getCardsForQuiz() {
		// select all cards for which the last(timestamp) + interval (days) <
		// today
		SQLiteDatabase db = getReadableDatabase();
		String selection = getSelected_categories();
        selection += (selection.isEmpty()?"":" and ") + "last != 0  and date(last, 'unixepoch', '+' || interval || ' days') <= date('now') ";
		L.d(CardDatabase.class.getName(), "Runing query: [%s]", selection);
		return db.query("word", null, selection, null, null, null,
				"last limit 3");
	}

	public void test() {
		SQLiteDatabase db = getReadableDatabase();
		String selection = getSelected_categories();
        selection += (selection.isEmpty()?"":" and ") + "last != 0 and date(last, 'unixepoch', '+' || interval || ' days') <= date('now') ";
		//Cursor cur = db.query("word", null, selection, null, null, null, "last DESC limit 3");
		Cursor cur = db.rawQuery("SELECT word, interval, last, date(last, 'unixepoch') as timestamp, date(last, 'unixepoch', '+' || interval || ' days') as repeat_day, date('now') as today FROM word WHERE " + selection + " ORDER BY last DESC LIMIT 3", null);
		if (cur.getCount() > 0) {
			while (cur.moveToNext()) {
				String word = cur.getString(cur.getColumnIndex("word"));
				Integer interval = cur.getInt(cur.getColumnIndex("interval"));
				Long last = cur.getLong(cur.getColumnIndex("last")) * 1000;
				String timestamp = cur.getString(cur.getColumnIndex("timestamp"));
				String repeatDay = cur.getString(cur.getColumnIndex("repeat_day"));
				String toDay = cur.getString(cur.getColumnIndex("today"));
				String moreOrLess = (last + 24*60*60*1000*interval <= new Date().getTime() ? " <= ":" > ");
				//Log.d("myLogs", word + " -> " + String.valueOf(last) + " + " + String.valueOf(24*60*60*1000*interval) + " = " + String.valueOf(last + 24*60*60*1000*interval) + moreOrLess + String.valueOf(new Date().getTime()));
				Log.d("myLogs", word + " -> " + timestamp + " + " + String.valueOf(interval) + " = " + repeatDay + moreOrLess + toDay);
			}
		}
	}

	public Cursor getNewCardsForQuiz(int limit) {
		// select all cards for which the last(timestamp) + interval (days) <
		// today
		SQLiteDatabase db = getReadableDatabase();
		String selection = getSelected_categories();
        selection += (selection.isEmpty()?"":" and ") + "last = 0 ";
		L.d(CardDatabase.class.getName(), "Runing query: [%s]", selection
				+ "; limit = " + String.valueOf(limit));
		// return db.rawQuery(cardsForQuizQryNew + String.valueOf(limit), null);
		return db.query("word", null, selection, null, null, null,
				"last limit " + String.valueOf(limit));
	}*/

	public Card getCard(Long id) {
		SQLiteDatabase db = getReadableDatabase();
		String[] selectionArgs = { id.toString() };
		Cursor cur = db.query("word", null, "id = ?", selectionArgs, null,
				null, null);
		if (cur.getCount() == 1) {
			cur.moveToFirst();
			return cursorRowToCard(cur);
		}
		return null;
	}


	public Card cursorRowToCard(Cursor cur) {
		Card card = new Card();
        card.setId(cur.getLong(cur.getColumnIndex("id")));
		card.setOldId(cur.getLong(cur.getColumnIndex("old_id")));
        card.setWord(cur.getString(cur.getColumnIndex("word")));
        card.setPinyin(cur.getString(cur.getColumnIndex("pinyin")));
        //card.setTranslation(cur.getString(cur.getColumnIndex("translation")));
        card.setCategoryId(cur.getInt(cur.getColumnIndex("category_id")));
        card.setQuestionType(cur.getString(cur.getColumnIndex("question_type")));
//        card.setCount(cur.getInt(cur.getColumnIndex("count")));
//        card.setEFactor(cur.getFloat(cur.getColumnIndex("ef")));
//        card.setInterval(cur.getInt(cur.getColumnIndex("interval")));
//        card.setPreviousTime(cur.getLong(cur.getColumnIndex("previous")) * 1000 );
//        card.setLastTime( cur.getLong(cur.getColumnIndex("last")) * 1000 );
		//String moreOrLess = (last + 24*60*60*1000*interval <= new Date().getTime() ? " <= ":" > ");
		//Log.d("myLogs", word + " -> " + String.valueOf(last) + " + " + String.valueOf(24*60*60*1000*interval) + " = " + String.valueOf(last + 24*60*60*1000*interval) + moreOrLess + String.valueOf(new Date().getTime()));
        return card;
	}

    /*
	public List<Card> cursorToCards(Cursor cur) {
		if (cur.getCount() > 0) {
			List<Card> cards = new ArrayList<Card>();
			while (cur.moveToNext()) {
				cards.add(cursorRowToCard(cur));
			}
			return cards;
		} else {
			return null;
		}
	}

	*//**
	 * Update/Insert a card in the database.
	 *
	 * @param card
	 *            a Card<S1, S2>.
	 * @returns a Card<S1, S2>.
	 *//*
	public Long upsertCard(Card card) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = contentValuesFromCard(card);
//		if (card.getId() == null) {
//			L.i("upsertCard", "Inserting card %s", card.getWord());
//			// an insert then
//			Long id = db.insertOrThrow(CARDS_TABLE_NAME, null, values);
//			card.setId(id);
//			L.i("upsertCard", "Cards id %d", card.getId());
//			return id;
//		} else {
			L.i("upsertCard", "Updating card %d", card.getId());
			// an update
			String[] whereArgs = { card.getId().toString() };
			db.update(CARDS_TABLE_NAME, values, "id = ?", whereArgs);

			// Continue the repetitions until all of these items score at least four.
			if (card.getQualityResponse() < 4) {
				ContentValues cv = new ContentValues();
				cv.put("last", card.getLastTimeSeconds());
				db.update("word_to_study", cv, "word_id = ?", whereArgs);
			} else
				db.delete("word_to_study", "word_id = ?", whereArgs);
			return card.getId();
//		}
	}*/

	public List<String> getHSKKQuestions(String question_type) {
		// select all cards for which the last(timestamp) + interval (days) <
		// today
		SQLiteDatabase db = getReadableDatabase();
		String selection = "type = '" + question_type + "'";
		Cursor cur = db.query("questions_speaking", null, selection, null, null, null, "RANDOM()");
		if (cur.getCount() > 0) {
			List<String> questions = new ArrayList<String>();
			while (cur.moveToNext()) {
				questions.add(unescape(cur.getString(cur.getColumnIndex("topic"))));
			}
			return questions;
		} else {
			return null;
		}
	}

	private String unescape(String stringFromDatabase) {
		return stringFromDatabase.replace("\\n", "\n");
	}

	public String getSelected_categories() {
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query("category", null, "is_skip = 0", null, null, null, null);
		String selected_categories = "";
		if (cursor.getCount() > 0) {
			while (cursor.moveToNext()) {
				selected_categories += (selected_categories == "" ? "category_id in ('" : ",'")
						+ cursor.getString(cursor.getColumnIndex("id"))
						+ "' ";
			}
            selected_categories += (selected_categories == "" ? "" : ")");
		}
		return selected_categories;
	}

    /*
    public Cursor getOldCardForQuiz() {
        // select all cards for which the last(timestamp) + interval (days) < today
        SQLiteDatabase db = getReadableDatabase();
        String selection = getSelected_categories();
        selection += (selection.isEmpty()?"":" and ") + "word.last != 0 and date(word.last, 'unixepoch', '+' || interval || ' days') <= date('now') and word_to_study.word_id IS NULL";
        L.d(CardDatabase.class.getName(), "Runing query: [%s]", selection);
        return db.query("word LEFT JOIN word_to_study on word_to_study.word_id = word.id", null, selection, null, null, null, "word.last limit 1");
    }

    public Cursor getNewCardForQuiz() {
        // select all cards for which the last(timestamp) + interval (days) < today
        SQLiteDatabase db = getReadableDatabase();
        String selection = getSelected_categories();
        selection += (selection.isEmpty()?"":" and ") + "word.last = 0 and word_to_study.word_id IS NULL";
        L.d(CardDatabase.class.getName(), "Runing query: [%s]", selection);
        return db.query("word LEFT JOIN word_to_study on word_to_study.word_id = word.id", null, selection, null, null, null, "word.last limit 1");
    }

    public Card generateCard() {
        Cursor cur = null;
        if (getOldCardForQuiz().getCount() == 1)
            cur = getOldCardForQuiz();
        else if (getNewCardForQuiz().getCount() == 1)
            cur = getNewCardForQuiz();

        Card card = null;
        if (cur != null) {
            cur.moveToFirst();
            card = cursorRowToCard(cur);
        }

        Log.d("myLogs", "generateCard");
        return card;
    }

    public void addCardToStudy(Card card) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("word_id", card.getId());
        cv.put("last", new Date().getTime() / 1000);
        db.insert("word_to_study", null, cv);
    }

    public Card getCardToStudy() {
		SQLiteDatabase db = getReadableDatabase();
		if (db.query("word_to_study", null, null, null, null, null, null).getCount() == 0) return null;
		Cursor cur = db
				.query("word_to_study inner join word on word_to_study.word_id = word.id",
						null, null, null, null, null, "word_to_study.last LIMIT 1");
		if (cur.getCount() == 1) {
			cur.moveToFirst();
			return cursorRowToCard(cur);
		}
		return null;
	}

	public int getCountOfCardToStudy() {
		SQLiteDatabase db = getReadableDatabase();
		Cursor cur = db.query("word_to_study", null, null, null, null, null, null);
		return cur.getCount();
	}

	public int getCountOfReviewCardStudiedToday() {
		SQLiteDatabase db = getReadableDatabase();
		String selection = getSelected_categories();
		selection += (selection.isEmpty()?"":" and ") + "word.previous != 0 and date(word.last, 'unixepoch') = date('now')";
		Cursor cur = db.query("word", null, selection, null, null, null, null);
		return cur.getCount();
	}

	public int getCountOfReviewCardLeft() {
		SQLiteDatabase db = getReadableDatabase();
		String selection = getSelected_categories();
		selection += (selection.isEmpty()?"":" and ") + "word.last != 0 and date(word.last, 'unixepoch', '+' || interval || ' days') <= date('now')";
		Cursor cur = db.query("word", null, selection, null, null, null, null);
		return cur.getCount();
	}

	public int getCountOfNewCardStudiedToday() {
        SQLiteDatabase db = getReadableDatabase();
        String selection = getSelected_categories();
        selection += (selection.isEmpty()?"":" and ") + "word.previous = 0 and date(word.last, 'unixepoch') = date('now')";
        Cursor cur = db.query("word", null, selection, null, null, null, null);
        return cur.getCount();
    }

    public int getCountOfNewCardLeft() {
		SQLiteDatabase db = getReadableDatabase();
		String selection = getSelected_categories();
        selection += (selection.isEmpty()?"":" and ") + "last = 0 ";
		Cursor cur = db.query("word", null, selection, null, null, null, null);
		return cur.getCount();
	}*/

    public Long getTimelimit(String question_type) {
		SQLiteDatabase db = getReadableDatabase();
		Cursor cur = db.query("questions_speaking", null, "type = '" + question_type + "'", null, null, null, "time LIMIT 1");
		if (cur.getCount() == 1) {
			cur.moveToFirst();
			return cur.getLong(cur.getColumnIndex("time"));
		}
		return null;
	}

	

}