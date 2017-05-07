package crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Marik Boswijk
 */
public class WikiCrawler {
    private static final Object key = new Object();
    private ThreadPoolExecutor tPool = new ThreadPoolExecutor(5, 10, 50000L, TimeUnit.MILLISECONDS,
            new PriorityBlockingQueue<>(11, (o1, o2) -> {
                if (!(o1 instanceof WikiPage) || !(o2 instanceof WikiPage))
                    return 0;
                return ((WikiPage) o1).compareTo((WikiPage) o2);
            }));
    private final byte steps;
    private final String end;
    private List<String> sequence = null;
    private Set<String> visited = new HashSet<>();

    public WikiCrawler() {
        this((byte) 3);
    }

    public WikiCrawler(byte steps) {
        this("https://en.wikipedia.org/wiki/thing", steps, "https://en.wikipedia.org/wiki/New_Haven,_Connecticut");
    }

    public WikiCrawler(String start, byte steps, String end) {
        this.steps = (byte) Math.min(steps, 6);
        synchronized (key) {
            try {
                tPool.submit(new WikiPage(start, Jsoup.connect(start).get(), (byte) 0, new LinkedList<>()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.end = end;
    }

/*
* private void readPage(String url, byte step) { //Get document from url.
* Make thread sleep so only 1 page can be received per second, as demanded
* by Wikipedia. Document doc; synchronized (key) { try { doc =
* Jsoup.connect(url).get(); } catch (IOException e) { e.printStackTrace();
* return; } try { Thread.sleep(1000); } catch (InterruptedException e) {
* e.printStackTrace(); } }
*
* Element main = doc.body().select("div#bodyContent").first(); //Get all
* div tags that match id "bodyContent". Only one could match, so first is
* chosen. if (main == null) return; //Return if no text exists or text
* couldn't be found. Elements links = main.select("a"); //Get all a tags in
* the text. for (Element link : links) { String href = link.attr("href");
* //Get url from link. if (href.equals("") || !href.startsWith("/wiki/"))
* continue; //Go to next link if url doesn't exist or couldn't be found or
* leaves Wikipedia. if (step < steps) urls.add(new
* WikiPage("https://en.wikipedia.org" + href, (byte)(step+1))); //Add page
* to priority queue if next step is within upper bound. } }
*/

    private static long time;
    private static int counter = 0;

    public static void main(String[] args) {
        time = System.currentTimeMillis();
        WikiCrawler c = new WikiCrawler("https://en.wikipedia.org/wiki/Jakob_Jakobeus", (byte) 1,
                "https://en.wikipedia.org/wiki/Lee_Chiao-ju");
    }

    private static void stop() {
        System.out.println("Time: " + (System.currentTimeMillis() - time) + " Pages: " + counter);
    }

    private class WikiPage implements Comparable<WikiPage>, Runnable {
        private String url;
        private Document doc;
        private int priority;
        private byte step;
        private List<String> previous;

        private WikiPage(String url, Document doc, byte step, List<String> previous) {
            this.url = url;
            this.doc = doc;
            this.priority = calcPriority();
            this.step = step;
            this.previous = previous;
        }

        private int calcPriority() {
            if (step >= steps) return Integer.MIN_VALUE;
            Element mainText = doc.select("div#bodyContent").first();
            if (mainText == null)
                return Integer.MIN_VALUE;
            Elements links = mainText.select("a");
            return links.size();
        }

        @Override
        public int compareTo(WikiPage o) {
            return o.priority - this.priority;
        }

        @Override
        public void run() {
            counter++;

            //Make sure this hasn't been visited yet. Otherwise, add it to visited.
            /*if (visited.contains(url)) return;
            visited.add(url);*/

            //Get document from url. Make thread sleep so only 1 page can be received per second, as demanded by Wikipedia.
            /*Document doc;
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
            }*/

            previous.add(doc.title());

            //Check if the current page is the one being searched for.
            if (url.equalsIgnoreCase(end)) {
                if (sequence == null || sequence.size() > previous.size()) sequence = previous;
                return;
            }

            Element main = doc.body().select("div#bodyContent").first(); //Get all div tags that match id "bodyContent". Only one could match, so first is chosen.
            if (main == null) return; //Return if no text exists or text couldn't be found.
            Elements links = main.select("a"); //Get all a tags in the text.
            for (Element link : links) {
                if (step > steps) break;
                String href = link.attr("href"); //Get url from link.
                if (href.equals("") || !href.startsWith("/wiki/")) continue; //Go to next link if url doesn't exist or couldn't be found or leaves Wikipedia.
                try {
                    href = URLDecoder.decode(href, doc.charset().displayName()); //Translate unicode to actual characters using charset of page.
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                if (visited.contains(href)) continue;
                visited.add(href);

                Document linkPage;
                synchronized (key) {
                    try {
                        linkPage = Jsoup.connect("https://en.wikipedia.org" + href).get();
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                    System.out.println(href + step);
                    /*try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }*/
                }
                tPool.submit(new WikiPage("https://en.wikipedia.org" + href, linkPage, (byte)(step+1), previous)); //Add page to threadpool if next step is within upper bound.
            }

            if (tPool.getQueue().isEmpty()) {
                stop();
                //tPool.shutdown();
            }
        }
    }
}
