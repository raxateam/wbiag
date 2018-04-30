package com.wbiag.app.clockInterface.testHarness;

import java.util.Vector;
import java.util.Date;
import java.util.Calendar;

class ReaderBuffer {

	private	Vector	_buff;
	private	boolean	_moreDumps;
	private	String	_name;

	public ReaderBuffer(String n) {

		_buff = new Vector();
		_moreDumps = true;
		_name = n;
	}

	public boolean isMoreDumps() {
		if ( _moreDumps ) return true;
		return false;
	}

	public void setMoreDumps( boolean b ) {
		_moreDumps = b;
	}

	public void addSwipes( Vector swipes ) {

		String tmpSwipe;
		for (int i=0; i < swipes.size(); i++) {
			tmpSwipe = (String) swipes.elementAt(i);

			java.util.Date curDate	= new Date( System.currentTimeMillis() );
			Calendar c = Calendar.getInstance();
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

			swipes.set( i, swipeDate + "," + swipeTime + "," + tmpSwipe );

		}
		_buff.addAll( swipes );
	}

	public String getName() {
		return _name;
	}

	public Vector getBuff() {
		return _buff;
	}

}
