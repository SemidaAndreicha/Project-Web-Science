package application;

import crawler.WikiCrawler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * @author Marik Boswijk
 */
public class Main {
    public static void main(String[] args) {
        List<String> urls = readLinks("D:/school/Year 2/Term 4/Project Web Science/LinkLists/List_of_vital_technology_articles.txt");

        List<WikiCrawler> crawlers = new LinkedList<>();

        CountDownLatch latch = new CountDownLatch(urls.size());

        WikiCrawler.time = System.currentTimeMillis();
        for (String url : urls) {
            WikiCrawler c = new WikiCrawler(url, (byte)0, latch);
            crawlers.add(c);
        }

        System.out.println("URLs: " + urls.size() + " Crawlers: " + crawlers.size() + " Latch counter: " + latch.getCount());

        /*try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        Scanner in = new Scanner(System.in);
        in.nextLine();

        System.out.println("Past the latch.");

        Map<String, Set<String>> adjacenties = new LinkedHashMap<>();
        for (WikiCrawler crawler : crawlers) {
            for (Map.Entry<String, List<String>> node : crawler.getAllAdj().entrySet()) {
                if (adjacenties.containsKey(node.getKey())) adjacenties.get(node.getKey()).addAll(node.getValue());
                else adjacenties.put(node.getKey(), new LinkedHashSet<>(node.getValue()));
            }
        }

        /*WikiCrawler crawler = new WikiCrawler("", (byte) 3);

        Map<String, List<String>> adjacenties = crawler.getAllAdj();*/

        /*Map<String, List<String>> adjacenties = new LinkedHashMap<>();
        List<String> testList = new LinkedList<>();
        testList.add("Test");
        testList.add("Another test");
        testList.add("Bananapancakes");
        adjacenties.put("A", testList);
        adjacenties.put("B", testList);
        adjacenties.put("C", testList);*/

        printResults(adjacenties, "D:/school/Year 2/Term 4/Project Web Science/Adjacencies/Adjacenties(Vital Technology).txt");
    }

    private static List<String> readLinks(String file) {
        List<String> urls = new ArrayList<>();

        try(BufferedReader br = new BufferedReader(new FileReader(file))) {
            //int c = 0;
            for (String line = br.readLine(); line != null/* && c < 2*/; line = br.readLine()) {
                //c++;
                if (!line.equals("") && !line.startsWith("https://en.wikipedia.org/wiki/List_")
                        && !(line.startsWith("https://en.wikipedia.org/wiki/File:")
                        || line.startsWith("https://en.wikipedia.org/wiki/Portal:") || line.startsWith("https://en.wikipedia.org/wiki/Help:")
                        || line.startsWith("https://en.wikipedia.org/wiki/Template:") || line.startsWith("https://en.wikipedia.org/wiki/Category:")
                        || line.startsWith("https://en.wikipedia.org/wiki/Template_talk:") || line.startsWith("https://en.wikipedia.org/wiki/User_talk:")
                        || line.startsWith("https://en.wikipedia.org/wiki/Talk:") || line.startsWith("https://en.wikipedia.org/wiki/Wikipedia:")
                        || line.startsWith("https://en.wikipedia.org/wiki/Special:") || line.startsWith("https://en.wikipedia.org/wiki/List_")
                        || line.endsWith("(disambiguation)") || line.startsWith("https://en.wikipedia.org/wiki/User:")
                        || line.startsWith("https://en.wikipedia.org/wiki/Category_talk:") || line.substring(10).contains(":")
                        || line.startsWith("https://en.wikipedia.org/w/"))) urls.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return urls;
    }

    private static void printResults(Map<String, Set<String>> adjacenties, String file) {
        PrintWriter writer;
        try {
            writer = new PrintWriter(file, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        for (Map.Entry<String, Set<String>> node : adjacenties.entrySet()) {
            writer.println(node.getKey());
            for (String neighbour : node.getValue()) {
                writer.println(neighbour);
            }
            writer.println("-");
        }

        writer.close();
    }
}
