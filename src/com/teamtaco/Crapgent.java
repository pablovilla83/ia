/**
 * 
 */
package com.teamtaco;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import com.teamtaco.util.HotelType;

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
					// TODO test here if we already have such an event!
					manageEventBid(client, (EventItem)item, auction);
				}
			}
		}
	}
	
	private void manageHotelBid(Client client, HotelItem item, int auction) {
		// allocate initial type if not done yet
		if(item.getType() == null) {
			throw new RuntimeException("hotel type not set!");
		}

		if (TACAgent.getAuctionType(auction) == item.getType().getTacType()
				&& TACAgent.getAuctionDay(auction) == ((HotelItem) item).getDay()) {
			if (item.getMaxPrice() >= prices[auction]){
				Bid bid = new Bid(auction);
				bid.addBidPoint(agent.getAllocation(auction), item.getMaxPrice());
				agent.submitBid(bid);
			}
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
//			if (item.getMaxPrice() >= prices[auction]) {
				Bid bid = new Bid(auction);
				bid.addBidPoint(1, 1000);
				synchronized (item) {
					if(!item.isSatisfied()) {
						agent.setAllocation(auction, agent.getAllocation(auction)+1);
						agent.submitBid(bid);
						client.bookItem(item,(int)prices[auction]);
					}
				}
//			}
		}
	}
	
	private void manageEventBid(Client client, EventItem item, int auction) {
/*		if (TACAgent.getAuctionType(auction) == item.getType().getBonusConstant()
				&& item.getPossibleDays()[TACAgent.getAuctionDay(auction)]) {
			if(client.getBonus(item.getType().getBonusConstant())> agent.getQuote(auction).getAskPrice()) {
				// place bid
				Bid bid = new Bid(auction);
				bid.addBidPoint(1, agent.getQuote(auction).getAskPrice()+1);
				agent.submitBid(bid);
				item.setBookedDay(TACAgent.getAuctionDay(auction));
				client.bookItem(item, (int)agent.getQuote(auction).getAskPrice());
			}
		}
*/
		int preference=0;
/*		if((client.getE1Bonus() > client.getE2Bonus()) && (client.getE1Bonus() > client.getE3Bonus()))
			preference = 1;
		if((client.getE2Bonus() > client.getE1Bonus()) && (client.getE2Bonus() > client.getE3Bonus()))
			preference = 2;
		if((client.getE3Bonus() > client.getE1Bonus()) && (client.getE3Bonus() > client.getE2Bonus()))
			preference = 3;
*/
		// this way should be faster
		if(client.getE1Bonus() > client.getE2Bonus())
			preference = (client.getE1Bonus() > client.getE3Bonus()) ? 1 : 3;
		else
			preference = (client.getE2Bonus() > client.getE3Bonus()) ? 2 : 3 ;
		
		
		System.out.println(client.getId()+"preference" + preference +" item.getType().getBonusConstant():"+item.getType().getBonusConstant()+ " " +agent.getAllocation(auction) +" <- getallocation , own -> "+ agent.getOwn(auction));
		
		System.out.println("item get type: "+item.getType());
		
		// don't understand item.getType().getBonusConstant() return 3 4 5 instead of 1 2 3 ????
		if(item.getType().getBonusConstant()==preference+2){
			//check if we already own a ticket...
			if (agent.getAllocation(auction) < agent.getOwn(auction)) {
				//mark the item as satisfied and update allocation
		//		int price = (int)prices[auction];
				int price = 0;
				client.bookItem(item, price);
				agent.setAllocation(auction, agent.getAllocation(auction) + 1);
				calculateMaxPrice(client, item);
			}
			else{
				//let's try to buy a ticket for that day...
				if (item.getMaxPrice() > 200){
					Bid bid = new Bid(auction);
					bid.addBidPoint(agent.getAllocation(auction), 201);
					agent.submitBid(bid);
				}
			}
		}
		else{
			// satisfy another client
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
		float avgHotelBonus = 0;
		for(int i = 0;i<8;i++){
			Client c = new Client(i);
			c.setE1Bonus(agent.getClientPreference(i, TACAgent.E1));
			c.setE2Bonus(agent.getClientPreference(i, TACAgent.E2));
			c.setE3Bonus(agent.getClientPreference(i, TACAgent.E3));
			c.setArrivalDay(agent.getClientPreference(i, TACAgent.ARRIVAL));
			c.setDepartureDay(agent.getClientPreference(i, TACAgent.DEPARTURE));
			c.setHotelBonus(agent.getClientPreference(i, TACAgent.HOTEL_VALUE));
			avgHotelBonus += c.getHotelBonus();
			System.out.println(c.toString());
			clients.add(c);
		}
		
		// learn the hotelBonus from past games (how much more expensive are good hotels
		avgHotelBonus /= 8;
		for(Client client : clients) {
			if(client.getHotelBonus() > avgHotelBonus) {
				client.setHotelType(HotelType.GOOD);
			} else {
				client.setHotelType(HotelType.CHEAP);
			}
			client.initializeItemList();
		}
		calculateAllocation();
		
	}

	private void calculateAllocation() {
		for(Client client : clients) {
			for(int i = client.getArrivalDay(); i< client.getDepartureDay();i++) {
				int type=client.getHotelType().getTacType();
				int auction = TACAgent.getAuctionFor(TACAgent.CAT_HOTEL, type, i);
				agent.setAllocation(auction, agent.getAllocation(auction)+1);
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
		// notification only necessary for Hotels
		if(TACAgent.getAuctionCategory(auction) == TACAgent.CAT_HOTEL) {
			int quantity = agent.getOwn(auction);
			
			// try to finish almost finished clients first
			ArrayList<Client> clientList = new ArrayList<Client>(clients);
			Collections.sort(clientList, new Comparator<Client>() {
				@Override
				public int compare(Client o1, Client o2) {
					return((Integer)o1.unallocatedHotelDays()).compareTo(o2.unallocatedHotelDays());
				}
			});
			HotelItem closedHotel = new HotelItem(TACAgent.getAuctionDay(auction), 
					HotelType.getTypeForConstant(TACAgent.getAuctionType(auction)));
			for(Client client : clientList) {
				if(quantity > 0 
						&& client.bookItem(closedHotel, (int)agent.getQuote(auction).getAskPrice())) {
					quantity--;
				}
				client.auctionClosed(closedHotel);
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
			
			if(hotel.getType() == HotelType.GOOD)
				budget += c.getHotelBonus();
			
			budget /= (c.unallocatedHotelDays());
			// days in the middle of the journey are of higher importance
			// formula: 0.75 +(-0.25*(x-arrivalDay)*(x-departureDay))^0.5
			double dayPosFactor =Math.pow(-0.25 * (hotel.getDay()-c.getArrivalDay()+0.01)*(hotel.getDay()-c.getDepartureDay()-0.01), 0.5);
			budget *=(0.75+dayPosFactor);
					
			// because we won't always pay our max bid we can add a threshold!
			budget *= 1.1;
			// hotels that need connect two days with each other are more important
			if(c.isInBetweenAllocatedDays(hotel)) {
				budget *=1.25;
			}
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
				budget -= c.getCurrentExpenses();
				budget/=2;
//				if(flight.getDay() != c.getInitialArrivalDay()) {
//					float travelPenalty = 100 * (Math.abs(flight.getDay() - c.getInitialArrivalDay()));
//					budget -= travelPenalty;
//				}
				float time = agent.getGameTimeLeft()/1000-30;
				time = time < 1? 1 : time;
				budget*= (1+1/time);
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
