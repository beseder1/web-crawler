package com.vincent.webcrawler;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

/**
 * This class defines one web-crawling task that is one retrieval of the used js
 * libraries for a given domain.
 * 
 * @author vincent
 *
 */
public class WebCrawlerTask implements Runnable {

	private String domain;
	private WebCrawler webCrawler;
	private Map<String, Integer> frequenciesMap;
	private AtomicInteger processedDomains;
	private int numberOfDomains;

	private Lock lock;
	private static int TABLE_SIZE = 5;

	public WebCrawlerTask(String domain, Map<String, Integer> frequenciesMap, AtomicInteger processedDomains,
			int numberOfDomains, Lock lock) {
		this.domain = domain;
		this.frequenciesMap = frequenciesMap;
		this.processedDomains = processedDomains;
		this.numberOfDomains = numberOfDomains;
		this.lock = lock;
		webCrawler = new WebCrawler();
	}

	@Override
	public void run() {
		Set<String> libraries = webCrawler.extractJSLibraries(domain);

		// Critical section where the common frequency map is updated.
		lock.lock();
		for (String lib : libraries) {
			if (frequenciesMap.containsKey(lib)) {
				Integer frequency = frequenciesMap.get(lib);
				frequenciesMap.put(lib, frequency + 1);
			}

			else {
				frequenciesMap.put(lib, 1);
			}
		}
		lock.unlock();

		System.out.print(processedDomains.incrementAndGet() + " ");
		if (numberOfDomains == processedDomains.get()) {
			// Since processedDomains is atomic only the last task should access this piece
			// of code. Therefore the non thread safe type of the frequenciesMap should
			// irrelevant.
			System.out.println("\n\nProcess completed!");
			final Map<String, Integer> sortedByFrequency = frequenciesMap.entrySet().stream()
					.sorted(Map.Entry.comparingByValue((e1, e2) -> e2.compareTo(e1))).collect(Collectors
							.toMap(Map.Entry::getKey, Map.Entry::getValue, (e2, e1) -> e1, LinkedHashMap::new));
			printFrequencies(sortedByFrequency);
		}
	}

	/**
	 * The first {@link WebCrawlerTask#TABLE_SIZE} retrieved js library frequencies
	 * are printed here in increasing order.
	 * 
	 * @param frequenciesMap the given frequency map.
	 */
	private void printFrequencies(Map<String, Integer> frequenciesMap) {
		Iterator<Entry<String, Integer>> it = frequenciesMap.entrySet().iterator();
		System.out.println("\nTop " + TABLE_SIZE + " used java script libraries:\n");
		int count = 0;
		while (it.hasNext() && count < TABLE_SIZE) {
			Entry<String, Integer> e = it.next();
			System.out.println(e.getValue() + " - " + e.getKey());
			count++;
		}
	}

}
