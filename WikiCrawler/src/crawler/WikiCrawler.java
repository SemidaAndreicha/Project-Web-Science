package crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Marik Boswijk
 */
public class WikiCrawler {
    private static final Object key = new Object();
    private ThreadPoolExecutor tPool = new ThreadPoolExecutor(5, 10,
            50000L, TimeUnit.MILLISECONDS, new PriorityBlockingQueue<>(11,
            (o1, o2) -> {
                if (!(o1 instanceof UrlPriority) || !(o2 instanceof UrlPriority)) return 0;
                return ((UrlPriority) o1).compareTo(((UrlPriority) o2));
            }));
    private byte steps = 3;

    /*private void readPage(String url, byte step) {
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
        Elements links = main.select("a"); //Get all a tags in the text.
        for (Element link : links) {
            String href = link.attr("href"); //Get url from link.
            if (href.equals("") || !href.startsWith("/wiki/")) continue; //Go to next link if url doesn't exist or couldn't be found or leaves Wikipedia.
            if (step < steps) urls.add(new UrlPriority("https://en.wikipedia.org" + href, (byte)(step+1))); //Add page to priority queue if next step is within upper bound.
        }
    }*/

    private class UrlPriority implements Comparable<UrlPriority>, Runnable {
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

        @Override
        public int compareTo(UrlPriority o) {
            return o.priority - this.priority;
        }

        @Override
        public void run() {
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
            Elements links = main.select("a"); //Get all a tags in the text.
            for (Element link : links) {
                String href = link.attr("href"); //Get url from link.
                if (href.equals("") || !href.startsWith("/wiki/")) continue; //Go to next link if url doesn't exist or couldn't be found or leaves Wikipedia.
                if (step < steps) tPool.submit(new UrlPriority("https://en.wikipedia.org" + href, (byte)(step+1))); //Add page to threadpool if next step is within upper bound.
            }
        }
    }
}
