import java.util.*;
import java.util.concurrent.*;
import java.io.*;

/**
 * AMCAT-like adaptive practice console app with DYNAMIC QUESTIONS
 * - Questions are randomly generated for each user
 * - Adaptive difficulty (easy=1, medium=2, hard=3)
 * - Total exam timer enforced
 * - File handling for saving past scores
 */
public class AMCATSimulator {

    static class Question {
        int id;
        int difficulty;
        String prompt;
        String[] options;
        String answer;

        Question(int id, int difficulty, String prompt, String[] options, String answer) {
            this.id = id;
            this.difficulty = difficulty;
            this.prompt = prompt;
            this.options = options;
            this.answer = answer.trim().toLowerCase();
        }

        void display() {
            System.out.println(prompt);
            if (options != null) {
                for (int i = 0; i < options.length; i++) {
                    System.out.printf("  %c) %s\n", 'A' + i, options[i]);
                }
            }
        }

        boolean check(String userAns) {
            if (userAns == null) return false;
            return userAns.trim().toLowerCase().equals(answer);
        }
    }

    private final Random rand = new Random();
    private final Scanner scanner = new Scanner(System.in);
    private final List<String> scoreHistory = new ArrayList<>();
    private final String SCORE_FILE = "scores.txt";
    private int questionIdCounter = 1000;

    public static void main(String[] args) {
        AMCATSimulator app = new AMCATSimulator();
        app.loadScoresFromFile();
        app.mainMenu();
    }

    void mainMenu() {
        while (true) {
            System.out.println("\n=== AMCAT Practice Simulator ===");
            System.out.println("1) Start Timed Exam (Adaptive)");
            System.out.println("2) Practice Mode (choose difficulty)");
            System.out.println("3) View Past Scores");
            System.out.println("4) Save Scores to File");
            System.out.println("5) Exit");
            System.out.print("Choose an option: ");
            String opt = scanner.nextLine().trim();
            switch (opt) {
                case "1": startTimedExam(); break;
                case "2": practiceMode(); break;
                case "3": viewScores(); break;
                case "4": saveScoresToFile(); break;
                case "5": System.out.println("Goodbye!"); return;
                default: System.out.println("Invalid option. Try again.");
            }
        }
    }

    void startTimedExam() {
        System.out.println("\n-- Start Timed Exam (Aptitude + Reasoning) --");
        int numQuestions = promptInt("Number of questions (suggested 10): ", 1, 100, 10);
        int minutes = promptInt("Total time in minutes (suggested 10): ", 1, 180, 10);

        long totalMillis = minutes * 60L * 1000L;
        long endTime = System.currentTimeMillis() + totalMillis;

        int currentDifficulty = 2;
        int correctStreak = 0;
        int wrongStreak = 0;
        int score = 0;

        for (int q = 1; q <= numQuestions; q++) {
            long now = System.currentTimeMillis();
            long remaining = endTime - now;
            if (remaining <= 0) {
                System.out.println("\nTime's up!");
                break;
            }

            Question question = generateRandomQuestion(currentDifficulty);

            System.out.printf("\nQuestion %d of %d (Difficulty: %s)\n", q, numQuestions, diffName(currentDifficulty));
            question.display();

            int perQTimeout = (int)Math.min(60, (remaining+999)/1000);
            System.out.printf("You have up to %d seconds to answer. Enter answer: ", perQTimeout);

            String userAns = readLineWithTimeout(perQTimeout);
            if (userAns == null) {
                System.out.println("\nNo answer entered in time. Marked wrong.");
                wrongStreak++;
                correctStreak = 0;
            } else {
                boolean correct = question.check(userAns);
                if (correct) {
                    System.out.println("Correct!");
                    score += pointsForDifficulty(question.difficulty);
                    correctStreak++;
                    wrongStreak = 0;
                } else {
                    System.out.println("Incorrect. Correct answer: " + question.answer);
                    wrongStreak++;
                    correctStreak = 0;
                }
            }

            if (correctStreak >= 2 && currentDifficulty < 3) {
                currentDifficulty++;
                System.out.println("Difficulty increased to " + diffName(currentDifficulty));
                correctStreak = 0;
            } else if (wrongStreak >= 2 && currentDifficulty > 1) {
                currentDifficulty--;
                System.out.println("Difficulty decreased to " + diffName(currentDifficulty));
                wrongStreak = 0;
            }

            now = System.currentTimeMillis();
            remaining = endTime - now;
            if (remaining <= 0) {
                System.out.println("\nTime's up!");
                break;
            }
        }

        String result = String.format("%s | score: %d | date: %s", "TimedExam", score, new Date());
        scoreHistory.add(result);
        System.out.println("\nExam finished. Your score: " + score);
    }

