package kz.algakzru.youtubevideovocabulary;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import kz.algakzru.youtubevideovocabulary.mnemogogo.CardDatabase;

public class MainActivity extends Activity {

    TextView actionBarTitle;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_main);
        actionBarTitle = setActionBar(this, false);
        actionBarTitle.setText(R.string.app_name);

		CardDatabase cardDatabase = new CardDatabase(this);
		cardDatabase.getReadableDatabase();
		cardDatabase.close();
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
            case R.id.menu_add_shortcut:
                installHomescreenShortcut();
                break;
//            case R.id.menu_export_database:
//                exportDataBase();
//                break;
//            case R.id.menu_import_database:
//                //importDataBase();
//                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void installHomescreenShortcut() {
        //shorcutIntent object
        Intent shortcutIntent = new Intent(this, MainActivity.class);
        shortcutIntent.setAction(Intent.ACTION_MAIN);

        //shortcutIntent is added with addIntent
        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getResources().getString(R.string.youtube_api_demo));
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.mipmap.ic_launcher));
        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        addIntent.putExtra("duplicate", false);

        // finally broadcast the new Intent
        sendBroadcast(addIntent);
        Toast.makeText(this, "Shortcut has been added to home screen", Toast.LENGTH_LONG).show();
    }

    public void showWordlist(View view) {
        startActivity(new Intent(getApplicationContext(), VideoListActivityYouku.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));


	}

	public void showProductive(View view) {
		startActivity(new Intent(getApplicationContext(), StudyActivity.class)
				.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
	}

	public void showPracticeActivity(View view) {
//		startActivity(new Intent(getApplicationContext(), PracticeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

		Intent i = new Intent(getApplicationContext(), StudyActivityYouku.class);
		i.putExtra("vid", "XMTMxNDc4NDIzNg==");
		startActivity(i);
	}

	class class_category {
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

	public void importDataBase() {
		try {
			String fileFrom = getExternalFilesDir(null).toString()
					+ File.separator + CardDatabase.DB_NAME2;
			String fileTo = getDatabasePath(CardDatabase.DB_NAME2).toString();
			copyDataBase(fileFrom, fileTo);
		} catch (IOException ioe) {
			throw new Error("Unable to copy database");
		}
	}

	public void exportDataBase() {
		try {
			String fileFrom = getDatabasePath(CardDatabase.DB_NAME2).toString();
			String fileTo = getExternalFilesDir(null).toString()
					+ File.separator + CardDatabase.DB_NAME2;
			Log.d("myLogs", "all right");
			copyDataBase(fileFrom, fileTo);
			File file = new File(fileTo);
			file.setWritable(true);
		} catch (IOException ioe) {
			throw new Error("Unable to copy database");
		}
	}

	public void copyDataBase(String fileFrom, String fileTo) throws IOException {
		InputStream in = new FileInputStream(new File(fileFrom));
		OutputStream out = new FileOutputStream(new File(fileTo));

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
		Log.d("", "--- " + fileFrom + " has been copied to " + fileTo + " ---");
		Toast.makeText(this, fileFrom + "\n\nhas been copied to\n\n" + fileTo,
				Toast.LENGTH_LONG).show();
	}

	public static TextView setActionBar(Activity activity, Boolean isChild) {
        View viewActionBar = activity.getLayoutInflater().inflate(R.layout.actionbar_layout, null);
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(//Center the textview in the ActionBar !
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.MATCH_PARENT,
                Gravity.CENTER);
        activity.getActionBar().setCustomView(viewActionBar, params);
        activity.getActionBar().setDisplayShowCustomEnabled(true);
        activity.getActionBar().setDisplayShowTitleEnabled(false);
        activity.getActionBar().setDisplayHomeAsUpEnabled(isChild);
        //getActionBar().setIcon(android.R.color.transparent);

        return (TextView) viewActionBar.findViewById(R.id.actionbar_title);
	}
}
