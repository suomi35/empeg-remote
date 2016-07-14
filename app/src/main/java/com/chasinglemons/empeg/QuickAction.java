package com.chasinglemons.empeg;

import android.content.Context;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.TextView;

public class QuickAction extends PopupWindows {

	private Animation mTrackAnim;
	private LayoutInflater inflater;
	private ViewGroup mTrack;
	private OnActionItemClickListener mListener;
	private int mChildPos;
	private boolean animateTrack;
	Context mContext;
	double tabletMinimum = 6;

	public QuickAction(Context context) {
		super(context);

		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		mTrackAnim = AnimationUtils.loadAnimation(context, R.anim.rail);

		mTrackAnim.setInterpolator(new Interpolator() {
			@Override
			public float getInterpolation(float t) {
				final float inner = (t * 1.55f) - 1.1f;

				return 1.2f - inner * inner;
			}
		});

		setRootViewId(R.layout.quickaction);

		animateTrack = true;
		mChildPos = 0;
		mContext = context;
	}

	public void setRootViewId(int id) {
		mRootView = inflater.inflate(id, null);
		mTrack = (ViewGroup) mRootView.findViewById(R.id.tracks);

		setContentView(mRootView);
	}

	public void animateTrack(boolean animateTrack) {
		this.animateTrack = animateTrack;
	}

	public void addActionItem(ActionItem action) {

		String title = action.getTitle();

		View container = inflater.inflate(R.layout.action_item, null);

		TextView text = (TextView) container.findViewById(R.id.tv_title);

		if (title != null)
			text.setText(title);
		else
			text.setVisibility(View.GONE);

		final int pos = mChildPos;

		container.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mListener != null)
					mListener.onItemClick(pos);

				dismiss();
			}
		});

		container.setFocusable(true);
		container.setClickable(true);

		mTrack.addView(container, mChildPos + 1);
		mChildPos++;
	}

	public void setOnActionItemClickListener(OnActionItemClickListener listener) {
		mListener = listener;
	}

	public void show(View anchor) {
		preShow();

		int[] location = new int[2];

		anchor.getLocationOnScreen(location);
		Log.i("QUICKACTION","location[0] = "+location[0]);
		Log.i("QUICKACTION","location[1] = "+location[1]);

		Rect anchorRect = new Rect(location[0], location[1], location[0]
				+ anchor.getWidth(), location[1] + anchor.getHeight());

		mRootView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT));
		mRootView.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		int rootWidth = mRootView.getMeasuredWidth();
		int rootHeight = mRootView.getMeasuredHeight();

		int screenWidth = mWindowManager.getDefaultDisplay().getWidth();
		//Log.i("QUICKACTION","screenWidth = "+screenWidth);

		int xPos;
		if (isTablet()) { // tablet
			xPos = (int) ((screenWidth * .75) - (rootWidth / 2));
		} else {
			xPos = (screenWidth - rootWidth) / 2;
		}
		int yPos = anchorRect.top - rootHeight;

//		Log.i("QUICKACTION","rootHeight = "+rootHeight+", anchor.getTop() = "+anchor.getTop());
		if (rootHeight > anchor.getTop()) {
//			Log.i("QUICKACTION","rootHeight > anchor.getTop()...");
			yPos = anchorRect.bottom;
		}

		mWindow.setAnimationStyle(R.style.Animations_PopDownMenu_Center);

		//Log.i("QA","anchorRect.right = "+anchorRect.right);
		//Log.i("QA","anchorRect.top = "+anchorRect.top);
		//Log.i("QA","xPos = "+xPos);
		//Log.i("QA","yPos = "+yPos);
		mWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, xPos, yPos);

		if (animateTrack)
			mTrack.startAnimation(mTrackAnim);
	}
	
	public boolean isTablet() { 
		try { 
			// Compute screen size 
			DisplayMetrics dm = mContext.getResources().getDisplayMetrics(); 
			float screenWidth  = dm.widthPixels / dm.xdpi; 
			float screenHeight = dm.heightPixels / dm.ydpi; 
			double size = Math.sqrt(Math.pow(screenWidth, 2) + 
					Math.pow(screenHeight, 2));

			// Tablet devices should have a screen size greater than 6 inches 
			return size >= tabletMinimum; 
		} catch(Throwable t) { 
//			Log.e("START", "Failed to compute screen size", t);
			return false; 
		}
	}

	public interface OnActionItemClickListener {
		public abstract void onItemClick(int pos);
	}
}