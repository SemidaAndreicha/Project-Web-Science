package crawler;

import java.io.IOException;
import java.util.HashSet;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
/**
 * @author Tal Buaron
 */
public class BasicCrawler {
	private static final int MAX_DEPTH = 2; //max depth
	private HashSet<String> pagesVisited; //set of pages visited
	private String other = "Latin - Wikipedia"; //the page title to look for
	
	//constructor
	public BasicCrawler() {
		pagesVisited = new HashSet<>();
	}
	
	/**
	 * the crawler takes two parameters:
	 * @param url - url to start from
	 * @param depth - the depth the crawler is at
	 */
	public void findPage(String url, int depth) {
		if ((!pagesVisited.contains(url) && (depth < MAX_DEPTH))) { //while page wasn't visited and depth is still in condition, keep going
			try {
				pagesVisited.add(url);
				Document document = Jsoup.connect(url).get(); //connect to the url
				Elements linksOnPage = document.select("a"); //select all the links from that url
				String title = document.title(); //array of strings of all titles

				System.out.println(title + " depth: " + depth);
					
				if (title.equalsIgnoreCase(other)) { //if the title match the page we look for, print found and degrees of separation
					System.out.println("Found page " + title + " in " + depth
							+ (depth == 1 ? " degree of separation" : " degrees of separation")
					);
					return;
				}
				
				//depth++; //increment the depth every time you go deeper from starting page
				for (Element page : linksOnPage) { //for every page in the links we found
					String href = page.attr("href");
					if (href.startsWith("/wiki/")) findPage("https://en.wikipedia.org" + href, depth + 1); //do recursion
				}

			} catch (IOException e) {
				System.err.println("For '" + url + "': " + e.getMessage());
			}
		}

	}

	public static void main(String[] args) {
		new BasicCrawler().findPage("https://en.wikipedia.org/wiki/Engineer", 0);
	}

}
