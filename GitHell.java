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
    private static final int COMMIT_INTERVAL_MINUTES = 72; // Tiap 72 menit = ~20 commit per hari (24jam/72mnt)
    private static final String[] TARGET_BRANCHES = {"main", "abbys"};
    private static final int COMMITS_PER_PUSH = 5; // Commit per siklus (mode loop)
    private static final int DAILY_COMMIT_TARGET = 20; // Total commit mode --daily

    public static void main(String[] args) {

        // Cek apakah pakai mode --daily (untuk Task Scheduler)
        boolean isDailyMode = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--daily")) {
                isDailyMode = true;
                break;
            }
        }

        if (isDailyMode) {
            runDailyMode();
        } else {
            runLoopMode();
        }
    }

    // =========================================================
    // MODE 1: --daily (dipanggil Task Scheduler, lalu exit)
    // Membuat tepat 20 commit, push, lalu program selesai
    // =========================================================
    private static void runDailyMode() {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   🌑 GitHell - DAILY AUTO MODE       ║");
        System.out.println("║   Target: " + DAILY_COMMIT_TARGET + " commits → push → exit     ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println("[" + new Date() + "] Memulai daily run...\n");

        try {
            // Pastikan folder ada
            File folder = new File(FOLDER_NAME);
            if (!folder.exists()) folder.mkdir();

            // Buat DAILY_COMMIT_TARGET commit
            for (int i = 1; i <= DAILY_COMMIT_TARGET; i++) {
                String uuid = UUID.randomUUID().toString();
                String currentTime = new Date().toString();
                int execCount = getExecutionCount();

                String content = buildContent(uuid, execCount, i, DAILY_COMMIT_TARGET, currentTime);

                File file = new File(folder, FILE_NAME);
                Files.writeString(file.toPath(), content,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                System.out.printf("  [%02d/%02d] Commit #%d...\n", i, DAILY_COMMIT_TARGET, execCount);
                runCommand("git", "add", FOLDER_NAME + "/" + FILE_NAME);

                String msg = "🌑 Daily auto-commit (" + i + "/" + DAILY_COMMIT_TARGET
                        + ") | " + currentTime;
                runCommand("git", "commit", "-m", msg);

                Thread.sleep(300); // jeda kecil agar timestamp unik
            }

            // Push ke semua branch
            System.out.println("\n📤 Pushing ke remote...");
            for (String branch : TARGET_BRANCHES) {
                System.out.println("  → Push ke branch: " + branch);
                runCommand("git", "push", "origin", "HEAD:" + branch, "-f");
            }

            System.out.println("\n✅ Daily run selesai! " + DAILY_COMMIT_TARGET + " commit berhasil di-push.");
            System.out.println("[" + new Date() + "] Program selesai.\n");

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        System.exit(0); // Keluar bersih agar Task Scheduler tahu sukses
    }

    // =========================================================
    // MODE 2: Loop terus (perilaku lama, jalankan start.bat manual)
    // =========================================================
    private static void runLoopMode() {
        System.out.println("🔥 Memulai GitHell (Loop Mode)...");
        System.out.println("Menjadwalkan eksekusi setiap " + COMMIT_INTERVAL_MINUTES + " menit.");
        System.out.println("Gunakan: java GitHell --daily  → untuk daily mode (Task Scheduler)\n");

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(GitHell::executeLoopTask, 0, COMMIT_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    private static void executeLoopTask() {
        try {
            System.out.println("\n[" + new Date() + "] Mengeksekusi tugas GitHell...");

            File folder = new File(FOLDER_NAME);
            if (!folder.exists()) folder.mkdir();

            for (int i = 1; i <= COMMITS_PER_PUSH; i++) {
                String uuid = UUID.randomUUID().toString();
                String currentTime = new Date().toString();
                int executionCount = getExecutionCount();

                String content = buildContent(uuid, executionCount, i, COMMITS_PER_PUSH, currentTime);

                File file = new File(folder, FILE_NAME);
                Files.writeString(file.toPath(), content,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                System.out.println("  -> Membuat Commit ke-" + i + " dari " + COMMITS_PER_PUSH + "...");
                runCommand("git", "add", FOLDER_NAME + "/" + FILE_NAME);

                String commitMessage = "🌑 The abyss deepens... (Exec: " + executionCount
                        + ", Batch: " + i + "/" + COMMITS_PER_PUSH + ")";
                runCommand("git", "commit", "-m", commitMessage);

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

    // =========================================================
    // Helper: Build content file
    // =========================================================
    private static String buildContent(String uuid, int execCount, int batchNum, int batchTotal, String time) {
        return "  _____         _    _ __  __          _   _ _____          \n" +
               " |  __ \\       | |  | |  \\/  |   /\\   | \\ | |  __ \\   /\\    \n" +
               " | |__) |__ _  | |__| | \\  / |  /  \\  |  \\| | |  | | /  \\   \n" +
               " |  _  // _` | |  __  | |\\/| | / /\\ \\ | . ` | |  | |/ /\\ \\  \n" +
               " | | \\ \\ (_| | | |  | | |  | |/ ____ \\| |\\  | |__| / ____ \\ \n" +
               " |_|  \\_\\__,_| |_|  |_|_|  |_/_/    \\_\\_| \\_|_____/_/    \\_\\\n" +
               "----------------------------------------------------\n" +
               "The abyss grows deeper with each commit.\n" +
               "----------------------------------------------------\n" +
               "Commit ID: " + uuid + "\n" +
               "Execution Count: " + execCount + "\n" +
               "Batch Commit: " + batchNum + " of " + batchTotal + "\n" +
               "Last Execution Time: " + time + "\n" +
               "----------------------------------------------------\n";
    }

    // =========================================================
    // Helper: Baca execution count dari file
    // =========================================================
    private static int getExecutionCount() {
        int count = 1;
        File file = new File(FOLDER_NAME, FILE_NAME);
        if (file.exists()) {
            try {
                String content = Files.readString(file.toPath());
                for (String line : content.split("\n")) {
                    if (line.startsWith("Execution Count: ")) {
                        return Integer.parseInt(line.replace("Execution Count: ", "").trim()) + 1;
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        return count;
    }

    // =========================================================
    // Helper: Jalankan command
    // =========================================================
    private static void runCommand(String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

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
