package analysis.graph;

import java.util.*;

/**
 * @author Marik Boswijk
 */
public class WikiGraph {
    private Map<String, Set<String>> adjacenties;
    private Set<String> pages = new LinkedHashSet<>();

    public WikiGraph() {
        this(new HashMap<>());
    }

    public WikiGraph(Map<String, Set<String>> adjacenties) {
        for (Map.Entry<String, Set<String>> page : adjacenties.entrySet()) {
            pages.add(page.getKey());
            for (String url : page.getValue()) {
                if (!adjacenties.containsKey(url)) {
                    adjacenties.put(url, new HashSet<>());
                }
                pages.add(url);
            }
        }
        this.adjacenties = adjacenties;
    }

    public void addAll(Map<String, Set<String>> adjacenties) {
        for (Map.Entry<String, Set<String>> page : adjacenties.entrySet()) {
            pages.add(page.getKey());
            if (this.adjacenties.containsKey(page.getKey())) {
                this.adjacenties.get(page.getKey()).addAll(page.getValue());
            } else {
                this.adjacenties.put(page.getKey(), page.getValue());
            }
            for (String url : page.getValue()) {
                if (!this.adjacenties.containsKey(url)) {
                    this.adjacenties.put(url, new HashSet<>());
                }
                pages.add(url);
            }
        }
    }

    public Set<String> getAdjacenties(String page) {
        return new HashSet<>(adjacenties.get(page));
    }

    public Set<String> getPages() {
        return new LinkedHashSet<>(pages);
    }

    @Override
    public String toString() {
        return "WikiGraph{" +
                "adjacenties=" + adjacenties +
                '}';
    }
}
