package com.m_obj.pericaroid;

import android.R.drawable;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.Toast;

@SuppressLint("ShowToast")
public class SimpleImageViewerActivity extends Activity  implements OnLongClickListener {
	public static final String VIEWSRC = "com.m_obj.pericaroid.picname";
	public static final String FROMI = "com.m_obj.pericaroid.fromi";
	private MenuItem	mItemNext = null;
	private MenuItem    mItemBefore = null;

	private MenuItem    mItemMark = null;
	private MenuItem    mItemPaintmode = null;

	private MenuItem    mItemLand = null;
	private MenuItem    mItemPort = null;
	private MenuItem    mItemSens = null;

	private MenuItem    mItemSaveProp = null;
	private MenuItem    mItemAddTetra = null;
	private MenuItem    mItemRmTetra = null;
	private MenuItem    mItemSetDefPos = null;
	private MenuItem    mItemShowMode = null;
	private MenuItem    mItemHighCont = null;
	private MenuItem    mItemTransImage = null;

	private MenuItem    mItemSaveDefTemp = null;

	//    static DspMode	mDmode;
	static PieView	mPieView;
	static String	mSrcPath;
	//    static EditText mEditText;
	static AnnotationMenu	mAnnoMenu;
	static ContrastMenu	mContrastMenu;

	Bitmap mDstBmp;
	Bitmap mSrcBmp;

	private String mFromi;

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		WindowManager wm = (WindowManager)getSystemService(WINDOW_SERVICE);
		Display disp = wm.getDefaultDisplay();
		int dwidth = disp.getWidth();
		int dheight = disp.getHeight();

