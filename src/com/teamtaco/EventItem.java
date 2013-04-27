/**
 *
 */
package com.teamtaco;

import com.teamtaco.util.EventType;

/**
 * @author Frederik
 *
 */
public class EventItem extends Item {
	private int bookedDay;
	private EventType type = null;
	public int getBookedDay() {
		return bookedDay;
	}
	public void setBookedDay(int bookedDay) {
		this.bookedDay = bookedDay;
	}
	public EventType getType() {
		return type;
	}
	public void setType(EventType type) {
		this.type = type;
	}
}
