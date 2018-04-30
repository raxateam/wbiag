package com.wbiag.app.clockInterface.testHarness;

import java.util.*;

public class PrintStatus {
	Timer	timer;
	Vector	_rdrBuffs;

	public PrintStatus(int msec, Vector rdrBuffs) {
		_rdrBuffs = rdrBuffs;
		timer = new Timer();
		timer.schedule(new RemindTask(), 0, msec);
	}

	class RemindTask extends TimerTask {
		public void run() {

			Printer();	
			if ( allEmptyAndAllDone() ) {
				timer.cancel(); //Terminate the timer thread
			}
		}
    }

	public boolean allEmptyAndAllDone() {

		ReaderBuffer rb;
		for( int i=0; i<_rdrBuffs.size(); i++ ) {
			rb = (ReaderBuffer) _rdrBuffs.elementAt(i);
			if ( rb.isMoreDumps() || !rb.getBuff().isEmpty() ) return false;
		}
		return true;
	} 

	public void Printer() {

		String	tstr;
		int 	vsize;
		ReaderBuffer rb;

		System.out.println("printing "+_rdrBuffs.size()+" reader buffers...");

		for( int i=0; i<_rdrBuffs.size(); i++ ) {
			rb = (ReaderBuffer) _rdrBuffs.elementAt(i);
			System.out.println( "P: " + rb.getName() + " size - " + 
				rb.getBuff().size());			
/*
			for( int j=0; j<rb.getBuff().size(); j++ ) {
				tstr = (String) rb.getBuff().elementAt(j);				
				System.out.println("  p - " + tstr);
			}
*/

		}//for all reader buffers
		System.out.println("done printing reader buffers.");
	}//Filler

/*
	public static void main(String args[]) {
		System.out.println("About to schedule task.");
		new PrintStatus(5,"TGW");
		System.out.println("Task scheduled.");
	}
*/


}
