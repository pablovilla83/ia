/**
 * 
 */
package com.teamtaco;

import java.util.ArrayList;
import java.util.List;

import com.teamtaco.exceptions.HotelTypeChangeException;
import com.teamtaco.exceptions.InfeasiblePackageException;

import se.sics.tac.aw.Quote;
import se.sics.tac.aw.TACAgent;


/**
 * @author Frederik
 *
 */
public class Client implements Comparable<Client>{
	
	public static final int RANDOM_HOTEL_TYPE = 1337;
	
	private int id;
	private int arrivalDay;
	private int departureDay;
	private int hotelBonus;
	
	private int e1Bonus = 0;
	private int e2Bonus = 0;
	private int e3Bonus = 0;
	
	private List<Item> satisfiedItems = new ArrayList<Item>();
	
	
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
	 * Gives a list of which items still need to be bought
	 * 
	 * @return list of items that still need to be bought
	 * @throws InfeasiblePackageException indicates that with the current satisfied items, 
	 * there is no chance to actually make this packet feasible. It is assumed, that
	 * the auctions for hotels follow the pattern "auction for day x ends before auction for day x+1"
	 * @throws HotelTypeChangeException if there are already hotels booked but those hotels are of different types
	 */
	public List<Item> whatToBuyNext() throws InfeasiblePackageException, HotelTypeChangeException{
		List<Item> items = new ArrayList<Item>();
		
		// look up what's already there
		Item inFlight = null;
		Item outFlight = null;
		int firstHotelDay = 999;
		boolean[] hotels = new boolean[10];
		int actualHotelType = RANDOM_HOTEL_TYPE;
		for(Item item : satisfiedItems){
			if(item.getType() == TACAgent.TYPE_INFLIGHT){
				inFlight = item;
			} else if (item.getType() == TACAgent.TYPE_OUTFLIGHT){
				outFlight = item;
			}
			if(item.getType() == TACAgent.TYPE_CHEAP_HOTEL || item.getType() == TACAgent.TYPE_GOOD_HOTEL){
				if(item.getDay() < firstHotelDay){
					firstHotelDay = item.getDay();
				}
				hotels[item.getDay()] = true;
				if(actualHotelType != RANDOM_HOTEL_TYPE && actualHotelType != item.getType()){
					throw new HotelTypeChangeException(this);
				} else {
					actualHotelType = item.getType();
				}
			}
		}
		
		// check for infeasible package
		if(inFlight != null){
			if(firstHotelDay != 999 && inFlight.getDay()<firstHotelDay){
				throw new InfeasiblePackageException("Impossible to create a feasible package for this client", this);
			}
		}


		// calculate the days on which the client will actually stay / can actually stay
		int startDate;
		if(firstHotelDay!= 999){
			startDate = firstHotelDay;
		} else if(inFlight != null){
			startDate = inFlight.getDay();
		} else {
			startDate = getArrivalDay();
		}
		
		int endDate;
		if(outFlight != null){
			endDate = outFlight.getDay();
		} else {
			endDate = getDepartureDay();
		}
		
		// do not allocate hotel for the last day!
		for(int i = startDate; i < endDate;i++ ){
			if(!hotels[i]){
				// TODO get max price here 
				int maxPrice = 0;
				items.add(new Item(actualHotelType, i, maxPrice, 0));
			}
		}
		
		// buy hotels first!
		if(items.size()>0){
			return items;
		}
		
		// find out dates by hotel-bookings (which should be done at this point!)
		boolean arrived = false;
		for(int i = 0; i< hotels.length;i++){
			if(hotels[i] && !arrived){
				startDate = i;
				arrived = true;
			}
			if(!hotels[i] && arrived){
				endDate = i;
				arrived = false;
				break;
			}
		}
		
		// create matrix for satisfied events
		boolean[] events = new boolean[3];
		for(Item item : satisfiedItems){
			switch(item.getType()){
			case TACAgent.E1: events[0] = true;break;
			case TACAgent.E2: events[1] = true;break;
			case TACAgent.E3: events[2] = true;break;
			default: break;
			}
		}

		// check if inFlight already exists
		if(inFlight == null){
			// TODO get maxPrice via method
			int maxPrice = 0;
			items.add(new Item(TACAgent.TYPE_INFLIGHT, startDate, maxPrice, 0));
		}
		// check if outFlight already exists
		if (outFlight == null){
			// TODO get from method
			int maxPrice = 0;
			items.add(new Item(TACAgent.TYPE_OUTFLIGHT, endDate, maxPrice, 0));
		}
		// add missing events
		for(int i = 0;i<events.length;i++){
			if(!events[i]){
				int type = i == 0? TACAgent.E1: i == 1? TACAgent.E2:TACAgent.E3;
				// TODO get maxPrice from function
				int maxPrice = 0;
				items.add(new Item(type, 0, maxPrice, 0));
			}
		}
		return items;
	}
	
