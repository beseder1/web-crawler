package com.vincent.webcrawler;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * This class forms the engine for web crawling for java script libraries. It
 * uses multithreading in order to increase the velocity/performance.
 * 
 * @author vincent
 *
 */
public class WebCrawlerEngine {

	// Static constants
	public final static String HTTPS_PROTOCOL = "https://";
	private final static String GOOGLE_SEARCH_URL = "https://www.google.com/search";
	public final static String USER_AGENT = "Mozilla/5.0";
	private final static String HYPERLINK = "a[href]";

	// This regular expression I looked up on the internet.
	private final static Pattern DOMAIN_NAME_PATTERN = Pattern
			.compile("([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,6}");

	// The number of threads. The best performing one needs to be explored.
	private final static short N_THREADS = 10;

	// The map which stores the frequency of the libraries. Since I am using a
	// non thread safe type since the lock
	// should guarantee thread safety for the critical section part.
	private Map<String, Integer> frequenciesMap = new HashMap<>();
	private Lock lock = new ReentrantLock();

	// This basically counts the number of processed domains. It also needs to be
	// thread save.
	private AtomicInteger processedDomains = new AtomicInteger(0);

	public void findMostUsedJSLibraries(String searchTerm, int numberOfResults) {
		// First execute the google search.
		final Elements results = googleSearch(searchTerm, numberOfResults);
		if (results == null) {
			System.out.println("The google search with parameters (searchTerm, numberOfResults) = " + "(" + searchTerm
					+ "," + numberOfResults + ") produced 0 results.");
			return;
		}
		// Extract the found domains from the results. For safety reasons only
		// HTTPS-URLs are considered.
		final Set<String> domains = extractDomains(results, "href", "/url?q=" + HTTPS_PROTOCOL);

		// Save the number of found domains in order to have an abort condition for the
		// thread pool and also for information since it does not necessarily match the
		// user input "numberOfResults".
		final int numberOfDomains = domains.size();
		System.out.println("Number of found https domains matching " + searchTerm + ": " + numberOfDomains);

		// Create a thread pool for the configured number of threads.
		final ExecutorService pool = Executors.newFixedThreadPool(N_THREADS);

		System.out.println("Number of processed https domains: ");

		// Create a task for each domain to extract js libraries from.
		for (String domain : domains) {
			pool.execute(new WebCrawlerTask(domain, frequenciesMap, processedDomains, numberOfDomains, lock));
		}

		// Shutdown the pool properly.
		pool.shutdown();
	}

	/**
	 * Extract domains which lie in specified attributes and start with a specified
	 * prefix.
	 * 
	 * @param results      to extract domains from
	 * @param attributeKey the attribute key in order to identify the domains
	 * @param prefix       the prefix the domains should start with
	 * @return the extracted domains in a set
	 */
	private Set<String> extractDomains(Elements results, String attributeKey, String prefix) {
		Set<String> domains = new HashSet<>();
		for (Element result : results) {
			String attrValue = result.attr(attributeKey);
			if (attrValue.startsWith(prefix)) {
				String domainName = extractDomainName(attrValue);
				domains.add(domainName);
			}
		}
		return domains;
	}

	/**
	 * Extracts the domain name from a given url.
	 * 
	 * @param url the given url
	 * @return the extracted domain
	 */
	public static String extractDomainName(String url) {
		String domainName = "";
		Matcher matcher = DOMAIN_NAME_PATTERN.matcher(url);
		if (matcher.find()) {
			domainName = matcher.group(0).toLowerCase().trim();
		}
		return domainName;
	}

	/**
	 * Executes a google search according to the given search term and the number of
	 * desired results.
	 * 
	 * @param searchTerm      the term to search for
	 * @param numberOfResults the number of results that should be found by google
	 * @return the result of the desired google search
	 */
	private Elements googleSearch(String searchTerm, int numberOfResults) {
		String searchURL = GOOGLE_SEARCH_URL + "?q=" + searchTerm + "&num=" + numberOfResults;

		Document doc;
		try {
			doc = Jsoup.connect(searchURL).userAgent(USER_AGENT).get();
			return doc.select(HYPERLINK);

		} catch (IOException e) {
			System.out.println("Searching for " + searchTerm + " with " + numberOfResults
					+ " desired results ended up with an error.");
		}

		// Nothing was found.
		return null;
	}
}
