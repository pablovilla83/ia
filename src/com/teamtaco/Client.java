/**
 *
 */
package com.teamtaco;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.sics.tac.aw.TACAgent;

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
	public float getBuget(String ctd){
		float value = this.budget.get(ctd);
		return value;
	}


	private List<Item> items = new ArrayList<Item>();



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
	}

	/**
	 * updates the list of the items regarding possible changes in arrival-and departure dates and so on
	 */
	private void updateItemList(){
		boolean[] allocatedHotelDays = new boolean[DAY_COUNT];
		boolean[] occupiedDays = new boolean[DAY_COUNT];
		// look for constraints
		for(Item item : items){
			if(item instanceof EventItem && item.isSatisfied()){
				occupiedDays[((EventItem)item).getBookedDay()] = true;
			}
			if (item instanceof HotelItem && item.isSatisfied()) {
				allocatedHotelDays[((HotelItem)item).getDay()] = true;
			}
		}
		// find out first day on which a hotel was booked
		int firstDay = -1;
		int lastDay = -1;
		for(int i = 0;i<allocatedHotelDays.length;i++) {
			if(allocatedHotelDays[i]) {
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
			// remove hotels that cannot be booked anymore
			if(!item.isSatisfied() && item instanceof HotelItem) {
				if(((HotelItem)item).getDay() < firstDay) {
					itemsToRemove.add(item);
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
		}
		
		// make sure hotels are bought first - if there's still a hotel to be bough, buy this first!
		// TODO probably already allow to buy inflight when there's already a hotel (not necessary all) booked?
		// TODO allow to buy events for the days that are already booked?
		if(unsatisfiedItems.isEmpty()) {
			for(Item item : items) {
				if(!item.isSatisfied()) {
					unsatisfiedItems.add(item);
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
		String print = this.getId() + " " + this.getE1Bonus() + " " + this.getE2Bonus() + " " + this.getE3Bonus() + " "
				+ this.getArrivalDay() + " " + this.getDepartureDay() + " " + this.getHotelBonus() +
				" sorted by : " + this.getHotelBonus();

		return print;
	}
}
