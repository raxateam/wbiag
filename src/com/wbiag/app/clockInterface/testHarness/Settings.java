package com.wbiag.app.clockInterface.testHarness;

import java.io.*;
import java.util.*;

class Settings {

	File _f;	//pointer to settings.conf file
	Vector _v;	//stores name value pairs from _f

	//constructor
	public Settings(String sFile) {

		_v = new Vector();
		_f = new File( sFile ); 

		if ( _f == null ) return;

	}

	public String get( String s ) {

		if ( s == null ) return null;

		s = s.toUpperCase();

		SettingsNode sn = null;
		for( int i = 0; i < _v.size(); i++ ) {
			
			sn = (SettingsNode) _v.elementAt(i);

			if ( s.trim().compareTo(sn.name) == 0 ) {
				return sn.value;
			} 

		}
		return null;
	}//get

	public boolean init() {

		if ( _f == null ) return false;

		BufferedReader br = null;
		String sLine = null;
		try {
			br = new BufferedReader(new FileReader(_f));

			String sTok = null;
			StringTokenizer st;
			SettingsNode sn;
			while( (sLine = br.readLine()) != null ) {
				st = new StringTokenizer( sLine, "=" );
				sTok = st.nextToken().trim();
				if ( sTok.indexOf("#") == 0 ) { 
					continue;
				}
				sn = new SettingsNode();
				sn.name = sTok.toUpperCase();	
				sn.value = st.nextToken().trim();
				_v.add(sn);
			}//while
		

		} catch ( IOException e ) {
			System.err.println("settings.conf file corruption at line: " + sLine);
		} finally {
			try {
    			if ( br != null ) br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return true;
			
	}//init


	public void print() {

		System.out.println("---------");
		System.out.println("SETTINGS:");
		System.out.println("---------");
		SettingsNode sn;
		for( int i=0; i < _v.size(); i++ ) {
			sn = (SettingsNode) _v.elementAt(i);
			System.out.println(""+sn.name+" - "+sn.value);
		}
		System.out.println("\n");
	}//print

	


}
