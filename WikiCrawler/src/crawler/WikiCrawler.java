package crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
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
    private Set<String> visitedOut = new HashSet<>();
    private Set<String> visitedIn = new HashSet<>();
    private Map<String, List<String>> allAdj = new HashMap<>();

    public WikiCrawler() {
        this((byte) 3);
    }

    public WikiCrawler(byte steps) {
        this("https://en.wikipedia.org/wiki/thing", steps);
    }

    public WikiCrawler(String start, byte steps) {
        this.steps = (byte) Math.min(steps, 6);
        tPool.submit(() -> {
            try {
                readPageOut(URLDecoder.decode(start, "UTF-8"), (byte) 0);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        });
        tPool.submit(() -> {
            try {
                readPageIn(URLDecoder.decode(start, "UTF-8"), (byte) 0);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        });
    }

    public Map<String, List<String>> getAllAdj() {
        return new LinkedHashMap<>(allAdj);
    }

    private static long time;
    private static int counter = 0;

    public static void main(String[] args) {
        time = System.currentTimeMillis();
        WikiCrawler c = new WikiCrawler("https://en.wikipedia.org/wiki/Special:Random", (byte) 0);
    }

    private void stop() {
        System.out.println("Time: " + (System.currentTimeMillis() - time) + " Pages: " + counter);
    }

    private void readPageOut(String url, byte step) {
        counter++;
        System.out.println(url.substring(30));

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
                    || href.startsWith("/wiki/Template_talk:") || href.startsWith("/w/")) continue;

            adjacent.add("https://en.wikipedia.org" + href);

            if (visitedOut.contains(href)) continue;

            if (step < steps) {
                tPool.submit(() -> readPageOut("https://en.wikipedia.org" + href, (byte)(step+1))); //Add page to threadpool if next step is within upper bound.
                visitedOut.add(href);
            }
        }

        if (step == steps && tPool.getQueue().isEmpty()) {
            stop();
            if (!tPool.isShutdown()) tPool.shutdown();
        }

        if (allAdj.containsKey(url)) allAdj.get(url).addAll(adjacent);
        else allAdj.put(url, adjacent);
    }

    private void readPageIn(String url, byte step) {
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

            //Go to next link if url doesn't exist, couldn't be found, leaves Wikipedia, or goes to a page type we don't want.
            if (href.equals("") || !href.startsWith("/wiki/") || href.startsWith("/wiki/File:")
                    || href.startsWith("/wiki/Portal:") || href.startsWith("/wiki/Help:")
                    || href.startsWith("/wiki/Template:") || href.startsWith("/wiki/Category:")
                    || href.startsWith("/wiki/Template_talk:") || href.startsWith("/w/")) continue;

            if (visitedIn.contains(href)) continue;

            if (step < steps) {
                tPool.submit(() -> readPageIn("https://en.wikipedia.org" + href, (byte)(step+1))); //Add page to threadpool if next step is within upper bound.
                visitedIn.add(href);
            }

            List<String> adj = new LinkedList<>();
            adj.add(url);
            if (allAdj.containsKey("https://en.wikipedia.org" + href)) allAdj.get("https://en.wikipedia.org" + href).addAll(adj);
            else allAdj.put("https://en.wikipedia.org" + href, adj);
        }

        if (step == steps && tPool.getQueue().isEmpty()) {
            stop();
            if (!tPool.isShutdown()) tPool.shutdown();
        }
    }
}
