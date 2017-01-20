package com.m_obj.pericaroid;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifThumbnailDirectory;

//import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

public class ScaViewerBook {
	public static final String TAG = "SVB";
	public static final String INDEXFILE = "index.txt";
	public static final String THUMSFOLDER = ".thums";
	public static final String FOLDERNAME = "FolderName";
	public static final String BASEFOLDER = "BaseFolder";
	public static final String PROPSAVEFOLDER = "PropSaveFolder";
	public static final int	PORT = 8600;

	static private Context mContext;
	static public Context getContext(){
		return mContext;
	}

	static ArrayList<String>	mFileList = new ArrayList<String>();
	static int	mCurPicno;
	static String mLastViewPic = "";
	static String mMarkPic = "";

	private static Momo	mMomo;
	public static Momo	getMomo() { return mMomo; }

	static void init( Context context ){
		setContext(context);
		mMomo = new Momo(context);
//		loadBook();
	}

	static void setContext( Context context ){
		mContext = context;
	}
	public static void loadBook() {
		Log.d(TAG, "ScaViewerBook:loadBook()");
		makePicfileList();
		mCurPicno = 0;
		loadProp( getCurrentFolder() );
		mLastViewPic = getLastViewPic();
	}

	public static void goViewer( String picname ){
		goViewer(picname, "");
	}
	public static void goViewer( String picname, String fromi ){
		// 現Dir設定をファイルに保存
		saveCurProp();

		Intent intent = new Intent(mContext, com.m_obj.pericaroid.SimpleImageViewerActivity.class);
		intent.putExtra(SimpleImageViewerActivity.VIEWSRC, picname);
		intent.putExtra(SimpleImageViewerActivity.FROMI, fromi);
		intent.setAction(Intent.ACTION_MAIN);
		mContext.startActivity(intent);
	}

	////////////////////////////////////////////////////////////////
	public static String getPropertyStr(String srcfile){
		int p = srcfile.lastIndexOf("/");
		if(p != -1){
			srcfile = srcfile.substring(p+1);
		}
		return getMomo().getItem("prop:" + srcfile);
		//	return getMomo().getItem(getFullPath(srcfile));
	}
	public static void savePropertyStr(String srcpath, String property){
		int p = srcpath.lastIndexOf("/");
		if(p != -1){
			srcpath = srcpath.substring(p+1);
		}
		getMomo().setItem("prop:" + srcpath, property);
		//	getMomo().setItem(getFullPath(srcpath), property);
	}
	public static void rmPropertyStr(String srcpath){
		getMomo().removeItem("prop:" + srcpath);
		//	getMomo().removeItem(getFullPath(srcpath));
	}

	public static String getPropertyList(){
		String lastViewing = "";
		if(!getMarkPic().isEmpty()){
			lastViewing = "<item><title>Mark:"+getMarkPic()+"</title>"+
					"<body><prop><cap><text>★</text></cap></prop></body></item>";
		}
		return lastViewing + getMomo().getTitleList("prop:");
		//	return getMomo().getTitleList(getCurrentFolder());
	}
	public static void rmPropertyList(){
		Log.d(TAG, "ScaViewerBook:rmPropertyList()");
		getMomo().rmTitleList("prop:");
	}

	////////////////////////////////////////////////////////////////
	public static String getPicList( ){
		return getFileList( ScaViewerBook.getCurrentFolder()+"/" );
	}
	static class FileSort implements Comparator<File>{
		public int compare(File src, File target){
			int diff = src.getName().compareTo(target.getName());
			return diff;
		}
	}

	public static String getFileList( String foldername ) {
		//	mLastViewPic = getLastViewPic();
		if( foldername.startsWith("/storage/emulated/")&& !foldername.startsWith("/storage/emulated/0/")){
			foldername = foldername.replace("/storage/emulated/","/storage/emulated/0/");
		}
		File dir = new File( foldername );
		StringBuffer fl = new StringBuffer();
		final File[] files = dir.listFiles();
		if(files!=null){
			Arrays.sort(files, new FileSort());
			for(int n=0; n < files.length; ++n){
				if(files[n].isDirectory() )
					fl.append( "<folder>" );
				else
					fl.append( "<file>" );

				fl.append("<name>"+files[n].getName() + "</name><length>"+files[n].length()+"</length>");
				if( mLastViewPic.equals(files[n].getName())){
					fl.append( "<cur>yes</cur>" );
				}
				String cap = getMomo().getItem(foldername+"/"+files[n].getName());
				String[] annos = cap.split("<anno");
				for( int m=1; m < annos.length; ++m ){
					String atext = SimpleXml.str2Value("text", annos[m]);
					if( !atext.equals("") ){
						fl.append(SimpleXml.value2Tag("text", atext));
					}
				}
				Date date = new Date(files[n].lastModified());
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				fl.append("<date>"+ sdf.format(date) + "</date>");

				if(files[n].isFile())
					fl.append( "</file>" );
				else
					fl.append( "</folder>" );
			}
		}
		return "<files>"+fl.toString()+"</files>";
	}

