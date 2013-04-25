/**
 * 
 */
package com.teamtaco;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import se.sics.tac.aw.TACAgent;


/**
 * @author Frederik
 *
 */
public class Client {
	
	private int id;
	private int arrivalDay;
	private int departureDay;
	private int hotelBonus;
	
	private int e1Bonus = 0;
	private int e2Bonus = 0;
	private int e3Bonus = 0;
	
	private Map<Integer, Integer> satisfiedThings = new HashMap<Integer,Integer>();
	
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
	 * marks that field field was satisfied for a price of price
	 * 
	 * @param field the field that was satisfied
	 * @param price the price
	 */
	public void satisfy(int field, int price){
		satisfiedThings.put(field, price);
	}
	
	/**
	 * looks up if field field is already satisfied.
	 * Fields that are not necessary (events) and do not have a bonus are always satisfied.
	 * Such fields would have a negative influence on the utility as soon as the price is > 0.
	 * 
	 * @param field the field
	 * @return true if the field field is satisfied or unnecessary, false otherwise
	 */
	public boolean isSatisfied(int field){
		if(satisfiedThings.containsKey(field)){
			return satisfiedThings.containsKey(field);
		}
		if(field != TACAgent.ARRIVAL && field != TACAgent.DEPARTURE && field != TACAgent.HOTEL_VALUE && getBonus(field) == 0){
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * looks up the price for field field
	 * 
	 * @param field the field
	 * @return the price for field field if field was already allocated, -1 otherwise
	 */
	public int priceFor(int field){
		if(satisfiedThings.containsKey(field)){
			return satisfiedThings.get(field);
		}
		return -1;
	}
	
	/**
	 * Calculates the current utility
	 * 
	 * @return the current utility
	 */
	public int getCurrentUtility(){
		if(isSatisfied(TACAgent.ARRIVAL) 
				&& isSatisfied(TACAgent.DEPARTURE)
				&& isSatisfied(TACAgent.HOTEL_VALUE)){
			int utility = 1000;
			for(Entry<Integer,Integer> entry : satisfiedThings.entrySet()){
				utility -= entry.getValue();
				utility += getBonus(entry.getKey());
			}
			return utility;
		}
		return 0;
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
}
