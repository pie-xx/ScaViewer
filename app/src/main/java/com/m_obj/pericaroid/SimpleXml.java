package com.m_obj.pericaroid;

public class SimpleXml {
	// 
	static public String str2Value(String key, String target ){
		String[] iStrs =  target.split("<"+key);
		if(iStrs.length < 2)
			return "";

		String[] orgs = iStrs[1].split("</"+key);
		String[] poss = orgs[0].split(">", 2);
		if( poss.length < 2)
			return "";

		return poss[1];
	}
	
	static public String value2Tag(String tag, String val){
		return "<"+tag+">"+val+"</"+tag+">";
	}

}
