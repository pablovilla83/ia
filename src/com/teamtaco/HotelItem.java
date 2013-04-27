/**
 *
 */
package com.teamtaco;

import com.teamtaco.util.HotelTypes;

/**
 * @author Frederik
 *
 */
public class HotelItem extends Item {
	private HotelTypes type = null;
	
	public HotelItem() {
		
	}
	
	public HotelItem(int day, HotelTypes type) {
		this.day = day;
		this.type = type;
	}
	
	public HotelTypes getType() {
		return type;
	}
	public void setType(HotelTypes type) {
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
}