		try{
			mAnnoMenu = new AnnotationMenu(this);
			mAnnoMenu.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					ScaViewerBook.savePropertyStr(ScaViewerBook.getCurrentFilename(),
							SimpleXml.value2Tag("prop", SimpleXml.value2Tag("cap", SimpleXml.value2Tag("text", mAnnoMenu.getText()))));
					ScaViewerBook.saveCurProp();
					mAnnoMenu.erase();
					mPieView.setDispToView();
					mPieView.invalidate();

					InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
				}
			});

			mContrastMenu = new ContrastMenu(this, dwidth);
			mContrastMenu.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					ScaViewerBook.getMomo().setItem("Contrast", "1.0");
					ScaViewerBook.getMomo().setItem("Brightness", "0");
					mPieView.setContrast(1.0f);
					mPieView.setBlightness(0f);
					mPieView.invalidate();
					mContrastMenu.disp(0f, 1.0f);
				}
			});
			mContrastMenu.setOnBlightnessBarChangeListener(
					new SeekBar.OnSeekBarChangeListener() {

						@Override
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
							mPieView.setBlightness(progress2blightness(progress));
							mPieView.invalidate();
							return;
						}

						@Override
						public void onStartTrackingTouch(SeekBar seekBar) {

						}

						@Override
						public void onStopTrackingTouch(SeekBar seekBar) {
							ScaViewerBook.getMomo().setItem("Brightness", String.valueOf(mPieView.mBrightness));
						}
					}
			);
			mContrastMenu.setOnContrastBarChangeListener(
					new SeekBar.OnSeekBarChangeListener(){

						@Override
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
							mPieView.setContrast(progress2contrast(progress));
							mPieView.invalidate();
							return;
						}

						@Override
						public void onStartTrackingTouch(SeekBar seekBar) {

						}

						@Override
						public void onStopTrackingTouch(SeekBar seekBar) {
							ScaViewerBook.getMomo().setItem("Contrast", String.valueOf(mPieView.mContrast));
							return;
						}
					}
			);

			mPieView = new PieView(this, dwidth, dheight, mAnnoMenu);
			setContentView(mPieView);
			addContentView(mAnnoMenu.getLayout(), new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			addContentView(mContrastMenu.getLayout(), new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

			Intent intent = getIntent();
			String	picfile = intent.getStringExtra(VIEWSRC);
			if(picfile.indexOf("/")==-1){
				mSrcPath = ScaViewerBook.getCurrentFolder()+"/"+picfile;
			}else{
				mSrcPath = picfile;
			}
			ScaViewerBook.setContext(this);
			ScaViewerBook.setCurrentFileIndex(picfile);

			mFromi = intent.getStringExtra(FROMI);

			mPieView.load1stFile(mSrcPath);
			mPieView.setDispToTrns();
		}catch(java.lang.OutOfMemoryError e){
			Toast.makeText(this, "OutOfMemoryError", Toast.LENGTH_LONG).show();
		}
	}
	// @param brightness -255..255 0 is default
	float progress2blightness(int progress){
		return 255f*(progress - 50)/50f;
	}
	// @param contrast 0..10 1 is default
	float progress2contrast(int progress){
		return (float)Math.pow(10.0, (float)(progress/50.0f) - 1.0f);
		//	return (float)Math.log10((float)progress)/50 +1;
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		WindowManager wm = (WindowManager)getSystemService(WINDOW_SERVICE);
		Display disp = wm.getDefaultDisplay();
		int dwidth = disp.getWidth();
		int dheight = disp.getHeight();

		mPieView.setViewSize(dwidth, dheight);
		mPieView.invalidate();
	}

	public boolean  onKeyDown  (int keyCode, KeyEvent event){

		switch( keyCode ){
			case KeyEvent.KEYCODE_BACK:
				switch(mPieView.getDispMode()){
					case D_VIEW:
						if(mAnnoMenu.isAlive()){
							mAnnoMenu.erase();
							return true;
						}
						if(mContrastMenu.isAlive()){
							mContrastMenu.erase();
							return true;
						}
						mPieView.clearOrgBitmap();
						HtmlViewerActivity.mUug.setLastKeytime( System.currentTimeMillis() );
						finish();
						break;
					case D_AREA_EDIT:
						mPieView.loadImageFile(mPieView.mSrcFilename);
						mPieView.setDispToTrns();
						break;
					case D_COLORSET:
						mContrastMenu.erase();
						mPieView.setColorSetmode(false);
						mPieView.setTransFlg(false);
						return true;
					default:
						break;
				}

				return true;
			case KeyEvent.KEYCODE_VOLUME_UP:
				this.openOptionsMenu();
				//	mPieView.BeforeTetra();
				return true;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				mPieView.NextTetra();
				return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onLongClick(View v) {
		//	if( mPieView.getVisibility() != View.GONE)
		//		mPieView.setVisibility(View.GONE);
		//	else
		//		mPieView.setVisibility(View.VISIBLE);
		return false;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		mAnnoMenu.erase();
		mContrastMenu.erase();
		menu.clear();
		switch(mPieView.getDispMode()){
			case D_VIEW:
				setViewModeMenu(menu);
				break;
			case D_AREA_EDIT:
				setAreaModeMenu(menu);
				break;
			default:
				setViewModeMenu(menu);
				break;
		}
		return true;
	}

	void setViewModeMenu(Menu menu){
		mItemShowMode = menu.add("Area setting");	mItemShowMode.setIcon(drawable.ic_menu_crop);
		setOrientationMenu(menu);

		/*
		if( mPieView.getTransFlg() ){
			mItemTransImage = menu.add("◌view");
		}else{
			mItemTransImage = menu.add("☀setting");
		}
		*/
		mItemBefore  = menu.add("Before");	mItemBefore.setIcon(drawable.ic_media_previous);
		mItemTransImage = menu.add("Picture");
		mItemTransImage.setIcon(R.drawable.ic_white_balance_sunny_white_24dp);

		mItemNext  = menu.add("Caption");	mItemNext.setIcon(drawable.ic_menu_edit);
		mItemMark = menu.add("Bookmark"); mItemMark.setIcon(drawable.btn_star);

	}

	void setAreaModeMenu(Menu menu){
		mItemShowMode = menu.add("View mode");	mItemShowMode.setIcon(drawable.ic_menu_view);
		mItemAddTetra = menu.add("Add Tetra");	mItemAddTetra.setIcon(drawable.ic_menu_add);
		mItemRmTetra = menu.add("Remove Tetra");	mItemRmTetra.setIcon(drawable.ic_input_delete);
		setOrientationMenu(menu);
		mItemSaveDefTemp = menu.add("Save Default");	mItemSaveDefTemp.setIcon(drawable.ic_menu_set_as);

	}

	void setOrientationMenu(Menu menu){
		switch(this.getRequestedOrientation()){
			case ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR:
				mItemLand = menu.add("Landscape");	mItemLand.setIcon(drawable.ic_menu_always_landscape_portrait);
				break;
			case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
				mItemPort = menu.add("Portlate");	mItemPort.setIcon(drawable.ic_menu_always_landscape_portrait);
				break;
			case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
				mItemSens = menu.add("Sensor");		mItemSens.setIcon(drawable.ic_menu_always_landscape_portrait);
				break;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item==mItemNext){
			//	mPieView.NextTetra();
			String fname = ScaViewerBook.getCurrentFilename();
			String propstr = ScaViewerBook.getPropertyStr(fname);
			String rtn = SimpleXml.str2Value("cap", propstr );

			mAnnoMenu.setText(SimpleXml.str2Value("text",rtn));
			mAnnoMenu.move(0,0);
			return true;
		}else
		if(item==mItemBefore){
			mPieView.BeforeTetra();
			mAnnoMenu.erase();
			return true;
		}else
		if(item==mItemShowMode){
			switch(mPieView.getDispMode()){
				case D_AREA_EDIT:
					mPieView.loadImageFile(mPieView.mSrcFilename);
					mPieView.setDispToTrns();
					break;
				case D_VIEW:
					mPieView.setDispToSrc();
				case D_PAINT:
					break;
				default:
					break;
			}
			return true;
		}else
		if(item==mItemSetDefPos){
			mPieView.setDefPos();
			mPieView.invalidate();
			return true;
		}else
		if(item==mItemSaveProp){
			mPieView.savePropertyStr();
			return true;
		}else
		if(item==mItemAddTetra){
			mPieView.AddTetra();
		}else
		if(item==mItemRmTetra){
			mPieView.RmTetra();
		}else
		if(item==mItemSens){
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
			return true;
		}else
		if(item==mItemLand){
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			return true;
		}else
		if(item==mItemPort){
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			return true;
		}else
		if(item==mItemHighCont){
			mPieView.setHighContFlg(!mPieView.getHighContFlg());
			mPieView.loadFile(mSrcPath);
			mPieView.invalidate();
			return true;
		}else
		if(item==mItemSaveDefTemp){
			mPieView.savePropertyAsDefault();
			return true;
		}else
		if(item==mItemTransImage){
			/*
			if(mPieView.getTransFlg()){
				mContrastMenu.erase();
			//	mPieView.setColorSetmode(false);
			//	mPieView.setTransFlg(false);
			}else {
				mContrastMenu.disp(mPieView.getBlightness(), mPieView.getContrast());
			//	mPieView.setColorSetmode(true);
			//	mPieView.setTransFlg(true);
			}
			*/
			mContrastMenu.disp(mPieView.getBlightness(), mPieView.getContrast());
			mPieView.invalidate();
			return true;
		}else
		if(item==mItemMark){
			ScaViewerBook.setMarkPic(mPieView.getCurrentPic());
			Toast.makeText(this, "★ "+mPieView.getCurrentPic(), Toast.LENGTH_SHORT).show();
			mPieView.invalidate();
		}else
		if(item==mItemPaintmode){
			mPieView.startPaint();
		}
		return false;
	}

	@Override
	public void onPause(){
		super.onPause();
		ScaViewerBook.savePropertyStr(mPieView.mSrcFilename + "#pos",
				mPieView.getPosProperty());
		ScaViewerBook.saveCurProp();
	}
}
