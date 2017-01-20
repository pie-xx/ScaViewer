package com.m_obj.pericaroid;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AnnotationMenu {
	LinearLayout	mBody;

	LinearLayout 	mTitleLayout;
	Button			mButton;

	LinearLayout 	mPanellLayout;

	Button 			mResetButton;

	EditText		mEditText;
	WebView			mWeb;
	Context			mContext;
	TextView		mFil;

	String	mDefText;

	public AnnotationMenu(Context context){
		mDefText = "";
		mContext = context;
		mBody = new LinearLayout(context);
		mBody.setOrientation(LinearLayout.VERTICAL);
		mBody.setBackgroundColor(Color.argb(128, 0, 0, 0));
		mBody.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));

		mTitleLayout = new LinearLayout(context);
		mTitleLayout.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
		mTitleLayout.setOrientation(LinearLayout.HORIZONTAL);
		mTitleLayout.setGravity(Gravity.RIGHT);

		mButton = new Button(context);
		mButton.setText("✓");
		mButton.setTextColor(Color.argb(255, 255, 255, 255));
//		mButton.setBackgroundDrawable(context.getResources().getDrawable(android.R.drawable.ic_menu_close_clear_cancel));
		mButton.setBackgroundColor(Color.argb(0, 0, 0, 0));
		mTitleLayout.addView(mButton);

		mPanellLayout = new LinearLayout(context);
		mPanellLayout.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));

		mResetButton = new Button(context);
		mResetButton.setBackgroundDrawable(context.getResources().getDrawable(android.R.drawable.ic_menu_revert));
		mResetButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mEditText.getText().toString().equals(mDefText)){
					mEditText.setText("");
				}else {
					mEditText.setText(mDefText);
				}
			}
		});
		mPanellLayout.addView(mResetButton);

		mEditText = new EditText(context);
		mEditText.setWidth(2000);
		mEditText.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));

		mPanellLayout.addView(mEditText);

		mBody.addView(mTitleLayout);
		mBody.addView(mPanellLayout);

		mBody.setVisibility(View.INVISIBLE);
	}

	public String getText(){
		return mEditText.getText().toString();
	}
	public void setText(String str){
		mDefText = str;
		mEditText.setText(str);
	}

	public void setOnClickListener(OnClickListener onclicked){
		mButton.setOnClickListener(onclicked);
	}

	public LinearLayout getLayout(){
		return mBody;
	}

	public void move(int leftPos, int topPos){
		mBody.setVisibility(View.VISIBLE);
		FrameLayout.LayoutParams	lp =
				new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT,
						Gravity.TOP | Gravity.LEFT);
		lp.setMargins(leftPos, topPos, 0, 0);
		mBody.setLayoutParams(lp);
		mEditText.setFocusable(true);
		mEditText.setFocusableInTouchMode(true);
		mEditText.requestFocus();

		InputMethodManager inputMethodManager = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
		inputMethodManager.showSoftInput(mEditText, InputMethodManager.SHOW_FORCED);

	}

	public void erase(){
//		mButton.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
		mBody.setVisibility(View.INVISIBLE);
	}
	public boolean isAlive(){
		return mBody.isShown();
	}
}
