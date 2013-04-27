/**
 *
 */
package com.teamtaco;

import com.teamtaco.util.FlightType;

/**
 * @author Frederik
 *
 */
public class FlightItem extends Item {
	private FlightType type;
	int day;
	public FlightType getType() {
		return type;
	}
	public void setType(FlightType type) {
		this.type = type;
	}
	public int getDay() {
		return day;
	}
	public void setDay(int day) {
		this.day = day;
	}

}
