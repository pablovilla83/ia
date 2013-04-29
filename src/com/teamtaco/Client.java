/**
 *
 */
package com.teamtaco;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

	private static final int DAY_COUNT = 6;

	private int id;
	private int arrivalDay;
	private int initialArrivalDay;
	private int departureDay;
	private int initialDepartureDay;
	private int hotelBonus;

	private int e1Bonus = 0;
	private int e2Bonus = 0;
	private int e3Bonus = 0;

	private Map<String, Float> budget = new HashMap<String, Float>();

	public void addToBudget(String categoryTypeDay, float price){
		if (this.budget.containsKey(categoryTypeDay)) {
			price += this.budget.get(categoryTypeDay);
		}

		this.budget.put(categoryTypeDay, price);
	}

	/**
	 * get the current bubget available
	 * ctd = category-type-day
	 * @return float
	 */

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
	 * updates the list of the items regarding possible changes in arrival-and departure dates and so on
	 * @throws Exception 
	 */
	private void updateItemList(){
		boolean[] possibleAllocations = new boolean[DAY_COUNT];
		boolean[] allocatedHotelDays = new boolean[DAY_COUNT];
		boolean[] occupiedDays = new boolean[DAY_COUNT];
		HotelType allocatedHotelType = null;
		// look for constraints
		for(Item item : items){
			if(item instanceof EventItem && item.isSatisfied()){
				occupiedDays[((EventItem)item).getBookedDay()] = true;
			}
			else if (item instanceof HotelItem && item.isSatisfied()) {
				allocatedHotelDays[((HotelItem)item).getDay()] = true;
				if(allocatedHotelType != null && allocatedHotelType != ((HotelItem)item).getType()) {
					// Throw exception or whatever
					System.err.println("Different hotel types");
				}
				allocatedHotelType = ((HotelItem)item).getType();
			} 
		}
		
		// check which hotels are still available
		if(allocatedHotelType != null) {
			possibleAllocations = getLongestPossibleStay(allocatedHotelDays, allocatedHotelType);
		} else {
			boolean[] cheapStay = getLongestPossibleStay(allocatedHotelDays, HotelType.CHEAP);
			boolean[] goodStay = getLongestPossibleStay(allocatedHotelDays, HotelType.GOOD);
			boolean[] together = new boolean[DAY_COUNT];
			int cheapDayCount = 0;
			int goodDayCount = 0;
			for(int i = 0;i< DAY_COUNT;i++) {
				if(cheapStay[i]) {
					cheapDayCount++;
				}
				if(goodStay[i]) {
					goodDayCount++;
				}
				together[i] = cheapStay[i] || goodStay[i];
			}
			if(cheapDayCount == goodDayCount) {
				possibleAllocations = together;
			} else if (cheapDayCount<goodDayCount) {
				possibleAllocations = goodStay;
				allocatedHotelType = HotelType.GOOD;
			} else {
				possibleAllocations = cheapStay;
				allocatedHotelType = HotelType.CHEAP;
			}
		}
		
		// find out first possible day (either already booked or still possible)
		int firstDay = -1;
		int lastDay = -1;
		for(int i = 0;i<allocatedHotelDays.length;i++) {
			if(allocatedHotelDays[i] || possibleAllocations[i]) {
				if(firstDay == -1) {
					firstDay = i;
				}
				lastDay = i;
			}
		}
		if(firstDay == -1 || lastDay == -1) {
			arrivalDay = 0;
			departureDay = 0;
		} else {
			arrivalDay = firstDay;
			departureDay = lastDay+1;
		}
		
		// look up which days are still free for activities
		boolean[] unoccupiedDays = new boolean[DAY_COUNT];
		for(int i = 0; i< occupiedDays.length; i++) {
			unoccupiedDays[i] = !occupiedDays[i];
		}
		List<Item> itemsToRemove = new ArrayList<Item>();
		for(Item item : items) {
			if(!item.isSatisfied() && item instanceof EventItem) {
				((EventItem)item).setPossibleDays(unoccupiedDays);
			}
			// if an hotel has already be allocated update types of other hotels!
			if(item instanceof HotelItem && !item.isSatisfied()) {
				if(!possibleAllocations[((HotelItem)item).getDay()]) {
					itemsToRemove.add(item);
				}
				if(allocatedHotelType != null) {
					((HotelItem)item).setType(allocatedHotelType);
				}
			}
			// update flight-days
			if(!item.isSatisfied() && item instanceof FlightItem) {
				switch(((FlightItem)item).getType()) {
					case IN: ((FlightItem)item).setDay(arrivalDay);break;
					case OUT: ((FlightItem)item).setDay(departureDay);break;
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
	
	/**
	 * returns the longest possible stay for hoteltype
	 * 
	 * If there are already some hotels booked, the interval containing these bookings will be taken
	 * 
	 * @param bookedDays
	 * @param hotelType
	 * @return
	 */
	private boolean[] getLongestPossibleStay(boolean[] bookedDays,HotelType hotelType ) {
		boolean[] overallStay = new boolean[DAY_COUNT];
		boolean[] unbookedStay = new boolean[DAY_COUNT];
		for(int i = arrivalDay; i< departureDay;i++) {
			if(!bookedDays[i]) {
				boolean possible = !closedHotelAuctions.contains(new HotelItem(i,hotelType));
				overallStay[i] = possible;
				unbookedStay[i] = possible;
			} else {
				overallStay[i] = true;
			}
		}
		
		int longestStart = -1,longestEnd = -1;
		int currentStart=-1, currentEnd=-1;
		boolean isBooked =false;
		for(int i=arrivalDay;i<departureDay;i++) {
			if(bookedDays[i]) {
				isBooked=true;
			}
			if(overallStay[i]) {
				if(currentStart == -1) {
					currentStart = i;
				}
				currentEnd = i;
			} else {
				if(currentEnd-currentStart >= longestEnd-longestStart) {
					longestEnd = currentEnd;
					longestStart = currentStart;
				}
				if(isBooked) {
					boolean[] finalArray = new boolean[DAY_COUNT];
					for(int j = currentStart;j<=currentEnd;j++) {
						finalArray[j] = true;
					}
					return finalArray;
				}
			}
		}
		if(longestStart == -1 || longestEnd == -1) {
			longestStart = currentStart;
			longestEnd = currentEnd;
		}
		boolean[] finalArray = new boolean[DAY_COUNT];
		if(longestStart == -1 || longestEnd == -1) {
			return finalArray;
		}
		for(int i = longestStart;i<=longestEnd;i++) {
			finalArray[i] = true;
		}
		return finalArray;
		
	}
	
	/**
	 * Gives a list of which items still need to be bought
	 * 
	 * @return
	 */
	public List<Item> whatToBuyNext(){
		updateItemList();
		List<Item> unsatisfiedItems = new ArrayList<Item>();
		boolean inFlightReady = false;
		for(Item item : items) {
			if(item instanceof HotelItem && !item.isSatisfied()) {
				unsatisfiedItems.add(item);
			} else if (item instanceof HotelItem && item.isSatisfied() && ((HotelItem)item).getDay() == arrivalDay) {
				inFlightReady = true;
			}
		}
		
		// make sure hotels are bought first - if there's still a hotel to be bough, buy this first!
		boolean add = unsatisfiedItems.isEmpty();
		
		// TODO allow to buy events for the days that are already booked?
		for(Item item : items) {
			if(!item.isSatisfied() && add) {
				unsatisfiedItems.add(item);
			} 
			// add inflight as soon as the hotel for the first day is booked
			else if (item instanceof FlightItem 
					&& ! item.isSatisfied() 
					&& inFlightReady 
					&& ((FlightItem)item).getType() == FlightType.IN
					&& !add) {
				unsatisfiedItems.add(item);
			}
		}
		//System.out.println(createItemListString(unsatisfiedItems));
		return unsatisfiedItems;
		
	}

	/**
	 * marks an item was satisfied (auction won)
	 *
	 * @param item the item
	 */
	public void bookItem(Item item, int actualPrice){
		item.setActualPrice(actualPrice);
		for(Item tmpItem : items) {
			if(item.equals(tmpItem)) {
				tmpItem.setActualPrice(actualPrice);
				if(item instanceof EventItem && tmpItem instanceof EventItem) {
					((EventItem)tmpItem).setBookedDay(((EventItem)item).getBookedDay());
				}
			}
		}
		updateItemList();
	}
	
	/**
	 * needs to be called whenever a Hotel-auction was closed
	 * 
	 * @param item
	 */
	public void auctionClosed(HotelItem item) {
		closedHotelAuctions.add(item);
		System.out.println("auction closed for " + item.getDay() + " " + item.getType());
		updateItemList();
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
