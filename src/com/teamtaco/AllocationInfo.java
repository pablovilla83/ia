/**
 * 
 */
package com.teamtaco;

/**
 * @author Frederik
 *
 */
public class AllocationInfo {

	private boolean[] bookedHotels = new boolean[Client.DAY_COUNT];
	private boolean[] unbookedDays = new boolean[Client.DAY_COUNT];
	
	private boolean fuckedUp;
	private boolean intervalCertainty;
	
	private int actualArrival;
	private int actualDeparture;
	
	public boolean[] getBookedHotels() {
		return bookedHotels;
	}
	
	public void setBookedHotels(int start, int end) {
		if(start >=0 && end >=0) {
			for(int i = start; i<end;i++) {
				bookedHotels[i]=true;
			}
		}
	}
	
	public void setBookedHotels(boolean[] bookedHotels) {
		this.bookedHotels = bookedHotels;
	}
	public boolean[] getUnbookedDays() {
		return unbookedDays;
	}
	public void setUnbookedDays(boolean[] unbookedDays) {
		this.unbookedDays = unbookedDays;
	}
	public boolean isFuckedUp() {
		return fuckedUp;
	}
	public void setFuckedUp(boolean fuckedUp) {
		this.fuckedUp = fuckedUp;
	}
	public boolean isIntervalCertainty() {
		return intervalCertainty;
	}
	public void setIntervalCertainty(boolean hasIntervalCertainty) {
		this.intervalCertainty = hasIntervalCertainty;
	}
	public int getActualArrival() {
		return actualArrival;
	}
	public void setActualArrival(int actualArrival) {
		this.actualArrival = actualArrival;
	}
	public int getActualDeparture() {
		return actualDeparture;
	}
	public void setActualDeparture(int actualDeparture) {
		this.actualDeparture = actualDeparture;
	}
}
