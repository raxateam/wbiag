package com.wbiag.app.clockInterface.testHarness;

import java.lang.Thread;
import java.util.*;
import java.io.*;
import java.net.*;

class SynelReader extends Thread {

	ReaderBuffer _rb;
	String _name;
	String _ipaddr;
	String _svrIP;
	String _svrPort;
	String _heartFirmware;
	String _timeDuration;

	public static final byte[] clockACK = {0x06,0x30,0x39,0x3c,0x3f,0x35,0x04};
	public static final byte[] clockNOMORE={0x6e,0x30,0x31,0x3e,0x37,0x36,0x04};
	public static final byte[] wbcsACK = {0x43,0x30,0x36,0x3e,0x3c,0x3c,0x04};
	public static final int MAX_SWIPES_PER_SESSION = 100;

	//const
	public SynelReader( String name, String ipaddr, String svrIP, 
		String svrPort, String heartFirmware, ReaderBuffer buff) {

		this._rb = buff;
		this._name = name;
		this._ipaddr = ipaddr;
		this._svrIP = svrIP;
		this._svrPort = svrPort;
		this._heartFirmware = heartFirmware;

	}
	
	public void run() {

		//setup timer
		long _timeDuration = System.currentTimeMillis();
	

		//If we have sent the first swipe
		boolean firstSwipeSent = false;
		boolean extendedEnding = false;

		//If we have slept 3 times then send a heart-beat
		//this means we'll send a heartbeat roughly every 45 seconds
		int	numConsecutiveSleeps = 0;

		int numSwipesPerSession = 0;

		//total number of swipes per run of the harness per virtual clock
		int numSwipesPerRun = 0;

		int numIterations = 0;

		Socket			sock	= null;
		OutputStream	os		= null;
		DataInputStream	is		= null;
		byte[]			buff	= new byte[1000];

		while ( true ) {

			numIterations++;

			boolean buffEmpty = this._rb.getBuff().isEmpty();

			if ( sock == null /*&& _rb.isMoreDumps()*/ &&
				( /*numConsecutiveSleeps == 3 ||*/ !buffEmpty)) {
				try {
					sock = new Socket(_svrIP, Integer.parseInt(_svrPort), 
						InetAddress.getByName(_ipaddr), 0);

					System.out.print( _name + " create sock - ");
					System.out.print("svrIP:"+_svrIP);
					System.out.print(":"+_svrPort);
					System.out.print(" IP:"+_ipaddr);
					System.out.println(":"+sock.getLocalPort());

					os = sock.getOutputStream();
					is = new DataInputStream(sock.getInputStream());

				} catch (UnknownHostException uhe) {
					System.out.println( "TGW - UNKNOWN HOST.");
					System.out.flush();
					uhe.printStackTrace();
					cleanup( os, is, sock );
					sock = null;
					try { this.sleep(200); } catch (InterruptedException e) { }
					currentThread().yield();
					continue;
				} catch (IOException ioe) {
					System.out.println( _name +" Socket busy try again later.");
					System.out.flush();
					cleanup( os, is, sock );
					sock = null;
					try { this.sleep(200); } catch (InterruptedException e) { }
					currentThread().yield();
					continue;
				}

			}//if sock is null

			//check to see if something is in my buffer
			if ( !buffEmpty ) {

				if (!firstSwipeSent) {
					sendSwipes(this._rb.getBuff(), 1, os);

					if ( receiveACK(buff,is) == -9 ) {
						cleanup( os, is, sock );
    					sock = null;
						continue;
					}

					firstSwipeSent = true;
					this._rb.getBuff().remove(0);
					numSwipesPerSession++;
					numSwipesPerRun++;
				} else {
					if ( this._rb.getBuff().size() >= 5 ) {
						sendSwipes( this._rb.getBuff(), 5, os);

						if ( receiveACK(buff,is) == -9 ) {
							cleanup( os, is, sock );
    						sock = null;
							continue;
						}

						for(int b=0; b<5; b++){
							this._rb.getBuff().remove(0);
						}
						extendedEnding = true;
						numSwipesPerSession += 5;
						numSwipesPerRun += 5;
					} else {
						int buffsize = this._rb.getBuff().size();
						sendSwipes( this._rb.getBuff(), buffsize, os );

						if ( receiveACK(buff,is) == -9 ) {
							cleanup( os, is, sock );
    						sock = null;
							continue;
						}

						for(int b=0; b<buffsize; b++){
							this._rb.getBuff().remove(0);
						}
						extendedEnding = true;
						numSwipesPerSession += buffsize;
						numSwipesPerRun += buffsize;
					}
				}
				sendACK(os);
				receiveMORE(buff,is);

				if ( this._rb.getBuff().size() == 0 || 
						(numSwipesPerSession >= MAX_SWIPES_PER_SESSION) ) {
					sendNOMORE(os);

/* protocol change...dont' need this anymore

					receiveACK(buff,is);
					sendACK(os);
					if ( extendedEnding ) {
						receiveACK(buff,is);
						sendACK(os);
						receiveACK(buff,is);
					}
*/
					firstSwipeSent = false;
					extendedEnding = false;
					try {

						currentThread().sleep(500);

						cleanup( os, is, sock );
						sock = null;
						System.out.println( _name + "socket closed.");
						numSwipesPerSession = 0;

					} catch (InterruptedException ie ) {
						ie.printStackTrace();
					} finally {
						sock = null;
					}

				}	

			} else if ( !_rb.isMoreDumps() && _rb.getBuff().isEmpty() ) {
				System.out.print( _name + " completed successfully sending ");
				System.out.print( "" + numSwipesPerRun + " swipes in ");	
				System.out.println( "" + (System.currentTimeMillis() - 
					_timeDuration) + " milliseconds.");

				break;	

			//if there is nothing in the buffer, then sleep for 15 seconds
			} else {
				numConsecutiveSleeps++;

				if ( numConsecutiveSleeps == 4 ) {
					numConsecutiveSleeps = 0;
					/*
					System.out.println(_name + " Heartbeat");
					sendHeartBeat(os,is);
					receiveACK(buff, is);			
					sendACK(os);
					*/
					cleanup( os, is, sock );
					sock = null;
				} else {

/*
					try {
						int idelay = 200;
						this.sleep(idelay);
					} catch (InterruptedException e) { }
*/
				}
			}

			currentThread().yield();
			
		}//for
	}

