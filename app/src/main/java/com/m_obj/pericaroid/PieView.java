package com.m_obj.pericaroid;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.m_obj.pericaroid.AnnotationTag.AnnoType;
import com.m_obj.pericaroid.TetraArea.FitTo;

import android.R.drawable;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.GestureDetector;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;

public class PieView extends ImageView implements GestureDetector.OnGestureListener{
	private final String TAG = "PieView";
	public enum DspMode {D_AREA_EDIT, D_VIEW, D_PAINT, D_COLORSET };
	static final int FINGERBIAS = 30;
	Bitmap	mCursorBmp;

	float mContrast=1.0f;	// 0..10 1is default.
	float mBrightness=0;	// -255..255 0 is default
	float mOContrast=1.0f;	// 0..10 1is default.
	float mOBrightness=0;	// -255..255 0 is default

	Context	mContext;
	private GestureDetector gestureDetector;
	AnnotationMenu	mAnnoMenu;
	AnnotationCtrl	mAnnoCtrl;

	static String	mCaption = "";
	final static int	TapMenuHeightRatio = 16;

	String	mSrcFilename;		// 表示中のファイル
	Bitmap	mOrgBmp = null;		// 元画像
	Bitmap	mDstBmp = null;		// 表示前に変換した画像
	Bitmap	mViewBmp = null;	// 画面用
	int	mLoadScale;				// メモリ不足時は縮小して読み込む、その倍率

	Point	mViewSize;	// 表示スクリーンの縦横サイズ
	float	mScale ;	// 表示倍率
	Point	mOrgPos;	// 表示窓の中心

	DspMode mDispMode = DspMode.D_VIEW;
	boolean mHighCont = false;
	boolean mTransImage = false;

	ViewMenu		mViewMenu;
	TetraEditMenu	mTetraEditMenu;

	ArrayList<TetraArea>	mTA;	// 部分表示領域
	int	mTetFocus = 0;
	TetraArea	mDefaultTetra;		//注目される領域がないときは全領域を返す
	Point	mPropBmpSize;

	Point	mTouchOrgsize = new Point();

	TetraArea.FitTo	mFitTo;

//	Point	mHomePos;	// ホームメニューで戻る中心
//	float	mHomeScale;	// ホームメニューで戻る倍率

	Rect	mOrgCopyArea;	// コピー元領域　元画像
	Rect	mViewCopyArea;	// コピー先領域　表示

	public PieView(Context context){
		super(context);
		this.gestureDetector = new GestureDetector(context, this);
	}

