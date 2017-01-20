package com.m_obj.pericaroid;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Set;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

class Global {
	static Object lock = new Object();
}

public class HtmlViewerActivity extends Activity {
	private MenuItem    mItemCamera = null;

	private long mLastKeytime = 0;
	public void setLastKeytime( long t ){
		mLastKeytime = t;
	}
	private final String TAG = "HtmlViewerActivity";
	// OpenCV Manager
	public static final String JSobjName = "uug";

	@Override
	public void onResume()
	{
		super.onResume();
		try {
			server = new PeriServer(this);
		} catch (Exception e) {
			e.printStackTrace();
		}

		webView.loadUrl("javascript:init()");
	}

	public static HtmlViewerActivity mUug;
	private WebView webView;
	private static JSInterface jsi;
	private PeriServer server;

	private static boolean mfromIntent = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		ScaViewerBook.init( this );

		makeWebView();
		mUug = this;

//        prepareLocation();

		Intent intent = getIntent();
		String action = intent.getAction();

		if(Intent.ACTION_MAIN.equals(action)){
			jsi.goPage("start.html");
		}else
		if (Intent.ACTION_VIEW.equals(action) ) {
			ScaViewerBook.goViewer(content2path(intent.getData()), Intent.ACTION_VIEW);
		}else
		if(Intent.ACTION_SEND.equals(action)){
			ScaViewerBook.goViewer(content2path(Uri.parse(Intent2Str(intent))), Intent.ACTION_SEND);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		jsi.goCamera();
		return false;
	/*
		menu.clear();
		mItemCamera = menu.add("Camera");
		mItemCamera.setIcon(drawable.ic_menu_camera);

		return true;
	*/
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item==mItemCamera){
			jsi.goCamera();
			return true;
		}
		return false;
	}

	String content2path(Uri cstr){
		ContentResolver cr = getContentResolver();
		String[] columns = {MediaStore.Images.Media.DATA };
		Cursor c = cr.query(cstr, columns, null, null, null);

		c.moveToFirst();
		return c.getString(0);
	}

	public static String Intent2Str(Intent data){
		if(data==null){
			return "";
		}
		Bundle bundle = data.getExtras();
		if(bundle==null){
			return data.getDataString();
		}else{
			Set<String> set = bundle.keySet();
			for(String entry: set){
				Object go = bundle.get(entry);
				if(go!=null){
					String value = go.toString();
					if( value.indexOf("://")!=-1)
						return value;
				}
			}
		}
		return "";
	}

	void prepareLocation(){
		// LocationManagerを取得
		LocationManager mLocationManager =
				(LocationManager) getSystemService(Context.LOCATION_SERVICE);
		// Criteriaオブジェクトを生成
		Criteria criteria = new Criteria();
		// Accuracyを指定(低精度)
		criteria.setAccuracy(Criteria.ACCURACY_COARSE);
		// PowerRequirementを指定(低消費電力)
		criteria.setPowerRequirement(Criteria.POWER_LOW);
		// ロケーションプロバイダの取得
		String provider = mLocationManager.getBestProvider(criteria, true);

		Toast.makeText(this, provider, Toast.LENGTH_LONG).show();

		// LocationListenerを登録
		mLocationManager.requestLocationUpdates(provider, 0, 0, new LocationListener() {

			@Override
			public void onLocationChanged(Location arg0) {
				// TODO Auto-generated method stub
				Toast.makeText(getApplicationContext(), "onLocationChanged "+arg0.toString(), Toast.LENGTH_LONG).show();
			}

			@Override
			public void onProviderDisabled(String arg0) {
				// TODO Auto-generated method stub
				Toast.makeText(getApplicationContext(), "onProviderDisabled "+arg0, Toast.LENGTH_LONG).show();
			}

			@Override
			public void onProviderEnabled(String arg0) {
				// TODO Auto-generated method stub
				Toast.makeText(getApplicationContext(), "onProviderEnabled "+arg0, Toast.LENGTH_LONG).show();
			}

			@Override
			public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
				// TODO Auto-generated method stub
				Toast.makeText(getApplicationContext(), "onStatusChanged "+arg0, Toast.LENGTH_LONG).show();
			}
		});
	}

	//	@SuppressLint("SetJavaScriptEnabled")
	private void makeWebView(){
		webView=new WebView(this);
		WebSettings settings = webView.getSettings();
		settings.setJavaScriptEnabled(true);
		settings.setSavePassword(false);
		settings.setSaveFormData(false);
		settings.setSupportZoom(true);
		settings.setBuiltInZoomControls(true);
		settings.setRenderPriority(WebSettings.RenderPriority.HIGH);

		//JavaScript
		jsi = new JSInterface(new Handler());
		webView.addJavascriptInterface( jsi, JSobjName);
		webView.setWebChromeClient(new WebChromeClient());

		String scalestr = ScaViewerBook.getMomo().getItem("#scale");
		if( scalestr.equals("") ){
			scalestr = "0";
		}
		int scale = (int)(Float.valueOf(scalestr)*100);
		webView.setInitialScale(scale);

		setContentView(webView);
	}

	public final class JSInterface {
		//	private String mBaseName = "http://localhost:"+Integer.toString(ScaViewerBook.PORT)+"/";
		private String mBaseName = "ScaViewer - ";

		public JSInterface(Handler handler) {
			new HashMap<String,String>();
		}

		public String getPropertyList(){
			return ScaViewerBook.getPropertyList();
		}

		public String getPropSaveDir(){
			return ScaViewerBook.getPropSaveDir();
		}

		public String loadBook(){
			Log.d(ScaViewerBook.TAG, "HtmlViererActivity:loadBook()");
			ScaViewerBook.loadBook();
			return ScaViewerBook.getPicList();
		}
		public void saveBook(){
			Log.d(ScaViewerBook.TAG, "HtmlViererActivity:saveBook()");
			ScaViewerBook.saveCurProp();
		}
		public void goViewer( String picname ){
			Log.d(ScaViewerBook.TAG, "HtmlViererActivity:goViewer(" + picname + ")");
			File f = new File(ScaViewerBook.getFullPath(picname));
			if( f.exists() ) {
				ScaViewerBook.mLastViewPic = picname;
				ScaViewerBook.goViewer(picname);
			}
		}
		public void goCamera(){
			Intent intent = new Intent(getApplicationContext(),com.m_obj.pericaroid.PicMonActivity.class);
			intent.setAction(Intent.ACTION_MAIN);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}
		public void intentFileView(String path){
			File file = new File(path);
			Intent intent = new Intent(Intent.ACTION_VIEW );
			intent.setData(Uri.fromFile(file));
			//	intent.setDataAndType(Uri.fromFile(file), "text/plain");
			startActivity(intent);
		}

		public boolean canGo(){
			long curtime = System.currentTimeMillis();
			return mLastKeytime+800 < curtime;
		}

		public boolean goPage(String pagename){
			Log.d(ScaViewerBook.TAG, "HtmlViererActivity:goPage(" + pagename + ")");
			if( canGo() ) {
				mLastKeytime = System.currentTimeMillis();
				String pstr = ScaViewerBook.getMomo().getItem( "APP/"+pagename);
				if (!pstr.equals("")) {
					webView.loadDataWithBaseURL(mBaseName + pagename, pstr, "text/html", "utf-8", null);
				} else {
					webView.loadUrl(pagename);
				}
				return true;
			}
			return false;
		}

		public void startPage(String path) {
			Log.d(ScaViewerBook.TAG, "HtmlViererActivity:startPage(" + path + ")");
			synchronized (Global.lock) {

				while (!canGo()) {
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				String current = path;
				if (current.endsWith("/")) {
					current = current.substring(0, current.length() - 1);
				}

				saveBook();

				int p = current.lastIndexOf('/');
				setBaseFoler(current.substring(0, p));
				setItem("FolderName", current.substring(p + 1));

				ScaViewerBook.loadProp( path );

				goPage("start.html");
			}
		}

		public String getIPAddressList(){
			return PeriServer.getIPAddressList();
		}
		/////////////////////////
		public void setItem(String key, String val){
			Log.d(ScaViewerBook.TAG, "HtmlViererActivity:setItem("+key+","+val+")");
			ScaViewerBook.getMomo().setItem(key, val);
		}
		public String getItem(String key){
			return ScaViewerBook.getMomo().getItem(key);
		}
		public void clearItem(String key){
			ScaViewerBook.getMomo().removeItem(key);
		}
		public void saveDBtoFile(String key){
			ScaViewerBook.saveDBtoFile(key);
		}
		//////////////////////////
		public String getBaseFoler(){
			return ScaViewerBook.getBaseFolder();
		}
		public void setBaseFoler(String dir){
			Log.d(ScaViewerBook.TAG, "HtmlViererActivity:setBaseFoler("+dir+")");
			ScaViewerBook.setBaseFolder(dir);
		}
		public void saveCurrentDBtoFile(){
			ScaViewerBook.saveCurrentDBtoFile();
		}

		//////////////////////////
		public String getCaption( String fname ){
			String propstr = ScaViewerBook.getPropertyStr(fname);
			String rtn = SimpleXml.str2Value("cap", propstr );

			return  SimpleXml.str2Value("text",rtn);
		}
		public void setCaption( String fname, String value ){
			ScaViewerBook.savePropertyStr(fname,
					SimpleXml.value2Tag("prop", SimpleXml.value2Tag("cap", SimpleXml.value2Tag("text", value))));
			ScaViewerBook.saveCurProp();
			/*
			String prptall = ScaViewerBook.getPropertyStr(fname);
			String propstr = SimpleXml.str2Value("prop", prptall);
			if( propstr.isEmpty()){
				propstr = prptall;
			}
			String[] props = propstr.split("<cap>");
			String propbody = props[0];
			if( props.length > 1){
				String[] propss = props[1].split("</cap>");
				propbody = propbody + propss[1];
			}
			ScaViewerBook.savePropertyStr(ScaViewerBook.getCurrentFolder()+"/"+fname,
					SimpleXml.value2Tag("prop", props[0]+SimpleXml.value2Tag("cap", SimpleXml.value2Tag("text", value))));
			*/
		}
		//////////////////////////
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
		public void rmFile(String fname){
			ScaViewerBook.rmFile(fname);
		}
		//////////////////////////
		public void makeFolder(String foldername){
			ScaViewerBook.makeFolder(foldername);
		}
		public void rmFolder(String foldername){
			ScaViewerBook.rmFolder(foldername);
		}
		public void renameFolder(String oldfoldername, String newfoldername){
			ScaViewerBook.renameFolder(oldfoldername, newfoldername);
		}
		public String getCurrentBook(){
			return ScaViewerBook.getCurrentFolder();
		}
		//////////////////////////
		public String getBookList() {
			return ScaViewerBook.getBookList();
		}
		public String getFileList(String folder){
			return ScaViewerBook.getFileList(folder);
		}
		public String getLastPicno(){
			return Integer.toString(ScaViewerBook.getLastPicno());
		}
		public void putFile( String fname, String data ){
			ScaViewerBook.putFile(fname, data);
		}
		public String getFile( String fname ){
			return ScaViewerBook.getFile(fname);
		}
		///////////////////////////
		@SuppressWarnings("resource")
		public String getImage(String name) {
			File pf = new File( ScaViewerBook.getCurrentFolder()+"/"+name );
			byte[] buffer;
			try {
				buffer = new byte[(int) pf.length()];
				FileInputStream fis;
				fis = new FileInputStream(pf);
				fis.read(buffer);
			} catch (IOException e) {
				InputStream ris = getBaseContext().getResources().openRawResource(R.drawable.icon);
				buffer = new byte[16*1024];
				try {
					ris.read(buffer);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}

			return "data:base64,"+Base64.encodeToString(buffer, Base64.DEFAULT);
		}

		public String getQRcode( String str ){
			byte[] buffer= new byte[16*1024];
			ByteArrayOutputStream outs = new ByteArrayOutputStream();

			InputStream ris = QRCodeControler.mkQRimage( str );
			int readBytes = 0;
			try {
				while ((readBytes = ris.read(buffer)) != -1) {
					outs.write(buffer,0,readBytes);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return "";
			}
			return "data:image/png;base64,"+Base64.encodeToString(outs.toByteArray(), Base64.DEFAULT);
		}

		public String getThum(String name){
			InputStream fis = ScaViewerBook.getThum(name);
			if(fis==null){
				return "error";
			}

			//	return "file://"+ScaViewerBook.getCurrentFolder()+"/"+name;

			byte[] buffer = new byte[1024*1024];
			String thumStr;
			try {
				int s = fis.read(buffer);
				thumStr = "data:image/png;base64,"+Base64.encodeToString(buffer, 0, s, Base64.DEFAULT);
			} catch (IOException e) {
				return "error";
			}
			return thumStr;

		}
	}

	// onKeyDown() 
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_BACK:
				//	jsi.goPage("Javascript:goBack()");
				jsi.goPage("start.html");
				return true;
		}
		return super.dispatchKeyEvent(event);
	}

	@Override
	protected void onPause() {
		server.stop();

		ScaViewerBook.getMomo().setItem("#scale", new String().valueOf(webView.getScale()));
		super.onPause();
	}
}
