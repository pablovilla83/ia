/**
 *
 */
package com.teamtaco;

import se.sics.tac.aw.TACAgent;

import com.teamtaco.util.EventType;

/**
 * @author Frederik
 *
 */
public class EventItem extends Item {
	
	public static final int UNBOOKED = -1;

	private int bookedDay = UNBOOKED;

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
	public boolean isSatisfied() {
		return bookedDay != UNBOOKED;
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
			if(this.getBookedDay() != UNBOOKED) {
				return this.getBookedDay() == ((EventItem)item).getBookedDay();
			}
			return true;
		}
		return false;
	}
	@Override
	public int getTacCategory() {
		return TACAgent.CAT_ENTERTAINMENT;
	}
}
