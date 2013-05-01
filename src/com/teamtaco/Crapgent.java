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
	boolean[][] tmpCat = new boolean[8][3];
	int[] tmpEvent = new int[28];
	
	
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
			if (item.getMaxPrice() >= prices[auction]) {
				Bid bid = new Bid(auction);
				bid.addBidPoint(1, item.getMaxPrice());
				synchronized (item) {
					if(!item.isSatisfied()) {
						agent.setAllocation(auction, agent.getAllocation(auction)+1);
						agent.submitBid(bid);
						client.bookItem(item,(int)prices[auction]);
					}
				}
			}
		}
	}
	
	private void manageEventBid(Client client, EventItem item, int auction) {
//		int preference=0;
//		if(client.getE1Bonus() > client.getE2Bonus())
//			preference = (client.getE1Bonus() > client.getE3Bonus()) ? TACAgent.E1 : TACAgent.E3;
//		else
//			preference = (client.getE2Bonus() > client.getE3Bonus()) ? TACAgent.E2 : TACAgent.E3;
				
//		if(item.getType().getBonusConstant()==preference){
		
		if (client.getBonus(item.getType().getBonusConstant()) > 100){
			//check if we already own a ticket...
			if (item.getPossibleDays()[TACAgent.getAuctionDay(auction)]){
				if (tmpEvent[auction] < agent.getOwn(auction)) {
					//mark the item as satisfied and update allocation
					int price = 0;
					client.bookItem(item, price);
					tmpEvent[auction]++;
					System.out.println("saving in tmpEvent");
				}
				else{
					//let's try to buy a ticket for that day...
					// TODO: if my asking prince is > utility sell for asking price, else sell for utility+1
					if(evaluateBonusEvent(client, item, auction)){
						Bid bid = new Bid(auction);
						bid.addBidPoint(agent.getAllocation(auction)+1, prices[auction]);
						agent.submitBid(bid);
						agent.setAllocation(auction, agent.getAllocation(auction)+1);
						client.bookItem(item, item.getMaxPrice());
						System.out.println("submitted " + bid.getBidString() + " price: " + item.getMaxPrice() + " for auction " + auction);
					}
				}
			}
		}
		else{
			// sell the ticket
			
			if (agent.getAllocation(auction) < agent.getOwn(auction) && tmpEvent[auction] < agent.getOwn(auction)){
				int i = agent.getAllocation(auction) - agent.getOwn(auction);
				Bid bid = new Bid(auction);
				// TODO: fix this dick
				if (prices[auction] > 40)
					bid.addBidPoint(i, prices[auction]/3);
				agent.submitBid(bid);
				tmpEvent[auction]++;
				System.out.println("I'm selling for auction " + auction + " " + bid.getBidString());
			}
			
		}
	}

	private boolean evaluateBonusEvent(Client client, EventItem item, int auction){
		int auctionType = TACAgent.getAuctionType(auction);
		boolean evaluateBonus = false;
		
		switch (auctionType) {
		case TACAgent.TYPE_ALLIGATOR_WRESTLING:
			evaluateBonus = prices[auction]<client.getE1Bonus();
			break;

		case TACAgent.TYPE_AMUSEMENT:
			evaluateBonus = prices[auction]<client.getE2Bonus();
			break;
		
		default:
			evaluateBonus = prices[auction]<client.getE3Bonus(); 
			break;
		}
		return evaluateBonus;
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
		if(bid.getProcessingState()==Bid.TRANSACTED){ 
			int auction = bid.getAuction();
			if (TACAgent.getAuctionCategory(auction)==TACAgent.CAT_ENTERTAINMENT){
				int alloc = bid.getQuantity();
				if (alloc<0){
					agent.setAllocation(auction, agent.getAllocation(auction)-alloc);
				}
			}
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
			for(int j=0;j<3;j++){
				tmpCat[i][j]=false;
			}
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
		agent.printOwn(); // sysout on TACAgent line 783
	}

	private void calculateAllocation() {
		for(Client client : clients) {
			for(int i = client.getArrivalDay(); i< client.getDepartureDay();i++) {
				int type=client.getHotelType().getTacType();
				int auction = TACAgent.getAuctionFor(TACAgent.CAT_HOTEL, type, i);
				agent.setAllocation(auction, agent.getAllocation(auction)+1);
				
					for (int etype=1; etype<4; etype++){
					int eventAuction = TACAgent.getAuctionFor(TACAgent.CAT_ENTERTAINMENT, etype, i);
					// TODO: check the actual utility
					if(agent.getAllocation(eventAuction)<agent.getOwn(eventAuction) && !tmpCat[i][etype-1] &&((etype==1 && client.getE1Bonus()>80) 
							|| (etype==2 && client.getE2Bonus()>80) || (etype==3 && client.getE3Bonus()>80) )){
						agent.setAllocation(eventAuction, agent.getAllocation(eventAuction)+1);
						tmpCat[i][etype-1]=true;
						System.out.println(client.getId()+ " wants 1+ of auction"+eventAuction);
					}
					}
				
			}
		}
		for(int i=0; i<8;i++){
			System.out.println("user " + i);
			for(int j=0;j<3;j++){
				System.out.print(tmpCat[i][j] + " - ");
			}
			System.out.println(" ");
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
			// increase the budget every 9.9 game-seconds
			float time = agent.getGameTimeLeft()/9900-4;
			time = time < 1? 1 : time;
			// start at a really low price for the flights
			budget = 325;
			// increase the budget slowly until it reaches 800 (250*3.2) in the last 30 seconds
			budget*= (1+1.5/Math.pow(time, 0.6));
			System.out.println( budget + " " + time);
			// at the end of the game just buy the flights!
			return budget;
		}
	}
}