	/**
	 * marks an item was satisfied (auction won)
	 * 
	 * @param item the item
	 */
	public void satisfy(Item item){
		satisfiedItems.add(item);
	}
	
	/**
	 * looks up if field field is already satisfied.
	 * Fields that are not necessary (events) and do not have a bonus are always satisfied.
	 * Such fields would have a negative influence on the utility as soon as the price is > 0.
	 * 
	 * @deprecated method whatToBuyNext contains everything you need
	 * 
	 * @param field the field
	 * @return true if the field field is satisfied or unnecessary, false otherwise
	 */
	@Deprecated
	public boolean isSatisfied(int field){
		for(Item item : satisfiedItems){
			if(item.getType() == field){
				return true;
			}
		}
		if(field != TACAgent.ARRIVAL && field != TACAgent.DEPARTURE && getBonus(field) == 0){
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * looks up the price for field field
	 * 
	 * should not be used
	 * 
	 * @param field the field
	 * @return the price for field field if field was already allocated, -1 otherwise
	 */
	@Deprecated
	public int priceFor(int field){
		
		for(Item item : satisfiedItems){
			if(item.getType() == field){
				return item.getActualPrice();
			}
		}
		return -1;
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
			for(Item item:satisfiedItems){
				utility -= item.getActualPrice();
				utility += getBonus(item.getType());
				if(item.getType() == TACAgent.TYPE_GOOD_HOTEL){
					goodHotel = true;
				}
			}
			if(goodHotel){
				utility+= getBonus(TACAgent.HOTEL_VALUE);
			}
			return utility;
		}
		return 0;
	}
	
	public boolean isFeasible(){
		int actualArrival = -1;
		int actualDeparture =-1;
		int hotelType = RANDOM_HOTEL_TYPE;
		boolean[] hotels = new boolean[8];
		for(Item item : satisfiedItems){
			if(item.getType() == TACAgent.TYPE_INFLIGHT){
				actualArrival = item.getDay();
			} else if (item.getType() == TACAgent.TYPE_OUTFLIGHT) {
				actualDeparture = item.getDay();
			} else if (item.getType() == TACAgent.TYPE_CHEAP_HOTEL || item.getType() == TACAgent.TYPE_GOOD_HOTEL){
				// different hotel-types are infeasible
				if(hotelType != RANDOM_HOTEL_TYPE && item.getType() != RANDOM_HOTEL_TYPE){
					return false;
				}
				hotelType = item.getType();
				hotels[item.getDay()] = true;
			}
		}
		
		if(actualArrival >=0 && actualDeparture >=0 && (hotelType == TACAgent.TYPE_CHEAP_HOTEL || hotelType == TACAgent.TYPE_GOOD_HOTEL)){
			for(int i = actualArrival; i< actualDeparture;i++){
				if(!hotels[i]){
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
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

	public int getActualArrival(){
		for(Item item: satisfiedItems){
			if(item.getType() == TACAgent.TYPE_INFLIGHT){
				return item.getDay();
			}
		}
		return getArrivalDay();
	}
	
	public int getActualDeparture(){
		for(Item item : satisfiedItems){
			if(item.getType() == TACAgent.TYPE_OUTFLIGHT){
				return item.getDay();
			}
		}
		return getDepartureDay();
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
	
	public int calculateMaxPrice(int type){
		int budget = 1000;
		switch (type){
			//TODO: potential optimization
			case TACAgent.TYPE_CHEAP_HOTEL: 
				budget -= 600;
				budget /= (getActualDeparture()-getActualArrival());
				return budget;
				//TODO: potential optimization
			case TACAgent.TYPE_GOOD_HOTEL:
				budget -= 600;
				budget += getHotelBonus();
				budget /= (getActualDeparture()-getActualArrival());
				return budget;
			default: return 0;
		}
	}

}
