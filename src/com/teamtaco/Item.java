package com.teamtaco;

public class Item {
	private int type;
	private int day;
	private int maxPrice;
	private int actualPrice;
	
	public Item(){
		
	}
	
	public Item(int type, int day, int maxPrice, int actualPrice){
		this.type = type;
		this.day = day;
		this.maxPrice = maxPrice;
		this.actualPrice = actualPrice;
	}
	
	public int getType() {
		return type;
	}
	
	public void setType(int type) {
		this.type = type;
	}
	public int getDay() {
		return day;
	}
	public void setDay(int day) {
		this.day = day;
	}
	public int getMaxPrice() {
		return maxPrice;
	}
	public void setMaxPrice(int maxPrice) {
		this.maxPrice = maxPrice;
	}

	public int getActualPrice() {
		return actualPrice;
	}

	public void setActualPrice(int actualPrice) {
		this.actualPrice = actualPrice;
	}
}