	public PieView(Context context, int width, int height, AnnotationMenu et) {
		super(context);
		mContext = context;
		mAnnoMenu = et;
		this.gestureDetector = new GestureDetector(context, this);
		init(width, height);
		mAnnoCtrl = new AnnotationCtrl(this);
	}
	void init(int width, int height){
		mTA = new ArrayList<TetraArea>(); // new TetraArea[MaxAreaSetting];
		mDefaultTetra = new TetraArea(width, height);

		mPropBmpSize = new Point(0,0);
		mOrgPos = new Point(0,0);

		mViewBmp = Bitmap.createBitmap( Math.max(width, height), Math.max(width, height), Bitmap.Config.ARGB_8888 );
		setImageBitmap(mViewBmp);

		mOrgCopyArea = new Rect();
		mViewCopyArea = new Rect();

		mDispMode = DspMode.D_VIEW;
		mFitTo = TetraArea.FitTo.Center;



		mViewMenu = new ViewMenu();
		mTetraEditMenu = new TetraEditMenu(width, height);
		mCursorBmp = BitmapFactory.decodeResource(getResources(), drawable.ic_menu_edit);

		setViewSize(width, height);
	}
	public void eraseAllAnnotations(){
		AlertDialog.Builder alertDialog=new AlertDialog.Builder(mContext);

		// ダイアログの設定
		alertDialog.setIcon(drawable.ic_delete);   //アイコン設定
		alertDialog.setTitle("削除");	//タイトル設定
		alertDialog.setMessage("注釈書き込みをすべて削除しますか？");  //内容(メッセージ)設定

		// OK(肯定的な)ボタンの設定
		alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// OKボタン押下時の処理
				Log.d("AlertDialog", "Positive which :" + which);
				mAnnoCtrl.clearTag();
				savePropertyStr();
				invalidate();
			}
		});

		// NG(否定的な)ボタンの設定
		alertDialog.setNegativeButton("NG", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// NGボタン押下時の処理
				Log.d("AlertDialog", "Negative which :" + which);
			}
		});

		// ダイアログの作成と描画
		alertDialog.show();
	}
	public String getCurrentPic(){
		File f = new File(mSrcFilename);
		return f.getName();
	}
	public String getCurrentBook(){
		return getPathDir(mSrcFilename);
	}

	public void setViewSize(int width, int height){
		mViewSize = new Point(width, height);
		mViewMenu.reSize(width, height);
		mTetraEditMenu.setSize(width, height);
	}

	public void loadFile(String bmpfile){
		mSrcFilename = bmpfile;

		loadImageFile(bmpfile);
		loadImageProperty(mSrcFilename);

		if( mOrgBmp != null ){
			//		adjustProperty();
			if(ScaViewerBook.getPropertyStr(mSrcFilename).isEmpty()){
				setDefPos();
			}
		}
	}
	public void load1stFile(String bmpfile){
		mSrcFilename = bmpfile;

		loadImageFile(bmpfile);
		loadImageProperty(mSrcFilename);

		String poskey = ScaViewerBook.getPropertyStr(mSrcFilename+"#pos");
		if( !poskey.isEmpty() ){
			setPosProperty(poskey);
		}

		if( mOrgBmp != null ){
			//		adjustProperty();
			if(ScaViewerBook.getPropertyStr(mSrcFilename).isEmpty()){
				setDefPos();
			}
		}
	}
	/**
	 void adjustProperty(){
	 if(mPropBmpSize.x!=0.0){
	 double ratio = mOrgBmp.getWidth() / mPropBmpSize.x;
	 for(int n=0; n < mTA.size(); ++n){
	 mTA.get(n).adjustProperty(ratio);
	 }
	 mAnnoCtrl.adjustProperty(ratio);
	 }
	 }
	 **/
	String getPropertyStr(String pfile){
		// 個別ページファイルの設定取得
		String pstr = ScaViewerBook.getPropertyStr(pfile);
		String[] props = pstr.split("<cap>");
		mCaption = "";
		if( props.length > 1 ){
			mCaption = props[1].split("</cap>")[0];
			pstr = props[0] + props[1].split("</cap>")[1];
			if( pstr.isEmpty() || pstr.equals("<prop></prop>")){
				// 個別設定がなければBook全体定義を取得
				pstr = ScaViewerBook.getPropertyStr("#Default" /*ScaViewerBook.getCurrentFolder()*/) + "<cap>"+mCaption+"</cap>";
			}
		}else{
			if( pstr.isEmpty() || pstr.equals("<prop></prop>")){
				// 個別設定がなければBook全体定義を取得
				pstr = ScaViewerBook.getPropertyStr("#Default" /*ScaViewerBook.getCurrentFolder()*/);
			}
		}
		return pstr;
	}

	public void loadImageProperty(String pfile){
		// 個別ページファイルの設定取得
		String pstr = getPropertyStr(pfile);
		if(!pstr.isEmpty()){
			setProperty(pstr);
		}
	}
	String getPathDir( String fullpath ){
		int p = fullpath.lastIndexOf('/');
		if( p!= -1 ){
			return fullpath.substring(0, p+1);
		}
		return fullpath;
	}
	String getPathFilename(String fullpath){
		int p = fullpath.lastIndexOf('/');
		if( p!= -1 ){
			return fullpath.substring(p+1);
		}
		return fullpath;
	}
	public void loadImageFile(String bmpfile){
		for( mLoadScale=1; mLoadScale < 8; mLoadScale=mLoadScale+1 ){
			try{
				BitmapFactory.Options opt = new BitmapFactory.Options();
				opt.inSampleSize = mLoadScale;
				if(mOrgBmp!=null){
					mOrgBmp.recycle();
				}
				mOrgBmp = BitmapFactory.decodeFile(bmpfile, opt);

				if( mOrgBmp != null ){
					if(mDispMode == DspMode.D_VIEW){
						mDefaultTetra = new TetraArea(mOrgBmp.getWidth(),mOrgBmp.getHeight(),mOrgBmp.getWidth(),mOrgBmp.getHeight());
						SetTransView();
					}
				}
				return;
			}catch(java.lang.OutOfMemoryError e){
				if(mDstBmp!=null)
					mDstBmp.recycle();
				if(mOrgBmp!=null)
					mOrgBmp.recycle();
			}
		}
	}

	public boolean NextTetra(){
		if( mTetFocus < mTA.size()-1 ){
			++mTetFocus;
			setDispToTrns();
			//	if( mTransImage ) {
			//		loadImageFile(mSrcFilename);
			//	}
			return true;
		}

		String nextFile = ScaViewerBook.getNextFile();
		if( nextFile != null ){
			loadFile( nextFile );
			mTetFocus = 0;
			setDispToTrns();
			return true;
		}
		return false;
	}

	public boolean BeforeTetra(){
		if( mTetFocus > 0 ){
			--mTetFocus;
			setDispToTrns();
			//	if( mTransImage ) {
			//		loadImageFile(mSrcFilename);
			//	}
			return true;
		}

		String beforeFile = ScaViewerBook.getBeforFile();
		if( beforeFile != null ){
			loadImageProperty(beforeFile);
			loadFile( beforeFile );
			mTetFocus = mTA.size() - 1;
			setDispToTrns();
			return true;
		}
		return false;
	}

	float[] mSmat;
	float[] mDmat;
	void SetTransView(){
/***
 if( !mTransImage )
 return;

 Mat mat = new Mat();
 Utils.bitmapToMat(mOrgBmp, mat);
 mOrgBmp.recycle();

 int width = mOrgBmp.getWidth();
 int height = mOrgBmp.getHeight();

 Mat dstMat = new Mat();
 mDstBmp = Bitmap.createBitmap( width, height, Bitmap.Config.ARGB_8888 );
 Utils.bitmapToMat(mDstBmp, dstMat);

 if( mHighCont ){
 Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY);
 Imgproc.equalizeHist(mat, mat);
 Imgproc.cvtColor(mat, mat, Imgproc.COLOR_GRAY2RGBA, 4);
 }

 Mat srcPointMat = new Mat(4,2,CvType.CV_32F);
 mSmat = getCurrentTetra().getSrcPoints();
 srcPointMat.put( 0, 0, mSmat );
 Mat dstPointMat = new Mat(4,2,CvType.CV_32F);
 getCurrentTetra().setCircumscribed();
 mDmat = getCurrentTetra().getDstPoints();
 dstPointMat.put(0, 0, mDmat );

 Mat r_mat = Imgproc.getPerspectiveTransform(srcPointMat, dstPointMat);

 Imgproc.warpPerspective(mat, dstMat, r_mat, dstMat.size(), Imgproc.INTER_LINEAR);

 Utils.matToBitmap(dstMat, mDstBmp);
 drawBitmap(mDstBmp);

 mDstBmp.recycle();
 ***/
	}

	public void AddTetra(){
		if( mTetFocus < mTA.size() && mTetFocus >= 0 ) {
			mTA.add(new TetraArea(mTA.get(mTetFocus)));
		}else{
			mTA.add(new TetraArea((int)mViewSize.x, (int)mViewSize.y,mOrgBmp.getWidth(),mOrgBmp.getHeight()));
		}
		mDispMode = DspMode.D_AREA_EDIT;
		mTetFocus = mTA.size() -1;
		mTA.get(mTetFocus).moveRectR(new Point(80,80));
		invalidate();

		savePropertyStr();
	}
	public void RmTetra() {
		if( mTA.size() > 0 ){
			try{
				mTA.remove(mTetFocus);
				mDispMode = DspMode.D_AREA_EDIT;
				if( mTetFocus > mTA.size() - 1 ){
					mTetFocus = mTA.size() - 1;
				}
				savePropertyStr();
			}catch(IndexOutOfBoundsException e){}
		}else{
			ScaViewerBook.rmPropertyStr(mSrcFilename);
			loadImageProperty(mSrcFilename);
		}
		setDefPos();
		invalidate();

	}

	public DspMode getDispMode() {
		return mDispMode;
	}

	public void setDispToTrns(){
		mDispMode = DspMode.D_VIEW;

		TetraArea sta = getCurrentTetra();

		sta.setCircumscribed();

		float[] points = sta.getDstPoints();

		float	top = points[1];
		float	left = points[0];
		float	bottom = points[3];
		float	right = points[4];

		if(sta.getFitTo()!=FitTo.Undef){
			mFitTo = sta.getFitTo();
		}
		fitArea(top, left, right, bottom, mFitTo);

		invalidate();
	}
	public void setDispToSrc(){
		mDispMode = DspMode.D_AREA_EDIT;
		loadFile(mSrcFilename);
		invalidate();
	}

	public void setDispToView(){
		mDispMode = DspMode.D_VIEW;
		mTmode = TMODE.T_None;
	}

	/**
	 *
	 <prop>
	 <name>xxxx.jpg</name>
	 <scale>0.0</scale>
	 <orgpos>
	 <x>897.6</x><y>122.4</y>
	 </orgpos>
	 <tetra>
	 <src>
	 <p0><x>897.6</x><y>122.4</y></p0>
	 <p1><x>897.6</x><y>1101.6</y></p1>
	 <p2><x>1468.7999</x><y>1101.6</y></p2>
	 <p3><x>1468.7999</x><y>122.4</y></p3>
	 </src>
	 <dst>
	 <p0><x>897.6</x><y>122.4</y></p0>
	 <p1><x>897.6</x><y>1101.6</y></p1>
	 <p2><x>1468.7999</x><y>1101.6</y></p2>
	 <p3><x>1468.7999</x><y>122.4</y></p3>
	 </dst>
	 </tetra>
	 <anno>
	 <type>FREEHAND, LINE, CIRCLE, BOX, TEXT, GROUP</type>
	 <font-size>
	 <color>
	 <text>
	 <p0><x>897.6</x><y>122.4</y></p0>
	 <p1><x>897.6</x><y>1101.6</y></p1>
	 ....
	 </anno>
	 </prop>
	 */

	public String getProperty() {
		StringBuffer pfstr = new StringBuffer();

		pfstr.append("<prop>");
		pfstr.append(SimpleXml.value2Tag("name", getPathFilename(mSrcFilename)));
		pfstr.append(SimpleXml.value2Tag("scale", Float.toString(mScale)));
		pfstr.append(SimpleXml.value2Tag("orgpos",
				SimpleXml.value2Tag("x", Double.toString(mOrgPos.x))+
						SimpleXml.value2Tag("y", Double.toString(mOrgPos.y))
		));
		pfstr.append(SimpleXml.value2Tag("bmpsize",
				SimpleXml.value2Tag("width", Integer.toString(mOrgBmp.getWidth()))+
						SimpleXml.value2Tag("height", Integer.toString(mOrgBmp.getHeight()))
		));
		////TetraArea//////////////////////////////////
		for( int n=0; n < mTA.size(); ++n ){
			pfstr.append("<tetra>");
			pfstr.append( mTA.get(n).toString() );
			pfstr.append("</tetra>");
		}
		////Annotations////////////////////////////////
		for( int n=0; n < mAnnoCtrl.getSize(); ++n){
			AnnotationTag at = mAnnoCtrl.get(n);
			switch( at.getType() ){
				case TEXT:
					if( !at.getText().equals("")){
						pfstr.append( at.toString() );
					}
					break;
				default:
					if( at.getSize() > 0){
						pfstr.append( at.toString() );
					}
			}
		}
		pfstr.append(SimpleXml.value2Tag("tetfocus",Integer.toString(mTetFocus)));

		if( !mCaption.isEmpty() ){
			pfstr.append(SimpleXml.value2Tag("cap",SimpleXml.value2Tag("text", mCaption)));
		}

		pfstr.append("</prop>");
		return pfstr.toString();
	}

	public String getPosProperty() {
		StringBuffer pfstr = new StringBuffer();
		pfstr.append(SimpleXml.value2Tag("scale", Float.toString(mScale)));
		pfstr.append(SimpleXml.value2Tag("orgpos",
				SimpleXml.value2Tag("x", Double.toString(mOrgPos.x))+
						SimpleXml.value2Tag("y", Double.toString(mOrgPos.y))
		));
		pfstr.append(SimpleXml.value2Tag("bmpsize",
				SimpleXml.value2Tag("width", Integer.toString(mOrgBmp.getWidth()))+
						SimpleXml.value2Tag("height", Integer.toString(mOrgBmp.getHeight()))
		));
		pfstr.append(SimpleXml.value2Tag("tetfocus",Integer.toString(mTetFocus)));
		return pfstr.toString();
	}

	void setPosProperty(String prstr){
		try{
			mScale = Float.parseFloat(SimpleXml.str2Value("scale",prstr));
			String orgPosStr = SimpleXml.str2Value("orgpos",prstr);
			mOrgPos.x = Double.parseDouble(SimpleXml.str2Value("x",orgPosStr));
			mOrgPos.y = Double.parseDouble(SimpleXml.str2Value("y",orgPosStr));
			String bmpsizestr = SimpleXml.str2Value("bmpsize",prstr);
			if( !bmpsizestr.isEmpty() ){
				mPropBmpSize.x = Double.parseDouble(SimpleXml.str2Value("width", bmpsizestr));
				mPropBmpSize.y = Double.parseDouble(SimpleXml.str2Value("height", bmpsizestr));
			}
			mTetFocus = Integer.parseInt(SimpleXml.str2Value("tetfocus",prstr));
		}catch( NumberFormatException e ){

		}
	}

	void setProperty(String prstr){
		setPosProperty(prstr);

		String[] tetStrs = prstr.split("<tetra");
		mTA.clear();
		for(int n=1; n<tetStrs.length; ++n){
			String tetstr = tetStrs[n];
			if( !tetstr.equalsIgnoreCase("Infinity")){
				mTA.add(new TetraArea(tetstr,mOrgBmp.getWidth(),mOrgBmp.getHeight()));
				//	mTA.get(n-1).setProperty( tetstr );
			}
		}
		String[] annoStrs = prstr.split("<anno");
		mAnnoCtrl.clearTag();
		for(int n=1; n<annoStrs.length; ++n){
			String astr = annoStrs[n];
			mAnnoCtrl.addTag(astr);
		}

		String capStr = SimpleXml.str2Value("cap", prstr);
		mCaption = SimpleXml.str2Value("text", capStr);

		try {
			mContrast = Float.parseFloat(ScaViewerBook.getMomo().getItem("Contrast"));
		}catch(java.lang.NumberFormatException e){
			mContrast = 1.0f;
		}
		if( mContrast < 0 || mContrast > 10){
			mContrast = 1.0f;
		}
		try {
			mBrightness = Float.parseFloat(ScaViewerBook.getMomo().getItem("Brightness"));
		}catch(java.lang.NumberFormatException e){
			mBrightness = 0;
		}
		if( mBrightness < -255 || mBrightness > 255){
			mBrightness = 0;
		}
	}


	public void savePropertyStr(){
		ScaViewerBook.savePropertyStr(mSrcFilename, getProperty());
	}
	public void savePropertyAsDefault(){
		String[] props = getProperty().split("<anno>");
		ScaViewerBook.savePropertyStr("#Default"/*ScaViewerBook.getCurrentFolder()*/, props[0]);
	}
	public Bitmap getBitmap(){
		return mOrgBmp;
	}
	public int getOrgWidth(){
		if(mOrgBmp==null)
			return 0;
		return mOrgBmp.getWidth();
	}
	public int getOrgHeight(){
		if(mOrgBmp==null)
			return 0;
		return mOrgBmp.getHeight();
	}

	public TetraArea getCurrentTetra(){
		try {
			return mTA.get(mTetFocus);
		}catch(IndexOutOfBoundsException e){
			return mDefaultTetra;
		}
	}

	public void clearOrgBitmap(){
		if(mOrgBmp != null){
			mOrgBmp.recycle();
		}
		if(mViewBmp != null ){
			mViewBmp.recycle();
		}
	}
	public boolean isRecycled(){
		return mOrgBmp.isRecycled() || mOrgBmp==null;
	}
	public void drawBitmap( Bitmap bmp ) {
		mOrgBmp = bmp.copy(bmp.getConfig(), false);
	}

	public boolean getHighContFlg() {
		return mHighCont;
	}
	public void setHighContFlg(boolean flg) {
		mHighCont = flg;
	}
	public boolean getTransFlg() {
		return mTransImage;
	}
	public void setTransFlg(boolean flg) {
		mTransImage = flg;
	}
	////////////////////////////////////////////////////////////////////////////
	void fitArea(float top, float left, float right, float bottom, TetraArea.FitTo ft ){
		float width = right - left;
		float height = bottom - top;

		mOrgPos.x = (right + left) / 2;
		mOrgPos.y = (top + bottom) / 2;

		switch( ft ){
			case Up:
				mScale = (float) (mViewSize.x / width);
				mOrgPos.y = top + mViewSize.y /( mScale * 2);
				break;
			case Left:
				mScale = (float) (mViewSize.y / height);
				mOrgPos.x = left + mViewSize.x /( mScale * 2);
				break;
			case Right:
				mScale = (float) (mViewSize.y / height);
				mOrgPos.x = right - mViewSize.x /( mScale * 2);
				break;
			case Down:
				mScale = (float) (mViewSize.x / width);
				mOrgPos.y = bottom - mViewSize.y /( mScale * 2);
				break;
			case Width:
				mScale = (float) (mViewSize.x / width);
				break;
			case Height:
				mScale = (float) (mViewSize.y / height);
				break;
			case Center:
			default:
				mScale = (float) (mViewSize.y / height);
				float tmpWidth = mScale * width;
				if((int)tmpWidth > mViewSize.x ){
					mScale = (float) (mViewSize.x / width);
				}
		}
	}

	void setDefPos(){
		fitArea( 0, 0, mOrgBmp.getWidth(), mOrgBmp.getHeight(), mFitTo );
	}
