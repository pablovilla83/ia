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
	Event1(TACAgent.E1, TACAgent.TYPE_ALLIGATOR_WRESTLING),
	Event2(TACAgent.E2, TACAgent.TYPE_AMUSEMENT),
	Event3(TACAgent.E3, TACAgent.TYPE_MUSEUM);
	
	private int constant;
	private int tactype;
	
	EventType(int constant, int tactype) {
		this.constant = constant;
		this.tactype = tactype;
	}
	
	public int getBonusConstant() {
		return constant;
	}
	
	public int getTacType() {
		return tactype;
	}
	
	public static EventType getTypeByTacType(int type) {
		for(EventType evtType : EventType.values()) {
			if(evtType.getTacType() == type) {
				return evtType;
			}
		}
		return null;
	}
}
