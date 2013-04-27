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
	float[] prices= new float[28];
	
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
		prices[auction] = quote.getAskPrice();
		
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
			//TODO: calculateMaxPrice(c);
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
	
	public void calculateMaxPrice(Client c,int category, int type, int day ){
		int budget = 1000;
		int auction;
		//TODO: call the flights before the hotels so the budget is normalized
		switch (category){
			case TACAgent.CAT_HOTEL: 
				String ctd_in_flight = TACAgent.CAT_FLIGHT+"-"+ TACAgent.TYPE_INFLIGHT+"-"+c.getActualArrival();
				String ctd_out_flight = TACAgent.CAT_FLIGHT+"-"+ TACAgent.TYPE_INFLIGHT+"-"+c.getActualDeparture();
				for(int i=c.getActualArrival(); i <= c.getActualDeparture(); i++ ){
					auction = agent.getAuctionFor(category, type, i);
					if (type == TACAgent.TYPE_GOOD_HOTEL){
						String ctd_hotel = TACAgent.CAT_HOTEL+"-"+ TACAgent.TYPE_GOOD_HOTEL+"-"+i;
						//subtract flight  ... getBudget("1-1-i")
						budget -= c.getBuget(ctd_in_flight);
						budget -= c.getBuget(ctd_out_flight);
						budget += c.getHotelBonus();
						budget /= (c.getActualDeparture()-c.getActualArrival());
						if(budget > prices[auction])c.addToBudget(ctd_hotel, budget);
						//TODO: flag for bid
					}
					else{
						String ctd = TACAgent.CAT_HOTEL+"-"+ TACAgent.TYPE_CHEAP_HOTEL+"-"+i;
						budget -= c.getBuget(ctd_in_flight);
						budget -= c.getBuget(ctd_out_flight);
						budget /= (c.getActualDeparture()-c.getActualArrival());
						if(budget > prices[auction])c.addToBudget(ctd, budget);
						//TODO: flag for bid
					}
				}
			case TACAgent.CAT_FLIGHT:
				if (type == TACAgent.TYPE_INFLIGHT){
					String ctd = TACAgent.CAT_FLIGHT+"-"+ TACAgent.TYPE_INFLIGHT+"-"+c.getActualArrival();
					auction=agent.getAuctionFor(category, type, c.getActualArrival());
					budget -= prices[auction];
					if(budget > prices[auction])c.addToBudget(ctd,budget);
				}
				else{
					String ctd = TACAgent.CAT_FLIGHT+"-"+ TACAgent.TYPE_OUTFLIGHT+"-"+c.getActualDeparture();
					auction=agent.getAuctionFor(category, type, c.getActualDeparture());
					budget -= prices[auction];
					if(budget > prices[auction])c.addToBudget(ctd,budget);
				}
			case TACAgent.CAT_ENTERTAINMENT:
				for(int i=c.getActualArrival(); i <= c.getActualDeparture(); i++ ){
					String ctd_good_hotel = TACAgent.CAT_HOTEL+"-"+ TACAgent.TYPE_GOOD_HOTEL+"-"+i;
					String ctd_cheap_hotel = TACAgent.CAT_HOTEL+"-"+ TACAgent.TYPE_CHEAP_HOTEL+"-"+i;
					auction=agent.getAuctionFor(category, type, i);
					if (type == TACAgent.TYPE_ALLIGATOR_WRESTLING){
						String ctd = TACAgent.CAT_ENTERTAINMENT+"-"+ TACAgent.TYPE_ALLIGATOR_WRESTLING+"-"+i;
						budget += c.getE1Bonus();
						if(c.getBuget(ctd_good_hotel)>0){budget -= c.getBuget(ctd_cheap_hotel);}
						else budget -=c.getBuget(ctd_cheap_hotel);
						if(budget > prices[auction])c.addToBudget(ctd,budget);
					}
					else if(type == TACAgent.TYPE_AMUSEMENT){
						String ctd = TACAgent.CAT_ENTERTAINMENT+"-"+ TACAgent.TYPE_AMUSEMENT+"-"+i;
						budget += c.getE2Bonus();
						if(c.getBuget(ctd_good_hotel)>0){budget -= c.getBuget(ctd_cheap_hotel);}
						else budget -=c.getBuget(ctd_cheap_hotel);
						if(budget > prices[auction])c.addToBudget(ctd,budget);
					}
					else{
						String ctd = TACAgent.CAT_ENTERTAINMENT+"-"+ TACAgent.TYPE_MUSEUM+"-"+i;
						budget += c.getE3Bonus();
						if(c.getBuget(ctd_good_hotel)>0){budget -= c.getBuget(ctd_cheap_hotel);}
						else budget -=c.getBuget(ctd_cheap_hotel);
						if(budget > prices[auction])c.addToBudget(ctd,budget);
					}
				}
			default: break;
		}
	}
	
	

}
