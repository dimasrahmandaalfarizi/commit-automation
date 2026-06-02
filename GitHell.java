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
    private static final int COMMIT_INTERVAL_MINUTES = 2;
    private static final String[] TARGET_BRANCHES = {"main", "abbys"}; // Tambahkan nama branch di sini
    private static final int COMMITS_PER_PUSH = 100; // Jumlah commit sekaligus per siklus jalan

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

            // 2. Lakukan perulangan pembuatan file dan commit
            for (int i = 1; i <= COMMITS_PER_PUSH; i++) {
                String uuid = UUID.randomUUID().toString();
                String currentTime = new Date().toString();
                int executionCount = getExecutionCount();
                
                String content = "  _____         _    _ __  __          _   _ _____          \n" +
                                 " |  __ \\       | |  | |  \\/  |   /\\   | \\ | |  __ \\   /\\    \n" +
                                 " | |__) |__ _  | |__| | \\  / |  /  \\  |  \\| | |  | | /  \\   \n" +
                                 " |  _  // _` | |  __  | |\\/| | / /\\ \\ | . ` | |  | |/ /\\ \\  \n" +
                                 " | | \\ \\ (_| | | |  | | |  | |/ ____ \\| |\\  | |__| / ____ \\ \n" +
                                 " |_|  \\_\\__,_| |_|  |_|_|  |_/_/    \\_\\_| \\_|_____/_/    \\_\\\n" +
                                 "----------------------------------------------------\n" +
                                 "The abyss grows deeper with each commit.\n" +
                                 "----------------------------------------------------\n" +
                                 "Commit ID: " + uuid + "\n" +
                                 "Execution Count: " + executionCount + "\n" +
                                 "Batch Commit: " + i + " of " + COMMITS_PER_PUSH + "\n" +
                                 "Last Execution Time: " + currentTime + "\n" +
                                 "----------------------------------------------------\n";

                File file = new File(folder, FILE_NAME);
                Files.writeString(file.toPath(), content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                // 3. Eksekusi perintah Git untuk Add & Commit (Tanpa Push)
                System.out.println("  -> Membuat Commit ke-" + i + " dari " + COMMITS_PER_PUSH + "...");
                runCommand("git", "add", FOLDER_NAME + "/" + FILE_NAME);

                String commitMessage = "🌑 The abyss deepens... (Executions: " + executionCount + ", Batch: " + i + "/" + COMMITS_PER_PUSH + ")";
                runCommand("git", "commit", "-m", commitMessage);
                
                // Jeda 500ms agar timestamp aman tidak bentrok
                Thread.sleep(500);
            }

            System.out.println("Melakukan Push ke Remote...");
            for (String branch : TARGET_BRANCHES) {
                System.out.println("  -> Push ke branch: " + branch);
                runCommand("git", "push", "origin", "HEAD:" + branch, "-f");
            }

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
