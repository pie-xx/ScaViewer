package com.m_obj.pericaroid;

import com.m_obj.pericaroid.Point;

public class TetraArea {
	Point	mSrcApxs[];		// ４角形頂点
	Point	mDstApxs[];		// ４角形頂点

	Point	mBmpSize;
	
	public enum FitTo {Undef, Center, Up, Left, Right, Down, Width, Height,  } ;
	FitTo	mFitTo;

	public enum MOVEID {MI_none, MI_Ap0, MI_Ap1, MI_Ap2, MI_Ap3, MI_Ln0, MI_Ln1, MI_Ln2, MI_Ln3, MI_Rect, };

	void init( int width, int height){
		mBmpSize = new Point(width,height);

		mFitTo = FitTo.Undef;
		mSrcApxs = new Point[4];
		mDstApxs = new Point[4];
		for(int n=0; n<mSrcApxs.length; ++n){
			mSrcApxs[n] = new Point();
			mDstApxs[n] = new Point();
		}
	}
	// init
	TetraArea(int bmpWidth, int bmpHeight){
		init(bmpWidth,bmpHeight);
	}
	// AddTetra
	TetraArea( int width, int height, int bmpWidth, int bmpHeight ){
		init(width,height);

		mSrcApxs[0].x = 0;
		mSrcApxs[0].y = 0;
		mSrcApxs[1].x = 0;
		mSrcApxs[1].y = height;
		mSrcApxs[2].x = width;
		mSrcApxs[2].y = height;
		mSrcApxs[3].x = width;
		mSrcApxs[3].y = 0;

		mBmpSize.x = bmpWidth;
		mBmpSize.y = bmpHeight;
		
		mFitTo = FitTo.Undef;
		/**
		mSrcApxs = new Point[4];
		mSrcApxs[0] = new Point(0,0);
		mSrcApxs[1] = new Point(0,height);
		mSrcApxs[2] = new Point(width,height);
		mSrcApxs[3] = new Point(width,0);

		mDstApxs = new Point[4];
		mDstApxs[0] = new Point(0,0);
		mDstApxs[1] = new Point(0,height);
		mDstApxs[2] = new Point(width,height);
		mDstApxs[3] = new Point(width,0);
		
		mFitTo = FitTo.Undef;
		**/
	}
	// AddTetra
	TetraArea( TetraArea org ){
		mBmpSize = org.mBmpSize.clone();

		mSrcApxs = new Point[4];
		mSrcApxs[0] = org.mSrcApxs[0].clone();
		mSrcApxs[1] = org.mSrcApxs[1].clone();
		mSrcApxs[2] = org.mSrcApxs[2].clone();
		mSrcApxs[3] = org.mSrcApxs[3].clone();

		mDstApxs = new Point[4];
		mDstApxs[0] = org.mDstApxs[0].clone();
		mDstApxs[1] = org.mDstApxs[1].clone();
		mDstApxs[2] = org.mDstApxs[2].clone();
		mDstApxs[3] = org.mDstApxs[3].clone();
	
		mFitTo = org.mFitTo;
	}
	// setProperty
	TetraArea(String tstr, int bmpWidth, int bmpHeight){
		init(bmpWidth,bmpHeight);
		setProperty(tstr);
	}
/**
	TetraArea(double[] apexes){
		mSrcApxs = new Point[4];

		for( int n=0; n < mSrcApxs.length; ++n ){
			if(n < apexes.length*2){
				mSrcApxs[n] = new Point(apexes[n*2], apexes[n*2+1]);
			}else{
				mSrcApxs[n] = new Point(apexes[0], apexes[1]);
			}
		}
	}
**/
/**
 * 
		<tetra>
			<fitto>Undef, Center, Up, Left, Right, Down, Width, Height</fitto>
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
 */
	public String toString(){
		return SimpleXml.value2Tag("fitto",mFitTo.toString())+
				SimpleXml.value2Tag("srcr",toString(mSrcApxs))+
				SimpleXml.value2Tag("dstr",toString(mDstApxs));
	}
	String toString(Point[] primtetra){
		StringBuffer tastr = new StringBuffer();
		for( int n=0; n<mSrcApxs.length; ++n ){
			tastr.append("<p"+Integer.toString(n)+">");
			tastr.append(SimpleXml.value2Tag("x",Double.toString(primtetra[n].x/mBmpSize.x)));
			tastr.append(SimpleXml.value2Tag("y",Double.toString(primtetra[n].y/mBmpSize.y)));
			tastr.append("</p"+Integer.toString(n)+">");
		}
		return tastr.toString();
	}

