/**
 *
 */
package com.teamtaco;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.sics.tac.aw.TACAgent;

import com.teamtaco.util.EventType;
import com.teamtaco.util.FlightType;
import com.teamtaco.util.HotelType;


/**
 * @author Frederik
 *
 */
public class Client implements Comparable<Client>{

	public static final int DAY_COUNT = 6;

	private int id;
	private int arrivalDay;
	private int initialArrivalDay;
	private int departureDay;
	private int initialDepartureDay;
	private int hotelBonus;

	private int e1Bonus = 0;
	private int e2Bonus = 0;
	private int e3Bonus = 0;
	
	private HotelType hotelType;


	private List<Item> items = new ArrayList<Item>();
	private Set<HotelItem> closedHotelAuctions = new HashSet<HotelItem>();

	public Client (int id){
		this.setId(id);
	}

	public Client(int id, int arrivalDay, int departureDay, int hotelBonus, int e1Bonus, int e2Bonus, int e3Bonus){
		this.setId(id);
		this.arrivalDay = arrivalDay;
		initialArrivalDay = arrivalDay;
		this.hotelBonus = hotelBonus;
		this.departureDay = departureDay;
		this.initialDepartureDay = departureDay;
		this.e1Bonus = e1Bonus;
		this.e2Bonus = e2Bonus;
		this.e3Bonus = e3Bonus;
		
		initializeItemList();
	}
	
	public void initializeItemList() {
		items.clear();
		boolean[] possibleDays = new boolean[DAY_COUNT];
		for(int i = arrivalDay;i<departureDay;i++) {
			HotelItem hotel = new HotelItem();
			hotel.setDay(i);
			if(hotelType != null) {
				hotel.setType(hotelType);
			}
			items.add(hotel);
			possibleDays[i] = true;
		}
		for(EventType type : EventType.values()) {
			if(getBonus(type.getBonusConstant())>0) {
				EventItem item = new EventItem();
				item.setPossibleDays(possibleDays);
				item.setType(type);
				items.add(item);
			}
		}
		FlightItem flight1 = new FlightItem();
		flight1.setType(FlightType.IN);
		flight1.setDay(arrivalDay);
		items.add(flight1);
		
		FlightItem flight2 = new FlightItem();
		flight2.setType(FlightType.OUT);
		flight2.setDay(departureDay);
		items.add(flight2);
	}
	
	public int unallocatedHotelDays() {
		int count = 0;
		for(Item item : items) {
			if(item instanceof HotelItem && !item.isSatisfied()) {
				count++;
			}
		}
		return count;
	}
	
	public boolean isInBetweenAllocatedDays(HotelItem hotelItem) {
		int firstAllocated = 200, lastAllocated = -1;
		for(Item item : items) {
			if(item instanceof HotelItem && item.isSatisfied()) {
				int currDay = ((HotelItem)item).getDay();
				if(currDay < firstAllocated) {
					firstAllocated = currDay;
				}
				if(currDay > lastAllocated) {
					lastAllocated = currDay;
				}
			}
		}
		return (hotelItem.getDay() >firstAllocated && hotelItem.getDay() < lastAllocated);
	}
	
