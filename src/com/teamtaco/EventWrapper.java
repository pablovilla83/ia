/**
 * 
 */
package com.teamtaco;

/**
 * @author Frederik
 *
 */
public class EventWrapper {

	private EventItem item;
	private Client client;
	
	public EventWrapper(EventItem item, Client client) {
		this.item = item;
		this.client = client;
	}
	
	public EventItem getItem() {
		return item;
	}
	public void setItem(EventItem item) {
		this.item = item;
	}
	public Client getClient() {
		return client;
	}
	public void setClient(Client client) {
		this.client = client;
	}
	
}
