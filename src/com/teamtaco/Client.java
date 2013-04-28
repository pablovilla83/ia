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
import com.teamtaco.util.HotelTypes;


/**
 * @author Frederik
 *
 */
public class Client implements Comparable<Client>{

	public static final int RANDOM_HOTEL_TYPE = 1337;
	private static final int DAY_COUNT = 6;

	private int id;
	private int arrivalDay;
	private int departureDay;
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
		this.hotelBonus = hotelBonus;
		this.departureDay = departureDay;
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

	/**
	 * updates the list of the items regarding possible changes in arrival-and departure dates and so on
	 */
	private void updateItemList(){
//		System.out.println("before: ");
//		System.out.println(createItemListString(items));
		boolean[] possibleAllocations = new boolean[DAY_COUNT];
		boolean[] allocatedHotelDays = new boolean[DAY_COUNT];
		boolean[] occupiedDays = new boolean[DAY_COUNT];
		HotelTypes allocatedHotelType = null;
		// look for constraints
		for(Item item : items){
			if(item instanceof EventItem && item.isSatisfied()){
				occupiedDays[((EventItem)item).getBookedDay()] = true;
			}
			if (item instanceof HotelItem && item.isSatisfied()) {
				allocatedHotelDays[((HotelItem)item).getDay()] = true;
				if(allocatedHotelType != null && allocatedHotelType != ((HotelItem)item).getType()) {
					// Throw exception or whatever
				}
				allocatedHotelType = ((HotelItem)item).getType();
			} 
		}
		
		// check which hotels are still available
		if(allocatedHotelType != null) {
			possibleAllocations = getLongestPossibleStay(allocatedHotelDays, allocatedHotelType);
		} else {
			boolean[] cheapStay = getLongestPossibleStay(allocatedHotelDays, HotelTypes.CHEAP);
			boolean[] goodStay = getLongestPossibleStay(allocatedHotelDays, HotelTypes.GOOD);
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
				allocatedHotelType = HotelTypes.GOOD;
			} else {
				possibleAllocations = cheapStay;
				allocatedHotelType = HotelTypes.CHEAP;
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
					case IN: ((FlightItem)item).setDay(firstDay);break;
					case OUT: ((FlightItem)item).setDay(lastDay);break;
				}
			}
		}
		items.removeAll(itemsToRemove);
//		System.out.println("removing: ");
//		System.out.println(createItemListString(itemsToRemove));
//		System.out.println("after removal: ");
//		System.out.println(createItemListString(items));
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
	private boolean[] getLongestPossibleStay(boolean[] bookedDays,HotelTypes hotelType ) {
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
		for(Item item : items) {
			if(item instanceof HotelItem && !item.isSatisfied()) {
				unsatisfiedItems.add(item);
			}
			// I know this is wrong, just trying something
			else if(item instanceof FlightItem && !item.isSatisfied()){unsatisfiedItems.add(item);}
		}
		
		// make sure hotels are bought first - if there's still a hotel to be bough, buy this first!
		// TODO probably already allow to buy inflight when hotel for first day is already booked?
		
		
		// TODO allow to buy events for the days that are already booked?
		if(unsatisfiedItems.isEmpty()) {
			for(Item item : items) {
				if(!item.isSatisfied()) {
					unsatisfiedItems.add(item);
				}
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
	public void bookItem(Item item){
		Item remove = null;
		for(Item tmpItem : items) {
			if(item.equals(tmpItem)) {
				remove = tmpItem;
			}
		}
		items.remove(remove);
		items.add(item);
		updateItemList();
	}
	
	/**
	 * needs to be called whenever a Hotel-auction was closed
	 * 
	 * @param item
	 */
	public void auctionClosed(HotelItem item) {
		closedHotelAuctions.add(item);
	}

	/**
	 * Calculates the current utility
	 *
	 * @return the current utility
	 */
	public int getCurrentUtility(){
		if(isFeasible()){
			int utility = 1000;

			// TODO consider penalties using getActualArrival and getActualDeparture and so on
			boolean goodHotel = false;
			for(Item item:items){
				if(item.isSatisfied()) {
					utility -= item.getActualPrice();
					if(item instanceof EventItem) {
						utility += getBonus(((EventItem)item).getType().getBonusConstant());
					} else if(item instanceof HotelItem) {
						if(((HotelItem)item).getType() == HotelTypes.GOOD) {
							goodHotel = true;
						}
					}
				}
			}
			if(goodHotel){
				utility+= getBonus(TACAgent.HOTEL_VALUE);
			}
			return utility;
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
	
	public int getActualArrivalDay() {
		boolean[] allocatedHotelDays = new boolean[DAY_COUNT];
		for(Item item : items) {
			if(item instanceof HotelItem) {
				allocatedHotelDays[((HotelItem)item).getDay()] = true;
			}
		}
		
		int firstDay = -1;
		for(int i = 0;i<allocatedHotelDays.length;i++) {
			if(allocatedHotelDays[i]) {
				if(firstDay == -1) {
					firstDay = i;
				}
			}
		}
		return firstDay;
	}
	
	public int getActualDepartureDay() {
		boolean[] allocatedHotelDays = new boolean[DAY_COUNT];
		for(Item item : items) {
			if(item instanceof HotelItem) {
				allocatedHotelDays[((HotelItem)item).getDay()] = true;
			}
		}
		
		int lastDay = -1;
		for(int i = 0;i<allocatedHotelDays.length;i++) {
			if(allocatedHotelDays[i]) {
				lastDay = i;
			}
		}
		return lastDay;
	}
	
	public int getArrivalDay() {
		return arrivalDay;
	}

	public void setArrivalDay(int arrivalDay) {
		this.arrivalDay = arrivalDay;
	}

	public int getDepartureDay() {
		return departureDay;
	}

	public void setDepartureDay(int departureDay) {
		this.departureDay = departureDay;
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