	/**
	 * collects info about current allocations
	 * 
	 * @return {@link AllocationInfo}
	 */
	private AllocationInfo getAllocationInfo() {
		AllocationInfo info = new AllocationInfo();
		
		boolean[] allocatedHotelDays = new boolean[DAY_COUNT];
		boolean[] occupiedDays = new boolean[DAY_COUNT];
		// look for constraints
		for(Item item : items){
			if(item instanceof EventItem && item.isSatisfied()){
				occupiedDays[((EventItem)item).getBookedDay()] = true;
			}
			else if (item instanceof HotelItem && item.isSatisfied()) {
				allocatedHotelDays[((HotelItem)item).getDay()] = true;
				if(hotelType != null && hotelType != ((HotelItem)item).getType()) {
					// Throw exception or whatever
					System.err.println("Different hotel types");
				}
			} 
		}
		
		boolean[] overallStay = new boolean[DAY_COUNT];
		// calculate overall stay (booked + possible) and unbooked but possible stay
		for(int i = arrivalDay; i< departureDay;i++) {
			if(!allocatedHotelDays[i]) {
				boolean possible = !closedHotelAuctions.contains(new HotelItem(i,hotelType));
				overallStay[i] = possible;
			} else {
				overallStay[i] = true;
			}
		}
		int[] longestPossibleStay = longestIntervalInArray(overallStay);
		int[] longestBookedStay = longestIntervalInArray(allocatedHotelDays);
		
		
		// no hotels available anymore
		if(longestPossibleStay[0] == -1) {
			// client fucked up - no satisfaction possible anymore
			// deactivate by removing all items
			items.clear();
			info.setFuckedUp(true);
			return info;
		} else if(longestPossibleStay[0] == -2) {
			// there are several intervals with the same length
			// no certainty about final interval yet
			info.setActualArrival(arrivalDay);
			info.setActualDeparture(departureDay);
		} else {
			// check that there is no other interval where longer stays would still be possible
			int start = 0;
			int[] interval;
			boolean certain = true;
			while((interval = nextIntervalInArray(overallStay, start)) != null) {
				if(interval[0] != longestPossibleStay[0]) {
					if(interval[1]-interval[0] >= longestBookedStay[1]-longestBookedStay[0]) {
						certain = false;
					}
				}
				start = interval[1];
			}
			
			// make sure the current interval cannot be splitted up into bigger parts
			if(certain && longestBookedStay[1]-longestBookedStay[0]>= (float)(longestPossibleStay[1]-longestPossibleStay[0])/2f) {
				// it is clear now which interval will be used
				// days within longestBookedStay can now be allocated
				info.setActualArrival(longestPossibleStay[0]);
				info.setActualDeparture(longestPossibleStay[1]);
				
				arrivalDay = info.getActualArrival();
				departureDay = info.getActualDeparture();
				
				info.setIntervalCertainty(true);
				info.setBookedHotels(longestBookedStay[0], longestBookedStay[1]);
			}
			
		}
		
		// look up which days are available for activities (booked with certainty and not used by another event)
		boolean[] unoccupiedDays = new boolean[DAY_COUNT];
		if(info.isIntervalCertainty() && longestBookedStay[0] >=0 && longestBookedStay[1]>= 0) {
			for(int i = longestBookedStay[0];i<longestBookedStay[1];i++) {
				unoccupiedDays[i] = !occupiedDays[i];
			}
		}
		info.setUnbookedDays(unoccupiedDays);
		
		return info;
	}

	/**
	 * updates the list of the items regarding possible changes in arrival-and departure dates and so on
	 * @throws Exception 
	 */
	private void updateItemList(AllocationInfo info){
		
		List<Item> itemsToRemove = new ArrayList<Item>();
		for(Item item : items) {
			if(!item.isSatisfied()) {
				if( item instanceof EventItem) {
					((EventItem)item).setPossibleDays(info.getUnbookedDays());
				}
				// if an hotel has already be allocated update types of other hotels!
				if(item instanceof HotelItem) {
					int day =((HotelItem)item).getDay(); 
					if(!(day >= arrivalDay && day < departureDay)) {
						itemsToRemove.add(item);
					}
					if(hotelType != null) {
						((HotelItem)item).setType(hotelType);
					}
				}
				// update flight-days
				if(item instanceof FlightItem) {
					switch (((FlightItem) item).getType()) {
					case IN:
						if (((FlightItem) item).getDay() != arrivalDay) {
							System.out.println("set inflight from"
									+ ((FlightItem) item).getDay() + " to "
									+ arrivalDay);
							((FlightItem) item).setDay(arrivalDay);
						}
						break;
					case OUT:
						if (((FlightItem) item).getDay() != departureDay) {
							System.out.println("set outflight from"
									+ ((FlightItem) item).getDay() + " to "
									+ departureDay);
							((FlightItem) item).setDay(departureDay);
						}
						break;
					}
				}
			}
		}
		if(!itemsToRemove.isEmpty()) {
			System.out.println("removing: ");
			System.out.println(createItemListString(itemsToRemove));
			
			System.out.println("after removal: ");
			System.out.println(createItemListString(items));
		}
		
		items.removeAll(itemsToRemove);
	}
	
	public void setHotelType(HotelType type) {
		for(Item item : items) {
			if(item instanceof HotelItem && !item.isSatisfied()) {
				((HotelItem)item).setType(type);
			} else if(item instanceof HotelItem && item.isSatisfied() && ((HotelItem)item).getType() != type) {
				throw new RuntimeException("distinct types");
			}
		}
		this.hotelType = type;
		updateItemList(getAllocationInfo());
	}
	