	public static String getBookList() {
		return getFileList( getBaseFolder() );
	}
////////////////////////////////////////////////////////////////
	/**
	 <index>
	 <base>
	 <prop>...
	 </base>
	 <prop>....
	 </index>
	 */
	public static void saveCurrentDBtoFile(){
		File dir = new File( getCurrentFolder() );
		final File[] files = dir.listFiles();

		if(files!=null){
			Arrays.sort(files, new FileSort());
			StringBuffer sb = new StringBuffer();
			sb.append("<book><base>");
			String key = getCurrentFolder();
			String baseprop = getMomo().getItem(key);
			sb.append(baseprop);
			sb.append("</base>");
			for(int n=0; n < files.length; ++n){
				if(files[n].isFile()){
					sb.append(getMomo().getItem(files[n].getPath()));
				}
			}
			sb.append("</book>");
			try {
				FileOutputStream f = new FileOutputStream( getCurrentFolder()+"/index.xml" );
				f.write(sb.toString().getBytes());
				f.close();
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}
		}
	}

	public static void loadCurrentFiletoDB(){

	}

	public static void saveDBtoFile(String key){
		try {
			FileOutputStream f = new FileOutputStream( getCurrentFolder()+"/"+key );
			f.write(getMomo().getItem(key).getBytes());
			f.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}
////////////////////////////////////////////////////////////////

	public static void makePicfileList(){
		File dir = new File( getCurrentFolder() );
		final File[] files = dir.listFiles();

		mFileList.clear();

		if(files!=null){
			Arrays.sort(files, new FileSort());
			for(int n=0; n < files.length; ++n){
				if(files[n].isFile()){
					mFileList.add(files[n].getName());
				}
			}
		}
	}
	static String	getNextFile(){
		if( mCurPicno < mFileList.size() - 1 ){
			mLastViewPic = mFileList.get(++mCurPicno);
			setLastViewPic(mLastViewPic);
			return getCurrentFolder()+"/"+mLastViewPic;
		}
		return null;
	}
	static String	getBeforFile(){
		if( mCurPicno > 0 ){
			mLastViewPic = mFileList.get(--mCurPicno);
			setLastViewPic(mLastViewPic);
			return getCurrentFolder()+"/"+mLastViewPic;
		}
		return null;
	}
	static void 	setCurrentFileIndex(String filename) {
		mCurPicno = 0;
		for( int n=0; n < mFileList.size(); ++n ){
			if( filename.equals(mFileList.get(n)) ){
				setLastViewPic(filename);
				mCurPicno = n;
				return;
			}
		}
	}
	static String getCurrentFilename(){
		return mLastViewPic;
	}
	static int getCurrentPinNo(){
		return mCurPicno;
	}

	static void setMarkPic(String filename) {
		getMomo().setItem(getCurrentFolder()+"/"+":MarkView", filename);
	}
	static String getMarkPic() {
		String rtv = getMomo().getItem(getCurrentFolder()+"/"+":MarkView");
		return rtv;
	}

	static void setLastViewPic(String filename){
		//	mLastViewPic = filename;
		//	getMomo().setItem("prop:LastView", filename);
		getMomo().setItem(getCurrentFolder()+"/"+":LastView", filename);
	}
	static String getLastViewPic(){
		//	String rtv = getMomo().getItem("prop:LastView");
		//	return rtv;
		return getMomo().getItem(getCurrentFolder()+"/"+":LastView");
	}
	static int getLastPicno(){
		String filename = getLastViewPic();
		mCurPicno = 0;
		for( int n=0; n < mFileList.size(); ++n ){
			if( filename.equals(mFileList.get(n)) ){
				mCurPicno = n;
				return n;
			}
		}
		return -1;
	}

	static String getPropSaveDir(){
		String	basefolder = getMomo().getItem(PROPSAVEFOLDER);
		if(basefolder.isEmpty()){
			return Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_PICTURES ).getAbsolutePath()+"/Scaviewer/";
		}
		return basefolder;
	}

	static void putPropSaveDir(String propsavedir){
		getMomo().setItem(PROPSAVEFOLDER, propsavedir);
	}