    void practiceMode() {
        System.out.println("\n-- Practice Mode --");
        System.out.println("Select difficulty: 1) Easy  2) Medium  3) Hard");
        int d = promptInt("Choose: ",1,3,2);

        Question q = generateRandomQuestion(d);
        q.display();
        System.out.print("Enter answer (no timeout in practice): ");
        String ans = scanner.nextLine();
        if (q.check(ans)) System.out.println("Correct!"); 
        else System.out.println("Incorrect. Correct: " + q.answer);
    }

    Question generateRandomQuestion(int difficulty) {
        int type = rand.nextInt(10); // 10 different question types
        
        switch (difficulty) {
            case 1: return generateEasyQuestion(type);
            case 2: return generateMediumQuestion(type);
            case 3: return generateHardQuestion(type);
            default: return generateMediumQuestion(type);
        }
    }

    Question generateEasyQuestion(int type) {
        int id = questionIdCounter++;
        
        switch (type % 10) {
            case 0: { // Simple linear equation
                int a = rand.nextInt(5) + 2;
                int b = rand.nextInt(20) + 5;
                int result = rand.nextInt(15) + 10;
                int x = (result - b) / a;
                String prompt = String.format("If %dx + %d = %d, what is x?", a, b, result);
                return new Question(id, 1, prompt, null, String.valueOf(x));
            }
            case 1: { // Number series
                int start = rand.nextInt(10) + 1;
                int diff = rand.nextInt(5) + 1;
                int next = start + 4 * diff;
                String prompt = String.format("Find the next number: %d, %d, %d, %d, ?", 
                    start, start+diff, start+2*diff, start+3*diff);
                return new Question(id, 1, prompt, null, String.valueOf(next));
            }
            case 2: { // Simple percentage
                int whole = rand.nextInt(9) + 1;
                int percent = (rand.nextInt(4) + 1) * 25;
                int answer = whole * percent / 100;
                String prompt = String.format("What is %d%% of %d?", percent, whole * 10);
                return new Question(id, 1, prompt, null, String.valueOf(answer * 10));
            }
            case 3: { // Simple average
                int a = rand.nextInt(20) + 10;
                int b = rand.nextInt(20) + 10;
                int c = rand.nextInt(20) + 10;
                int avg = (a + b + c) / 3;
                String prompt = String.format("What is the average of %d, %d, and %d?", a, b, c);
                return new Question(id, 1, prompt, null, String.valueOf(avg));
            }
            case 4: { // Age problems
                int myAge = rand.nextInt(30) + 20;
                int years = rand.nextInt(10) + 5;
                int future = myAge + years;
                String prompt = String.format("I am %d years old. How old will I be in %d years?", myAge, years);
                return new Question(id, 1, prompt, null, String.valueOf(future));
            }
            case 5: { // Simple multiplication
                int a = rand.nextInt(10) + 5;
                int b = rand.nextInt(10) + 5;
                String prompt = String.format("What is %d × %d?", a, b);
                return new Question(id, 1, prompt, null, String.valueOf(a * b));
            }
            case 6: { // Odd one out
                String[] items = {"Apple", "Banana", "Carrot", "Mango"};
                String prompt = "Which one is NOT a fruit?";
                return new Question(id, 1, prompt, items, "C");
            }
            case 7: { // Simple ratio
                int total = (rand.nextInt(5) + 2) * 10;
                int ratio1 = rand.nextInt(3) + 1;
                int ratio2 = rand.nextInt(3) + 1;
                int part1 = total * ratio1 / (ratio1 + ratio2);
                String prompt = String.format("Divide %d in the ratio %d:%d. What is the first part?", 
                    total, ratio1, ratio2);
                return new Question(id, 1, prompt, null, String.valueOf(part1));
            }
            case 8: { // Antonym
                String[][] pairs = {
                    {"Happy", "Sad", "Joyful", "Excited", "Cheerful"},
                    {"Hot", "Cold", "Warm", "Boiling", "Heated"},
                    {"Big", "Small", "Large", "Huge", "Giant"}
                };
                String[] pair = pairs[rand.nextInt(pairs.length)];
                String prompt = String.format("What is the opposite of '%s'?", pair[0]);
                return new Question(id, 1, prompt, 
                    new String[]{pair[1], pair[2], pair[3], pair[4]}, "A");
            }
            default: { // Simple addition
                int a = rand.nextInt(50) + 10;
                int b = rand.nextInt(50) + 10;
                String prompt = String.format("What is %d + %d?", a, b);
                return new Question(id, 1, prompt, null, String.valueOf(a + b));
            }
        }
    }

