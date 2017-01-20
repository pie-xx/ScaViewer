package com.m_obj.pericaroid;

import java.util.ArrayList;

import com.m_obj.pericaroid.AnnotationTag.AnnoType;

import android.R.drawable;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
/**
 * 注釈の編集制御
 * ストロークデータの保持
 * 
 * @author kajikawa
 *
 */
public class AnnotationCtrl {
	ArrayList<AnnotationTag> 	mATags;
	int	mFNo;

	PieView	mPieView;
	Point	mCursor;
	
	public AnnotationCtrl(PieView pv){
		mATags = new ArrayList<AnnotationTag>();
		mPieView = pv;
		mCursor = new Point();
	}
	public int getSize(){
		return mATags.size();
	}
	public AnnotationTag get(int n){
		return mATags.get(n);
	}
	public AnnotationTag getCurTag(){
		return mATags.get(mFNo);
	}

	public void setFocus(int fno){
		mFNo = fno;
	}
	
	public void start(float f, float g ){
		mCursor.x = f;
		mCursor.y = g;
		mATags.add(new AnnotationTag(mPieView.getOrgWidth(), mPieView.getOrgHeight()));
		mFNo = mATags.size() - 1;
		addStroke( f, g );
	}
	public void addStroke(float f, float g ){
		mATags.get(mFNo).AddStroke(new Point(f, g));
	}
	
	public void clearTag(){
		mATags.clear();
	}
	public void addTag(String astr){
		mATags.add(new AnnotationTag(astr, mPieView.getOrgWidth(), mPieView.getOrgHeight()));
	}
	
	public int	checkFocus(float orgx, float orgy){
		for( int n=0; n < mATags.size(); ++n ){
			if(mATags.get(n).getType() == AnnoType.TEXT ){
				Point r = mATags.get(n).get(0);
				if( r.x - 30 <= (int)orgx && (int)orgx <= r.x + 30 &&
						r.y - 30 <= (int)orgy && (int)orgy <= r.y + 30	){
					return n;
				}
			}
		}
		return -1;
	}
	

//	public void adjustProperty( double ratio){
//		for( int n=0; n < mATags.size(); ++n ){
//			mATags.get(n).adjustStroke(ratio);
//		}		
//	}
	
	public AnnotationTag.AnnoType decision(){
		AnnotationTag fat = mATags.get(mFNo);
		if(fat.getStrokeDist() < 100 ) {
			Rect r = mATags.get(mFNo).getStrokeRect();
			float hdist = r.width;
			if(hdist < r.height)
				hdist = r.height;
			if( hdist < 10){
				mATags.get(mFNo).setType(AnnotationTag.AnnoType.TEXT);
				return AnnotationTag.AnnoType.TEXT;
			}
			mATags.get(mFNo).setType(AnnotationTag.AnnoType.CIRCLE);
			return AnnotationTag.AnnoType.CIRCLE;
		}
		return AnnotationTag.AnnoType.FREEHAND;			
	}
	
	public void show(Canvas canvas){
		Paint pTextBack = new Paint();
		pTextBack.setColor(Color.DKGRAY);
		pTextBack.setStrokeWidth(4);
		pTextBack.setStyle(Paint.Style.STROKE);
		pTextBack.setFakeBoldText(true);
		pTextBack.setTextSize(30);

		Paint pText = new Paint();
		pText.setColor(Color.YELLOW);
		pText.setStrokeWidth(1);
		pText.setStyle(Paint.Style.FILL);
		pText.setFakeBoldText(true);
		pText.setTextSize(30);

		Paint pLine = new Paint();
		pLine.setColor(Color.RED);
		pLine.setStyle(Paint.Style.STROKE);
		pLine.setStrokeWidth(4);

		for( int m = 0; m < getSize(); ++m ){
			AnnotationTag at = get(m);
			switch(at.getType()){
			case TEXT:
				canvas.drawCircle(
						mPieView.org2viewX((float)at.get(0).x), mPieView.org2viewY((float)at.get(0).y), 20, pText);
				canvas.drawText(at.getText(), 
						mPieView.org2viewX((float)at.get(0).x), mPieView.org2viewY((float)at.get(0).y), pTextBack);
				canvas.drawText(at.getText(), 
						mPieView.org2viewX((float)at.get(0).x), mPieView.org2viewY((float)at.get(0).y), pText);
				break;
			case CIRCLE:
				Rect r = at.getStrokeRect();
				canvas.drawOval(new RectF(mPieView.org2viewX(r.x),mPieView.org2viewY(r.y),
						mPieView.org2viewX(r.x+r.width),mPieView.org2viewY(r.y+r.height)), pLine);
				break;
			default:
				for( int n=1; n < at.getSize(); ++n ){
					canvas.drawLine(mPieView.org2viewX((float)at.get(n-1).x), mPieView.org2viewY((float)at.get(n-1).y),
							mPieView.org2viewX((float)at.get(n).x), mPieView.org2viewY((float)at.get(n).y), pLine);
				}
			}
		}
		
	}
}
