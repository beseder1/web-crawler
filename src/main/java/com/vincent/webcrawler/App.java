package com.vincent.webcrawler;

import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Entry point of the web-crawler.
 * 
 * @author vincent
 *
 */
public class App {
	public static void main(String[] args) {
		// The user can input search parameters using stdin.
		Scanner scanner = new Scanner(System.in);

		System.out.println("Please enter a search term.");
		String searchTerm = scanner.nextLine();

		System.out.println("Please enter the number of desired search results.");
		int numberOfResults = 0;

		// This part tries to catch wrong inputs for the desired number of results, e.g.
		// no numbers.
		boolean wrongInput;
		do {
			try {
				numberOfResults = scanner.nextInt();
				wrongInput = false;
			}

			catch (NoSuchElementException e) {
				wrongInput = true;
				System.out.println("The number of desired search results needs to be a number!");
				scanner.nextLine();
			}
		} while (wrongInput);
		scanner.close();

		// This start the web-crawling for java script libraries.
		WebCrawlerEngine webCrawlerEngine = new WebCrawlerEngine();
		webCrawlerEngine.findMostUsedJSLibraries(searchTerm, numberOfResults);
	}
}
