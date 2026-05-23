import java.io.*;
import java.nio.file.*;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GitHell {

    // Konfigurasi
    private static final String FOLDER_NAME = "abyss";
    private static final String FILE_NAME = "README.yml";
    private static final int COMMIT_INTERVAL_MINUTES = 5;

    public static void main(String[] args) {
        System.out.println("🔥 Memulai GitHell (100% Java Version)...");
        System.out.println("Menjadwalkan eksekusi setiap " + COMMIT_INTERVAL_MINUTES + " menit.");

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        // Menjalankan tugas secara berulang
        scheduler.scheduleAtFixedRate(GitHell::executeGitHellTask, 0, COMMIT_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    private static void executeGitHellTask() {
        try {
            System.out.println("\n[" + new Date() + "] Mengeksekusi tugas GitHell...");

            // 1. Buat folder jika belum ada
            File folder = new File(FOLDER_NAME);
            if (!folder.exists()) {
                folder.mkdir();
            }

            // 2. Tulis file dengan ASCII Art dan metadata
            String uuid = UUID.randomUUID().toString();
            String currentTime = new Date().toString();
            int executionCount = getExecutionCount();
            
            String content = "  ________.__  __     ___ ___         .__  .__   \n" +
                             " /  _____/|__|/  |_  /   |   \\   ____ |  | |  |  \n" +
                             "/   \\  ___|  \\   __\\/    ~    \\_/ __ \\|  | |  |  \n" +
                             "\\    \\_\\  \\  ||  |  \\    Y    /\\  ___/|  |_|  |__\n" +
                             " \\______  /__||__|   \\___|_  /  \\___  >____/____/ \n" +
                             "        \\/                 \\/       \\/            \n" +
                             "----------------------------------------------------\n" +
                             "The abyss grows deeper with each commit.\n" +
                             "----------------------------------------------------\n" +
                             "Commit ID: " + uuid + "\n" +
                             "Execution Count: " + executionCount + "\n" +
                             "Last Execution Time: " + currentTime + "\n" +
                             "----------------------------------------------------\n";

            File file = new File(folder, FILE_NAME);
            Files.writeString(file.toPath(), content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // 3. Eksekusi perintah Git
            System.out.println("Menambahkan ke Git...");
            runCommand("git", "add", FOLDER_NAME + "/" + FILE_NAME);

            System.out.println("Melakukan Commit...");
            String commitMessage = "🌑 The abyss has no bottom... (Executions: " + executionCount + ", Last run: " + currentTime + ")";
            runCommand("git", "commit", "-m", commitMessage);

            System.out.println("Melakukan Push ke Remote...");
            runCommand("git", "push");

            System.out.println("✅ Eksekusi selesai.");

        } catch (Exception e) {
            System.err.println("❌ Terjadi kesalahan: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static int getExecutionCount() {
        int count = 1;
        File file = new File(FOLDER_NAME, FILE_NAME);
        if (file.exists()) {
            try {
                String content = Files.readString(file.toPath());
                String[] lines = content.split("\n");
                for (String line : lines) {
                    if (line.startsWith("Execution Count: ")) {
                        return Integer.parseInt(line.replace("Execution Count: ", "").trim()) + 1;
                    }
                }
            } catch (Exception e) {
                // Ignore and return 1 if failed to parse
            }
        }
        return count;
    }

    private static void runCommand(String... command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("  " + line);
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("  ⚠️ Peringatan: Perintah keluar dengan kode " + exitCode);
        }
    }
}
