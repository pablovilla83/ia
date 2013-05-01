/**
 * 
 */
package com.teamtaco;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Frederik
 *
 */
public class EventManager {
	
	List<EventWrapper> events = new ArrayList<>();
	
	public EventManager(List<EventWrapper> events) {
		this.events = events;
	}
	
	public EventWrapper getClientWithHighestBonus(EventItem item) {
		EventWrapper currentBest = null;
		for(EventWrapper wrapper : getPotentialBuyers(item)) {
			int bonusConstant = item.getType().getBonusConstant();
			if(currentBest == null || 
					wrapper.getClient().getBonus(bonusConstant)> currentBest.getClient().getBonus(bonusConstant)) {
				currentBest = wrapper;
			}
		}
		return currentBest;
	}
	
	public int getNumberOfPotentialBuyers(EventItem item) {
		return getPotentialBuyers(item).size();
	}
	
	private List<EventWrapper> getPotentialBuyers(EventItem item){
		List<EventWrapper> potentialBuyers = new ArrayList<EventWrapper>();
		for(EventWrapper wrapper : events) {
			// find needed items
			if(wrapper.getItem().getType() == item.getType()
					&& wrapper.getItem().getPossibleDays()[item.getBookedDay()]
					&& !wrapper.getItem().isSatisfied()) {
				potentialBuyers.add(wrapper);
			}
		}
		return potentialBuyers;
	}
	
	public int getUtility(EventWrapper wrapper, EventItem item) {
		if(wrapper == null || item == null) {
			return 0;
		}
		return wrapper.getClient().getBonus(item.getType().getTacType())-item.getActualPrice();
	}

}
