package com.m_obj.pericaroid;

import java.util.ArrayList;

public class AnnotationTag {
	public enum AnnoType {FREEHAND, LINE, CIRCLE, BOX, TEXT, GROUP};
	AnnoType	mAnnoType;

	ArrayList<Point> 	mStrokes;
	String		mText;
	Point	mBmpSize;
	
	public AnnotationTag(int bmpwidth, int bmpheight) {
		if( bmpwidth == 0 )
			bmpwidth = 1024;
		if( bmpheight == 0 )
			bmpheight = 1024;

		mBmpSize = new Point(bmpwidth, bmpheight);
		mAnnoType = AnnoType.FREEHAND;
		mStrokes = new ArrayList<Point>();
		mText = "";
	}
	public AnnotationTag(String prp, int bmpwidth, int bmpheight){
		if( bmpwidth == 0 )
			bmpwidth = 1024;
		if( bmpheight == 0 )
			bmpheight = 1024;

		mBmpSize = new Point(bmpwidth, bmpheight);
		mText = "";
		mStrokes = new ArrayList<Point>();

		String tstr = SimpleXml.str2Value("type", prp);
		mAnnoType = AnnoType.FREEHAND;
		if( tstr.equalsIgnoreCase("TEXT")){
			mAnnoType = AnnoType.TEXT;
			mText = SimpleXml.str2Value("text", prp);
		}else
			if( tstr.equalsIgnoreCase("CIRCLE")){
				mAnnoType = AnnoType.CIRCLE;
			}else
				if( tstr.equalsIgnoreCase("FREEHAND")){
					mAnnoType = AnnoType.FREEHAND;
				}else
					if( tstr.equalsIgnoreCase("LINE")){
						mAnnoType = AnnoType.LINE;
					}else
						if( tstr.equalsIgnoreCase("BOX")){
							mAnnoType = AnnoType.BOX;
						}else
							if( tstr.equalsIgnoreCase("GROUP")){
								mAnnoType = AnnoType.GROUP;
							}
		
		String[] pStrs = prp.split("<p");
		if( pStrs.length > 1 ){
			for( int n=1; n < pStrs.length; ++n ){
				String pstr = pStrs[n];
				String xstr = SimpleXml.str2Value("x", pstr);
				String ystr = SimpleXml.str2Value("y", pstr);
				float xf = Float.parseFloat(xstr);
				float yf = Float.parseFloat(ystr);
				mStrokes.add(new Point(xf*bmpwidth,yf*bmpheight));
			}
		}
	}
//	public void adjustStroke(double ratio){
//		for( int n=0; n < mStrokes.size(); ++n ){
//			mStrokes.get(n).x = mStrokes.get(n).x * ratio;
//			mStrokes.get(n).y = mStrokes.get(n).y * ratio;
//		}
//	}
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("<anno>");
		sb.append("<type>"+mAnnoType.toString()+"</type>");
		switch(mAnnoType){
		case TEXT:
			sb.append("<text>"+mText+"</text>");
			break;
		case FREEHAND:
		case LINE:
		case CIRCLE:
		case BOX:
		case GROUP:
		default:
		}

		for( int n=0; n < mStrokes.size(); ++n ){
			sb.append("<p"+Integer.toString(n)+">");
			sb.append("<x>"+Float.toString( (float)(mStrokes.get(n).x / mBmpSize.x) )+"</x>");
			sb.append("<y>"+Float.toString( (float)(mStrokes.get(n).y / mBmpSize.y) )+"</y>");			
			sb.append("</p"+Integer.toString(n)+">");
		}
		sb.append("</anno>");

		return sb.toString();
	}	
	
	public int getSize() {
		return mStrokes.size();
	}
	public Point	get(int n){
		try{
		return mStrokes.get(n);
		}catch(IndexOutOfBoundsException e){
			
		}
		return new Point(0,0);
	}
	public void setType(AnnoType at){
		mAnnoType = at;
	}
	public AnnoType getType(){
		return mAnnoType ;
	}
	public String getText(){
		return mText;
	}
	public void setText(String str){
		mText = str;
	}
	
	public float getStrokeDist(){
		int lastno = mStrokes.size() - 1;
		if( lastno > 0 ){
			return dist(mStrokes.get(0), mStrokes.get(lastno));
		}
		return 0;
	}
	
	float dist( Point a, Point b ){
		return (float)((a.x - b.x)*(a.x - b.x) + (a.y - b.y)*(a.y - b.y));
	}
	
	public Rect getStrokeRect(){
		float top, left, bottom, right;
		if( mStrokes.size()==0){
			return new Rect(0,0,0,0);
		}
		top = bottom = (float) mStrokes.get(0).y;
		left = right = (float) mStrokes.get(0).x;
		for( int n=1; n < mStrokes.size(); ++n ){
			Point p = mStrokes.get(n);
			if(top > p.y)
				top = (float) p.y;
			if(bottom < p.y)
				bottom = (float)p.y;
			if(left > p.x)
				left = (float)p.x;
			if(right < p.x)
				right = (float)p.x;
		}
		return new Rect((int)left,(int)top,(int)(right-left),(int)(bottom-top));
	}
	
	public void AddStroke(Point p){
		mStrokes.add(p.clone());
	}

}
