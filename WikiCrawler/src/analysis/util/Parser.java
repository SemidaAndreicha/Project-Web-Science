package analysis.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

/**
 * @author Marik Boswijk
 */
public class Parser {
    public static Map<String, Set<String>> parseMapFile(String fileLocation) {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileLocation))) {
            Map<String, Set<String>> output = new HashMap<>();
            for (String line = reader.readLine(); line != null && !line.equals(""); line = reader.readLine()) {
                String node = line;
                Set<String> adjacent = new HashSet<>();
                for (line = reader.readLine(); !line.equals("-"); line = reader.readLine()) {
                    adjacent.add(line);
                }
                output.put(node, adjacent);
            }
            return output;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Set<String> parseSetFile(String fileLocation) {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileLocation))) {
            Set<String> output = new HashSet<>();
            for (String line = reader.readLine(); line != null && !line.equals(""); line = reader.readLine()) {
                if (line.startsWith("https://")) output.add(line);
            }
            return output;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
