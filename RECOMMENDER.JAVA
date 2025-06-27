import java.io.*;
import java.util.*;

public class Recommender {
    private static Map<Integer, Map<Integer, Double>> userRatings = new HashMap<>();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter user ID to get recommendations: ");
        int userId = scanner.nextInt();

        System.out.print("Enter number of recommendations to retrieve: ");
        int numRecommendations = scanner.nextInt();

        String filePath = "ratings.csv";

        if (!new File(filePath).exists()) {
            createSampleData(filePath);
        }

        loadRatings(filePath);
        Map<Integer, Double> recommendations = recommend(userId, numRecommendations);

        if (recommendations.isEmpty()) {
            System.out.println("No recommendations available for user " + userId);
        } else {
            for (Map.Entry<Integer, Double> entry : recommendations.entrySet()) {
                System.out.printf("Recommended item: %d with score: %.2f%n", entry.getKey(), entry.getValue());
            }
        }

        scanner.close();
    }

    private static void createSampleData(String filePath) {
        String[] data = {
            "1,101,5.0",
            "1,102,3.0",
            "1,103,2.5",
            "2,101,2.0",
            "2,102,2.5",
            "2,103,5.0",
            "2,104,2.0",
            "3,101,2.5",
            "3,104,4.0",
            "3,105,4.5",
            "3,107,5.0"
        };
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String line : data) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Failed to create sample data: " + e.getMessage());
        }
    }

    private static void loadRatings(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                int user = Integer.parseInt(parts[0]);
                int item = Integer.parseInt(parts[1]);
                double rating = Double.parseDouble(parts[2]);
                userRatings.computeIfAbsent(user, k -> new HashMap<>()).put(item, rating);
            }
        } catch (IOException e) {
            System.err.println("Failed to load data: " + e.getMessage());
        }
    }

    private static Map<Integer, Double> recommend(int targetUser, int topN) {
        Map<Integer, Double> scores = new HashMap<>();
        Map<Integer, Double> targetRatings = userRatings.get(targetUser);

        if (targetRatings == null) return scores;

        for (Map.Entry<Integer, Map<Integer, Double>> entry : userRatings.entrySet()) {
            int otherUser = entry.getKey();
            if (otherUser == targetUser) continue;
            double similarity = computeSimilarity(targetRatings, entry.getValue());

            for (Map.Entry<Integer, Double> itemRating : entry.getValue().entrySet()) {
                int item = itemRating.getKey();
                if (!targetRatings.containsKey(item)) {
                    scores.put(item, scores.getOrDefault(item, 0.0) + similarity * itemRating.getValue());
                }
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(topN)
                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
    }

    private static double computeSimilarity(Map<Integer, Double> ratingsA, Map<Integer, Double> ratingsB) {
        Set<Integer> common = new HashSet<>(ratingsA.keySet());
        common.retainAll(ratingsB.keySet());
        if (common.isEmpty()) return 0.0;

        double sumA = 0, sumB = 0, sumA2 = 0, sumB2 = 0, sumAB = 0;
        for (int item : common) {
            double a = ratingsA.get(item);
            double b = ratingsB.get(item);
            sumA += a;
            sumB += b;
            sumA2 += a * a;
            sumB2 += b * b;
            sumAB += a * b;
        }

        double numerator = sumAB - (sumA * sumB / common.size());
        double denominator = Math.sqrt(sumA2 - (sumA * sumA / common.size())) *
                             Math.sqrt(sumB2 - (sumB * sumB / common.size()));

        return denominator == 0 ? 0 : numerator / denominator;
    }
}

