package crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.PriorityQueue;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Marik Boswijk
 */
public class WikiCrawler {
    private static final Object key = new Object();
    private PriorityQueue<UrlPriority> urls;
    private ThreadPoolExecutor tPool;
    private byte steps;

    private void readPage(String url, byte step) {
        //Get document from url. Make thread sleep so only 1 page can be received per second, as demanded by Wikipedia.
        Document doc;
        synchronized (key) {
            try {
                doc = Jsoup.connect(url).get();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Element main = doc.body().select("div#bodyContent").first(); //Get all div tags that match id "bodyContent". Only one could match, so first is chosen.
        if (main == null) return; //Return if no text exists or text couldn't be found.
        Elements links = main.select("a"); //Get all a tags in the body.
        for (Element link : links) {
            String href = link.attr("href"); //Get url from link.
            if (href.equals("") || !href.startsWith("/wiki/")) continue; //Go to next link if url doesn't exist or couldn't be found or leaves Wikipedia.
            if (step < steps) urls.add(new UrlPriority("https://en.wikipedia.org" + href, (byte)(step+1)));
        }
    }

    private class UrlPriority {
        private String url;
        private int priority;
        private byte step;

        private UrlPriority(String url, byte step) {
            this.url = url;
            this.priority = calcPriority(url);
            this.step = step;
        }

        private int calcPriority(String url) {
            //TODO: Insert greedy algorithm.
            return 0;
        }
    }
}