	private int sendHeartBeat(OutputStream os,DataInputStream is) {

		Calendar	c			= Calendar.getInstance();
		SynelCRC	syncrc		= new SynelCRC();
		java.util.Date curDate  = new Date( System.currentTimeMillis() );
		c.setTime( curDate );


		//CREATE DATE  -  "20041210"
		String swipeDate = "";
		swipeDate += c.get( Calendar.YEAR );
		if ( ( c.get( Calendar.MONTH ) + 1 ) < 10 ) {
    		swipeDate += "0" + ( c.get( Calendar.MONTH ) + 1 );
		} else {
    		swipeDate += ( c.get( Calendar.MONTH ) + 1 );
		}

		if ( c.get( Calendar.DATE ) < 10 ) {
    		swipeDate += "0" + c.get( Calendar.DATE );
		} else {
    		swipeDate += c.get( Calendar.DATE );
		}


		//CREATE TIME  -  "173318"
		String swipeTime = "";
		if ( c.get( Calendar.HOUR_OF_DAY ) < 10 ) {
    		swipeTime += "0" + c.get( Calendar.HOUR_OF_DAY );
		} else {
    		swipeTime += c.get( Calendar.HOUR_OF_DAY );
		}


		if ( c.get( Calendar.MINUTE ) < 10 ) {
    		swipeTime += "0" + c.get( Calendar.MINUTE );
		} else {
    		swipeTime += c.get( Calendar.MINUTE );
		}

		if ( c.get( Calendar.SECOND ) < 10 ) {
        	swipeTime += "0" + c.get( Calendar.SECOND );
		} else {
       		swipeTime += c.get( Calendar.SECOND );
		}


		String totalSwipe = "q0" + _heartFirmware + swipeDate + swipeTime;

		byte[] swpbytes = syncrc.formatBytes( totalSwipe.getBytes() );

		for( int i=0; i < swpbytes.length; i++){
    		Byte bobj = new Byte(swpbytes[i]);
    		//System.out.print("" + (char)bobj.intValue());
		}
		

		try {
    		os.write( swpbytes );
    		os.flush();
		} catch ( IOException ioe ) {
			System.out.println(_name+" BAD WRITE HEARTBEAT!");
    		//ioe.printStackTrace();
    		return 0;
		}

		return 1;

	}//heartbeat


