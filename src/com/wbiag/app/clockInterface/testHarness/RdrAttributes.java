package com.wbiag.app.clockInterface.testHarness;

import java.io.*;
import java.util.*;

class RdrAttributes {

	File _f;			//pointer to readers.conf file
	Vector _v;			//stores reader details including IP address from _f
	String _Sfirmware;	//the firmware version of the readers for swipes
	String _Hfirmware;	//the firmware version of the readers for heartbeats
	String _svrIP;		//the wbcs ip address
	String _svrPort;	//the wbcs port

	private static final String swipeHead = "script_";
	private static final String swipeTail = ".dat";

	//constructor
	public RdrAttributes(String sFile) {

		_v = new Vector();
		_f = new File( sFile ); 

	}

	public int size() {
		if ( _v == null ) return -1;
		return _v.size();
	}

	//note the firmware version for the readers is set in the settings.conf file
	public void setSwipeFirmware(String s) {
		_Sfirmware = s;	
	}
	public String getSwipeFirmware() {
		return _Sfirmware;
	}

	public void setHeartFirmware(String s) {
		_Hfirmware = s;	
	}
	public String getHeartFirmware() {
		return _Hfirmware;
	}



	public void setSvrIP(String ip) {
		_svrIP = ip;
	}	
	public String getSvrIP() {
		return _svrIP;
	}


	public void setSvrPort(String port) {
		_svrPort = port;
	}
	public String getSvrPort() {
		return _svrPort;
	}

	public String getRdrNameByIndex( int i ) {
		if ( i < 0 || i > size()-1 ) return null;
		RdrAttribNode ran = (RdrAttribNode) _v.elementAt(i);
		return ran.name;
	}

	public String getRdrIPByIndex( int i ) {
		if ( i < 0 || i > size()-1 ) return null;
		RdrAttribNode ran = (RdrAttribNode) _v.elementAt(i);
		return ran.ip_addr;
	}

	public int getSwipeSizeByIndex( int i ) {
		if ( i < 0 || i > size()-1 ) return -1;
		RdrAttribNode ran = (RdrAttribNode) _v.elementAt(i);
		return ran.getNumSwipes();
	}

	public Vector getRdrSwipesByIndex( int i ) {
		if ( i < 0 || i > size()-1 ) return null;
		RdrAttribNode ran = (RdrAttribNode) _v.elementAt(i);
		return ran.getSwipes();
	}

	public int getRdrSwipeIndexByIndex( int i ) {
		if ( i < 0 || i > size()-1 ) return -1;
		RdrAttribNode ran = (RdrAttribNode) _v.elementAt(i);
		return ran.getSwipeIndex();
	}

	public void addToRdrSwipeIndexByIndex( int i, int j ) {
		if ( i < 0 || i > size()-1 ) return;
		RdrAttribNode ran = (RdrAttribNode) _v.elementAt(i);
		ran.addToSwipeIndex(j);
	}

	public String getIPAddrByName( String s ) {
		if ( s == null ) return null;

		s = s.toUpperCase();

		RdrAttribNode ran = null;
		for( int i = 0; i < _v.size(); i++ ) {
			
			ran = (RdrAttribNode) _v.elementAt(i);

			if ( s.trim().compareTo(ran.name) == 0 ) {
				return ran.ip_addr;
			} 

		}
		return null;
	}//get


	//initialize the object
	public boolean init() {

		if ( _f == null ) return false;

		BufferedReader br = null;
		String sLine = null;
		try {
			br = new BufferedReader(new FileReader(_f));

			String sTok = null;
			StringTokenizer st;
			RdrAttribNode ran;
			while( (sLine = br.readLine()) != null ) {
				st = new StringTokenizer( sLine, "=" );
				sTok = st.nextToken().trim();
				if ( sTok.indexOf("#") == 0 ) { 
					continue;
				}
				ran = new RdrAttribNode();
				ran.name = sTok.toUpperCase();	
				ran.ip_addr = st.nextToken().trim();
				_v.add(ran);
			}//while
		

		} catch ( IOException e ) {
			System.err.println("settings.conf file corruption at line: "+sLine);
		} finally {
			try {
				if ( br != null ) br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return true;
			
	}//init

	//load the swipe script for each reader
	public boolean loadSwipes(String path) {

		File swipeFile;
		BufferedReader br = null;
		String sLine = null;

		String	rdrName;	
		int		swipeSize;
		RdrAttribNode ran;

		for ( int a = 0; a < size(); a++ ) {
			ran = (RdrAttribNode) _v.elementAt(a);
			rdrName = ran.name;

			//first try to get a unique script for the reader
			swipeFile = new File( path + swipeHead + rdrName + swipeTail ); 

			if ( !swipeFile.exists() ) {
				rdrName = ran.name.substring(0,ran.name.length()-1);

				swipeFile = new File( path + swipeHead + rdrName + swipeTail ); 

				//next try to see if a swipe script exists for a branch
				//if this doesn't exist, we can't go any further
				if ( !swipeFile.exists() ) {
					System.err.print("Error: no swipe script file exists for ");
					System.err.println("branch - " + rdrName + ".\n");
					return false;
				}

			}//if no file exists
			
			swipeSize = 0;

			try {

				br = new BufferedReader( new FileReader(swipeFile) );

				while( (sLine = br.readLine()) != null ) {

					if (sLine.length() == 0) continue;
					if ( sLine.indexOf("#") == 0 ) continue; 
					swipeSize++;
					ran.addSwipe( sLine + "," + _Sfirmware );

				}//while

				ran.setNumSwipes( swipeSize );

			} catch ( IOException e ) {
				e.printStackTrace();
			} finally {
				try {
					if ( br != null ) br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}//for

		return true;

	}//loadSwipes


	public void print() {

		System.out.println("------------------");
		System.out.println("READER ATTRIBUTES:");
		System.out.println("------------------");
		RdrAttribNode ran;
		for( int i=0; i < _v.size(); i++ ) {
			ran = (RdrAttribNode) _v.elementAt(i);
			System.out.println(""+ran.name+" - "+ran.ip_addr);
		}
		System.out.println("\n");
	}//print

	


}
