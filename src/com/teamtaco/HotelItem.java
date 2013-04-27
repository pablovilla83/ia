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

}
