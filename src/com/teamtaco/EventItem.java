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

	private boolean[] possibleDays = new boolean[6];

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
	public boolean[] getPossibleDays() {
		return possibleDays;
	}
	public void setPossibleDays(boolean[] possibleDays) {
		this.possibleDays = possibleDays;
	}
	@Override
	public boolean isMandatory() {
		return false;
	}
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof EventItem)) {
			return false;
		}
		EventItem item = (EventItem) obj;
		if(this.getType() == item.getType()) {
			return true;
		}
		return false;
	}
}