    Question generateMediumQuestion(int type) {
        int id = questionIdCounter++;
        
        switch (type % 10) {
            case 0: { // Train/Speed problems
                int length = (rand.nextInt(10) + 5) * 10;
                int time = rand.nextInt(8) + 3;
                int speed = length / time;
                String prompt = String.format("A train %dm long passes a pole in %d seconds. What is the speed (m/s)?", 
                    length, time);
                return new Question(id, 2, prompt, null, String.valueOf(speed));
            }
            case 1: { // Ratio with sum
                int ratio1 = rand.nextInt(4) + 2;
                int ratio2 = rand.nextInt(4) + 3;
                int sum = (ratio1 + ratio2) * (rand.nextInt(5) + 3);
                int num1 = sum * ratio1 / (ratio1 + ratio2);
                int num2 = sum - num1;
                String prompt = String.format("The ratio of two numbers is %d:%d and their sum is %d. What are the numbers? (smaller,larger)", 
                    ratio1, ratio2, sum);
                return new Question(id, 2, prompt, null, String.format("%d,%d", num1, num2));
            }
            case 2: { // Profit/Loss percentage
                int cp = (rand.nextInt(10) + 10) * 10;
                int profitPercent = (rand.nextInt(4) + 1) * 5;
                int sp = cp + (cp * profitPercent / 100);
                String prompt = String.format("An item is bought for ₹%d and sold at %d%% profit. What is the selling price?", 
                    cp, profitPercent);
                return new Question(id, 2, prompt, null, String.valueOf(sp));
            }
            case 3: { // Time and Work
                int days1 = (rand.nextInt(5) + 3) * 4;
                int days2 = (rand.nextInt(5) + 4) * 4;
                double combined = 1.0/days1 + 1.0/days2;
                int together = (int)Math.round(1.0/combined);
                String prompt = String.format("A can complete work in %d days, B in %d days. Working together, how many days?", 
                    days1, days2);
                return new Question(id, 2, prompt, null, String.valueOf(together));
            }
            case 4: { // Compound interest
                int principal = (rand.nextInt(5) + 5) * 1000;
                int rate = rand.nextInt(4) + 5;
                int time = 2;
                int amount = (int)(principal * Math.pow(1 + rate/100.0, time));
                int ci = amount - principal;
                String prompt = String.format("Find compound interest on ₹%d at %d%% for 2 years.", 
                    principal, rate);
                return new Question(id, 2, prompt, null, String.valueOf(ci));
            }
            case 5: { // Geometric progression
                int first = rand.nextInt(5) + 2;
                int ratio = rand.nextInt(3) + 2;
                int next = first * ratio * ratio * ratio;
                String prompt = String.format("Find the next term: %d, %d, %d, ?", 
                    first, first*ratio, first*ratio*ratio);
                return new Question(id, 2, prompt, null, String.valueOf(next));
            }
            case 6: { // Permutation
                int n = rand.nextInt(4) + 4;
                int r = rand.nextInt(n-1) + 1;
                int perm = factorial(n) / factorial(n-r);
                String prompt = String.format("How many ways can you arrange %d items from %d distinct items?", r, n);
                return new Question(id, 2, prompt, null, String.valueOf(perm));
            }
            case 7: { // Probability
                int total = rand.nextInt(10) + 10;
                int favorable = rand.nextInt(total/2) + 1;
                String prompt = String.format("In a bag of %d balls, %d are red. What is the probability of drawing a red ball? (as fraction, e.g., 1/4)", 
                    total, favorable);
                int gcd = gcd(favorable, total);
                return new Question(id, 2, prompt, null, 
                    String.format("%d/%d", favorable/gcd, total/gcd));
            }
            case 8: { // Blood relation
                String[][] relations = {
                    {"A's mother", "B", "grandmother", "mother"},
                    {"A's brother", "C", "uncle", "father"},
                    {"A's sister", "D", "aunt", "mother"}
                };
                String[] rel = relations[rand.nextInt(relations.length)];
                String prompt = String.format("If %s is %s, then what is B to A's child?", rel[0], rel[1]);
                return new Question(id, 2, prompt, 
                    new String[]{rel[2], "Sibling", "Cousin", "Niece"}, "A");
            }
            default: { // Mixture problem
                int qty1 = rand.nextInt(20) + 10;
                int qty2 = rand.nextInt(20) + 10;
                int price1 = rand.nextInt(10) + 10;
                int price2 = rand.nextInt(10) + 15;
                int avgPrice = (qty1*price1 + qty2*price2) / (qty1+qty2);
                String prompt = String.format("Mix %dkg at ₹%d/kg with %dkg at ₹%d/kg. Average price/kg?", 
                    qty1, price1, qty2, price2);
                return new Question(id, 2, prompt, null, String.valueOf(avgPrice));
            }
        }
    }