	public HotelType getHotelType() {
		return this.hotelType;
	}
	
	
	/**
	 * searches for the longest true-interval in the given array
	 * 
	 * if there are two or more longest intervals of the same length, an interval of -2 -2 will be returned.
	 * if there are no intervals, -1 -1 will be returned;
	 * 
	 * @param array the array to search in
	 * @return the interval
	 */
	private int[] longestIntervalInArray(boolean[] array) {
		int[] interval = {-1,-1};
		int currentStart = -1, currentEnd = -1;
		boolean undecided = false;
		for(int i = 0;i<array.length;i++) {
			if(array[i]) {
				currentStart = currentStart == -1? i:currentStart;
				currentEnd = i;
			}
			else {
				currentEnd = i;
				if(interval[1]-interval[0] == currentEnd - currentStart) {
					undecided = true;
				} else if(interval[1] - interval[0] < currentEnd - currentStart && currentStart != -1) {
					interval[0] = currentStart;
					interval[1] = currentEnd;
					undecided = false;
				}
				currentStart = -1;
				currentEnd = -1;
			}
		}
		if(undecided) {
			interval[1]= -2;
			interval[0]= -2;
		}
		return interval;
	}
	
	private int[] nextIntervalInArray(boolean[] array, int start) {
		if(start>= array.length) {
			return null;
		}
		int[] interval = {-1,-1};
		for(int i = start;i<array.length;i++) {
			if(interval[0] == -1 && array[i]) {
				interval[0] = i;
			} else if(interval[0] != -1 && !array[i]) {
				interval[1]=i;
				return interval;
			}
		}
		if(interval[0]!= -1) {
			interval[1] = array.length;
			return interval;
		}
		return null;
	}
	
	/**
	 * Gives a list of which items still need to be bought
	 * 
	 * @return
	 */
	public List<Item> whatToBuyNext(){
		//updateItemList();
		List<Item> unsatisfiedItems = new ArrayList<Item>();
		boolean[] bookedHotels = new boolean[DAY_COUNT];
		for(Item item : items) {
			if(item instanceof HotelItem && !item.isSatisfied()) {
				unsatisfiedItems.add(item);
			} 
			if (item instanceof HotelItem && item.isSatisfied()) {
				bookedHotels[((HotelItem)item).getDay()] = true;
			}
		}
		
		AllocationInfo info = getAllocationInfo();
		
		for(Item item : items) {
			if(info.isIntervalCertainty()) {
				if(!item.isSatisfied()) {
					if(item instanceof FlightItem) {
						// add flights if possible
						FlightItem fItem = (FlightItem)item;
						switch(fItem.getType()) {
						case IN: 
							if(info.getBookedHotels()[info.getActualArrival()]) {
								unsatisfiedItems.add(fItem);
							}
							break;
						case OUT:
							if (info.getBookedHotels()[info.getActualDeparture()-1]) {
								unsatisfiedItems.add(fItem);
							}
						}
					} else {
						// add all events
						unsatisfiedItems.add(item);
					}
				}
			}
		}
		return unsatisfiedItems;
		
	}
	
	/**
	 * marks an item was satisfied (auction won)
	 *
	 * @param item the item
	 */
	public boolean bookItem(Item item, int actualPrice){
		item.setActualPrice(actualPrice);
		boolean hit = false;
		for(Item tmpItem : items) {
			if(item.equals(tmpItem)) {
				hit = true;
				tmpItem.setActualPrice(actualPrice);
				if(item instanceof EventItem && tmpItem instanceof EventItem) {
					((EventItem)tmpItem).setBookedDay(((EventItem)item).getBookedDay());
				}
			}
		}
		if(hit) {
			updateItemList(getAllocationInfo());
		}
		return hit;
	}
	