	private int sendACK( OutputStream os ) {
		try {
			os.write(clockACK);
			os.flush();
		} catch ( IOException ioe ) {
			System.out.println(_name+" BAD WRITE ACK!");
			//ioe.printStackTrace();
			return 0;
		}
		System.out.println( _name + "> ACK");
		return 1;
	}

	private int sendNOMORE( OutputStream os ) {

		try {
			os.write(clockNOMORE);
			os.flush();
		} catch (IOException ioe) {
			System.out.println(_name+" BAD WRITE NOMORE!");
			//ioe.printStackTrace();
			return 0;
		}
		System.out.println(_name + "> NOMORE");
		return 1;
	}

	private int receiveACK( byte[] buff, DataInputStream is ) {

		try {
			System.out.println( this._name + "before read");
			System.out.flush();


			int buffSize=0;
			int i=0;
			for( i = 0; i < 5; i++ ) {
				if ( is.available() > 0 ) {
					buffSize = is.read(buff);
					break;
				}
				try { sleep(1000); }
				catch (InterruptedException e) { }
			}

			//bad news... close connection and try again
			if ( i > 14 ) {
				System.out.println( _name + " Harness closing connection no timely response.");
				return -9;	
			}


			//int buffSize = is.read(buff);
			System.out.println( this._name + "after read");
			System.out.flush();
			System.out.print( _name + "< (" + buffSize + "): ");
			for (int b = 0; b< buffSize; b++ ) {
    			Byte bobj = new Byte(buff[b]);
    			System.out.print("" + (char)bobj.intValue());
			}
			System.out.println("");
		} catch ( IOException ioe ) {
			System.out.println(_name+" BAD READ ACK!");
			return 0;
		}
		return 1;
	}

	private int receiveMORE( byte[] buff, DataInputStream is ) {

		try {
			int buffSize = is.read(buff);
			System.out.print( _name + "< (" + buffSize + "): ");
			for (int b = 0; b< buffSize; b++ ) {
    			Byte bobj = new Byte(buff[b]);
    			System.out.print("" + (char)bobj.intValue());
			}
			System.out.println("");
		} catch (IOException ioe){
			System.out.println(_name+" BAD READ MORE!");
			//ioe.printStackTrace();
			return 0;
		}
		return 1;
	}


	/* THIS METHOD WILL ATTEMPT TO SEND THE SWIPES TO THE WBCS */	
	private int sendSwipes( Vector v, int numSwipes, OutputStream o) {

		//the string is of the form: 0006,01,0308002

		Calendar	c			= Calendar.getInstance();
		SynelCRC	syncrc		= new SynelCRC();
		String		swipeHead	= "d";
		String		firmware	= "";
		String		badge		= "";
		String		dir			= "";	
		String		sDate		= "";
		String		sTime		= "";
		String		swipes		= new String("");

		for (int i=0; i<numSwipes; i++) {
			String tmp = (String) v.elementAt(i);
			System.out.println( this._name + " popping " + tmp);
			StringTokenizer st = new StringTokenizer(tmp, ",");
			sDate = st.nextToken();
			sTime = st.nextToken();
			badge = st.nextToken();
			dir = st.nextToken().substring(1,2); //only want 2nd digit
			firmware = st.nextToken();

			//add Overtime swipe
			if ( "6".compareTo( dir ) == 0 ) {
				swipes += "X80" + sTime + badge + "OT              ";
			//add normal swipe in or out
			} else {
				swipes += "X12" + sTime + badge + dir;
			}
		}//for

		String totalSwipe = swipeHead + firmware + sDate + swipes;

		byte[] swpbytes = syncrc.formatBytes( totalSwipe.getBytes() );		

		try {
			o.write( swpbytes );
			o.flush();
			System.out.println( this._name + "after write");
/*
			System.out.print( _name + "> ");
			for (int b = 0; b< swpbytes.length; b++ ) {
    			Byte bobj = new Byte(swpbytes[b]);
    			System.out.print("" + (char)bobj.intValue());
			}
			System.out.println("");
*/

		} catch ( IOException ioe ) {
			System.out.println(this._name+" BAD WRITE!");
			System.out.flush();
			return 0;
		}

		return 1;

	}//sendSwipes

	private void cleanup( OutputStream o, DataInputStream i, Socket s ) {
		if ( null != o ) {
    		try {o.close();}catch(IOException e1){}
		} if ( null != i ) {
    		try {i.close();}catch(IOException e2){}
		} if ( null != s ) {
    		try {s.close();}catch(IOException e3){}
		}
	}//cleanup

}
