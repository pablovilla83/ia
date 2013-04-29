/**
 *
 */
package com.teamtaco.util;

import se.sics.tac.aw.TACAgent;

/**
 * @author Frederik
 *
 */
public enum HotelType {
	CHEAP(-1, TACAgent.TYPE_CHEAP_HOTEL),
	GOOD(TACAgent.HOTEL_VALUE, TACAgent.TYPE_GOOD_HOTEL);
	
	private int constant;
	private int tacType;
	
	HotelType(int constant, int tacType){
		this.constant = constant;
		this.tacType = tacType;
	}
	
	public int getBonusConstant() {
		return constant;
	}
	
	public int getTacType() {
		return tacType;
	}
	
	public static HotelType getTypeForConstant(int constant) {
		switch(constant) {
		case TACAgent.TYPE_CHEAP_HOTEL: return CHEAP;
		case TACAgent.TYPE_GOOD_HOTEL: return GOOD;
		default: return null;
		}
	}
}