	public void setProperty(String prpstr){
		String ft = SimpleXml.str2Value("fitto",prpstr);
		if(ft.equalsIgnoreCase("Undef")){
			mFitTo = FitTo.Undef;
		}else
			if(ft.equalsIgnoreCase("Up")){
				mFitTo = FitTo.Up;
			}else
				if(ft.equalsIgnoreCase("Left")){
					mFitTo = FitTo.Left;
				}else
					if(ft.equalsIgnoreCase("Down")){
						mFitTo = FitTo.Down;
					}else
						if(ft.equalsIgnoreCase("Right")){
							mFitTo = FitTo.Right;
						}
		
		String srcStr = SimpleXml.str2Value("src",prpstr);
		setApxs(mSrcApxs, srcStr);
		String dstStr = SimpleXml.str2Value("dst",prpstr);
		setApxs(mDstApxs, dstStr);
		String srcStrR = SimpleXml.str2Value("srcr",prpstr);
		if(!srcStrR.isEmpty())
			setApxs(mSrcApxs, srcStrR);
	}
	void setApxs(Point[] points, String pstr){
		for( int n=0; n < points.length; ++n ){
			String istr = SimpleXml.str2Value("p"+Integer.toString(n), pstr);
			points[n].x = Double.parseDouble(SimpleXml.str2Value("x", istr )) ;
			if(points[n].x < 1.0){
				points[n].x = points[n].x * mBmpSize.x;
			}
			points[n].y = Double.parseDouble(SimpleXml.str2Value("y", istr )) ;
			if(points[n].y < 1.0){
				points[n].y = points[n].y * mBmpSize.y;
			}
		}
	}
	void setApxsR(Point[] points, String pstr){
		for( int n=0; n < points.length; ++n ){
			String istr = SimpleXml.str2Value("p"+Integer.toString(n), pstr);
			points[n].x = Double.parseDouble(SimpleXml.str2Value("x", istr ))  * mBmpSize.x;
			points[n].y = Double.parseDouble(SimpleXml.str2Value("y", istr ))  * mBmpSize.y;
		}
	}
/***
	public void adjustProperty( double ratio){
		for( int n=0; n < mSrcApxs.length; ++n ){
			mSrcApxs[n].x = mSrcApxs[n].x * ratio;
			mSrcApxs[n].y = mSrcApxs[n].y * ratio;
		}
		for( int n=0; n < mDstApxs.length; ++n ){
			mDstApxs[n].x = mDstApxs[n].x * ratio;
			mDstApxs[n].y = mDstApxs[n].y * ratio;
		}
	}
***/

	public void setCircumscribed(){
		float maxX = 0, maxY = 0, minX = (float) mSrcApxs[0].x, minY = (float) mSrcApxs[0].y;
		for( int n=0; n < mSrcApxs.length; ++n ){
			maxX = (float) Math.max(maxX, mSrcApxs[n].x);
			minX = (float) Math.min(minX, mSrcApxs[n].x);
			maxY = (float) Math.max(maxY, mSrcApxs[n].y);
			minY = (float) Math.min(minY, mSrcApxs[n].y);
		}
		
		mDstApxs[0].x = minX;
		mDstApxs[0].y = minY;
		
		mDstApxs[1].x = minX;
		mDstApxs[1].y = maxY;
		
		mDstApxs[2].x = maxX;
		mDstApxs[2].y = maxY;
		
		mDstApxs[3].x = maxX;
		mDstApxs[3].y = minY;
	}
	

	public void setPoint(int n, Point p){
		mSrcApxs[n].x = p.x;
		mSrcApxs[n].y = p.y;
	}
	
//	public void set( TetraArea ta ){
//		setPoints( ta.getPoints() );
//	}
	
	public float[] getSrcPoints(){
		return getPoints(mSrcApxs);
	}
	public float[] getDstPoints(){
		return getPoints(mDstApxs);
	}

	public float[] getPoints(Point[] apxs){
		float[] points = new float[apxs.length*2];
		for( int n=0; n < apxs.length; ++n ){
			points[n*2] = (float) apxs[n].x;
			points[n*2+1] = (float) apxs[n].y;			
		}
		return points;
	}
	public void setPoints(float[] points){
		for( int n=0; n < mSrcApxs.length; ++n ){
			mSrcApxs[n].x = points[n*2];
			mSrcApxs[n].y = points[n*2+1];			
		}		
	}
	