    Question generateHardQuestion(int type) {
        int id = questionIdCounter++;
        
        switch (type % 10) {
            case 0: { // Sum of n natural numbers
                int sum = (rand.nextInt(10) + 10) * (rand.nextInt(10) + 10);
                int n = (int)Math.sqrt(2 * sum);
                while (n * (n+1) / 2 != sum) n++;
                String prompt = String.format("If sum of first n natural numbers is %d, find n.", sum);
                return new Question(id, 3, prompt, null, String.valueOf(n));
            }
            case 1: { // Complex work problem
                int days1 = rand.nextInt(8) + 8;
                int days2 = rand.nextInt(8) + 12;
                int workedDays = rand.nextInt(days1-2) + 2;
                double workDone = workedDays * 1.0 / days1;
                double remaining = 1.0 - workDone;
                int moreDays = (int)Math.ceil(remaining * days2);
                int total = workedDays + moreDays;
                String prompt = String.format("A can finish work in %d days, B in %d days. A works for %d days then B finishes. Total days?", 
                    days1, days2, workedDays);
                return new Question(id, 3, prompt, null, String.valueOf(total));
            }
            case 2: { // Calendar problems
                String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
                int startDay = rand.nextInt(7);
                int addDays = rand.nextInt(100) + 50;
                int endDay = (startDay + addDays) % 7;
                String prompt = String.format("If today is %s, what day will it be %d days later?", 
                    days[startDay], addDays);
                return new Question(id, 3, prompt, 
                    new String[]{days[endDay], days[(endDay+1)%7], days[(endDay+2)%7], days[(endDay+3)%7]}, "A");
            }
            case 3: { // Logarithm
                int base = rand.nextInt(3) + 2;
                int exp = rand.nextInt(4) + 2;
                int value = (int)Math.pow(base, exp);
                String prompt = String.format("What is log%d(%d)?", base, value);
                return new Question(id, 3, prompt, null, String.valueOf(exp));
            }
            case 4: { // Arithmetic progression
                int first = rand.nextInt(10) + 5;
                int diff = rand.nextInt(5) + 2;
                int n = rand.nextInt(10) + 10;
                int sum = n * (2*first + (n-1)*diff) / 2;
                String prompt = String.format("Sum of %d terms of AP with first term %d and common difference %d?", 
                    n, first, diff);
                return new Question(id, 3, prompt, null, String.valueOf(sum));
            }
            case 5: { // Pipe and cistern
                int fill1 = rand.nextInt(10) + 10;
                int fill2 = rand.nextInt(10) + 15;
                int empty = rand.nextInt(15) + 20;
                double netRate = 1.0/fill1 + 1.0/fill2 - 1.0/empty;
                int time = (int)Math.round(1.0/netRate);
                String prompt = String.format("Pipe A fills tank in %dh, B in %dh, C empties in %dh. All open, tank fills in?", 
                    fill1, fill2, empty);
                return new Question(id, 3, prompt, null, String.valueOf(time));
            }
            case 6: { // Clock angle
                int hour = rand.nextInt(11) + 1;
                int minute = rand.nextInt(12) * 5;
                double hourAngle = (hour % 12) * 30 + minute * 0.5;
                double minuteAngle = minute * 6;
                double angle = Math.abs(hourAngle - minuteAngle);
                if (angle > 180) angle = 360 - angle;
                String prompt = String.format("What is the angle between hour and minute hands at %d:%02d?", 
                    hour, minute);
                return new Question(id, 3, prompt, null, String.valueOf((int)angle));
            }
            case 7: { // Syllogism (complex)
                String prompt = "All X are Y. All Y are Z. Some Z are W. Which conclusion is valid?";
                return new Question(id, 3, prompt, 
                    new String[]{"All X are Z", "Some W are X", "No X are W", "None of these"}, "A");
            }
            case 8: { // Data sufficiency
                int x = rand.nextInt(20) + 10;
                String prompt = String.format("To find the value of x: (1) x + 5 = %d  (2) 2x = %d. Which statement(s) sufficient?", 
                    x+5, 2*x);
                return new Question(id, 3, prompt, 
                    new String[]{"(1) alone", "(2) alone", "Both together", "Each alone"}, "D");
            }
            default: { // Cube root
                int num = rand.nextInt(9) + 2;
                int cube = num * num * num;
                String prompt = String.format("What is the cube root of %d?", cube);
                return new Question(id, 3, prompt, null, String.valueOf(num));
            }
        }
    }

