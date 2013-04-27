/**
 *
 */
package com.teamtaco.util;

import se.sics.tac.aw.TACAgent;

/**
 * @author Frederik
 *
 */
public enum EventType {
	Event1(TACAgent.E1),
	Event2(TACAgent.E2),
	Event3(TACAgent.E3);
	
	private int constant;
	
	EventType(int constant) {
		this.constant = constant;
	}
	
	public int getBonusConstant() {
		return constant;
	}

}
