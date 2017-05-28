package analysis;

import analysis.graph.WikiGraph;
import analysis.util.Parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * @author Marik Boswijk
 */
public class Analyzer {
    private static Map<String, Set<String>> pageToPortals = new HashMap<>();
    private static Map<String, Set<String>> portalToPages = new HashMap<>(5);
    private static File folder = new File("D:/School/Year 2/Term 4/Project Web Science/Adjacenties");
    private static String[] allPortals = new String[5];

    public static void main(String[] args) {
        //Get all the files in the folder of adjacenties.
        String[] files = folder.list();
        if (files == null) {
            System.out.println("No files found.");
            return;
        }

        populatePortals();
        populatePortalToPages();

        //Create the graph and populate it, as well as the portalToPages and pageToPortals maps with the data from adjacenties files.
        WikiGraph graph = new WikiGraph();
        for (String file : files) {
            Map<String, Set<String>> portal = Parser.parseMapFile(folder.getAbsolutePath() + '\\' + file);
            if (portal == null) continue;
            String portalName = file.substring(12, file.length() - 5);
            if (!portalToPages.containsKey(portalName)) {
                portalToPages.put(portalName, new HashSet<>());
            }
            for (Map.Entry<String, Set<String>> page : portal.entrySet()) {
                portalToPages.get(portalName).add(page.getKey());
                if (pageToPortals.containsKey(page.getKey())) {
                    pageToPortals.get(page.getKey()).add(portalName);
                } else {
                    Set<String> portals = new HashSet<>(5);
                    portals.add(portalName);
                    pageToPortals.put(page.getKey(), portals);
                }
            }
            graph.addAll(portal);
        }

        //For each page, get all the portals it's in and update the portalToPages and pageToPortals maps accordingly.
        for (String page : graph.getPages()) {
            String[] portals = findPortals(page);
            for (String portal : portals) {
                if (portal == null || portal.equals("")) continue;
                if (!portalToPages.containsKey(portal)) {
                    Set<String> pages = new HashSet<>();
                    pages.add(page);
                    portalToPages.put(portal, pages);
                } else {
                    portalToPages.get(portal).add(page);
                }
            }
            if (pageToPortals.containsKey(page)) {
                pageToPortals.get(page).addAll(Arrays.asList(portals));
            } else {
                Set<String> pagePortals = new HashSet<>(5);
                for (String portal : portals) {
                    if (portal != null && !portal.equals("")) pagePortals.add(portal);
                }
                pageToPortals.put(page, pagePortals);
            }
        }

        //Create the result maps.
        Map<String, Integer> ingoingLinks = new HashMap<>(5);
        Map<String, Integer> outgoingLinks = new HashMap<>(5);
        Map<PortalPair, Integer> pairwiseOutgoing = new TreeMap<>();

        //Create all needed entries in the result maps.
        for (String portal1 : allPortals) {
            for (String portal2 : allPortals) {
                if (portal1.equals(portal2)) continue;
                PortalPair pair = new PortalPair(portal1, portal2);
                pairwiseOutgoing.put(pair, 0);
            }
            ingoingLinks.put(portal1, 0);
            outgoingLinks.put(portal1, 0);
        }

        //For each link in the graph, determine which entries in which result map(s) need(s) to be incremented.
        for (String page : graph.getPages()) {
            Set<String> portals = pageToPortals.get(page);
            for (String link : graph.getAdjacenties(page)) {
                for (String portal : portals) {
                    if (portal == null) continue;
                    if (portalToPages.get(portal).contains(link)) {
                        ingoingLinks.put(portal, ingoingLinks.get(portal) + 1);
                    } else {
                        outgoingLinks.put(portal, outgoingLinks.get(portal) + 1);
                    }
                    if (pageToPortals.containsKey(link)) {
                        for (String portal2 : pageToPortals.get(link)) {
                            if (portal2 == null || portal.equals(portal2)) continue;
                            PortalPair pair = new PortalPair(portal, portal2);
                            pairwiseOutgoing.put(pair, pairwiseOutgoing.get(pair) + 1);
                        }
                    }
                }
            }
        }

        //Write all results to a file.
        try (PrintWriter writer = new PrintWriter("D:/School/Year 2/Term 4/Project Web Science/Data.txt", "UTF-8")) {
            writer.print("Ingoing links:" + System.lineSeparator());
            for (Map.Entry<String, Integer> entry : ingoingLinks.entrySet()) {
                writer.print(entry.getKey() + ": " + entry.getValue() + System.lineSeparator());
            }
            writer.print(System.lineSeparator() + "Outgoing links:" + System.lineSeparator());
            for (Map.Entry<String, Integer> entry : outgoingLinks.entrySet()) {
                writer.print(entry.getKey() + ": " + entry.getValue() + System.lineSeparator());
            }
            writer.print(System.lineSeparator() + "Sizes:" + System.lineSeparator());
            for (Map.Entry<String, Set<String>> entry : portalToPages.entrySet()) {
                writer.print(entry.getKey() + ": " + entry.getValue().size() + System.lineSeparator());
            }
            writer.print(System.lineSeparator() + "In/out ratios:" + System.lineSeparator());
            for (String portal : allPortals) {
                writer.print(portal + ": " + ((double)ingoingLinks.get(portal)) / ((double)outgoingLinks.get(portal))
                        + System.lineSeparator()
                );
            }
            writer.print(System.lineSeparator() + "Links between:" + System.lineSeparator());
            for (Map.Entry<PortalPair, Integer> entry : pairwiseOutgoing.entrySet()) {
                writer.print(entry.getKey().toString() + ": " + entry.getValue() + System.lineSeparator());
            }
            writer.print(System.lineSeparator() + "Tospecific/in ratios:" + System.lineSeparator());
            for (Map.Entry<PortalPair, Integer> entry : pairwiseOutgoing.entrySet()) {
                writer.print(entry.getKey().toString() + ": " +
                        ((double)entry.getValue()) / ((double)ingoingLinks.get(entry.getKey().getPortal1()))
                        + System.lineSeparator()
                );
            }
            writer.print(System.lineSeparator() + "Tospecific/out ratios:" + System.lineSeparator());
            for (Map.Entry<PortalPair, Integer> entry : pairwiseOutgoing.entrySet()) {
                writer.print(entry.getKey().toString() + ": " +
                        ((double)entry.getValue()) / ((double)outgoingLinks.get(entry.getKey().getPortal1()))
                        + System.lineSeparator()
                );
            }
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Puts all 5 portal names into the allPortals array.
     */
    private static void populatePortals() {
        allPortals[0] = "Algorithms";
        allPortals[1] = "Mathematics";
        allPortals[2] = "Physical Sciences";
        allPortals[3] = "Statistics";
        allPortals[4] = "Technology";
    }

    /**
     * Puts all portals and the urls in said portal (obtained from a file) into the portalToPages map.
     */
    private static void populatePortalToPages() {
        for (String portal : allPortals) {
            Set<String> set = Parser.parseSetFile(
                    "D:/School/Year 2/Term 4/Project Web Science/Portals/" + portal + ".txt"
            );
            portalToPages.put(portal, set == null ? new HashSet<>() : set);
        }
    }

    /**
     * Finds all portals which contain a given page.
     *
     * @param page Page to find the portals of
     * @return Array of all the portals which contain page.
     */
    private static String[] findPortals(String page) {
        String[] portals = new String[5];
        int i = 0;
        for (Map.Entry<String, Set<String>> entry : portalToPages.entrySet()) {
            if (entry.getValue().contains(page)) portals[i++] = entry.getKey();
        }
        return portals;
    }

    /**
     * Represents a directed pair of portals.
     */
    private static class PortalPair implements Comparable<PortalPair> {
        private String stillAlive;
        private String wantYouGone;

        private PortalPair(String p1, String p2) {
            stillAlive = p1;
            wantYouGone = p2;
        }

        private String getPortal1() {
            return stillAlive;
        }

        private String getPortal2() {
            return wantYouGone;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PortalPair that = (PortalPair) o;

            return stillAlive.equals(that.stillAlive) && wantYouGone.equals(that.wantYouGone);
        }

        @Override
        public int hashCode() {
            int result = stillAlive.hashCode();
            result = 31 * result + wantYouGone.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return stillAlive + " -> " + wantYouGone;
        }

        @Override
        public int compareTo(PortalPair o) {
            return stillAlive.equals(o.stillAlive) ? wantYouGone.compareToIgnoreCase(o.wantYouGone) : stillAlive.compareToIgnoreCase(o.stillAlive);
        }
    }
}