	public FitTo getFitTo(){
		return mFitTo;
	}
	public void setFitTo(FitTo ft){
		mFitTo = ft;
	}
///////////////////////////////////////////////////////////////////////
	public void	moveRectR(Point r){
		for( int n=0; n < mSrcApxs.length; ++n ){
			mSrcApxs[n].x += r.x;
			mSrcApxs[n].y += r.y;
		}
	}
	public void	moveRectTo(Point p){
		for( int n=0; n < mSrcApxs.length; ++n ){
			mSrcApxs[n].x = p.x;
			mSrcApxs[n].y = p.y;
		}
	}
	
	public void	moveR( MOVEID lno, Point r){
		switch(lno){
		case MI_Ap0:	mSrcApxs[0].x += r.x; mSrcApxs[0].y += r.y;	break;
		case MI_Ap1:	mSrcApxs[1].x += r.x; mSrcApxs[1].y += r.y;	break;
		case MI_Ap2:	mSrcApxs[2].x += r.x; mSrcApxs[2].y += r.y;	break;
		case MI_Ap3:	mSrcApxs[3].x += r.x; mSrcApxs[3].y += r.y;	break;
		case MI_Ln0:	mSrcApxs[0].x += r.x; mSrcApxs[0].y += r.y; mSrcApxs[1].x += r.x; mSrcApxs[1].y += r.y;	break;
		case MI_Ln1:	mSrcApxs[1].x += r.x; mSrcApxs[1].y += r.y; mSrcApxs[2].x += r.x; mSrcApxs[2].y += r.y;	break;
		case MI_Ln2:	mSrcApxs[2].x += r.x; mSrcApxs[2].y += r.y; mSrcApxs[3].x += r.x; mSrcApxs[3].y += r.y;	break;
		case MI_Ln3:	mSrcApxs[3].x += r.x; mSrcApxs[3].y += r.y; mSrcApxs[0].x += r.x; mSrcApxs[0].y += r.y;	break;
		case MI_Rect:	moveRectR(r);	break;
		}
	}
	public void	moveRrm( MOVEID lno, Point r){
		switch(lno){
		case MI_Ap0:
			mSrcApxs[0].x += r.x; mSrcApxs[0].y += r.y;	
			mSrcApxs[1].x += r.x; mSrcApxs[3].y += r.y;	
			break;
		case MI_Ap1:	
			mSrcApxs[1].x += r.x; mSrcApxs[1].y += r.y;	
			mSrcApxs[0].x += r.x; mSrcApxs[2].y += r.y;	
			break;
		case MI_Ap2:
			mSrcApxs[2].x += r.x; mSrcApxs[2].y += r.y;	
			mSrcApxs[3].x += r.x; mSrcApxs[1].y += r.y;	
			break;
		case MI_Ap3:
			mSrcApxs[3].x += r.x; mSrcApxs[3].y += r.y;	
			mSrcApxs[2].x += r.x; mSrcApxs[0].y += r.y;	
			break;
		case MI_Ln0:
			mSrcApxs[0].x += r.x;  mSrcApxs[1].x += r.x; 	
			break;
		case MI_Ln1:
			mSrcApxs[1].y += r.y; mSrcApxs[2].y += r.y;	
			break;
		case MI_Ln2:
			mSrcApxs[2].x += r.x; mSrcApxs[3].x += r.x; 	
			break;
		case MI_Ln3:
			mSrcApxs[3].y += r.y; mSrcApxs[0].y += r.y;	
			break;
		case MI_Rect:	moveRectR(r);	break;
		}
	}
	public void moveTo( MOVEID lno, Point p){
		switch(lno){
		case MI_Ap0:	mSrcApxs[0].x = p.x; mSrcApxs[0].y = p.y;	break;
		case MI_Ap1:	mSrcApxs[1].x = p.x; mSrcApxs[1].y = p.y;	break;
		case MI_Ap2:	mSrcApxs[2].x = p.x; mSrcApxs[2].y = p.y;	break;
		case MI_Ap3:	mSrcApxs[3].x = p.x; mSrcApxs[3].y = p.y;	break;
		case MI_Ln0:	mSrcApxs[0].x = p.x; mSrcApxs[0].y = p.y; mSrcApxs[1].x = p.x; mSrcApxs[1].y = p.y;	break;
		case MI_Ln1:	mSrcApxs[1].x = p.x; mSrcApxs[1].y = p.y; mSrcApxs[2].x = p.x; mSrcApxs[2].y = p.y;	break;
		case MI_Ln2:	mSrcApxs[2].x = p.x; mSrcApxs[2].y = p.y; mSrcApxs[3].x = p.x; mSrcApxs[3].y = p.y;	break;
		case MI_Ln3:	mSrcApxs[3].x = p.x; mSrcApxs[3].y = p.y; mSrcApxs[0].x = p.x; mSrcApxs[0].y = p.y;	break;
		case MI_Rect:	moveRectTo(p);	break;
		}
	}
	public void moveTorm( MOVEID lno, Point p){
		switch(lno){
		case MI_Ap0:
			mSrcApxs[0].x = p.x; mSrcApxs[0].y = p.y;	
			mSrcApxs[1].x = p.x; mSrcApxs[3].y = p.y;	
			break;
		case MI_Ap1:	
			mSrcApxs[1].x = p.x; mSrcApxs[1].y = p.y;	
			mSrcApxs[0].x = p.x; mSrcApxs[2].y = p.y;	
			break;
		case MI_Ap2:	
			mSrcApxs[2].x = p.x; mSrcApxs[2].y = p.y;	
			mSrcApxs[3].x = p.x; mSrcApxs[1].y = p.y;	
			break;
		case MI_Ap3:	
			mSrcApxs[3].x = p.x; mSrcApxs[3].y = p.y;	
			mSrcApxs[2].x = p.x; mSrcApxs[0].y = p.y;	
			break;
		case MI_Ln0:
			mSrcApxs[0].x = p.x; mSrcApxs[1].x = p.x; 
			break;
		case MI_Ln1:
			mSrcApxs[1].y = p.y; mSrcApxs[2].y = p.y;	
			break;
		case MI_Ln2:	
			mSrcApxs[2].x = p.x; mSrcApxs[3].x = p.x; 	
			break;
		case MI_Ln3:
			mSrcApxs[3].x = p.x; mSrcApxs[0].x = p.x; 
			break;
		case MI_Rect:	moveRectTo(p);	break;
		}
	}
	