//	void saveHomePos(){
//		mHomePos.x = mOrgPos.x;	// ホームメニューで戻る中心
//		mHomePos.y = mOrgPos.y;	// ホームメニューで戻る中心
//		mHomeScale = mScale;	// ホームメニューで戻る倍率
//	}
	/***
	 public void setHomePos(TetraArea.FitTo ft){
	 mFitTo = ft;
	 if( mFitTo==TetraArea.FitTo.Center) {
	 if( mOrgPos.equals(mHomePos) && mScale==mHomeScale ){
	 setDefPos();
	 }else{

	 mOrgPos.x = mHomePos.x ;	// ホームメニューで戻る中心
	 mOrgPos.y = mHomePos.y ;
	 mScale = mHomeScale;	// ホームメニューで戻る倍率
	 }
	 invalidate();
	 }else{
	 setDispToTrns();
	 }
	 }
	 ***/
	public void slideToEdge(TetraArea.FitTo ft){
		TetraArea sta = getCurrentTetra();
		sta.setCircumscribed();
		float[] points = sta.getDstPoints();

		float	top = points[1];
		float	left = points[0];
		float	bottom = points[3];
		float	right = points[4];

		switch(ft){
			case Up:
				mOrgPos.y = top + ( mViewSize.y / 2 )/mScale;
				break;
			case Left:
				mOrgPos.x = left + ( mViewSize.x / 2 )/mScale;
				break;
			case Right:
				mOrgPos.x = right - ( mViewSize.x / 2 )/mScale;
				break;
			case Down:
				mOrgPos.y = bottom - ( mViewSize.y / 2 )/mScale;
		}
		invalidate();
	}


	@SuppressLint("DrawAllocation")
	@Override
	public void onDraw(Canvas canvas){
		//	super.onDraw(canvas);
		if( mOrgBmp == null ){
			return;
		}

		drawBackScreen(canvas);
		drawCopyArea(canvas);

		Paint paint = new Paint();
		switch(mDispMode){
			case D_AREA_EDIT:
				showPoint(canvas);
				mTetraEditMenu.show( canvas);
				break;
			case D_VIEW:
				PopUpMenu.show( canvas);
				mViewMenu.show( mTouch, mCurTouch, canvas);
				break;
			case D_PAINT:
				canvas.drawBitmap(mCursorBmp, (float)mCurTouch.x, (float)mCurTouch.y-mCursorBmp.getHeight()-FINGERBIAS, paint);
				break;
			case D_COLORSET:
				drawTextStart();
				drawText("Contrast " + String.valueOf(mContrast), canvas);
				drawText("Brightness "+String.valueOf(mBrightness), canvas);
				break;
		}

		mAnnoCtrl.show(canvas);
	}
	public void setAnnoText(String str){
		//	mAnnoCtrl.getCurTag().setText(str);

	}
	public void setContrast(float contrast){
		mContrast=contrast;	// 0..10 1is default.
	}
	public float getContrast(){
		return mContrast;
	}
	public void setBlightness(float blightness){
		mBrightness=blightness;	// -255..255 0 is default
	}
	public float getBlightness(){
		return mBrightness;
	}

	void drawBackScreen(Canvas canvas){
		mViewCopyArea.top = 0;
		mViewCopyArea.left = 0;
		mViewCopyArea.right = (int) mViewSize.x;
		mViewCopyArea.bottom = (int) (mViewSize.y);

		Paint paint = new Paint();
		paint.setColor(Color.DKGRAY);

		canvas.drawRect(mViewCopyArea, paint);
	}

	void drawCopyArea(Canvas canvas){
		calcCopyArea();
		// @param contrast 0..10 1 is default
		// @param brightness -255..255 0 is default
		Paint paint = null;
		//	if( mTransImage ) {
		paint = new Paint();
		paint.setColorFilter(new ColorMatrixColorFilter(new float[]
						{
								mContrast, 0, 0, 0, mBrightness,
								0, mContrast, 0, 0, mBrightness,
								0, 0, mContrast, 0, mBrightness,
								0, 0, 0, 1, 0}
				)
		);
		//	}
		canvas.drawBitmap(mOrgBmp, mOrgCopyArea, mViewCopyArea, paint);
	}

	void calcCopyArea(){
		int tmpOrgWidth = (int) (mViewSize.x / mScale);
		int tmpOrgHeight = (int) (mViewSize.y / mScale);

		mOrgCopyArea.left = (int)mOrgPos.x - tmpOrgWidth / 2;
		mOrgCopyArea.top = (int)mOrgPos.y - tmpOrgHeight /2;
		mOrgCopyArea.right = mOrgCopyArea.left + tmpOrgWidth;
		mOrgCopyArea.bottom = mOrgCopyArea.top + tmpOrgHeight;

		if(mOrgCopyArea.left < 0){
			mViewCopyArea.left = (int) ((-mOrgCopyArea.left) * mScale);
			mOrgCopyArea.left = 0;
		}

		if( mOrgCopyArea.right > mOrgBmp.getWidth() ){
			mViewCopyArea.right = (int) (mViewSize.x - (mOrgCopyArea.right - mOrgBmp.getWidth())*mScale);
			mOrgCopyArea.right = mOrgBmp.getWidth();
		}

		if(mOrgCopyArea.top < 0){
			mViewCopyArea.top = (int) ((-mOrgCopyArea.top) * mScale);
			mOrgCopyArea.top = 0;
		}

		if( mOrgCopyArea.bottom > mOrgBmp.getHeight() ){
			mViewCopyArea.bottom = (int) (mViewSize.y - (mOrgCopyArea.bottom - mOrgBmp.getHeight())*mScale);
			mOrgCopyArea.bottom = mOrgBmp.getHeight();
		}

	}

	void showPoint(Canvas canvas){
		Paint spaint = new Paint();
		spaint.setColor(Color.argb(255, 180, 32, 32));
		Paint sFpaint = new Paint();
		sFpaint.setColor(Color.argb(255, 255, 0, 0));

		for( int i = 0; i < mTA.size(); ++i) {
			showTetra(mTA.get(i), canvas, spaint, Integer.toString(i));
		}

		try{
			showTetra(mTA.get(mTetFocus), canvas, sFpaint, Integer.toString(mTetFocus));
		}catch(IndexOutOfBoundsException e){}

	}

	static class PaintMenu {

	}
	static class ViewMenu {
		enum ComType { VC_NONE, VC_TOP, VC_LEFT, VC_RIGHT, VC_BOTTOM, VC_NEXT, VC_BEFORE }

		int TapMenuBottom;
		int TapMenuTop;
		int TapMenuRight;
		int TapMenuLeft;
		int TapJumpTH;

		float	mViewX, mViewY;

		ViewMenu(){
			mOldComType = ComType.VC_NONE;
		}
		ViewMenu(int width, int height){
			reSize( width, height);
		}
		void reSize(int width, int height){
			mViewX = width;
			mViewY = height;

			mOldComType = ComType.VC_NONE;

			int menuthin = (int) (Math.max(width, height) / TapMenuHeightRatio);
			TapMenuBottom = (int) (height - menuthin);
			TapMenuTop = menuthin;
			TapMenuRight = (int) (width - menuthin);
			TapMenuLeft = menuthin;
			TapJumpTH = (menuthin * 3 )/ 2;
		}
		// ビューモードでのタップ時の表示
		void show( Point fstTouch, Point curTouch, Canvas canvas){

			Paint paint = new Paint();
			paint.setColor(Color.argb(128, 80, 80, 80));
			Paint tpaint = new Paint();
			tpaint.setColor(Color.argb(128, 255, 80, 80));

			switch(getComType((float)curTouch.x, (float)curTouch.y )){
				case VC_NEXT:
					canvas.drawRect(0, (float)TapMenuBottom, mViewX, mViewY, tpaint);
					canvas.drawLine((float)curTouch.x, (float)curTouch.y, (float)fstTouch.x, (float)fstTouch.y, paint);
					canvas.drawCircle((float)fstTouch.x, (float)fstTouch.y, (float)20.0, paint);
					canvas.drawCircle((float)curTouch.x, (float)curTouch.y, (float)20.0, paint);
					break;
				case VC_BEFORE:
					canvas.drawRect(0, 0, mViewX, TapMenuTop, tpaint);
					canvas.drawLine((float)curTouch.x, (float)curTouch.y, (float)fstTouch.x, (float)fstTouch.y, paint);
					canvas.drawCircle((float)fstTouch.x, (float)fstTouch.y, (float)20.0, paint);
					canvas.drawCircle((float)curTouch.x, (float)curTouch.y, (float)20.0, paint);
					break;
				case VC_TOP:
					canvas.drawRect(0, 0, mViewX, TapMenuTop, paint);
					canvas.drawLine((float)curTouch.x, (float)curTouch.y, (float)fstTouch.x, (float)fstTouch.y, paint);
					canvas.drawCircle((float)fstTouch.x, (float)fstTouch.y, (float)20.0, paint);
					canvas.drawCircle((float)curTouch.x, (float)curTouch.y, (float)20.0, paint);
					break;
				case VC_BOTTOM:
					canvas.drawRect(0, TapMenuBottom, mViewX, mViewY, paint);
					canvas.drawLine((float)curTouch.x, (float)curTouch.y, (float)fstTouch.x, (float)fstTouch.y, paint);
					canvas.drawCircle((float)fstTouch.x, (float)fstTouch.y, (float)20.0, paint);
					canvas.drawCircle((float)curTouch.x, (float)curTouch.y, (float)20.0, paint);
					break;
				case VC_LEFT:
					canvas.drawRect(0, 0, TapMenuLeft, mViewY, paint);
					canvas.drawLine((float)curTouch.x, (float)curTouch.y, (float)fstTouch.x, (float)fstTouch.y, paint);
					canvas.drawCircle((float)fstTouch.x, (float)fstTouch.y, (float)20.0, paint);
					canvas.drawCircle((float)curTouch.x, (float)curTouch.y, (float)20.0, paint);
					break;
				case VC_RIGHT:
					canvas.drawRect(TapMenuRight, 0, mViewX, mViewY, paint);
					canvas.drawLine((float)curTouch.x, (float)curTouch.y, (float)fstTouch.x, (float)fstTouch.y, paint);
					canvas.drawCircle((float)fstTouch.x, (float)fstTouch.y, (float)20.0, paint);
					canvas.drawCircle((float)curTouch.x, (float)curTouch.y, (float)20.0, paint);
			}
		}

		ComType	mOldComType;

		ComType getTouchedComType(float cx, float cy ){
			mOldComType = ComType.VC_NONE;
			if( cy > TapMenuBottom ){
				mOldComType = ComType.VC_BOTTOM;
			}else
			if(cy < TapMenuTop){
				mOldComType = ComType.VC_TOP;
			}else
			if(cx > TapMenuRight){
				mOldComType = ComType.VC_RIGHT;
			}else
			if(cx < TapMenuLeft){
				mOldComType = ComType.VC_LEFT;
			}
			return mOldComType;
		}

		ComType getComType(float cx, float cy){
			switch(mOldComType){
				case VC_TOP:
					if( cy < TapMenuTop ){
						return ComType.VC_TOP;
					}else
					if( cy < TapMenuTop + TapJumpTH ){
						return ComType.VC_BEFORE;
					}
					break;
				case VC_BOTTOM:
					if( cy > TapMenuBottom ){
						return ComType.VC_BOTTOM;
					}else
					if( cy > TapMenuBottom - TapJumpTH ){
						return ComType.VC_NEXT;
					}
					break;
				case VC_LEFT:
					if( cx < TapMenuLeft ){
						return ComType.VC_LEFT;
					}
					break;
				case VC_RIGHT:
					if( cx > TapMenuRight ){
						return ComType.VC_RIGHT;
					}
				default:
					break;
			}
			return ComType.VC_NONE;
		}

		void resetOldType(){
			mOldComType = ComType.VC_NONE;
		}

	}

	static class PopUpMenu {
		static boolean mShowFlg = false;
		static float mX;
		static float mY;

		static void setShowFlg(boolean flg, float posx, float posy){
			mShowFlg = flg;
			mX = posx;
			mY = posy;
		}
		static void show(Canvas canvas){
			if(mShowFlg){
				Paint paint = new Paint();
				paint.setColor(Color.argb(128, 255, 255, 255));
				canvas.drawRect(mX, mY, mX+(float)200, mY+(float)300, paint);
			}
		}
	}

	static class TetraEditMenu {
		final static int textSize = 25;
		final static int	menuwidth = 100;
		static float menuLeftPos;
		enum MenuType {TE_None, TE_Add, TE_Delete};

		enum PointMoveType { PM_FREE, PM_RECT };
		PointMoveType	mPMT = PointMoveType.PM_RECT;
		public boolean isMoveFree(){
			return mPMT==PointMoveType.PM_FREE;
		}
		float	mViewX, mViewY;
		TetraArea.MOVEID	mMPId = TetraArea.MOVEID.MI_none;

		public void setMoveID(TetraArea.MOVEID id) {
			mMPId = id;
		}
		public TetraArea.MOVEID getMoveID(){
			return mMPId;
		}

		TetraEditMenu(int width, int height){
			setSize(width,height);
		}
		public void setSize(int width, int height){
			mViewX = width;
			mViewY = height;
		}

		// 領域編集モードでのタップ時の表示
		void show( Canvas canvas){
			drawBase(mViewX, mViewY, canvas);
			drawItem("delete", drawable.ic_input_delete, canvas);
			drawItem("add", drawable.ic_menu_add, canvas);
		}

		// 上部メニューのベース描画
		static void drawBase(float width, float height, Canvas canvas){
			menuLeftPos = width - 20;

			Paint paint = new Paint();
			paint.setColor(Color.argb(180, 0, 0, 0));
			canvas.drawRect(0, 0, width, textSize, paint);
			paint.setColor(Color.argb(130, 0, 0, 0));
			canvas.drawRect(0, textSize, width, textSize+4, paint);
			paint.setColor(Color.argb(80, 0, 0, 0));
			canvas.drawRect(0, textSize+4, width, textSize+8, paint);

			paint.setTextSize(textSize);
			paint.setColor(Color.argb(255, 255, 255, 255));
			canvas.drawText("Area Edit", 10, 20, paint);

		}

		static void drawItem(String str, int icon, Canvas canvas){
			menuLeftPos -= menuwidth;

			Paint paint = new Paint();
			paint.setTextSize(textSize);
			paint.setColor(Color.argb(255, 255, 255, 255));
			Bitmap ibm = BitmapFactory.decodeResource(ScaViewerBook.getContext().getResources(),icon);
			canvas.drawBitmap(ibm,
					(float) (menuLeftPos-ibm.getWidth()-4), 0, paint);
			canvas.drawText(str, menuLeftPos, 20, paint);

		}

		static MenuType getComType(float x, float y, float vwidth){
			if( y > textSize+8 )
				return MenuType.TE_None;
			if( x > vwidth - menuwidth ){
				return MenuType.TE_Delete;
			}
			if( x > vwidth - menuwidth * 2 ){
				return MenuType.TE_Add;
			}
			return MenuType.TE_None;
		}
	}


	final int	textSize = 25;
	int	lineHeight;
	void drawTextStart(){
		lineHeight = textSize;
	}
	void drawText(String str, Canvas canvas){
		Paint paint = new Paint();
		paint.setColor(Color.argb(180, 0, 0, 0));
		paint.setTextSize(textSize);
		canvas.drawRect(0, lineHeight-textSize, (float) mViewSize.x, lineHeight, paint);

		paint.setColor(Color.argb(130, 0, 0, 0));
		canvas.drawRect(0, lineHeight, (float) mViewSize.x, lineHeight+4, paint);
		paint.setColor(Color.argb(80, 0, 0, 0));
		canvas.drawRect(0, lineHeight+4, (float) mViewSize.x, lineHeight+8, paint);

		paint.setColor(Color.argb(255, 255, 255, 255));
		canvas.drawText(str, 10, lineHeight, paint);

		lineHeight += textSize;
	}
	String dstr( String label, double v ){
		return label + "=" + Float.toString((float) v) + " ";
	}
	String dstr( String label, int v ){
		return label + "=" + Integer.toString(v) + " ";
	}


	// 領域編集時の領域表示
	void showTetra(TetraArea tetra, Canvas canvas, Paint paint, String tetno){
		// 領域座標の取得
		float[] dpos = tetra.getSrcPoints();

		// 元座標からスクリーン座標に変換　および領域の中心を計算
		Point cp = new Point(0,0);
		for(int n=0; n<dpos.length; n=n+2 ){
			dpos[n] = org2viewX((float) dpos[n]);
			dpos[n+1] = org2viewY((float) dpos[n+1]);
			cp.x = cp.x + dpos[n];
			cp.y = cp.y + dpos[n+1];
		}
		Point[] points = new Point[5];
		for( int n=0; n < 4; ++n){
			points[n] = new Point(dpos[n*2], dpos[n*2+1]);
		}
		points[4] = new Point(dpos[0*2], dpos[0*2+1]);

		// テトラの各頂点の描画
		Paint tpaint = new Paint();
		tpaint.setColor(Color.WHITE);
		tpaint.setTextSize(18);
		for(int n=0; n<dpos.length; n=n+2 ){
			canvas.drawRect((float)(dpos[n]-16), dpos[n+1]-16, dpos[n]+16, dpos[n+1]+16, paint);
			canvas.drawText(tetno+"-"+Integer.toString(n/2), (float)(dpos[n]-14), dpos[n+1]+8, tpaint);
		}

		// フォーカス変更用の中央の大きな丸の描画
		canvas.drawCircle((float)cp.x/4, (float)cp.y/4, (float)30.0, paint);
		canvas.drawText(tetno, (float)(cp.x/4-6), (float)cp.y/4+8, tpaint);

		// 頂点をつなぐ辺の描画
		for(int n=0; n<dpos.length-2; n=n+2  ){
			canvas.drawLine(dpos[n], dpos[n+1], dpos[n+2], dpos[n+3], paint);
			canvas.drawRect((dpos[n]+dpos[n+2])/2-8, (dpos[n+1]+dpos[n+3])/2-8, (dpos[n]+dpos[n+2])/2+8, (dpos[n+1]+dpos[n+3])/2+8, paint);
		}
		canvas.drawLine(dpos[dpos.length-2], dpos[dpos.length-1], dpos[0], dpos[1], paint);
		canvas.drawRect((dpos[0]+dpos[dpos.length-2])/2-8, (dpos[0+1]+dpos[dpos.length-1])/2-8, (dpos[0]+dpos[dpos.length-2])/2+8, (dpos[0+1]+dpos[dpos.length-1])/2+8, paint);

		// 寄せる辺を強調
		paint.setStrokeWidth(3);
		switch( tetra.getFitTo() ){
			case Left:
				canvas.drawLine((float)points[0].x+4, (float)points[0].y+4, (float)points[1].x+4, (float)points[1].y+4, paint);
				canvas.drawLine((float)points[0].x  , (float)points[0].y  , (float)points[1].x  , (float)points[1].y  , paint);
				canvas.drawLine((float)points[0].x-4, (float)points[0].y-4, (float)points[1].x-4, (float)points[1].y-4, paint);
				break;
			case Down:
				canvas.drawLine((float)points[1].x+4, (float)points[1].y+4, (float)points[2].x+4, (float)points[2].y+4, paint);
				canvas.drawLine((float)points[1].x  , (float)points[1].y  , (float)points[2].x  , (float)points[2].y  , paint);
				canvas.drawLine((float)points[1].x-4, (float)points[1].y-4, (float)points[2].x-4, (float)points[2].y-4, paint);
				break;
			case Right:
				canvas.drawLine((float)points[2].x+4, (float)points[2].y+4, (float)points[3].x+4, (float)points[3].y+4, paint);
				canvas.drawLine((float)points[2].x  , (float)points[2].y  , (float)points[3].x  , (float)points[3].y  , paint);
				canvas.drawLine((float)points[2].x-4, (float)points[2].y-4, (float)points[3].x-4, (float)points[3].y-4, paint);
				break;
			case Up:
				canvas.drawLine((float)points[3].x+4, (float)points[3].y+4, (float)points[0].x+4, (float)points[0].y+4, paint);
				canvas.drawLine((float)points[3].x  , (float)points[3].y  , (float)points[0].x  , (float)points[0].y  , paint);
				canvas.drawLine((float)points[3].x-4, (float)points[3].y-4, (float)points[0].x-4, (float)points[0].y-4, paint);
				break;
			default:
				break;
		}
	}


	boolean	mInExt = false;
	Point	mTouch = new Point();
	Point	mCurTouch = new Point();
	float	mOldBias ;

	//	long	mEventTime = 0;
	enum TMODE {T_None, T_Ext, T_Scroll, T_MovePoint, T_MoveLine, T_MoveRect, T_ANNOEDIT }
	TMODE	mTmode = TMODE.T_None;

	Point	mExt1stCenter = new Point();
	double	mExt1stDist;
	double	mNewDist;
	float	mExt1stScale;
	Point	mExt1stPos = new Point();

	float		mEvPosX0, mEvPosY0, mEvPosX1, mEvPosY1;
	void	setEvPos(float x0, float y0, float x1, float y1){
		mEvPosX0 = x0;
		mEvPosY0 = y0;
		mEvPosX1 = x1;
		mEvPosY1 = y1;
	}

	float view2orgX(float x){
		return (float) ((x - mViewSize.x/2) / mScale + mOrgPos.x);
	}
	float view2orgY(float y){
		return (float) ((y - mViewSize.y/2) / mScale + mOrgPos.y);
	}


	float	org2viewX(float x){
		return (float) (( x - mOrgPos.x )* mScale + mViewSize.x / 2 );
	}
	float	org2viewY(float y){
		return (float) (( y - mOrgPos.y )* mScale + mViewSize.y / 2 );
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(this.gestureDetector.onTouchEvent(event)) return true;

		mCurTouch.x = event.getX();
		mCurTouch.y = event.getY();
		boolean	rtn = false;

		switch( mDispMode ){
			case D_VIEW:
				switch( mTmode ){
					case T_None:		rtn = switchNone(event);
						break;
					case T_Scroll:		rtn = switchScroll(event);
						break;
					case T_Ext:			rtn = switchExt(event);
						break;
					case T_ANNOEDIT:
						break;
				}
				break;
			case D_AREA_EDIT:
				switch( mTmode ){
					case T_None:		rtn = switchNone(event);
						break;
					case T_Scroll:		rtn = switchScroll(event);
						break;
					case T_Ext:			rtn = switchExt(event);
						break;

					case T_MovePoint:	rtn = switchMovePoint(event);
						break;
					case T_MoveLine:	rtn = switchMoveLine(event);
						break;
					case T_MoveRect:	rtn = switchMoveRect(event);
						break;
				}
				break;
			case D_PAINT:
				switchPaint(event);
				break;
			case D_COLORSET:
				rtn = switchNone(event);
		}

		invalidate();
		return rtn;
	}

	boolean switchPaint(MotionEvent event){
		switch( event.getAction() ){
			case MotionEvent.ACTION_DOWN:
				mAnnoCtrl.start(view2orgX(event.getX()), view2orgY(event.getY() - FINGERBIAS));
				break;
			case MotionEvent.ACTION_MOVE:
			case MotionEvent.ACTION_POINTER_DOWN:
				mAnnoCtrl.addStroke(view2orgX(event.getX()), view2orgY(event.getY() - FINGERBIAS));
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
				if( mAnnoCtrl.decision()==AnnotationTag.AnnoType.TEXT ) {
					mAnnoMenu.move((int)(event.getX()), (int)(event.getY()-FINGERBIAS));
					mDispMode = DspMode.D_PAINT;
				}else{
					mDispMode = DspMode.D_VIEW;
				}
				resetMovingPoint();
				savePropertyStr();
		}

		return false;
	}

	boolean switchExt(MotionEvent event){
		switch(event.getAction()){
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_POINTER_DOWN:
				return true;
			case MotionEvent.ACTION_MOVE:
				mNewDist = getPointDist(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
				if(mExt1stDist/mNewDist < 0.1 ){
					mNewDist = mExt1stDist * 10;
				}
				float sx = view2orgX((float)mExt1stCenter.x);
				float sy = view2orgY((float)mExt1stCenter.y);
				mScale = (float) (mExt1stScale * mNewDist / mExt1stDist);
				float ex = view2orgX((float)mExt1stCenter.x);
				float ey = view2orgY((float)mExt1stCenter.y);

				mOrgPos.x = mOrgPos.x + (sx - ex);
				mOrgPos.y = mOrgPos.y + (sy - ey);

				invalidate(); // �ʒm
				return false;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
			default:
				resetMovingPoint();
				return false;
		}
	}

	boolean switchScroll(MotionEvent event){
		switch(event.getAction()){
			case MotionEvent.ACTION_MOVE:
				if(event.getPointerCount() > 1){
					mExt1stDist = getPointDist(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
					setExt1st();

					mTmode = TMODE.T_Ext;
					return true;
				}
				mOrgPos.x = mOrgPos.x + (mTouch.x - event.getX())/mScale;
				mOrgPos.y = mOrgPos.y + (mTouch.y - event.getY())/mScale;

				mTouch.x = event.getX();
				mTouch.y = event.getY();

				invalidate();
				return false;
			case MotionEvent.ACTION_UP:
				switch( mDispMode ){
					case D_AREA_EDIT:
						switch(mTetraEditMenu.getComType(event.getX(), event.getY(), (float)mViewSize.x)){
							case TE_Delete:
								RmTetra();
								break;
							case TE_Add:
								AddTetra();
								break;
							default:
								break;
						}
						break;
					case D_VIEW:
						switch( mViewMenu.getComType(event.getX(), event.getY() ) ){
							case VC_BEFORE:
								BeforeTetra();
								break;
							case VC_NEXT:
								NextTetra();
								break;
							case VC_TOP:
								slideToEdge(TetraArea.FitTo.Up);
								break;
							case VC_BOTTOM:
								slideToEdge(TetraArea.FitTo.Down);
								break;
							case VC_LEFT:
								slideToEdge(TetraArea.FitTo.Left);
								break;
							case VC_RIGHT:
								slideToEdge(TetraArea.FitTo.Right);
							default:
								break;
						}
				}
				resetMovingPoint();
				invalidate();
				return false;
			case MotionEvent.ACTION_POINTER_DOWN:
			case MotionEvent.ACTION_DOWN:
				if(event.getPointerCount() > 1){
					mExt1stDist = getPointDist(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
					setExt1st();

					mTmode = TMODE.T_Ext;
				}
		}
		return false;
	}

	void setExt1st(){
		mExt1stCenter.x = (mEvPosX0 + mEvPosX1) / 2;
		mExt1stCenter.y = (mEvPosY0 + mEvPosY1) / 2;
		mExt1stScale = mScale;
		mExt1stPos.x = mOrgPos.x;
		mExt1stPos.y = mOrgPos.y;
	}

	double getPointDist(float x0,float y0,float x1,float y1){
		setEvPos(x0, y0, x1, y1);

		return (x0-x1)*(x0-x1)+(y0-y1)*(y0-y1);
	}

	boolean switchMovePoint(MotionEvent event){
		switch(event.getAction()){
			case MotionEvent.ACTION_MOVE:
				mTouchOrgsize.x = view2orgX(event.getX());
				mTouchOrgsize.y = view2orgY(event.getY());
				try {
					if(mTetraEditMenu.isMoveFree()){
						mTA.get(mTetFocus).moveTo(mTetraEditMenu.getMoveID(), mTouchOrgsize);
					}else{
						mTA.get(mTetFocus).moveTorm(mTetraEditMenu.getMoveID(), mTouchOrgsize);
					}
				}catch(IndexOutOfBoundsException e){}
				break;
			case MotionEvent.ACTION_DOWN:
				return true;
			case MotionEvent.ACTION_UP:
				resetMovingPoint();
		}
		return false;
	}

	boolean switchMoveRect(MotionEvent event){
		Point mTouchMoveSize = new Point();
		switch(event.getAction()){
			case MotionEvent.ACTION_MOVE:
				mTouchMoveSize.x = view2orgX(event.getX()) - mTouchOrgsize.x;
				mTouchMoveSize.y = view2orgY(event.getY()) - mTouchOrgsize.y;
				try{
					mTA.get(mTetFocus).moveRectR(mTouchMoveSize);
				}catch(IndexOutOfBoundsException e){}
				mTouchOrgsize.x = view2orgX(event.getX());
				mTouchOrgsize.y = view2orgY(event.getY());
				break;
			case MotionEvent.ACTION_DOWN:
				return true;
			case MotionEvent.ACTION_UP:
				resetMovingPoint();
		}
		return false;
	}

	boolean switchMoveLine(MotionEvent event){
		Point mTouchMoveSize = new Point();
		switch(event.getAction()){
			case MotionEvent.ACTION_MOVE:
				mTouchMoveSize.x = view2orgX(event.getX()) - mTouchOrgsize.x;
				mTouchMoveSize.y = view2orgY(event.getY()) - mTouchOrgsize.y;

				try {
					if(mTetraEditMenu.isMoveFree()){
						mTA.get(mTetFocus).moveR(mTetraEditMenu.getMoveID(), mTouchMoveSize);
					}else{
						mTA.get(mTetFocus).moveRrm(mTetraEditMenu.getMoveID(), mTouchMoveSize);
					}
				}catch(IndexOutOfBoundsException e){}

				mTouchOrgsize.x = view2orgX(event.getX());
				mTouchOrgsize.y = view2orgY(event.getY());
				break;
			case MotionEvent.ACTION_DOWN:
				return true;
			case MotionEvent.ACTION_UP:
				resetMovingPoint();
		}
		return false;
	}

	void	resetMovingPoint(){
		mTmode = TMODE.T_None;
		mTetraEditMenu.setMoveID(TetraArea.MOVEID.MI_none);
		if(mDispMode==DspMode.D_AREA_EDIT){
			savePropertyStr();
		}
		mViewMenu.resetOldType();
	}

	boolean switchNone(MotionEvent event){
		switch( mDispMode ){
			case D_VIEW:
				switch(event.getAction()){
					case MotionEvent.ACTION_DOWN:
						if(event.getPointerCount() > 1){
							mExt1stDist = getPointDist(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
							setExt1st();

							mTmode = TMODE.T_Ext;
							return true;
						}
						mTouch.x = event.getX();
						mTouch.y = event.getY();

						mTmode = TMODE.T_Scroll;
						switch(mViewMenu.getTouchedComType((float)mTouch.x, (float)mTouch.y)){
							case VC_TOP: case VC_LEFT: case VC_BOTTOM: case VC_RIGHT:
								mTmode = TMODE.T_None;
						}

						int fno = mAnnoCtrl.checkFocus(view2orgX((float)mTouch.x), view2orgY((float)mTouch.y));
						if( fno != -1 ){
							mAnnoCtrl.setFocus(fno);
							mTmode = TMODE.T_ANNOEDIT;
							mAnnoMenu.setText(mAnnoCtrl.getCurTag().getText());
							mAnnoMenu.move((int)event.getX()-30, (int)event.getY());
						}
						return true;

					case MotionEvent.ACTION_UP:
						switch( mViewMenu.getComType(event.getX(), event.getY()) ){
							case VC_BEFORE:	BeforeTetra();
								break;
							case VC_NEXT:	NextTetra();
								break;
							case VC_TOP:	slideToEdge(TetraArea.FitTo.Up);
								break;
							case VC_BOTTOM:	slideToEdge(TetraArea.FitTo.Down);
								break;
							case VC_LEFT:	slideToEdge(TetraArea.FitTo.Left);
								break;
							case VC_RIGHT:	slideToEdge(TetraArea.FitTo.Right);
							default:
								break;
						}

						resetMovingPoint();
						invalidate();
						break;
					default:
				}
				return false;
			/////////////////////////////////////////////////////////////////////////////////////////////
			case D_AREA_EDIT:
				switch(event.getAction()){
					case MotionEvent.ACTION_DOWN:
						if(event.getPointerCount() > 1){
							mExt1stDist = getPointDist(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
							setExt1st();

							mTmode = TMODE.T_Ext;
							return true;
						}
						mTouch.x = event.getX();
						mTouch.y = event.getY();
						mTouchOrgsize.x = view2orgX(event.getX());
						mTouchOrgsize.y = view2orgY(event.getY());

						mTmode = TMODE.T_Scroll;
						for( int i=0; i<mTA.size(); ++i ) {
							mTetraEditMenu.setMoveID(mTA.get(i).checkTouch(mTouchOrgsize, (float)(40/mScale)));
							switch( mTetraEditMenu.getMoveID() ){
								case MI_Ap0: case MI_Ap1: case MI_Ap2: case MI_Ap3:
									if(mTetFocus==i) {
										mTmode = TMODE.T_MovePoint;
										return true;
									}
									break;
								case MI_Ln0:
									if(mTetFocus==i) {
										mTA.get(i).setFitTo(FitTo.Left);
										mTmode = TMODE.T_MoveLine;
										return true;
									}
									break;
								case MI_Ln1:
									if(mTetFocus==i) {
										mTA.get(i).setFitTo(FitTo.Down);
										mTmode = TMODE.T_MoveLine;
										return true;
									}
									break;
								case MI_Ln2:
									if(mTetFocus==i) {
										mTA.get(i).setFitTo(FitTo.Right);
										mTmode = TMODE.T_MoveLine;
										return true;
									}
									break;
								case MI_Ln3:
									if(mTetFocus==i) {
										mTA.get(i).setFitTo(FitTo.Up);
										mTmode = TMODE.T_MoveLine;
										return true;
									}
									break;
								case MI_Rect:
									mTmode = TMODE.T_MoveRect;
									mTetFocus = i;
									return true;
							}
						}
						//	mTouchedVMenu = ViewMenu.MenuType.T_MenuNone;
						mViewMenu.resetOldType();
						return true;
					case MotionEvent.ACTION_UP:
						switch( mTetraEditMenu.getComType(event.getX(), event.getY(), (float)mViewSize.x) ){
							case TE_Delete:
								RmTetra();
								break;
							case TE_Add:
								AddTetra();
								break;
							default:
								break;
						}
						resetMovingPoint();
						break;
					default:
				}
				return false;
			case D_COLORSET:
				//	float mContrast			 0..10 1is default.
				//	float mBrightness		 -255..255 0 is default
				switch(event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						mTouch.x = event.getX();
						mTouch.y = event.getY();
						mOBrightness = mBrightness;
						mOContrast = mContrast;
						break;
					case MotionEvent.ACTION_MOVE:
						mContrast = mOContrast + (float)(mTouch.x - event.getX())/100;
						if(mContrast < 0){
							mContrast = 0;
						}
						if(mContrast > 10){
							mContrast = 10;
						}
						mBrightness = mOBrightness + (float)(mTouch.y - event.getY())/10;
						if(mBrightness < -255){
							mBrightness = -255;
						}
						if(mBrightness > 255){
							mBrightness = 255;
						}
						break;
					case MotionEvent.ACTION_UP:
						ScaViewerBook.getMomo().setItem("Contrast", String.valueOf(mContrast));
						ScaViewerBook.getMomo().setItem("Brightness", String.valueOf(mBrightness));
						break;
				}
				return true;
		}
		return false;
	}


	final float vlimit = 2000;
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,	float velocityY) {
		if( Math.abs(velocityX) > Math.abs(velocityY)){
			if( velocityX < -vlimit ){
				slideToEdge(TetraArea.FitTo.Right);
				return false;
			}
			if( velocityX > vlimit){
				slideToEdge(TetraArea.FitTo.Left);
				return false;
			}
		}else{
			if( velocityY > vlimit ){
				slideToEdge(TetraArea.FitTo.Up);
				return false;
			}
			if( velocityY < -vlimit ){
				slideToEdge(TetraArea.FitTo.Down);
				return false;
			}
		}
		return false;
	}

	@SuppressLint("NewApi")
	@Override
	public void onLongPress(MotionEvent e) {
		switch(mDispMode){
			case D_VIEW:
//			mAnnoCtrl.start(view2orgX(e.getX()), view2orgY(e.getY()-FINGERBIAS) );
//			mDispMode = DspMode.D_PAINT;
		}
	}
	public void startPaint(){
		mDispMode = DspMode.D_PAINT;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}
	// cmode true -> D_COLORSET
	// cmode false -> D_VIEW
	public void setColorSetmode(boolean cmode){
		mDispMode = DspMode.D_VIEW;
		if( cmode ){
			mDispMode = DspMode.D_COLORSET;
		}
	}
}
