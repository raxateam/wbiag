
package com.wbiag.app.clockInterface.testHarness;
 
import java.util.*;
import com.wbiag.app.clockInterface.testHarness.FillBuffers;
import com.wbiag.app.clockInterface.testHarness.Settings;
import com.wbiag.app.clockInterface.testHarness.SynelReader;
import com.wbiag.app.clockInterface.testHarness.ReaderBuffer;
import com.wbiag.app.clockInterface.testHarness.RdrAttributes;

class wbcsTestHarness {

	public static void main( String args[] ) {

		System.out.println("Starting wbcsTestHarness\n\n");

		String confPath = System.getProperty("user.dir") + "\\conf\\";
		String swipesPath = System.getProperty("user.dir") + "\\scripts\\";

		Vector rdrBuffers = new Vector( /* number of readers */);

		//GET PROGRAM SETTINGS
		Settings settings = new Settings( confPath + "settings.conf");	
		if ( !settings.init() ) {
			System.exit(1);
		}
		
		int NUM_READERS = Integer.parseInt(settings.get("NUM_READERS"));
		int DELAY_PER_DUMP = Integer.parseInt(settings.get("DELAY_PER_DUMP"));
		int NUM_SWIPES_PER_DUMP = 
			Integer.parseInt(settings.get("NUM_SWIPES_PER_DUMP")); 	
		int NUM_DUMPS = Integer.parseInt(settings.get("NUM_DUMPS"));
		settings.print();


		//GET READER ATTRIBUTES
		RdrAttributes rdrAttrib = new RdrAttributes(confPath + "readers.conf");
		if ( !rdrAttrib.init() ) {
			System.exit(1);
		}
		rdrAttrib.setSwipeFirmware(settings.get("SWIPE_FIRMWARE_VERSION"));
		rdrAttrib.setHeartFirmware(settings.get("HEART_FIRMWARE_VERSION"));
		rdrAttrib.setSvrIP(settings.get("REMOTE_SERVER"));
		rdrAttrib.setSvrPort(settings.get("REMOTE_PORT"));

		if ( !rdrAttrib.loadSwipes(swipesPath) ) {
			System.exit(1);
		}
		rdrAttrib.print();


		//create clock buffers
		setupReaderBuffers( rdrBuffers, NUM_READERS, rdrAttrib );

		printReaderBuffers( rdrBuffers );

		new FillBuffers(DELAY_PER_DUMP, 
						NUM_DUMPS,
						NUM_SWIPES_PER_DUMP,
						rdrBuffers,
						rdrAttrib);


		new PrintStatus(5000, rdrBuffers);	

		//START THE READERS!
		Vector readers = new Vector();
		SynelReader sr;
		
		for( int r=0; r<NUM_READERS; r++ ) {
			sr = new SynelReader( rdrAttrib.getRdrNameByIndex(r), 
				rdrAttrib.getRdrIPByIndex(r),
				rdrAttrib.getSvrIP(),
				rdrAttrib.getSvrPort(),			
				rdrAttrib.getHeartFirmware(),
				(ReaderBuffer) rdrBuffers.elementAt(r)); 		
			readers.add( sr );

			sr.start();
		}

	}//main


	//Create and setup the buffers for the virtual readers
	private static void setupReaderBuffers(Vector rdrBuffers, 
			int nReaders, RdrAttributes rdrs) {

		//rdrBuffers = new Vector();
		//rdrBuffers.removeAllElements();
		String rdrName;
		for( int i = 0; i < nReaders; i++ ) {
			rdrName = rdrs.getRdrNameByIndex(i);
			if ( rdrName == null ) {
				System.err.print("Error: not enough readers defined in ");
				System.err.print("conf\\readers.conf.  Please define more. ");
				System.err.println("or specify less in conf\\settings.conf.");
				System.exit(1);
			}
			rdrBuffers.add( new ReaderBuffer( rdrs.getRdrNameByIndex(i) ) );
		}

	}//setup


	//print the contents of the reader buffers
	private static void printReaderBuffers(Vector rdrBuffers) {
		System.out.println( "---------------" );
		System.out.println( "READER BUFFERS:" );
		System.out.println( "---------------" );
		ReaderBuffer tmpRB;
		String stmp;
		for( int i = 0; i < rdrBuffers.size(); i++ ) {
			tmpRB = (ReaderBuffer) rdrBuffers.elementAt(i);
			System.out.println( "P: " + tmpRB.getName() );
			for( int j=0; j< tmpRB.getBuff().size(); j++ ) {
				stmp = (String) tmpRB.getBuff().elementAt(j);
				System.out.println("- swipe: " + stmp);
			}
		}
		System.out.println("\n");
	}//print

}


