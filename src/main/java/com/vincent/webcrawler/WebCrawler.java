package com.vincent.webcrawler;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * This class defines the core of the web-crawling mechanism retrieving the used
 * js libraries on a given domain.
 * 
 * @author vincent
 *
 */
public class WebCrawler {

	// This is a simple approach in order to deduplicate the found js libraries.
	private static final String[] KNOWN_JS_LIBRARIES = { "jquery", "react", "angular", "vue" };

	// Technical constants
	private static final String SCRIPT_IDENTIFIER = "script[src]";
	private static final String SCRIPT_KEY = "src";

	/**
	 * Extracts the java script libraries used on the given domain.
	 * 
	 * @param domain the given domain.
	 * @return returns the found js libraries in a set. This tackles duplication
	 *         inside the html of the given domain.
	 */
	public Set<String> extractJSLibraries(String domain) {
		Document doc;
		Set<String> libraries = new HashSet<>();
		String url = WebCrawlerEngine.HTTPS_PROTOCOL + domain;

		try {
			doc = Jsoup.connect(url).userAgent(WebCrawlerEngine.USER_AGENT).get();
			extractJSLibraries(doc, libraries);

		} catch (IOException e) {
			System.out.println("Opening " + url + " ended up with an error.");
		}

		return libraries;
	}

	/**
	 * Core method to retrieve js libraries from a given (html) doc.
	 * 
	 * @param doc       the given document
	 * @param libraries the libraries to add the retrieved libraries to.
	 */
	private void extractJSLibraries(Document doc, Set<String> libraries) {
		Elements results = doc.select(SCRIPT_IDENTIFIER);
		for (Element result : results) {
			String libraryURL = result.attr(SCRIPT_KEY).toLowerCase();
			// I added this in order to guarantee the retrieval of js scripts. Might even be
			// unnecessary.
			if (!libraryURL.matches(".*\\.js.*")) {
				continue;
			}
			mapToLibrary(libraryURL, libraries);
		}
	}

	/**
	 * Maps a given libraryURL to a human readable library name, e.g. JQuery, and
	 * adds it to the library set.
	 * 
	 * @param libraryURL the given library url
	 * @param libraries  set of libraries where the library should be added to.
	 */
	private void mapToLibrary(String libraryURL, Set<String> libraries) {
		for (String lin : KNOWN_JS_LIBRARIES) {
			if (libraryURL.matches(".*" + lin + ".*")) {
				libraries.add(lin);
				return;
			}
		}
		libraries.add(libraryURL);
	}
}