	/**
	 * needs to be called whenever a Hotel-auction was closed
	 * 
	 * @param item
	 */
	public void auctionClosed(HotelItem item) {
		closedHotelAuctions.add(item);
		updateItemList(getAllocationInfo());
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Item> T findItem(T item) {
		for(Item current : items) {
			if(current.equals(item)) {
				return (T)current;
			}
		}
		return null;
	}

	/**
	 * Gives the overall expenses of the items already bought so far
	 * 
	 * @return overall expenses
	 */
	public int getCurrentExpenses() {
		int expenses = 0;
		for(Item item : items) {
			if(item.isSatisfied()) {
				expenses+=item.getActualPrice();
			}
		}
		return expenses;
	}
	
	public int getExpensesWithoutFlights() {
		int expenses = 0;
		for(Item item : items) {
			if(item.isSatisfied() && !(item instanceof FlightItem)) {
				if(item instanceof EventItem) {
					expenses -= getBonus(((EventItem)item).getType().getBonusConstant());
				}
				expenses += item.getActualPrice();
			}
		}
		return expenses;
	}
	
	public int getFlightCost(FlightType type) {
		for(Item item : items) {
			if(item.isSatisfied() && item instanceof FlightItem && ((FlightItem)item).getType() == type) {
				return item.getActualPrice();
			}
		}
		return 0;
	}

	/**
	 * Indicates if the package bought so far is feasible or not
	 * 
	 * @return true if feasible, false otherwise
	 */
	public boolean isFeasible(){
		int inFlight = -1;
		int outFlight = -1;
		boolean[] hotelBookings = new boolean[DAY_COUNT];
		for(Item item : items) {
			if(item.isSatisfied()) {
				if(item instanceof FlightItem) {
					FlightItem flightItem = (FlightItem)item;
					switch (flightItem.getType()) {
					case IN:inFlight = flightItem.getDay();break;
					case OUT: outFlight = flightItem.getDay();break;
					}
				} else if (item instanceof HotelItem) {
					hotelBookings[((HotelItem)item).getDay()] = true;
				}
			}
		}
		for(int i = inFlight; i< outFlight;i++) {
			if(!hotelBookings[i]) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * looks up the bonus for field field
	 *
	 * @param field the field
	 * @return the utility for field field
	 */
	public int getBonus(int field){
		switch(field){
		case TACAgent.E1: return e1Bonus;
		case TACAgent.E2: return e2Bonus;
		case TACAgent.E3: return e3Bonus;
		case TACAgent.HOTEL_VALUE: return hotelBonus;
		default: return 0;
		}
	}
	
	public int getInitialArrivalDay() {
		return initialArrivalDay;
	}
	
	public int getInitialDepartureDay() {
		return initialDepartureDay;
	}
	
	public int getArrivalDay() {
		return arrivalDay;
	}

	public void setArrivalDay(int arrivalDay) {
		this.arrivalDay = arrivalDay;
		this.initialArrivalDay = arrivalDay;
	}

	public int getDepartureDay() {
		return departureDay;
	}

	public void setDepartureDay(int departureDay) {
		this.departureDay = departureDay;
		this.initialDepartureDay = departureDay;
	}

	public int getHotelBonus() {
		return hotelBonus;
	}

	public void setHotelBonus(int hotelBonus) {
		this.hotelBonus = hotelBonus;
	}

	public int getE1Bonus() {
		return e1Bonus;
	}

	public void setE1Bonus(int e1Bonus) {
		this.e1Bonus = e1Bonus;
	}

	public int getE2Bonus() {
		return e2Bonus;
	}

	public void setE2Bonus(int e2Bonus) {
		this.e2Bonus = e2Bonus;
	}

	public int getE3Bonus() {
		return e3Bonus;
	}

	public void setE3Bonus(int e3Bonus) {
		this.e3Bonus = e3Bonus;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}


	/**
	 * add the method compareTo in order to have a set order by
	 * for the moment by hotel desc
	 */
	@Override
	public int compareTo(Client c){
		return c.getHotelBonus() - hotelBonus;
	}

	@Override
	public String toString(){
		String print = "id: " + this.getId() + "; e1: " + this.getE1Bonus() + "; e2: " + this.getE2Bonus() + "; e3: " + this.getE3Bonus() + "; arrival: "+ this.getArrivalDay() + "; departure: " + this.getDepartureDay() + "; hotel: " + this.getHotelBonus();

		return print;
	}
	
	// TODO remove
	public String createItemListString(List<Item> items) {
		StringBuilder sb = new StringBuilder(this.toString());
		for(Item item : items) {
			sb.append("\n\t");
			sb.append(item.getClass().getSimpleName());
			sb.append("[MaxPrice: ");
			sb.append(item.getMaxPrice());
			sb.append(", actualPrice: ");
			sb.append(item.getActualPrice());
			sb.append(", satisfied: ");
			sb.append(item.isSatisfied());
			if(item instanceof HotelItem) {
				sb.append(", day: ");
				sb.append(((HotelItem)item).getDay());
				sb.append(", type: ");
				sb.append(((HotelItem)item).getType());
			} else if(item instanceof FlightItem) {
				sb.append(", day: ");
				sb.append(((FlightItem)item).getDay());
				sb.append(", type: ");
				sb.append(((FlightItem)item).getType().name());
			} else if(item instanceof EventItem) {
				sb.append(", type: ");
				sb.append(((EventItem)item).getType().name());
			}
		}
		return sb.toString();
	}
}
