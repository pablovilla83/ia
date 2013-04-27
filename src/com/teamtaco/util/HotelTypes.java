/**
 *
 */
package com.teamtaco.util;

import se.sics.tac.aw.TACAgent;

/**
 * @author Frederik
 *
 */
public enum HotelTypes {
	CHEAP(-1),
	GOOD(TACAgent.HOTEL_VALUE);
	
	private int constant;
	
	HotelTypes(int constant){
		this.constant = constant;
	}
	
	public int getBonusConstant() {
		return constant;
	}
}
