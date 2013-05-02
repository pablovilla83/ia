/**
 * 
 */
package com.teamtaco.crap;

import twitter4j.AsyncTwitter;
import twitter4j.AsyncTwitterFactory;
import twitter4j.Status;
import twitter4j.TwitterAdapter;
import twitter4j.TwitterException;
import twitter4j.TwitterMethod;
import twitter4j.conf.ConfigurationBuilder;

/**
 * @author Frederik
 *
 */
public class Tweeter {

	public static void tweet(String message) {
		try {
			ConfigurationBuilder cb = new ConfigurationBuilder();
			cb.setDebugEnabled(true)
			.setOAuthConsumerKey("rnRyUbRVWZlzwqkxa5fWTw")
			.setOAuthConsumerSecret("3u4NYDVeSN6dXijY1Y3KGMEliS2RKAzOPWfHedaWOw")
			.setOAuthAccessToken("1396207994-AMzfRLdyK9rGtIsLbSejcbeVJLF9TLohJ9JIFqB")
			.setOAuthAccessTokenSecret("LIZSwV9JVBsR0hvP1EcHTNpCF4NVV47to2U5ZJig");
			
			AsyncTwitterFactory factory = new AsyncTwitterFactory(cb.build());
			AsyncTwitter twitter = factory.getInstance();
			twitter.addListener(new TwitterAdapter() {
				@Override
				public void updatedStatus(Status status) {
					System.out.println("Updated twitter to " + status.getText());
				}
				
				@Override
				public void onException(TwitterException e, TwitterMethod method) {
					if(method == TwitterMethod.UPDATE_STATUS) {
						System.err.println(e.getMessage());
					}
				}
			});
			twitter.updateStatus(message);
		} catch(Exception e) {
			System.err.println(e.getMessage());
		}
	}
}