    int factorial(int n) {
        if (n <= 1) return 1;
        int result = 1;
        for (int i = 2; i <= n; i++) result *= i;
        return result;
    }

    int gcd(int a, int b) {
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    void viewScores() {
        System.out.println("\n-- Past Scores --");
        if (scoreHistory.isEmpty()) {
            System.out.println("No past scores yet.");
            return;
        }
        for (String s : scoreHistory) System.out.println(s);
    }

    void saveScoresToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(SCORE_FILE, true))) {
            for (String s : scoreHistory) {
                writer.println(s);
            }
            scoreHistory.clear();
            System.out.println("Scores saved to '" + SCORE_FILE + "'.");
        } catch (IOException e) {
            System.out.println("Error saving scores: " + e.getMessage());
        }
    }

    void loadScoresFromFile() {
        File f = new File(SCORE_FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) scoreHistory.add(line);
            }
        } catch (IOException e) {
            System.out.println("Error loading past scores: " + e.getMessage());
        }
    }

    int promptInt(String prompt, int min, int max, int defaultVal) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) return defaultVal;
            try {
                int v = Integer.parseInt(line);
                if (v < min || v > max) {
                    System.out.println("Value out of range. Try again.");
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    String diffName(int d) {
        switch (d) {
            case 1: return "Easy";
            case 2: return "Medium";
            case 3: return "Hard";
            default: return "Unknown";
        }
    }

    int pointsForDifficulty(int d) {
        switch (d) {
            case 1: return 2;
            case 2: return 5;
            case 3: return 10;
            default: return 0;
        }
    }

    String readLineWithTimeout(int timeoutSeconds) {
        ExecutorService ex = Executors.newSingleThreadExecutor();
        Future<String> future = ex.submit(() -> {
            try {
                return scanner.nextLine();
            } catch (NoSuchElementException e) {
                return null;
            }
        });
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException te) {
            future.cancel(true);
            return null;
        } catch (InterruptedException | ExecutionException e) {
            return null;
        } finally {
            ex.shutdownNow();
        }
    }
}