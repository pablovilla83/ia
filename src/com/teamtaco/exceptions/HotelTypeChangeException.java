/**
 * 
 */
package com.teamtaco.exceptions;

import com.teamtaco.Client;

/**
 * @author Frederik
 *
 */
public class HotelTypeChangeException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8049399574145440391L;
	
	private Client client;
	
	public HotelTypeChangeException(Client client){
		this.client = client;
	}
	
	public Client getClient(){
		return this.client;
	}

}
