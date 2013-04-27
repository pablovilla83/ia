/**
 * 
 */
package com.teamtaco;

import java.util.SortedSet;
import java.util.TreeSet;

import se.sics.tac.aw.AgentImpl;
import se.sics.tac.aw.Bid;
import se.sics.tac.aw.Quote;
import se.sics.tac.aw.TACAgent;
import se.sics.tac.aw.Transaction;
import se.sics.tac.util.ArgEnumerator;

/**
 * @author Frederik
 * 
 */
public class Crapgent extends AgentImpl {
	
//	List<Client> clients = new ArrayList<Client>();
	SortedSet<Client> clients = new TreeSet<Client>();
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see se.sics.tac.aw.AgentImpl#init(se.sics.tac.util.ArgEnumerator)
	 */
	@Override
	protected void init(ArgEnumerator args) {
		// just print out the args
		while (args.hasNext()) {
			String current = args.next();
			String value = args.getArgument(current);
			System.out.println(current + ": " + value);
		}
	}
	
	@Override
	public void quoteUpdated(Quote quote) {
		// here comes update of bids
		int auction = quote.getAuction();
	}

	@Override
	public void quoteUpdated(int auctionCategory) {
		//useless
	}

	@Override
	public void transaction(Transaction transaction) {
		//useless
	}

	/**
	 * new information for the bid is available
	 */
	@Override
	public void bidUpdated(Bid bid) {
		//useless
	}

	/**
	 * bid got rejected (bid.getRejectReason for the reason)
	 */
	@Override
	public void bidRejected(Bid bid) {
		// probably not so useless - have to look what 'rejection' actually means
	}

	/**
	 * bid contained error
	 */
	@Override
	public void bidError(Bid bid, int error) {
		// pretty damn useless
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see se.sics.tac.aw.AgentImpl#gameStarted()
	 */
	@Override
	public void gameStarted() {
		
		// put clients in a wrapper that simplifies other tasks
		clients.clear();
		for(int i = 0;i<8;i++){
			Client c = new Client(i);
			c.setE1Bonus(agent.getClientPreference(i, TACAgent.E1));
			c.setE2Bonus(agent.getClientPreference(i, TACAgent.E2));
			c.setE3Bonus(agent.getClientPreference(i, TACAgent.E3));
			c.setArrivalDay(agent.getClientPreference(i, TACAgent.ARRIVAL));
			c.setDepartureDay(agent.getClientPreference(i, TACAgent.DEPARTURE));
			c.setHotelBonus(agent.getClientPreference(i, TACAgent.HOTEL_VALUE));
			clients.add(c);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see se.sics.tac.aw.AgentImpl#gameStopped()
	 */
	@Override
	public void gameStopped() {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see se.sics.tac.aw.AgentImpl#auctionClosed(int)
	 */
	@Override
	public void auctionClosed(int auction) {
		// TODO Auto-generated method stub

	}
	
	

}
