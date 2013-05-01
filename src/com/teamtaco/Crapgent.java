/**
 * 
 */
package com.teamtaco;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
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
	
	private static final String GAMECOUNT_PROPERTY = "gameCount";
	private static final String AVG_CHEAP_HOTEL_PRICE = "avgCheapHotelPrice";
	private static final String AVG_GOOD_HOTEL_PRICE = "avgGoodHotelPrice";
	
//	List<Client> clients = new ArrayList<Client>();
	SortedSet<Client> clients = new TreeSet<Client>();
	private static final Logger log =
		    Logger.getLogger(Crapgent.class.getName());
	float[] prices= new float[28];
	
	private float aggregatedCheapPrices;
	private float aggregatedGoodPrices;
	
	List<EventItem> ownedItems = new ArrayList<EventItem>();
	
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
		
		List<EventWrapper> eventWrappers = new ArrayList<EventWrapper>();
		Map<Client, HotelItem> clientHotelMap = new HashMap<Client, HotelItem>();
		
		//check if the quote that got updated is on the whatToBuyNext list...
		for (Client client: clients){
			List<Item> listItems = new ArrayList<Item>();
			listItems = client.whatToBuyNext();
			
			for(Item item : listItems){
				item.setMaxPrice((int) calculateMaxPrice(client, item));
				if(item instanceof FlightItem && TACAgent.getAuctionCategory(auction) == TACAgent.CAT_FLIGHT){
					manageFlightBid(client, (FlightItem)item, auction);
				}else if(item instanceof HotelItem && TACAgent.getAuctionCategory(auction) == TACAgent.CAT_HOTEL){
					if(evaluateHotelBid(client, (HotelItem)item, auction)) {
						clientHotelMap.put(client, ((HotelItem)item));
					}
				}else if (item instanceof EventItem && TACAgent.getAuctionCategory(auction) == TACAgent.CAT_ENTERTAINMENT){
					// prepare event-list
					if(!item.isSatisfied()) {
						eventWrappers.add(new EventWrapper((EventItem) item, client));
					}
				}
			}
		}
		placeHotelBids(clientHotelMap, auction);
		updateEventBids(new EventManager(eventWrappers));
	}
	
	// TODO probably call this method not only when quotes got updated but repeatedly every x seconds
	private void updateEventBids(EventManager manager) {
		// we only have to care about event-auctions obviously
		EventWrapper highestUtility = null;
		EventItem bestItem = null;
		
		// find the best item to currently bid for
		for (int auction = 0; auction < TACAgent.getAuctionNo(); auction++) {
			if (TACAgent.getAuctionCategory(auction) == TACAgent.CAT_ENTERTAINMENT) {
				int type = TACAgent.getAuctionType(auction);

				EventItem item = new EventItem();
				item.setBookedDay(TACAgent.getAuctionDay(auction));
				item.setType(EventType.getTypeByTacType(type));
				
				// TODO teach agent to ignore 0-askprice items (they are actually not on sale I guess
				if(agent.getQuote(auction).getAskPrice()>0) {
					item.setActualPrice((int) agent.getQuote(auction).getAskPrice()+1);
					EventWrapper wrapper = manager.getClientWithHighestBonus(item);
					if (manager.getUtility(wrapper, item) > manager.getUtility(
							highestUtility, bestItem)) {
						highestUtility = wrapper;
						bestItem = item;
					}
				}

			}
		}
		
		// if positive score -> buy (place bid and wait)
		// TODO probably define threshold so that we at least increase our score by 10?
		// effect could be, that the agent waits until he gets a better price
		if(manager.getUtility(highestUtility, bestItem)>0) {
			
			// first check if we have items of this on our own
			Iterator<EventItem> it = ownedItems.iterator();
			while(it.hasNext()) {
				EventItem next = it.next();
				
				if(next.getType() == bestItem.getType()
						&& next.getBookedDay() == bestItem.getBookedDay()) {
					ownedItems.remove(next);
					highestUtility.getItem().setBookedDay(next.getBookedDay());
					highestUtility.getClient().bookItem(highestUtility.getItem(), next.getActualPrice());
					return;
				}
			}
			
			int auction = TACAgent.getAuctionFor(TACAgent.CAT_ENTERTAINMENT, bestItem.getType().getTacType(), bestItem.getBookedDay());
			agent.setAllocation(auction, agent.getAllocation(auction)+1);
			Bid bid = new Bid(auction);
			bid.addBidPoint(1, bestItem.getActualPrice());
			agent.submitBid(bid);
			highestUtility.getItem().setBookedDay(bestItem.getBookedDay());
			highestUtility.getItem().setActualPrice(bestItem.getActualPrice());
			
			// even if we don't know if we won the auction we book the item as we have no further influence on it anyway
			highestUtility.getClient().bookItem(highestUtility.getItem(), bestItem.getActualPrice());
		}

		// remove owned items if the best bonus of a client is lower than the askprice
		// only remove one at a time
		for(int i = 0;i<ownedItems.size();i++) {
			EventItem item = ownedItems.get(i);
			EventWrapper wrapper = manager.getClientWithHighestBonus(item);
			int auction = TACAgent.getAuctionFor(TACAgent.CAT_ENTERTAINMENT, item.getType().getTacType(), item.getBookedDay());
			
			// try to avoid overselling
			if(agent.getOwn(auction)<0) {
				return;
			}
			
			// TODO do we want a threshold? The cheaper we sell it, the higher the others will score!
			float highestBid = agent.getQuote(auction).getBidPrice();
			if(highestBid >25 ) {
				if(wrapper != null) {
					int bonus = wrapper.getClient().getBonus(item.getType().getTacType());
					if(bonus+1 < highestBid) {
						ownedItems.remove(item);
						Bid bid = new Bid(auction);
						bid.addBidPoint(-1, highestBid-1);
						agent.submitBid(bid);
						i--;
					}
				} else if(agent.getGameTimeLeft()/1000 < 60 && manager.getNumberOfPotentialBuyers(item)==0) {
					// sell the crap in the last 60 seconds
					ownedItems.remove(item);
					Bid bid = new Bid(auction);
					bid.addBidPoint(-1, highestBid-1);
					agent.submitBid(bid);
					i--;
				}
			}
		}
	}
	
	private boolean evaluateHotelBid(Client client, HotelItem item, int auction) {
		// allocate initial type if not done yet
		if(item.getType() == null) {
			throw new RuntimeException("hotel type not set!");
		}

		if (TACAgent.getAuctionType(auction) == item.getType().getTacType()
				&& TACAgent.getAuctionDay(auction) == ((HotelItem) item).getDay()) {
//			if (item.getMaxPrice() >= prices[auction]){
//				Bid bid = new Bid(auction);
//				bid.addBidPoint(agent.getAllocation(auction), item.getMaxPrice());
//				agent.submitBid(bid);
//			}
			return true;
			
		}
		return false;
	}
	
	private void placeHotelBids(Map<Client, HotelItem> bids, int auction) {
		float avgPrice = 0;
		for(Entry<Client, HotelItem> entry : bids.entrySet()) {
			avgPrice+=entry.getValue().getMaxPrice();
		}
		avgPrice /= bids.size();
		if(avgPrice >= prices[auction]) {
			Bid bid = new Bid(auction);
			bid.addBidPoint(bids.size(), avgPrice);
			agent.submitBid(bid);
		}
//		if (item.getMaxPrice() >= prices[auction]){
//			Bid bid = new Bid(auction);
//			bid.addBidPoint(agent.getAllocation(auction), item.getMaxPrice());
//			agent.submitBid(bid);
//		}
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
		ownedItems.clear();
		aggregatedCheapPrices = 0;
		aggregatedGoodPrices = 0;
		
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
		Properties props = getGameProperties();
		
		String gameCountString = props.getProperty(GAMECOUNT_PROPERTY);
		String avgCheapString = props.getProperty(AVG_CHEAP_HOTEL_PRICE);
		String avgGoodString = props.getProperty(AVG_GOOD_HOTEL_PRICE);
		
		float gameCount;
		float avgCheap;
		float avgGood;
		if(gameCountString == null || avgCheapString == null || avgGoodString == null) {
			// initialize
			gameCount =0;
			avgCheap = 0;
			avgGood = 0;
		} else {
			gameCount = Float.parseFloat(gameCountString);
			avgCheap = Float.parseFloat(avgCheapString);
			avgGood = Float.parseFloat(avgGoodString);
		}
		
		if(gameCount != 0) {
			avgHotelBonus = avgGood - avgCheap;
			System.out.println("Read values for gameCount:" + gameCount + "; avgDifference: " + avgHotelBonus);
		} else {
			avgHotelBonus /= 8;
		}
		for(Client client : clients) {
			if(client.getHotelBonus() > avgHotelBonus) {
				client.setHotelType(HotelType.GOOD);
			} else {
				client.setHotelType(HotelType.CHEAP);
			}
			client.initializeItemList();
		}
		
		// save already owned items
		for(int auction = 0;auction<TACAgent.getAuctionNo();auction++) {
			if(agent.getOwn(auction)>0 && TACAgent.getAuctionCategory(auction)== TACAgent.CAT_ENTERTAINMENT) {
				for(int i = 0; i< agent.getOwn(auction);i++) {
					EventItem item = new EventItem();
					item.setBookedDay(TACAgent.getAuctionDay(auction));
					item.setType(EventType.getTypeByTacType(TACAgent.getAuctionType(auction)));
					item.setActualPrice(0);
					ownedItems.add(item);
				}
			}
		}
		
		calculateAllocation();
		agent.printOwn(); // sysout on TACAgent line 783
	}

	private void calculateAllocation() {
		for (Client client : clients) {
			for (int i = client.getArrivalDay(); i < client.getDepartureDay(); i++) {
				int type = client.getHotelType().getTacType();
				int auction = TACAgent.getAuctionFor(TACAgent.CAT_HOTEL, type,
						i);
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
		log.fine("Game Stopped!");
		
		Properties props = getGameProperties();
		
		String gameCountString = props.getProperty(GAMECOUNT_PROPERTY);
		String avgCheapString = props.getProperty(AVG_CHEAP_HOTEL_PRICE);
		String avgGoodString = props.getProperty(AVG_GOOD_HOTEL_PRICE);
		
		float gameCount;
		float avgCheap;
		float avgGood;
		if(gameCountString == null || avgCheapString == null || avgGoodString == null) {
			// initialize
			gameCount =0;
			avgCheap = 0;
			avgGood = 0;
		} else {
			gameCount = Float.parseFloat(gameCountString);
			avgCheap = Float.parseFloat(avgCheapString);
			avgGood = Float.parseFloat(avgGoodString);
		}
		
		
		aggregatedCheapPrices /=4f;
		aggregatedGoodPrices /= 4f;
		
		gameCount++;
		avgCheap = (avgCheap*(gameCount-1)/gameCount) + (aggregatedCheapPrices/gameCount);
		avgGood = (avgGood *(gameCount-1)/gameCount)+(aggregatedGoodPrices/gameCount);
		
		props.setProperty(GAMECOUNT_PROPERTY, ""+gameCount);
		props.setProperty(AVG_GOOD_HOTEL_PRICE, ""+avgGood);
		props.setProperty(AVG_CHEAP_HOTEL_PRICE, ""+avgCheap);
		
		try {
			props.store(new FileWriter(agent.getHost()+".properties"), null);
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
		
		System.out.println("GameCount: " + gameCount + "; avgDifference: "+(avgGood-avgCheap));
	}
	
	public Properties getGameProperties() {
		Properties props = new Properties();
		try {
			props.load(new FileReader(new File(agent.getHost()+".properties")));
		} catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
		return props;
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
			Quote quote = agent.getQuote(auction);
			
			HotelType type = HotelType.getTypeForConstant(TACAgent.getAuctionType(auction));
			
			switch(type) {
			case GOOD: aggregatedGoodPrices+=quote.getAskPrice();
			case CHEAP: aggregatedCheapPrices+= quote.getAskPrice();
			}
			
			final HotelItem closedHotel = new HotelItem(TACAgent.getAuctionDay(auction), 
					type);
			
			// try to satisfy clients with the highest maxPrice first
			// then order by amount of unsatisfied Items
			ArrayList<Client> clientList = new ArrayList<Client>(clients);
			Collections.sort(clientList, new Comparator<Client>() {
				@Override
				public int compare(Client o1, Client o2) {
					int maxPriceCompare = 0;
					HotelItem o1Item = o1.findItem(closedHotel);
					HotelItem o2Item = o2.findItem(closedHotel);
					if(o1Item == null && o2Item != null) {
						return new Integer(0).compareTo(1000);
					} else if(o1Item != null && o2Item == null) {
						return new Integer(1000).compareTo(0);
					} else if (o1Item != null && o2Item!= null){
						maxPriceCompare = ((Integer)o1Item.getMaxPrice()).compareTo(o2Item.getMaxPrice());
						if(maxPriceCompare != 0) {
							return maxPriceCompare;
						}
					}
					return((Integer)o1.unallocatedHotelDays()).compareTo(o2.unallocatedHotelDays()); 
				}
			});
			for(Client client : clientList) {
				if(quantity > 0 
						&& client.bookItem(closedHotel, (int)quote.getAskPrice())) {
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
			
			if(hotel.getType() == HotelType.GOOD) {
				budget += c.getHotelBonus();
			}
			
			budget /= (c.unallocatedHotelDays());
			// days in the middle of the journey are of higher importance
			// formula: 0.75 +(-0.25*(x-arrivalDay)*(x-departureDay))^0.5
			double dayPosFactor =Math.pow(-0.25 * (hotel.getDay()-c.getArrivalDay()+0.01)*(hotel.getDay()-c.getDepartureDay()-0.01), 0.5);
			budget *=(0.75+dayPosFactor);
					
			// because we won't always pay our max bid we can add a threshold!
			budget *= 1.25;
			// hotels that need connect two days with each other are more important
			if(c.isInBetweenAllocatedDays(hotel)) {
				budget *=1.25;
			}
			budget *= 0.8;
			return budget;
		}
		
		else if(item instanceof EventItem){
			EventItem event = (EventItem) item;
			budget+=c.getBonus(event.getType().getBonusConstant());
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
			// at the end of the game just buy the flights!
			return budget;
		}
	}
}
