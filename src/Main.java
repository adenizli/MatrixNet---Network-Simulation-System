
/*
 * MatrixNet
 *
 * This project assumes the universe is deterministic,
 * except for edge cases.
 * 
 *   ┌─┐┌─┐┌┐┌
 *   │  ├─┤│││
 *   └─┘┴ ┴┘└┘
 * 
 * Written with respect to Can Erer for their best efforts :)
 * 
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class Main {
    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        Network network = new Network();
        Service service = new Service(network);
        Controller controller = new Controller(service);
        if (args.length != 2) {
            System.err.println("Usage: java Main <input_file> <output_file>");
            System.exit(1);
        }

        String inputFile = args[0];
        String outputFile = args[1];

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
                BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    processCommand(controller, line, writer);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading/writing files: " + e.getMessage());
        }

    }

    private static void processCommand(Controller controller, String commandLine, BufferedWriter writer) throws IOException {
        String[] tokens = splitWhitespace(commandLine);
        String response = controller.handleRequest(tokens);
        writer.write(response);
        writer.newLine();
    }

    /**
     * Faster alternative to commandLine.split("\\s+"), avoids regex overhead on
     * each line.
     */
    private static String[] splitWhitespace(String s) {
        int n = s.length();
        ArrayList<String> parts = new ArrayList<>();
        int i = 0;

        while (i < n) {
            while (i < n && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
            if (i >= n) {
                break;
            }
            int j = i;
            while (j < n && !Character.isWhitespace(s.charAt(j))) {
                j++;
            }
            parts.add(s.substring(i, j));
            i = j;
        }

        return parts.toArray(new String[0]);
    }
}
