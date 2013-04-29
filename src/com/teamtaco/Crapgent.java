/**
 * 
 */
package com.teamtaco;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import se.sics.tac.aw.AgentImpl;
import se.sics.tac.aw.Bid;
import se.sics.tac.aw.Quote;
import se.sics.tac.aw.TACAgent;
import se.sics.tac.aw.Transaction;
import se.sics.tac.util.ArgEnumerator;

import com.teamtaco.util.EventType;
import com.teamtaco.util.FlightType;
import com.teamtaco.util.HotelTypes;

/**
 * @author Frederik
 * 
 */
public class Crapgent extends AgentImpl {
	
//	List<Client> clients = new ArrayList<Client>();
	SortedSet<Client> clients = new TreeSet<Client>();
	private static final Logger log =
		    Logger.getLogger(Crapgent.class.getName());
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
		//check if the quote that got updated is on the whatToBuyNext list...
		for (Client client: clients){
			List<Item> listItems = new ArrayList<Item>();
			listItems = client.whatToBuyNext();
			
			for(Item item : listItems){
				item.setMaxPrice((int) calculateMaxPrice(client, item));
				if(item instanceof FlightItem && TACAgent.getAuctionCategory(auction) == TACAgent.CAT_FLIGHT){
					manageFlightBid(client, (FlightItem)item, auction);
				}else if(item instanceof HotelItem && TACAgent.getAuctionCategory(auction) == TACAgent.CAT_HOTEL){
					manageHotelBid(client, (HotelItem)item, auction);
				}else if (item instanceof EventItem && TACAgent.getAuctionCategory(auction) == TACAgent.CAT_ENTERTAINMENT){
					manageEventBid(client, (EventItem)item, auction);
				}
			}
		}
	}
	
	private void manageHotelBid(Client client, HotelItem item, int auction) {
		int type;
		
		if (item.getType() == HotelTypes.GOOD || (item.getType() == null && client.getHotelBonus()>100) ) {
			type = TACAgent.TYPE_GOOD_HOTEL;
		} else {
			type = TACAgent.TYPE_CHEAP_HOTEL;
		}
		if (TACAgent.getAuctionType(auction) == type
				&& TACAgent.getAuctionDay(auction) == ((HotelItem) item).getDay()) {
			System.out.println("I am checking the max price");
			if (item.getMaxPrice() >= prices[auction]){
				//TODO: run setAllocation method here
				Bid bid = new Bid(auction);
				bid.addBidPoint(agent.getAllocation(auction), item.getMaxPrice());
				System.out.println("submitting bid with alloc="
						+ agent.getAllocation(auction) + " own="
						+ agent.getOwn(auction));
				agent.submitBid(bid);
				System.out.println("I bid " + bid.getBidString());
				//client.bookItem(item);
			}
			// if the MaxPrice is feasible continue to bid
		}
	}
	
	private void manageFlightBid(Client client, FlightItem item, int auction) {
		int type;
		if (item.getType() == FlightType.IN) {
			type = TACAgent.TYPE_INFLIGHT;
		} else {
			type = TACAgent.TYPE_OUTFLIGHT;
		}
		if (TACAgent.getAuctionType(auction) == type
				&& TACAgent.getAuctionDay(auction) == ((FlightItem) item).getDay()) {
			// if the MaxPrice is feasible continue to bid
			if (item.getMaxPrice() >= prices[auction]) {
				int alloc = agent.getAllocation(auction);
				if (agent.getOwn(auction) < alloc) {
					Bid bid = new Bid(auction);
					bid.addBidPoint(1, item.getMaxPrice());
					log.finest("submitting bid with alloc="
							+ agent.getAllocation(auction) + " own="
							+ agent.getOwn(auction));
					agent.submitBid(bid);
					System.out.println("I bid " + bid.getBidString());
					client.bookItem(item,(int)prices[auction]);
				}
			}
		}
	}
	
	private void manageEventBid(Client client, EventItem item, int auction) {
		int type;
		if (((EventItem) item).getType() == EventType.Event1) {
			type = TACAgent.E1;
		} else if (((EventItem) item).getType() == EventType.Event2) {
			type = TACAgent.E2;
		} else {
			type = TACAgent.E3;
		}
		// TODO: ask fred about getBookedDay
		if (TACAgent.getAuctionType(auction) == type
				&& TACAgent.getAuctionDay(auction) == ((EventItem) item)
						.getBookedDay()) {
			// if the MaxPrice is feasible continue to
		}
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
		log.fine("Bid Updated: id=" + bid.getID() + " auction="
			     + bid.getAuction() + " state="
			     + bid.getProcessingStateAsString());
		    log.fine("       Hash: " + bid.getBidHash());
		if(bid.getProcessingState()==Bid.VALID){
			//Check if the bid was successfully submitted and mark the item as submitted? 
		}
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
		calculateAllocation();
		for(int i = 0;i<8;i++){
			Client c = new Client(i);
			c.setE1Bonus(agent.getClientPreference(i, TACAgent.E1));
			c.setE2Bonus(agent.getClientPreference(i, TACAgent.E2));
			c.setE3Bonus(agent.getClientPreference(i, TACAgent.E3));
			c.setArrivalDay(agent.getClientPreference(i, TACAgent.ARRIVAL));
			c.setDepartureDay(agent.getClientPreference(i, TACAgent.DEPARTURE));
			c.setHotelBonus(agent.getClientPreference(i, TACAgent.HOTEL_VALUE));
			c.initializeItemList();
			System.out.println(c.toString());
			clients.add(c);
			//TODO: calculateMaxPrice(c);
		}
		
	}

	private void calculateAllocation() {
	    for (int i = 0; i < 8; i++) {
	      int inFlight = agent.getClientPreference(i, TACAgent.ARRIVAL);
	      int outFlight = agent.getClientPreference(i, TACAgent.DEPARTURE);   
		  int type;
		  //Allocation of flights
	      int auction = TACAgent.getAuctionFor(TACAgent.CAT_FLIGHT,TACAgent.TYPE_INFLIGHT, inFlight);
		  agent.setAllocation(auction, agent.getAllocation(auction) + 1);
		  auction = TACAgent.getAuctionFor(TACAgent.CAT_FLIGHT, TACAgent.TYPE_OUTFLIGHT, outFlight);
		  agent.setAllocation(auction, agent.getAllocation(auction) + 1);
		  //Allocation of hotels
		  int hotelValue = agent.getClientPreference(i, TACAgent.HOTEL_VALUE);
		  if(hotelValue > 100){ 
			  type = TACAgent.TYPE_GOOD_HOTEL;
		  }else{
			  type = TACAgent.TYPE_CHEAP_HOTEL;
		  }
			  for (int d = inFlight; d < outFlight; d++) {
				  auction = TACAgent.getAuctionFor(TACAgent.CAT_HOTEL, type, d);	
				  agent.setAllocation(auction, agent.getAllocation(auction) + 1);
			  }
		}
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see se.sics.tac.aw.AgentImpl#gameStopped()
	 */
	@Override
	public void gameStopped() {
		// TODO Get the final asking price array so that we can improve next rounds
		log.fine("Game Stopped!");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see se.sics.tac.aw.AgentImpl#auctionClosed(int)
	 */
	@Override
	public void auctionClosed(int auction) {
		log.fine("*** Auction " + auction + " closed!");
		//check if we placed a bid for the auction that closed
		if (agent.getAllocation(auction)>0){
			int quantity = agent.getOwn(auction);
			int type = TACAgent.getAuctionType(auction);
			int day = TACAgent.getAuctionDay(auction);
			for(Client client : clients) {
				for(Item item : client.whatToBuyNext()) {
					if(quantity > 0
							&& item.getTacCategory() == TACAgent.getAuctionCategory(auction)){
						// book hotels
						if(item instanceof HotelItem) {
							HotelItem hotelItem = (HotelItem)item;
							if(hotelItem.getDay() == day
									&& (hotelItem.getType() == null || hotelItem.getType().getTacType() == type)) {
								client.bookItem(hotelItem, (int)agent.getQuote(auction).getAskPrice());
								quantity--;
							}
						}
					}
				}
				// notify all clients that auction closed
				if(type == TACAgent.CAT_HOTEL) {
					HotelItem item = new HotelItem(day,HotelTypes.getTypeForConstant(type));
					client.auctionClosed(item);
				}
			}
		} 
	}
	
	/**
	 * finds all clients that need the given {@link Item}
	 * 
	 * @param item the item
	 * @return all clients that need the {@link Item} item
	 */
	public List<Client> findClientsByItem(Item item) {
		List<Client> clients = new ArrayList<Client>();
		for(Client client: this.clients) {
			for(Item currItem : client.whatToBuyNext()) {
				if(currItem.equals(item) && !currItem.isSatisfied()) {
					clients.add(client);
					break;
				}
			}
		}
		return clients;
	}
	
	public float calculateMaxPrice(Client c, Item item){
		float budget = 1000;
		int auction;
		int fixedFee = 600;
		
		if (item instanceof HotelItem){
			HotelItem hotel = (HotelItem) item;
			budget -= fixedFee;
			
			if(hotel.getType() == HotelTypes.GOOD)
				budget += c.getHotelBonus();
			
			budget /= (c.getDepartureDay()-c.getArrivalDay());
			return budget;
		}
		
		else if(item instanceof EventItem){
			EventItem event = (EventItem) item;
			if(event.getType() == EventType.Event1)
				budget += c.getE1Bonus();
			
			else if(event.getType() == EventType.Event2)
				budget += c.getE2Bonus();
			
			else
				budget += c.getE3Bonus();
			
			budget -= fixedFee;
			
			return budget;
			
		}
		else{
			FlightItem flight = (FlightItem) item;
			if(flight.getType() == FlightType.IN){
				auction = TACAgent.getAuctionFor(TACAgent.CAT_FLIGHT, TACAgent.TYPE_INFLIGHT, flight.getDay());
				budget -= prices[auction];
				if(flight.getDay() != c.getInitialArrivalDay()) {
					float travelPenalty = 100 * (Math.abs(flight.getDay() - c.getInitialArrivalDay()));
					budget -= travelPenalty;
				}
				return budget;
			}
			
			else {
				auction = TACAgent.getAuctionFor(TACAgent.CAT_FLIGHT, TACAgent.TYPE_OUTFLIGHT, flight.getDay());
				budget -= prices[auction];
				if (flight.getDay() != c.getInitialDepartureDay()){
					float travelPenalty = 100 * (Math.abs(flight.getDay() - c.getInitialDepartureDay()));
					budget -= travelPenalty;
				}
				return budget;
			}
		}
	}
}
