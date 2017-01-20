package com.m_obj.pericaroid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;

public class PeriServer extends SimpleHTTPserver {
	public static final int	PORT = 8600;
	private Context mContext;
	
	public PeriServer(Context context) throws IOException {
		super(PORT);
		mContext = context;
	}
	

	

	public static String getIPAddressList(){
		StringBuffer iplist = new StringBuffer();
		iplist.append("<iplist>");
		Enumeration<NetworkInterface> interfaces;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();
		    while(interfaces.hasMoreElements()){
		        NetworkInterface network = interfaces.nextElement();
		        Enumeration<InetAddress> addresses = network.getInetAddresses();
		            
		        while(addresses.hasMoreElements()){
		        	InetAddress ia = addresses.nextElement();	                
		            if( !network.getName().equals("lo") && ia.getAddress().length==4 ){
		            	iplist.append( "<item><net>"+network.getName()+"</net><ip>"+ia.getHostAddress()+"</ip></item>" );
		            }
		        }
		    }
		} catch (SocketException e) {
			e.printStackTrace();
		}		
		iplist.append("</iplist>");
		return iplist.toString();
	}

	public Response serve( String uri, String method, Properties header, Properties parms, byte[] fbuf, Socket mySocket )
	{
		if(uri.equals("/cmd/actname")){
			ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Service.ACTIVITY_SERVICE);
			List<ActivityManager.RunningTaskInfo> rt = activityManager.getRunningTasks(3);		
			String className = rt.get(0).topActivity.getClassName();

			return new Response( HTTP_OK, "text/xml", "<act>"+className+"</act>");
		}

		if( uri.equals("/page/camera.html")){
			return new Response( HTTP_OK, "text/html", Momo.getAsset("camera.html"));
		}
		if( uri.equals("/page/test.html")){
			return new Response( HTTP_OK, "text/html", Momo.getAsset("test.html"));
		}
		if( uri.equals("/favicon.ico")){
			return new Response( HTTP_OK, "image/png", mContext.getResources().openRawResource(R.drawable.icon));
		}
		if( uri.equals("/cmd/chgdir")){
			if( ScaViewerBook.setBaseFolderNm(parms.getProperty("p")) ){
				return new Response( HTTP_OK, "text/xml", "<result>ok</result>");
			}else{
				return new Response( HTTP_INTERNALERROR, "text/xml", "<error>not exist dir</error>");
			}
		}
		if( uri.equals("/cmd/mkdir")){
			ScaViewerBook.setBaseFolder(parms.getProperty("p"));
			return new Response( HTTP_OK, "text/xml", "<result>ok</result>");
		}
		if( uri.equals("/cmd/cap")){
			return new Response( HTTP_INTERNALERROR, "text/xml", "<error>camera not started</error>");
		}
		if( uri.equals("/cmd/cap2")){
			return new Response( HTTP_OK, "image/png", mContext.getResources().openRawResource(R.drawable.icon));
		}
		if( uri.equals("/cmd/preview")){
			return new Response( HTTP_OK, "image/jpeg", mContext.getResources().openRawResource(R.drawable.icon));
		}
		if( uri.equals("/cmd/folderlist")){
			File dir = new File( ScaViewerBook.getBaseFolder() );
			StringBuffer fl = new StringBuffer();
			final File[] files = dir.listFiles();
			if(files!=null) {
				for(int n=0; n < files.length; ++n){
					if(files[n].isDirectory())
						fl.append( "<name>"+files[n].getName() + "</name>" );
				}
			}
			return new Response( HTTP_OK, "text/xml", "<folders>"+fl.toString()+"</folders>");
		}
		if( uri.equals("/cmd/piclist")){
			return new Response( HTTP_OK, "text/xml", ScaViewerBook.getPicList());
		}
		if( uri.equals("/cmd/curdir")){
			return new Response( HTTP_OK, "text/xml", "<curdir>"+ScaViewerBook.getCurrentFolder()+"</curdir>");
		}
		if( uri.equals("/cmd/getpic")){
			File pf = new File( ScaViewerBook.getCurrentFolder()+"/"+parms.getProperty("p") );
			try {
				return new Response(HTTP_OK, "image/jpeg", new FileInputStream(pf));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			return new Response(HTTP_OK, "image/jpeg", mContext.getResources().openRawResource(R.drawable.icon));
		}
		if( uri.equals("/cmd/getthum")){
			return new Response(HTTP_OK, "image/png", ScaViewerBook.getThum(parms.getProperty("p")) );
		}
		if( uri.equals("/cmd/list")){
			return new Response( HTTP_OK, "text/xml", ScaViewerBook.getPicList());
		}

		return super.serve(uri, method, header, parms, fbuf, mySocket);
	}
	
	int getParamI( String p, int d){
		if( p==null || p.equals("")){
			return d;
		}
		return Integer.parseInt(p);
	}
	

}
