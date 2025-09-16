
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

public class solver {

    /**
     * A simple class to hold our (x, y) points using BigInteger.
     */
    public static class Point {

        final BigInteger x;
        final BigInteger y;

        Point(BigInteger x, BigInteger y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "Point(" + x + ", " + y + ")";
        }
    }

    public static void main(String[] args) {
        // Check if a filename was provided as a command-line argument
        if (args.length == 0) {
            System.err.println("Error: Please provide the input JSON filename as an argument.");
            System.err.println("Usage: mvn exec:java -Dexec.mainClass=\"Solver\" -Dexec.args=\"your_file.json\"");
            return;
        }
        String filename = args[0];

        try {
            // 1. Read the specified JSON file
            String content = new String(Files.readAllBytes(Paths.get(filename)));
            JSONObject json = new JSONObject(content);

            // 2. Extract k and parse the shares
            int k = json.getJSONObject("keys").getInt("k");
            System.out.println("Threshold (k) is: " + k);

            List<Point> points = new ArrayList<>();
            for (String key : json.keySet()) {
                if (!key.equals("keys")) {
                    JSONObject share = json.getJSONObject(key);
                    BigInteger x = new BigInteger(key);
                    int base = Integer.parseInt(share.getString("base"));
                    String valueStr = share.getString("value");
                    BigInteger y = new BigInteger(valueStr, base);
                    points.add(new Point(x, y));
                }
            }

            // 3. Sort points by x-coordinate to ensure deterministic results
            points.sort(Comparator.comparing(p -> p.x));

            // 4. Select the first k points for the calculation
            List<Point> pointsForCalculation = points.subList(0, k);
            System.out.println("Using the first " + k + " sorted points for calculation:");
            pointsForCalculation.forEach(System.out::println);

            // 5. Reconstruct the secret using Lagrange Interpolation
            BigInteger secret = calculateSecret(pointsForCalculation, k);

            // 6. Print the final result
            System.out.println("\n------------------------------------");
            System.out.println("✅ The calculated secret is: " + secret);
            System.out.println("------------------------------------");

        } catch (IOException e) {
            // Specifically for file-related errors
            System.err.println("Error reading file '" + filename + "': " + e.getMessage());
        } catch (JSONException e) {
            // Specifically for errors parsing the JSON
            System.err.println("Error parsing JSON in '" + filename + "': " + e.getMessage());
        } catch (NumberFormatException e) {
            // Specifically for errors converting numbers (e.g., a bad 'base' value)
            System.err.println("Error with a number in the JSON file: " + e.getMessage());
        } catch (ArithmeticException e) {
            // Specifically for division-by-zero errors in the calculation
            System.err.println("Calculation error: " + e.getMessage());
        } catch (Exception e) {
            // A final catch-all for any other unexpected errors
            System.err.println("An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Calculates P(0) using Lagrange Interpolation to find the secret. P(0) =
     * sum of [ y_j * l_j(0) ] for j = 0 to k-1 where l_j(0) = product of [
     * (-x_m) / (x_j - x_m) ] for m != j
     */
    public static BigInteger calculateSecret(List<Point> points, int k) {
        BigInteger sum = BigInteger.ZERO;

        for (int j = 0; j < k; j++) {
            BigInteger xj = points.get(j).x;
            BigInteger yj = points.get(j).y;

            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;

            for (int m = 0; m < k; m++) {
                if (j == m) {
                    continue; // Skip when m equals j
                }
                BigInteger xm = points.get(m).x;
                numerator = numerator.multiply(xm.negate());
                denominator = denominator.multiply(xj.subtract(xm));
            }

            BigInteger lagrangeBasis = numerator.divide(denominator);
            BigInteger term = yj.multiply(lagrangeBasis);
            sum = sum.add(term);
        }
        return sum;
    }
}