	public MOVEID checkTouch(Point p, float pr){
		for( int n=0; n < mSrcApxs.length; ++n ){
			if( isTouchPoint((float)mSrcApxs[n].x, (float)mSrcApxs[n].y, (float)p.x, (float)p.y, pr) ){
				return MOVEID.valueOf("MI_Ap"+Integer.toString(n));
			}
		}
		// �Ӓ����̃`�F�b�N
		Point tp[] = new Point[5];
		for( int n=0; n < mSrcApxs.length; ++n ){
			tp[n] = mSrcApxs[n];
		}
		tp[4] = mSrcApxs[0];
		for( int n=0; n < mSrcApxs.length; ++n ){
			if( isTouchPoint((float)(tp[n].x+tp[n+1].x)/2, (float)(tp[n].y+tp[n+1].y)/2, (float)p.x, (float)p.y, pr ) ){
				return MOVEID.valueOf("MI_Ln"+Integer.toString(n));
			}
		}

		if(inAreaCheck(p, mSrcApxs)){
			return MOVEID.MI_Rect;
		}
		
		return MOVEID.MI_none;
	}

	static boolean	isTouchPoint(float targetX, float targetY, float x, float y, float a ){
		return inRangeCheck(x, targetX, a ) &&
				inRangeCheck(y, targetY, a );
	}
	// 範囲チェック
	static boolean inRangeCheck( float v, float cv, float ev ){
		float lv = cv - ev;
		float hv = cv + ev;
		return ( lv < v && v < hv );
	}
	static float inAreaValue( Point pos, Point[] area ){
		Point grp = new Point();
		for( int n=0; n < 4; ++n ){
			grp.x = grp.x + (area[n].x - pos.x);
			grp.y = grp.y + (area[n].y - pos.y);
		}

		return (float)( grp.x * grp.x + grp.y * grp.y);
	}
	static boolean inAreaCheck( Point pos, Point[] area ){
		Point grp = new Point();
		for( int n=0; n < 4; ++n ){
			grp.x = grp.x + (area[n].x - pos.x);
			grp.y = grp.y + (area[n].y - pos.y);
		}

		return ( grp.x * grp.x + grp.y * grp.y) < 180000 ;
	}

}
