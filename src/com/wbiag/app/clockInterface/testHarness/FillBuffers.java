package com.wbiag.app.clockInterface.testHarness;

import java.util.*;

public class FillBuffers {
	Timer	timer;
	Vector	_rdrBuffs;
	int		_iterationsLeft;
	int		_nSwipes;
	int		_uniqueSwipe;
	RdrAttributes	_rdrAttrib;
	

	public FillBuffers(int msec, int iterations, int nSwipes, Vector rdrBuffs,
			RdrAttributes rdrAttrib) {
		_rdrBuffs = rdrBuffs;
		_rdrAttrib = rdrAttrib;
		_iterationsLeft = iterations;
		_nSwipes = nSwipes;
		_uniqueSwipe = 0;

		if ( _iterationsLeft <= 0 ) {
			return;
		} else {
			timer = new Timer();
			timer.schedule(new RemindTask(), 0, msec);
		}
	}

	class RemindTask extends TimerTask {
		public void run() {

			filler();	
			_iterationsLeft--;
			if ( _iterationsLeft == 0 ) {
				scheduleStopReaders();
				timer.cancel(); //Terminate the timer thread
			}
		}
    }

	//Once we have filled the buffers n times, let the readers know
	//that no more is coming, so when their buffers are empty,
	//they should kill themselves.
	private void scheduleStopReaders() {
		ReaderBuffer rb;
		for( int i = 0; i<_rdrBuffs.size(); i++ ) {
			rb = (ReaderBuffer) _rdrBuffs.elementAt(i);
			rb.setMoreDumps( false );	
		}
	}//scheduleStopReaders


	//fill the virtual reader buffers
	private void filler() {
		System.out.println("filling reader buffers...");

		Vector	tvec;
		Vector	rvec;
		int		numSwipes;
		int		swipeIndex;

		ReaderBuffer	rb;

		for( int i=0; i<_rdrBuffs.size(); i++ ) {

			//get the buffer for each virtual reader
			rb = (ReaderBuffer) _rdrBuffs.elementAt(i);

			//get the list of swipes for this particular reader
			tvec = _rdrAttrib.getRdrSwipesByIndex(i);
			numSwipes = _rdrAttrib.getSwipeSizeByIndex(i);	

			rvec = new Vector();

			while ( rvec.size() < _nSwipes ) {
				swipeIndex = _rdrAttrib.getRdrSwipeIndexByIndex(i);

				if ( (numSwipes - swipeIndex + rvec.size()) <= _nSwipes ) {
					rvec.addAll( tvec.subList(swipeIndex, numSwipes) );
					_rdrAttrib.addToRdrSwipeIndexByIndex(i, numSwipes - swipeIndex);
				} else {
					int count = _nSwipes - rvec.size();
					rvec.addAll( tvec.subList(swipeIndex, swipeIndex + count) );
					_rdrAttrib.addToRdrSwipeIndexByIndex(i, count);
				}

			}//while
		
			System.out.println(_rdrAttrib.getRdrNameByIndex(i) + " adding " + 
				rvec.size() );	
/*
			for(int k=0; k< rvec.size(); k++ ) {
				System.out.println("  f - " + (String) rvec.elementAt(k));
			}
*/
			rb.addSwipes( rvec );

		}//for all reader buffers

		System.out.println("done filling reader buffers.");
	}//Filler

/*
	public static void main(String args[]) {
		System.out.println("About to schedule task.");
		new FillBuffers(5,"TGW");
		System.out.println("Task scheduled.");
	}
*/


}