	static String getBaseFolder(){
		String	basefolder = getMomo().getItem(BASEFOLDER);
		if(basefolder.isEmpty()){
			return Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_PICTURES ).getAbsolutePath()+"/";
		}
		if(!basefolder.endsWith("/")){
			basefolder = basefolder + "/";
		}
		return basefolder;
	}

	static String getSettingFilename(){
		String soutdir = getPropSaveDir();
		String settingname = soutdir;
		if(soutdir.isEmpty()){
			// 保存ディレクトリが未設定のときはファイルディレクトリに index.txt として格納
			settingname = getCurrentFolder() + "/"+INDEXFILE;
		}else {
			makeFolder(soutdir);
			settingname = soutdir+"/"+getPath2Escape(getCurrentFolder());
		}
		return settingname;
	}

	static void saveCurProp(){
		String settingname = getSettingFilename();
		try {
			FileOutputStream f = new FileOutputStream( settingname );
			String propertylist = getPropertyList();
			f.write(propertylist.getBytes());
			f.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static void loadProp( String dirname ){
		Log.d(TAG,"ScaViewerBook:loadProp("+dirname+")");
		String propertystr="";
		try {
			// 保存ディレクトリからファイルを探す
			File file = new File(getPropSaveDir()+"/"+getPath2Escape(dirname));
			FileInputStream fi = new FileInputStream( file );
			byte[] buffer = new byte[(int)file.length()];
			fi.read(buffer);
			propertystr = new String(buffer);
		} catch (FileNotFoundException e) {
			// 無かったら、dirname から探す
			File file = new File(dirname+"/"+INDEXFILE);
			FileInputStream fi = null;
			try {
				fi = new FileInputStream( file );
				byte[] buffer = new byte[(int)file.length()];
				fi.read(buffer);
				propertystr = new String(buffer);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
				return;
			} catch (IOException e1) {
				e1.printStackTrace();
				return;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		// 現Dir設定を消去
		rmPropertyList();

		Log.d(TAG, "ScaViewerBook:loadProp() propertystr=" + shorter(propertystr, 24) + "...");
		// String 解析して DB に書き込む
		String[] items = propertystr.split("<item>");
		for (int n = 1; n < items.length; ++n) {
			String key = SimpleXml.str2Value("title", items[n]);
			String body = SimpleXml.str2Value("body", items[n]);
			getMomo().setItem(key, body);
		}
	}

	public static String shorter(String str, int num){
		if( str.length() <= num ){
			return str;
		}
		return str.substring(0,num);
	}

	// 該当ディレクトリがなければ作成する
	static void setBaseFolder(String dirname){

		String dirname2 = getFullPath(dirname);
		if( dirname2.startsWith("/storage/emulated/") ){
			if(!dirname2.startsWith("/storage/emulated/0")) {
				dirname2 = dirname2.replace("/storage/emulated/", "/storage/emulated/0/");
			}
		}
		makeFolder(dirname2);
		getMomo().setItem(BASEFOLDER, dirname2);
	}

	// 該当ディレクトリがないとき作成しない
	static boolean setBaseFolderNm(String dirname){
		if(dirname==null)
			return false;

		File foutdir = new File(dirname);
		if( !foutdir.exists() ){
			return false;
		}

		getMomo().setItem(BASEFOLDER, dirname);
		return true;
	}

	//@SuppressLint("NewApi")
	static public String getCurrentFolder(){
		String foldername = getMomo().getItem(FOLDERNAME);
		return getBaseFolder()+foldername;
	}
	static public int getCurrentPic(){
		return mCurPicno;
	}

	static String getFullPath(String fname){
		if( fname.indexOf("/")==-1){
			return getCurrentFolder()+"/"+fname;
		}
		return fname;
	}
	static String getPath2Escape( String fullpath){
		return fullpath.replaceAll("%","%25").replaceAll("/", "%2F");
	}
	static String getEscape2Path( String escape){
		return escape.replaceAll("%2F","/").replaceAll("%25","%");
	}
	////Folder/////////////////////////////////////////////
	public static void makeFolder(String foldername){
		File foutdir = new File(getFullPath(foldername));
		if( !foutdir.exists() ){
			foutdir.mkdirs();
			scanCurrent();
		}
	}
	public static void rmFolder(String foldername){
		rmdir(getBaseFolder()+foldername);
		scanCurrent();
	}
	static void rmdir(String dname){
		File fdir = new File(dname);
		if( fdir.isDirectory() ){
			final File[] files = fdir.listFiles();
			if(files!=null){
				for(int n=0; n < files.length; ++n){
					if(files[n].isFile()){
						File f = new File(files[n].getPath());
						Log.d("ScaViewerBook", "delete "+files[n].getPath());
						f.delete();
					}else
					if(files[n].isDirectory()){
						rmdir(files[n].getPath());
					}
				}
			}
			Log.d("ScaViewerBook", "deleteD "+fdir.getPath());
			fdir.delete();
			scanCurrent();
		}
	}
	public static void renameFolder(String oldfoldername, String newfoldername){
		File f = new File( getBaseFolder()+oldfoldername );
		f.renameTo(new File( getBaseFolder()+newfoldername ));
		scanCurrent();
	}

	public static void rmFile( String fname){
		File file = new File(ScaViewerBook.getFullPath(fname));
		file.delete();

		String[] paths = {ScaViewerBook.getFullPath(fname)};
		MediaScannerConnection.scanFile(mContext,
				paths,
				null,
				null);
	}

	public static void putFile( String fname, String data ){
		File file = new File(ScaViewerBook.getFullPath(fname));
		try {
			FileOutputStream fo = new FileOutputStream(file);
			fo.write(data.getBytes());
			scanCurrent();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static String getFile( String fname ){
		File file = new File(ScaViewerBook.getFullPath(fname));
		try {
			FileInputStream fi = new FileInputStream(file);
			StringBuffer sb = new StringBuffer();
			byte[] buffer = new byte[1024*1024];
			int s;
			while( (s = fi.read(buffer)) > 0 ){
				sb.append(new String(buffer,s));
			}
			return sb.toString();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}

	public static void scanCurrent() {
		mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
				Uri.parse("file://" + getCurrentFolder())));
	}

	////Exif///////////////////////////////////////
	public String getExif( String fname ){
		StringBuffer sb = new StringBuffer();
		File jpegFile = new File(ScaViewerBook.getFullPath(fname));
		try {
			Metadata metadata = ImageMetadataReader.readMetadata(jpegFile);
			for (Directory directory : metadata.getDirectories()) {
				for (Tag tag : directory.getTags()) {
					sb.append(tag.toString()+"\n");
				}
			}
		} catch (ImageProcessingException e) {
			// TODO Auto-generated catch block
			sb.append(e.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			sb.append(e.toString());
		}

		return sb.toString();
	}

	public static InputStream getThum( String picname ){
		File jpegFile = new File(getFullPath(picname));
		try {
			Metadata metadata = ImageMetadataReader.readMetadata(jpegFile);
			ExifThumbnailDirectory etd = metadata.getDirectory(ExifThumbnailDirectory.class);
			if( etd == null) {
				return getThumOther(picname);
			}
			byte[] d = etd.getThumbnailData();
			return new ByteArrayInputStream( d );
		} catch (ImageProcessingException e) {
			return getThumOther(picname);
		} catch (IOException e) {
			return null;
		}
	}

	public static InputStream getThumOther( String picname ){
		File thf = new File(getCurrentFolder()+"/"+THUMSFOLDER+"/S"+picname);
		try {
			FileInputStream fi = new FileInputStream(thf);
			return fi;
		} catch (FileNotFoundException e) {
			ThumbnailUtils tu = new ThumbnailUtils();
			Bitmap bmp = tu.createVideoThumbnail(getFullPath(picname), 0);

			if( bmp == null){
				bmp = bmpLoad(getCurrentFolder()+"/"+picname, 320, 240);
			}
			if(bmp!=null){
				ByteArrayOutputStream bo = new ByteArrayOutputStream(200000);
				bmp.compress(CompressFormat.PNG, 80, bo);

				ByteArrayInputStream bi = new ByteArrayInputStream( bo.toByteArray());
				mkThum( picname, bo.toByteArray() );

				return bi;
			}
		}
		return null;
	}
	static void mkThum( String picname, byte[] ba ){
		File foutdir = new File(ScaViewerBook.getCurrentFolder()+"/"+THUMSFOLDER);
		if( !foutdir.exists() ){
			foutdir.mkdirs();
		}
		File of = new File( getCurrentFolder()+"/"+THUMSFOLDER+"/S"+picname );
		try {
			of.createNewFile();
			FileOutputStream ois = new FileOutputStream(of);
			ois.write( ba );
			ois.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public static Bitmap bmpLoad(String path,int width,int height){
		Bitmap bmp = null;
		BitmapFactory.Options opt = new BitmapFactory.Options();

		opt.inJustDecodeBounds = true;

		BitmapFactory.decodeFile(path, opt);

		int scaleW = opt.outWidth / width + 1;
		int scaleH = opt.outHeight / height + 1;
		int scale = Math.max(scaleW, scaleH);

		opt.inJustDecodeBounds = false;
		opt.inSampleSize = scale;

		bmp = BitmapFactory.decodeFile(path, opt);

		return bmp;
	}

}
