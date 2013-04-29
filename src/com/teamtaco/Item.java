package com.teamtaco;

public abstract class Item {
	private int maxPrice;
	private int actualPrice = -1;

	public Item(){

	}

	public boolean isSatisfied(){
		return actualPrice != -1;
	}

	public Item(int maxPrice, int actualPrice){
		this.maxPrice = maxPrice;
		this.actualPrice = actualPrice;
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
	
	public abstract boolean isMandatory();
	
	public abstract boolean equals(Object obj);
	
	public abstract int getTacCategory();
}
