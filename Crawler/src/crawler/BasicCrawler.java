package crawler;

import java.io.IOException;
import java.util.HashSet;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
/**
 * @made by Tal Buaron 
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
	 * @param URL - url to start from
	 * @param depth - the maximum depth you want to go
	 */
	public void findPage(String URL, int depth) {
		while ((!pagesVisited.contains(URL) && (depth < MAX_DEPTH))) { //while page wasn't visited and depth is still in condition, keep going
			try {
				Document document = Jsoup.connect(URL).get(); //connect to the url
				Elements linksOnPage = document.select("a[href]"); //select all the links from that url
				String[] titles = document.title().split(","); //array of strings of all titles
				
				for (String title : titles) { //for every title in titles print the title and depth
					
					System.out.println(title + " depth: " + depth);
					
					if (title.equalsIgnoreCase(other)) { //if the title match the page we look for, print found and degrees of seperation
						System.out.println("Found page " + title + " in " + depth + " Degrees of seperation");
						System.exit(0); //stop the operation
					}
				}
				
				depth++; //increment the depth everytime you go deeper from starting page
				for (Element page : linksOnPage) { //for every page in the links we found
					findPage(page.attr("abs:href"), depth); //do recursion 
				}
				
			} catch (IOException e) {
				System.err.println("For '" + URL + "': " + e.getMessage());
			}
		}

	}

	public static void main(String[] args) {
		new BasicCrawler().findPage("https://en.wikipedia.org/wiki/Engineer", 0);
	}

}
