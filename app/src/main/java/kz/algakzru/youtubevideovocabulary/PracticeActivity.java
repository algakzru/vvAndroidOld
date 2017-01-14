package kz.algakzru.youtubevideovocabulary;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.ViewFlipper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import kz.algakzru.youtubevideovocabulary.mnemogogo.Card;
import kz.algakzru.youtubevideovocabulary.mnemogogo.CardDatabase;
import kz.algakzru.youtubevideovocabulary.video.FFMpeg;


public class PracticeActivity extends Activity {

	private static final int RESULT_VIDEO_CAP = 5;
	
	VideoView mVideoView;
    MediaController mc;
	private Uri mFileURI = null;
	private List<String> questions;
	private String followingWord;
	private Card currentCard;
	private TextView actionBarTitle;

	ListView lvWords;

	private static final class VideoEntry {
		private String word, questionType;
		private Long id;

		public VideoEntry(Long id, String word, String questionType) {
			this.id = id;
			this.word = word;
			this.questionType = questionType;
		}
	}

	private final List<VideoEntry> VIDEO_LIST = new ArrayList<VideoEntry>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_practice);
		actionBarTitle = MainActivity.setActionBar(this, true);
		actionBarTitle.setText("Select a word from the list");

		lvWords = (ListView) findViewById(R.id.lvWords);
		lvWords.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

		VIDEO_LIST.clear();
		// ArrayList<String> words = new ArrayList<String>();
		CardDatabase cardDatabase = new CardDatabase(this);
		SQLiteDatabase db = cardDatabase.getReadableDatabase();
		String selection = cardDatabase.getSelected_categories();
		Cursor cur = db.query("word", null, selection, null, null, null, "grade DESC");
		if (cur.getCount() > 0) {
			while (cur.moveToNext()) {
				VIDEO_LIST.add(new VideoEntry(cur.getLong(cur
						.getColumnIndex("id")), cur.getString(cur
						.getColumnIndex("word")), cur.getString(cur
						.getColumnIndex("question_type"))));
			}
		}
		cardDatabase.close();
		PageAdapter adapter = new PageAdapter(this, VIDEO_LIST);
		lvWords.setAdapter(adapter);

		lvWords.setOnItemClickListener(new OnItemClickListener() {
			@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				CardDatabase cardDatabase = new CardDatabase(
						getApplicationContext());
				currentCard = cardDatabase.getCard(VIDEO_LIST.get(position).id);
				questions = cardDatabase.getHSKKQuestions(VIDEO_LIST
						.get(position).questionType);
				followingWord = (VIDEO_LIST.get(position).questionType
						.equals("IELTS") ? "(use the following word)"
						: "（使用以下词语）");
				Long seconds = cardDatabase.getTimelimit(VIDEO_LIST
						.get(position).questionType);
				String time = new DecimalFormat("0.#")
						.format((double) seconds / 60);
				cardDatabase.close();

                ((TextView) findViewById(R.id.blueText)).setVisibility(View.INVISIBLE);
				final ViewPager viewPager = (ViewPager) findViewById(R.id.myviewpager);
                viewPager.setAdapter(new MyPagerAdapter());

				//getActionBar().setTitle("Select a question and answer for " + time + " minutes");
                getActionBar().setTitle("Answer a question with a word from the list");

			}
		});

	}

	private class MyPagerAdapter extends PagerAdapter {

		int NumberOfPages = questions.size();

		@Override
		public int getCount() {
			return NumberOfPages;
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}

		@Override
		public Object instantiateItem(ViewGroup container, final int position) {

			TextView tvQuestion = new TextView(getApplicationContext());
			// tvQuestion.setText(questions.get(position) + "\n" +
			// followingWord);
			tvQuestion.setText(questions.get(position));

			tvQuestion.setGravity(Gravity.CENTER);
			tvQuestion.setTextAppearance(getApplicationContext(),
					android.R.style.TextAppearance_Large);
			tvQuestion.setTypeface(null, Typeface.BOLD);
			tvQuestion.setTextColor(getResources().getColor(
					R.color.holo_red_dark));
			LinearLayout.LayoutParams textViewParams = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT, 1);
			int dpValue = 10;
			// margin in dips
			float d = getApplicationContext().getResources()
					.getDisplayMetrics().density;
			int margin = (int) (dpValue * d);
			// margin in pixels
			textViewParams.setMargins(margin, 0, margin, 0);
			tvQuestion.setLayoutParams(textViewParams);

			Button btnRecord = new Button(getApplicationContext());
			btnRecord.setText("Record Answer");
			btnRecord.setBackgroundResource(android.R.drawable.btn_default);
			btnRecord.setTextColor(getResources().getColor(
                    R.color.holo_red_dark));
			LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			btnRecord.setLayoutParams(buttonParams);
            btnRecord.setCompoundDrawablesWithIntrinsicBounds( R.drawable.ic_device_access_video, 0, 0, 0);
			btnRecord.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View arg0) {
					recordVideo(questions.get(position));
				}
			});

			LinearLayout layout = new LinearLayout(getApplicationContext());
			layout.setOrientation(LinearLayout.VERTICAL);
			ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			layout.setLayoutParams(layoutParams);
			layout.addView(tvQuestion);
			layout.addView(btnRecord);
			layout.setGravity(Gravity.CENTER_VERTICAL
					| Gravity.CENTER_HORIZONTAL);
			layout.setClickable(true);

			container.addView(layout);
			return layout;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((LinearLayout) object);
		}

	}

	public class PageAdapter extends BaseAdapter {

		List<VideoEntry> entries;
		LayoutInflater inflater;

		public PageAdapter(Context context, List<VideoEntry> entries) {
			this.entries = entries;
			this.inflater = LayoutInflater.from(context);
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
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				view = inflater
						.inflate(R.layout.video_list_item, parent, false);
			}
			VideoEntry entry = entries.get(position);

			TextView label = ((TextView) view.findViewById(R.id.text));
			label.setText(entry.word);
			return view;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case RESULT_VIDEO_CAP:
			if (resultCode == RESULT_OK) {
				Log.d("myLogs", "RESULT_VIDEO_CAP");
				if (mFileURI != null) {
					ViewFlipper vfHorizontal = (ViewFlipper) findViewById(R.id.vfHorizontal);
					vfHorizontal.setInAnimation(AnimationUtils.loadAnimation(
							getApplicationContext(), android.R.anim.slide_in_left));
					vfHorizontal.setOutAnimation(AnimationUtils.loadAnimation(
							getApplicationContext(), android.R.anim.slide_out_right));
					vfHorizontal.showNext();
					reviewVideo(mFileURI);
				}
			}
			break;
		}
	}

	public void recordVideo(String question) {
		String subtitlePath = getExternalFilesDir(null).toString()
				+ File.separator + FFMpeg.recordFileName + ".ass";
		File sdFile = new File(subtitlePath);
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(sdFile));
			long totalSecs = 20;
			CardDatabase cardDatabase = new CardDatabase(this);
			long timeLimit = cardDatabase.getTimelimit(currentCard.getQuestionType());
			String time = new DecimalFormat("0.#")
					.format((double) timeLimit / 60);
			cardDatabase.close();
			long mins = ((timeLimit + totalSecs) / 60) % 60;
			long secs = (timeLimit + totalSecs) % 60;

			bw.write("[Script Info]\n");
			bw.write("; Script generated by Aegisub 3.2.2\n");
			bw.write("; http://www.aegisub.org/\n");
			bw.write("Title: Default Aegisub file\n");
			bw.write("ScriptType: v4.00+\n");
			bw.write("WrapStyle: 0\n");
			bw.write("ScaledBorderAndShadow: yes\n");
			bw.write("YCbCr Matrix: None\n\n");

			bw.write("[Aegisub Project Garbage]\n");
			bw.write("Last Style Storage: Default\n");
			bw.write("Active Line: 1\n\n");

			bw.write("[V4+ Styles]\n");
			bw.write("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\n");
			bw.write("Style: BlackOnWhite,Droid Sans Fallback,20,&H00000000,&H000000FF,&H00FFFFFF,&HFFFFFFFF,0,0,0,0,100,100,0,0,1,1,1,2,10,10,10,1\n");
			bw.write("Style: WhiteOnBlack,Droid Sans Fallback,20,&H00FFFFFF,&H000000FF,&H00000000,&HFFFFFFFF,0,0,0,0,100,100,0,0,1,1,1,2,10,10,10,1\n\n");
			
			bw.write("[Events]\n");
			bw.write("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n");
			
			if (currentCard.getQuestionType().equals("IELTS")) {
				bw.write("Dialogue: 0,0:00:00.00,0:00:10.00,BlackOnWhite,,0,0,0,,{\\c&H0000CC&}PLEASE GRADE MY {\b1}ANSWER{\b0} BY FOLLOWING\\NIELTS SPEAKING GRADING CRITERIAS :\\N{\\c&HFFFFFF&}*\\N{\\c&H0000CC&}COHERENCE :{\\c} How fluently I speak and how well I link my ideas\\N{\\c&HFFFFFF&}*{\\c}\\N{\\c&H0000CC&}PRONUNCIATION :{\\c} How accurate my pronunciation is\\N{\\c&HFFFFFF&}*{\\c}\\N{\\c&H0000CC&}VOCABULARY :{\\c} How accurate and varied my vocabulary is\\N{\\c&HFFFFFF&}*{\\c}\\N{\\c&H0000CC&}GRAMMAR :{\\c} How accurate and varied my grammar is\n");
				bw.write("Dialogue: 0,0:00:10.00,0:00:"	+ String.format("%02d", totalSecs) + ".00,BlackOnWhite,,0,0,0,,{\\c&H0000CC&}I must talk about the following topic for 1 to 2 minutes{\\c}\\N{\\c&HFFFFFF&}*{\\c}\\N" + question + "\\N{\\c&HFFFFFF&}*{\\c}\\N{\\c&H0000CC&}" + followingWord	+ "{\\c}\\N{\\c&HFFFFFF&}*{\\c}\\N" + currentCard.getWord() + "\n");
			} else {
				String level = currentCard.getQuestionType();
				bw.write("Dialogue: 0,0:00:00.00,0:00:10.00,BlackOnWhite,,0,0,0,,{\\c&H0000CC&}请评分我的口语回答，\\NHSKK（" + level + "）评分档次如下：{\\c}\\N{\\c&HFFFFFF&}*\\N{\\c&H0000CC&}高：{\\c} 考生能就问题做出回答, 内容丰富, 表达流利, 有少量停顿、 重复、 语法错误。\\N{\\c&HFFFFFF&}*{\\c}\\N{\\c&H0000CC&}中：{\\c} 考生能就问题做出回答, 但信息量较少, 停顿、 重复、 语法错误较多。\\N{\\c&HFFFFFF&}*{\\c}\\N{\\c&H0000CC&}低：{\\c} 考生答非所问, 信息量少且不连贯。\n");
				bw.write("Dialogue: 0,0:00:10.00,0:00:"	+ String.format("%02d", totalSecs) + ".00,BlackOnWhite,,0,0,0,,{\\c&H0000CC&}要求回答以下问题 （" + time + "分钟）{\\c}\\N{\\c&HFFFFFF&}*{\\c}\\N" + question + "\\N{\\c&HFFFFFF&}*{\\c}\\N{\\c&H0000CC&}" + followingWord	+ "{\\c}\\N{\\c&HFFFFFF&}*{\\c}\\N" + currentCard.getWord() + "\n");
			}

			bw.write("Dialogue: 0,0:00:" + String.format("%02d", totalSecs) + ".00,0:" + String.format("%02d", mins) + ":" + String.format("%02d", secs) + ".00,WhiteOnBlack,,0,0,0,,");
			bw.write(currentCard.getWord() + "\n");

			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

		// Workaround for Nexus 7 Android 4.3 Intent Returning Null problem
		// create a file to save the video in specific folder (this works for
		// video only)
		mFileURI = getOutputMediaFileUri();
		// Log.d("myLogs", "getOutputMediaFileUri() = " + mFileURI.toString());
		intent.putExtra(MediaStore.EXTRA_OUTPUT, mFileURI);

		// set the video image quality to high
		intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);

		// start the Video Capture Intent
		startActivityForResult(intent, RESULT_VIDEO_CAP);
	}

	/** Create a file Uri for saving an image or video */
	private Uri getOutputMediaFileUri() {
		return Uri.fromFile(getOutputMediaFile());
	}

	/** Create a File for saving an image or video */
	private File getOutputMediaFile() {
		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.

		File mediaFile = new File(getExternalFilesDir(null).toString()
				+ File.separator + FFMpeg.recordFileName);

		return mediaFile;
	}
	
	private void reviewVideo(Uri mFileUri) {
        try {
            mVideoView = (VideoView) findViewById(R.id.videoView);
            mc = new MediaController(this);
            mVideoView.setMediaController(mc);
            mVideoView.setVideoURI(mFileUri);
            mc.show();
            mVideoView.start();
        } catch (Exception e) {
            Log.e(this.getLocalClassName(), e.toString());
        }
    }

	public void recordAgain(View view) {
        getActionBar().setTitle("Select a word from the list");
        ViewFlipper vfHorizontal = (ViewFlipper) findViewById(R.id.vfHorizontal);
		vfHorizontal.setInAnimation(AnimationUtils.loadAnimation(
				getApplicationContext(), R.anim.slide_in_right));
		vfHorizontal.setOutAnimation(AnimationUtils.loadAnimation(
				getApplicationContext(), R.anim.slide_out_left));
		vfHorizontal.showNext();
    }

    public void uploadVideo(View view) {
        // if a video is picked or recorded.
        if (mFileURI != null) {

            MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
            metaRetriever.setDataSource(this, mFileURI);
            String height = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String width = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        	FFMpeg.convertAndShare(this, getExternalFilesDir(null).toString() + File.separator + FFMpeg.recordFileName, width, height);
        }
    }

    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Video.Media.DATA };
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndex(proj[0]);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    public String getResolutionFromURI(Uri contentUri) {
        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        metaRetriever.setDataSource(this, contentUri);
        String height = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        String width = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        return height+":"+width;
    }

}
