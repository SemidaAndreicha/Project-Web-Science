package crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Marik Boswijk
 */
public class WikiCrawler {
    private static final Object key = new Object();
    private ThreadPoolExecutor tPool = new ThreadPoolExecutor(5, 10, 50000L,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    private final byte steps;
    private static Set<String> visitedOut = new HashSet<>();
    private static Set<String> visitedIn = new HashSet<>();
    private Map<String, List<String>> allAdj = new HashMap<>();
    private CountDownLatch latch;

    public WikiCrawler() {
        this((byte) 3);
    }

    public WikiCrawler(byte steps) {
        this("https://en.wikipedia.org/wiki/thing", steps, new CountDownLatch(1));
    }

    private static int c = 0;

    public WikiCrawler(String start, byte steps, CountDownLatch latch) {
        this.steps = (byte) Math.min(steps, 6);
        tPool.submit(() -> {
            try {
                readPageOut(URLDecoder.decode(start, "UTF-8"), (byte) 0);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        });
        this.latch = latch;
        /*tPool.submit(() -> {
            try {
                readPageIn(URLDecoder.decode(start, "UTF-8"), (byte) 0);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        });*/
    }

    public Map<String, List<String>> getAllAdj() {
        return new LinkedHashMap<>(allAdj);
    }

    public static long time;
    private static int counter = 0;

    public static void main(String[] args) {
        time = System.currentTimeMillis();
        WikiCrawler c = new WikiCrawler("https://en.wikipedia.org/wiki/Special:Random", (byte) 0,
                new CountDownLatch(1)
        );
    }

    private void stop() {
        while (!tPool.isShutdown()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        latch.countDown();
        counter++;
        System.out.println("Time: " + (System.currentTimeMillis() - time) + " Latch down: " + counter);
    }

    private void readPageOut(String url, byte step) {
        if (visitedOut.contains(url.substring(30))) return;
        visitedOut.add(url.substring(30));

        Document page;
        synchronized (key) {
            try {
                page = Jsoup.connect(url).get();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        List<String> adjacent = new LinkedList<>(); //List to hold adjacent pages.

        Element main = page.body().select("div#bodyContent").first(); //Get all div tags that match id "bodyContent". Only one could match, so first is chosen.
        if (main == null) return; //Return if no text exists or text couldn't be found.
        Elements links = main.select("a"); //Get all a tags in the text.
        for (Element link : links) {
            String href; //Get url from link.
            try {
                href = URLDecoder.decode(link.attr("href"), page.charset().displayName());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                continue;
            }

            //Go to next link if url doesn't exist, couldn't be found, leaves Wikipedia, or goes to a page type we don't want.
            if (href.equals("") || !href.startsWith("/wiki/") || href.startsWith("/wiki/File:")
                    || href.startsWith("/wiki/Portal:") || href.startsWith("/wiki/Help:")
                    || href.startsWith("/wiki/Template:") || href.startsWith("/wiki/Category:")
                    || href.startsWith("/wiki/Template_talk:") || href.startsWith("/wiki/User_talk:")
                    || href.startsWith("/wiki/Talk:") || href.startsWith("/wiki/Wikipedia:")
                    || href.startsWith("/wiki/Special:") || href.startsWith("/wiki/List_")
                    || href.endsWith("(disambiguation)") || href.startsWith("/wiki/User:")
                    || href.startsWith("/wiki/Category_talk:") || href.contains(":")
                    || href.startsWith("/w/")) continue;

            adjacent.add("https://en.wikipedia.org" + href);

            if (step < steps) {
                tPool.submit(() -> readPageOut("https://en.wikipedia.org" + href, (byte)(step+1))); //Add page to threadpool if next step is within upper bound.
            }
        }

        if (step == steps && tPool.getQueue().isEmpty()) {
            tPool.submit(this::stop);
            tPool.submit(() -> {
                try {
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                tPool.shutdown();
            });
        }

        if (allAdj.containsKey(url)) allAdj.get(url).addAll(adjacent);
        else allAdj.put(url, adjacent);
    }

    /*private void readPageIn(String url, byte step) {
        counter++;

        Document page;
        synchronized (key) {
            try {
                page = Jsoup.connect(
                        "https://en.wikipedia.org/w/index.php?title=Special:WhatLinksHere/"
                                + url.substring(30)
                                + "&limit=5000"
                ).get();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Element main = page.body().select("ul#mw-whatlinkshere-list").first(); //Get all ul tags that match id "mw-whatlinkshere-list". Only one could match, so first is chosen.
        if (main == null) return; //Return if no list exists or list couldn't be found.
        Elements links = main.select("a"); //Get all a tags in the text.
        for (Element link : links) {
            String href; //Get url from link.
            try {
                href = URLDecoder.decode(link.attr("href"), page.charset().displayName());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                continue;
            }

            if (link.text().equals("next 5,000"))
                tPool.submit(() -> readPageIn("https://en.wikipedia.org" + href, (byte)(step+1)));

            //Go to next link if url doesn't exist, couldn't be found, leaves Wikipedia, or goes to a page type we don't want.
            if (href.equals("") || !href.startsWith("/wiki/") || href.startsWith("/wiki/File:")
                    || href.startsWith("/wiki/Portal:") || href.startsWith("/wiki/Help:")
                    || href.startsWith("/wiki/Template:") || href.startsWith("/wiki/Category:")
                    || href.startsWith("/wiki/Template_talk:") || href.startsWith("/wiki/User_talk:")
                    || href.startsWith("/wiki/Talk:") || href.startsWith("/wiki/Wikipedia:")
                    || href.startsWith("/wiki/Special:") || href.startsWith("/wiki/List_")
                    || href.endsWith("(disambiguation)") || href.startsWith("/wiki/User:")
                    || href.startsWith("/wiki/Category_talk:") || href.contains(":")
                    || href.startsWith("/w/")) continue;

            if (step < steps && !visitedIn.contains(href)) {
                tPool.submit(() -> readPageIn("https://en.wikipedia.org" + href, (byte)(step+1))); //Add page to threadpool if next step is within upper bound.
                visitedIn.add(href);
            }

            List<String> adj = new LinkedList<>();
            adj.add(url);
            if (allAdj.containsKey("https://en.wikipedia.org" + href)) allAdj.get("https://en.wikipedia.org" + href).addAll(adj);
            else allAdj.put("https://en.wikipedia.org" + href, adj);
        }

        if (step == steps && tPool.getQueue().isEmpty()) {
            tPool.submit(this::stop);
            tPool.submit(() -> {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                tPool.shutdown();
            });
        }
    }*/
}
