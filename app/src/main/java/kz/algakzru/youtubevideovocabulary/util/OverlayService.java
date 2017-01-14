package kz.algakzru.youtubevideovocabulary.util;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import kz.algakzru.youtubevideovocabulary.VideoListActivityYouku;

public class OverlayService extends Service {

	private WindowManager windowManager;
	private Button button;

    private int currentPosition = -1;
	private int currentVideoPosition = -1;

	@Override
	public IBinder onBind(Intent intent) {
		// Not used
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

		button = new Button(this);
		button.setTextSize(25);

		final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
						| WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
				PixelFormat.TRANSLUCENT);

		params.gravity = Gravity.TOP | Gravity.LEFT;
		params.x = 0;
		params.y = 100;

		button.setOnTouchListener(new View.OnTouchListener() {
			private int initialX;
			private int initialY;
			private float initialTouchX;
			private float initialTouchY;
			boolean onClick = true;

			@Override
			public boolean onTouch(View v, MotionEvent event) {

				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					onClick = true;
					initialX = params.x;
					initialY = params.y;
					initialTouchX = event.getRawX();
					initialTouchY = event.getRawY();
					return true;
				case MotionEvent.ACTION_UP:
					if (onClick) {
						Intent intent = new Intent(OverlayService.this,
								VideoListActivityYouku.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("position", currentPosition);
						intent.putExtra("currentVideoPosition", currentVideoPosition);
                        startActivity(intent);
                        System.out.println("Clicked----><<<<<<<");
                        stopSelf();
                    } else
						System.out.println("Touched =----- > ");
					return true;
				case MotionEvent.ACTION_MOVE:
					onClick = false;
					params.x = initialX
							+ (int) (event.getRawX() - initialTouchX);
					params.y = initialY
							+ (int) (event.getRawY() - initialTouchY);
					windowManager.updateViewLayout(button, params);
					return true;
				}
				return false;
			}
		});

		windowManager.addView(button, params);
	}

    @Override
    public int onStartCommand( Intent intent , int flags , int startId )
    {
        super.onStartCommand(intent, flags , startId);

        Bundle extras = intent.getExtras();

        //just checking
        if( extras != null ) {
            button.setText(extras.getString("word"));
            currentPosition = extras.getInt("position");
			currentVideoPosition = extras.getInt("currentVideoPosition");

		} else
            Toast.makeText(this, "extras == null", Toast.LENGTH_LONG).show();

        return START_REDELIVER_INTENT;

    }

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (button != null)
			windowManager.removeView(button);
	}
}
