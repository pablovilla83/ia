/**
 *
 */
package com.teamtaco;

import se.sics.tac.aw.TACAgent;

import com.teamtaco.util.HotelType;

/**
 * @author Frederik
 *
 */
public class HotelItem extends Item {
	private HotelType type = null;
	
	public HotelItem() {
		
	}
	
	public HotelItem(int day, HotelType type) {
		this.day = day;
		this.type = type;
	}
	
	public HotelType getType() {
		return type;
	}
	public void setType(HotelType type) {
		this.type = type;
	}
	public int getDay() {
		return day;
	}
	public void setDay(int day) {
		this.day = day;
	}
	private int day;
	@Override
	public boolean isMandatory() {
		return true;
	}
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof HotelItem)) {
			return false;
		}
		if(this.getType() == ((HotelItem)obj).getType() && this.getDay() == ((HotelItem)obj).getDay()) {
			return true;
		}
		return false;
	}
	
	public int hashCode() {
		int code = 0;
		code+= (day<<4);
		if(type != null) {
			code+=type.ordinal();
		} else {
			code*=-1;
		}
		return code;
	}

	@Override
	public int getTacCategory() {
		return TACAgent.CAT_HOTEL;
	}
}
