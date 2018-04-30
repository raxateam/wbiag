package com.wbiag.app.clockInterface.testHarness;

import java.util.Vector;

class RdrAttribNode {

	public	String	name;
	public	String	ip_addr;
	private Vector	_swipes;
	private int		_numSwipes;
	private int		_swipeIndex;

	public RdrAttribNode() {
		_swipes = new Vector();
		_numSwipes = 0;
		_swipeIndex = 0;
	}

	public void addSwipe(String swipe) {
		_swipes.add(swipe); 
	}

	public Vector getSwipes() {
		return _swipes;
	}
	
	public void setNumSwipes(int i) {
		_numSwipes = i;
	}

	public int getNumSwipes() {
		return _numSwipes;
	}

	public int getSwipeIndex() {
		return _swipeIndex;
	}

	public void addToSwipeIndex(int i) {
		int tmp = getSwipeIndex() + i;
		_swipeIndex = tmp % _numSwipes;	
	}

}
