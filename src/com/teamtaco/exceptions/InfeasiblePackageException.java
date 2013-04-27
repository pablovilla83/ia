/**
 * 
 */
package com.teamtaco.exceptions;

import com.teamtaco.Client;

/**
 * @author Frederik
 *
 */
public class InfeasiblePackageException extends Exception {

	private Client client;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1476389170279134038L;
	
	public InfeasiblePackageException(String description, Client client){
		super(description);
		this.client = client;
	}
	
	public Client getClient(){
		return client;
	}

}
