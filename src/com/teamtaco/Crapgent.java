/**
 * 
 */
package com.teamtaco;

import java.util.SortedSet;
import java.util.TreeSet;

import com.teamtaco.util.EventType;
import com.teamtaco.util.FlightType;
import com.teamtaco.util.HotelTypes;

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
	
	public float calculateMaxPrice(Client c, Item item){
		float budget = 1000;
		int auction;
		if (item instanceof HotelItem){
			HotelItem hotel = (HotelItem) item;
			if(hotel.getType() == HotelTypes.GOOD){
				budget -= 600;
				budget += c.getHotelBonus();
				budget /= (c.getActualDepartureDay()-c.getActualArrivalDay());
				return budget;
			}else{
				budget -= 600;
				budget /= (c.getActualDepartureDay()-c.getActualArrivalDay());
				return budget;
			}
		}
		else if(item instanceof EventItem){
			EventItem event = (EventItem) item;
			if(event.getType() == EventType.Event1){budget += c.getE1Bonus();return budget;}
			else if(event.getType() == EventType.Event2){budget += c.getE2Bonus();return budget;}
			else{budget += c.getE3Bonus();return budget;}
		}
		else{
			FlightItem flight = (FlightItem) item;
/*			if(flight.getType() == FlightType.IN){
				//If we are buying a Flight it means that we have bought the Hotel so we get the last quote = closing price
				//We will break it up in days to calculate penalties
				if(flight.getDay()==c.getActualArrivalDay()){
					auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT, TACAgent.TYPE_INFLIGHT, flight.getDay());
					budget -= prices[auction];
					return budget;
				}
				//If we are referencing one of the days in the middle
				else if (flight.getDay()>c.getActualArrivalDay() && flight.getDay()<c.getActualDepartureDay()){
					auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT, TACAgent.TYPE_INFLIGHT, flight.getDay());
					budget -= prices[auction];
					return budget;
				}
				//TODO:
				else{return budget;}
			}
			//TODO:
			else{return budget;}
*/
			
			if(flight.getType() == FlightType.IN){
				auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT, TACAgent.TYPE_INFLIGHT, flight.getDay());
				budget -= prices[auction];
				if(flight.getDay() != c.getActualArrivalDay()) {
					float travelPenalty = 100 * (Math.abs(flight.getDay() - c.getActualArrivalDay()));
					budget -= travelPenalty;
				}
				return budget;
			}
			
			else {
				auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT, TACAgent.TYPE_OUTFLIGHT, flight.getDay());
				budget -= prices[auction];
				if (flight.getDay() != c.getActualDepartureDay()){
					float travelPenalty = 100 * (Math.abs(flight.getDay() - c.getActualDepartureDay()));
					budget -= travelPenalty;
				}
				return budget;
			}
		}
	}
}
