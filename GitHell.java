import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;

public class GitHell {

    // ================================================================
    // CONFIG — Loaded from config.properties
    // ================================================================
    private static int    DAILY_TARGET           = 20;
    private static int    COMMIT_INTERVAL_MINUTES= 72;
    private static int    COMMITS_PER_PUSH       = 5;
    private static String[] BRANCHES             = {"main", "abbys"};
    private static String[] REPOSITORIES         = {"."};
    private static String COMMIT_MESSAGE_STYLE   = "random";
    private static int    RETRY_COUNT            = 3;
    private static int    RETRY_DELAY_MINUTES    = 5;
    private static boolean DISCORD_ENABLED       = false;
    private static String DISCORD_WEBHOOK_URL    = "";
    private static boolean WINDOWS_TOAST_ENABLED = true;
    private static boolean RANDOMIZE_DELAYS      = true;
    private static boolean GIT_PULL_BEFORE_COMMIT= true;
    private static boolean STATS_ENABLED         = true;
    private static String STATS_FILE             = "stats.csv";

    // Commit message pool (random style)
    private static final String[] COMMIT_MESSAGES = {
        "refactor: improve code structure",
        "chore: update project files",
        "chore: cleanup and maintenance",
        "docs: update documentation",
        "fix: minor bug fixes",
        "fix: resolve edge cases",
        "feat: add new utility functions",
        "feat: enhance existing features",
        "style: improve code formatting",
        "perf: optimize performance",
        "test: add missing test cases",
        "build: update build scripts",
        "ci: update automation pipeline",
        "refactor: reorganize file structure",
        "chore: update dependencies",
        "docs: improve README",
        "fix: handle error gracefully",
        "feat: implement new helper methods",
        "chore: remove unused code",
        "refactor: extract common utilities"
    };

    private static final String FOLDER_NAME = "abyss";
    private static final String FILE_NAME   = "README.yml";
    private static final Random RNG         = new Random();
    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ================================================================
    // MAIN
    // ================================================================
    public static void main(String[] args) {
        loadConfig();

        boolean isDailyMode = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--daily")) { isDailyMode = true; break; }
        }

        if (isDailyMode) runDailyMode();
        else             runLoopMode();
    }

    // ================================================================
    // CONFIG LOADER
    // ================================================================
    private static void loadConfig() {
        Properties p = new Properties();
        File cfg = new File("config.properties");
        if (!cfg.exists()) {
            System.out.println("ℹ️  config.properties tidak ditemukan, pakai default values.");
            return;
        }
        try (FileInputStream fis = new FileInputStream(cfg)) {
            p.load(fis);
            DAILY_TARGET            = intProp(p, "daily.target",              DAILY_TARGET);
            COMMIT_INTERVAL_MINUTES = intProp(p, "commit.interval.minutes",   COMMIT_INTERVAL_MINUTES);
            COMMITS_PER_PUSH        = intProp(p, "commits.per.push",          COMMITS_PER_PUSH);
            BRANCHES                = p.getProperty("branches", "main,abbys").split(",");
            REPOSITORIES            = p.getProperty("repositories", ".").split(",");
            COMMIT_MESSAGE_STYLE    = p.getProperty("commit.message.style",   "random");
            RETRY_COUNT             = intProp(p, "retry.count",               RETRY_COUNT);
            RETRY_DELAY_MINUTES     = intProp(p, "retry.delay.minutes",       RETRY_DELAY_MINUTES);
            DISCORD_ENABLED         = boolProp(p, "discord.enabled",          false);
            DISCORD_WEBHOOK_URL     = p.getProperty("discord.webhook.url",    "");
            WINDOWS_TOAST_ENABLED   = boolProp(p, "windows.toast.enabled",    true);
            RANDOMIZE_DELAYS        = boolProp(p, "randomize.delays",         true);
            GIT_PULL_BEFORE_COMMIT  = boolProp(p, "git.pull.before.commit",   true);
            STATS_ENABLED           = boolProp(p, "stats.enabled",            true);
            STATS_FILE              = p.getProperty("stats.file",             "stats.csv");
            System.out.println("✅ Config loaded: " + cfg.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("⚠️  Gagal baca config.properties: " + e.getMessage());
        }
    }

    private static int  intProp(Properties p, String key, int def) {
        try { return Integer.parseInt(p.getProperty(key, String.valueOf(def)).trim()); }
        catch (Exception e) { return def; }
    }
    private static boolean boolProp(Properties p, String key, boolean def) {
        String v = p.getProperty(key); return (v == null) ? def : v.trim().equalsIgnoreCase("true");
    }

    // ================================================================
    // MODE 1: --daily (Task Scheduler)
    // ================================================================
    private static void runDailyMode() {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   🌑 GitHell v2.0 - DAILY AUTO MODE          ║");
        System.out.println("║   Multi-Repo | Smart Msg | Retry | Stats     ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        LocalDateTime startTime = LocalDateTime.now();
        System.out.println("[" + startTime.format(DT_FMT) + "] Memulai daily run...\n");

        int totalCommits = 0, successRepos = 0;
        StringBuilder summary = new StringBuilder();

        for (String rawPath : REPOSITORIES) {
            String repoPath = rawPath.trim();
            System.out.println("\n📁 Repo: " + repoPath);
            System.out.println("─".repeat(55));
            try {
                int n = runDailyForRepo(repoPath);
                totalCommits += n;
                successRepos++;
                summary.append("✅ ").append(repoPath).append(": ").append(n).append(" commits\\n");
            } catch (Exception e) {
                System.err.println("❌ Error repo " + repoPath + ": " + e.getMessage());
                summary.append("❌ ").append(repoPath).append(": FAILED\\n");
            }
        }

        long durSec = Duration.between(startTime, LocalDateTime.now()).getSeconds();
        String status = (successRepos == REPOSITORIES.length) ? "SUCCESS"
                      : (successRepos > 0) ? "PARTIAL" : "FAILED";

        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.printf( "║  ✅ Selesai! %d commit | %d/%d repo | %ds%n",
                totalCommits, successRepos, REPOSITORIES.length, durSec);
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println("[" + LocalDateTime.now().format(DT_FMT) + "] Program selesai.\n");

        if (STATS_ENABLED)           saveStats(LocalDate.now().toString(), totalCommits, durSec, status);
        if (DISCORD_ENABLED)         sendDiscordNotification(totalCommits, successRepos, status, durSec);
        if (WINDOWS_TOAST_ENABLED)   sendWindowsToast(status, totalCommits, successRepos);

        System.exit("FAILED".equals(status) ? 1 : 0);
    }

    // Run daily commits for one repository
    private static int runDailyForRepo(String repoPath) throws Exception {
        File repoDir = new File(repoPath);
        if (!repoDir.exists()) throw new Exception("Directory tidak ditemukan: " + repoPath);

        // Git pull
        if (GIT_PULL_BEFORE_COMMIT) {
            System.out.println("🔄 Git pull...");
            try { 
                runCmd(repoDir, "git", "pull", "--rebase"); 
            } catch (Exception e) { 
                System.out.println("⚠️  Pull gagal/konflik: " + e.getMessage() + ". Melakukan abort..."); 
                try { runCmd(repoDir, "git", "rebase", "--abort"); } catch (Exception ignore) {}
                try { runCmd(repoDir, "git", "merge", "--abort"); } catch (Exception ignore) {}
            }
        }

        File folder = new File(repoDir, FOLDER_NAME);
        if (!folder.exists()) folder.mkdir();

        int successCount = 0;
        for (int i = 1; i <= DAILY_TARGET; i++) {
            try {
                String uuid = UUID.randomUUID().toString();
                String now  = LocalDateTime.now().format(DT_FMT);
                int execCnt = getExecutionCount(folder);

                String content = buildContent(uuid, execCnt, i, DAILY_TARGET, now);
                Files.writeString(new File(folder, FILE_NAME).toPath(), content,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                System.out.printf("  [%02d/%02d] Commit #%d...%n", i, DAILY_TARGET, execCnt);
                runCmd(repoDir, "git", "add", FOLDER_NAME + "/" + FILE_NAME);
                runCmd(repoDir, "git", "commit", "-m", buildCommitMessage(i));

                successCount++;
                long delay = RANDOMIZE_DELAYS ? (200 + RNG.nextInt(1800)) : 300;
                Thread.sleep(delay);
            } catch (Exception e) {
                System.err.println("  ⚠️  Commit #" + i + " gagal: " + e.getMessage());
            }
        }

        System.out.println("\n📤 Pushing ke remote...");
        if (!pushWithRetry(repoDir)) throw new Exception("Push gagal setelah " + RETRY_COUNT + "x retry");

        return successCount;
    }

    // Push dengan retry logic
    private static boolean pushWithRetry(File repoDir) {
        for (int attempt = 1; attempt <= RETRY_COUNT; attempt++) {
            try {
                for (String branch : BRANCHES) {
                    branch = branch.trim();
                    String label = attempt > 1 ? " (retry " + attempt + "/" + RETRY_COUNT + ")" : "";
                    System.out.println("  → Branch: " + branch + label);
                    
                    // Supaya tidak nyangkut (hang) nunggu popup credential manager:
                    ProcessBuilder pb = new ProcessBuilder("git", "push", "origin", "HEAD:" + branch, "-f");
                    pb.directory(repoDir);
                    pb.environment().put("GIT_TERMINAL_PROMPT", "0");
                    pb.redirectErrorStream(true);
                    Process proc = pb.start();
                    
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                        String line;
                        while ((line = br.readLine()) != null) System.out.println("    " + line);
                    }
                    int code = proc.waitFor();
                    if (code != 0) throw new IOException("Exit code " + code);
                }
                return true;
            } catch (Exception e) {
                System.err.println("  ⚠️  Push attempt " + attempt + " gagal: " + e.getMessage());
                if (attempt < RETRY_COUNT) {
                    System.out.println("  ⏳ Tunggu " + RETRY_DELAY_MINUTES + " menit...");
                    try { Thread.sleep(RETRY_DELAY_MINUTES * 60_000L); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
        }
        return false;
    }

    // ================================================================
    // MODE 2: Loop terus
    // ================================================================
    private static void runLoopMode() {
        System.out.println("🔥 GitHell v2.0 - Loop Mode");
        System.out.println("Interval: " + COMMIT_INTERVAL_MINUTES + " menit | Repos: " + REPOSITORIES.length);
        System.out.println("Tip: pakai 'java GitHell --daily' untuk daily mode\n");

        ScheduledExecutorService sched = Executors.newScheduledThreadPool(1);
        sched.scheduleAtFixedRate(GitHell::executeLoopTask, 0, COMMIT_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    private static void executeLoopTask() {
        System.out.println("\n[" + LocalDateTime.now().format(DT_FMT) + "] Loop task mulai...");
        for (String rawPath : REPOSITORIES) {
            String repoPath = rawPath.trim();
            File repoDir = new File(repoPath);
            if (!repoDir.exists()) { System.err.println("Repo tidak ada: " + repoPath); continue; }

            // Git pull
            if (GIT_PULL_BEFORE_COMMIT) {
                try { runCmd(repoDir, "git", "pull", "--rebase"); }
                catch (Exception e) { 
                    try { runCmd(repoDir, "git", "rebase", "--abort"); } catch (Exception ignore) {}
                    try { runCmd(repoDir, "git", "merge", "--abort"); } catch (Exception ignore) {}
                }
            }

            File folder = new File(repoDir, FOLDER_NAME);
            if (!folder.exists()) folder.mkdir();

            try {
                for (int i = 1; i <= COMMITS_PER_PUSH; i++) {
                    String uuid = UUID.randomUUID().toString();
                    String now  = LocalDateTime.now().format(DT_FMT);
                    int execCnt = getExecutionCount(folder);
                    String content = buildContent(uuid, execCnt, i, COMMITS_PER_PUSH, now);
                    Files.writeString(new File(folder, FILE_NAME).toPath(), content,
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    System.out.println("  → Commit " + i + "/" + COMMITS_PER_PUSH + " (exec #" + execCnt + ")");
                    runCmd(repoDir, "git", "add", FOLDER_NAME + "/" + FILE_NAME);
                    runCmd(repoDir, "git", "commit", "-m", buildCommitMessage(i));
                    Thread.sleep(RANDOMIZE_DELAYS ? (200 + RNG.nextInt(1800)) : 500);
                }
                pushWithRetry(repoDir);
                System.out.println("✅ Eksekusi selesai untuk: " + repoPath);
            } catch (Exception e) {
                System.err.println("❌ Error: " + e.getMessage());
            }
        }
    }

    // ================================================================
    // HELPERS
    // ================================================================
    private static String buildCommitMessage(int batchNum) {
        if ("random".equalsIgnoreCase(COMMIT_MESSAGE_STYLE)) {
            String base = COMMIT_MESSAGES[RNG.nextInt(COMMIT_MESSAGES.length)];
            return base + " [" + LocalDate.now() + " #" + batchNum + "]";
        }
        return "🌑 Daily auto-commit (" + batchNum + "/" + DAILY_TARGET + ") | " + LocalDateTime.now().format(DT_FMT);
    }

    private static String buildContent(String uuid, int execCount, int batchNum, int batchTotal, String time) {
        return  "  _____         _    _ __  __          _   _ _____          \n" +
                " |  __ \\       | |  | |  \\/  |   /\\   | \\ | |  __ \\   /\\   \n" +
                " | |__) |__ _  | |__| | \\  / |  /  \\  |  \\| | |  | | /  \\  \n" +
                " |  _  // _` | |  __  | |\\/| | / /\\ \\ | . ` | |  | |/ /\\ \\ \n" +
                " | | \\ \\ (_| | | |  | | |  | |/ ____ \\| |\\  | |__| / ____ \\\n" +
                " |_|  \\_\\__,_| |_|  |_|_|  |_/_/    \\_\\_| \\_|_____/_/    \\_\\\n" +
                "----------------------------------------------------\n" +
                "The abyss grows deeper with each commit.\n" +
                "----------------------------------------------------\n" +
                "Commit ID      : " + uuid + "\n" +
                "Execution Count: " + execCount + "\n" +
                "Batch Commit   : " + batchNum + " of " + batchTotal + "\n" +
                "Timestamp      : " + time + "\n" +
                "----------------------------------------------------\n";
    }

    private static int getExecutionCount(File folder) {
        File f = new File(folder, FILE_NAME);
        if (!f.exists()) return 1;
        try {
            for (String line : Files.readString(f.toPath()).split("\n"))
                if (line.startsWith("Execution Count"))
                    return Integer.parseInt(line.replaceAll("[^0-9]", "").trim()) + 1;
        } catch (Exception ignored) {}
        return 1;
    }

    // Run command in specific directory
    private static void runCmd(File workDir, String... cmd) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workDir);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) System.out.println("    " + line);
        }
        int code = proc.waitFor();
        if (code != 0) {
            System.err.println("  ⚠️  Exit code: " + code);
            throw new IOException("Command failed with exit code: " + code);
        }
    }

    // ================================================================
    // STATISTICS CSV
    // ================================================================
    private static void saveStats(String date, int commits, long durationSec, String status) {
        File statsFile = new File(STATS_FILE);
        boolean isNew = !statsFile.exists();
        try (PrintWriter pw = new PrintWriter(new FileWriter(statsFile, true))) {
            if (isNew) pw.println("date,commits,duration_sec,status,repos");
            pw.printf("%s,%d,%d,%s,%d%n", date, commits, durationSec, status, REPOSITORIES.length);
            System.out.println("📊 Stats saved → " + statsFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("⚠️  Gagal simpan stats: " + e.getMessage());
        }
    }

    // ================================================================
    // DISCORD NOTIFICATION
    // ================================================================
    private static void sendDiscordNotification(int commits, int successRepos, String status, long durSec) {
        if (DISCORD_WEBHOOK_URL.isEmpty() || DISCORD_WEBHOOK_URL.equals("YOUR_DISCORD_WEBHOOK_URL_HERE")) {
            System.out.println("ℹ️  Discord webhook URL belum dikonfigurasi, skip notifikasi.");
            return;
        }
        String color = "SUCCESS".equals(status) ? "3066993" : ("PARTIAL".equals(status) ? "16776960" : "15158332");
        String emoji = "SUCCESS".equals(status) ? "✅" : ("PARTIAL".equals(status) ? "⚠️" : "❌");

        String payload = String.format(
            "{\"embeds\":[{\"title\":\"%s GitHell v2.0 Daily Run\",\"color\":%s," +
            "\"fields\":[" +
            "{\"name\":\"Status\",\"value\":\"%s\",\"inline\":true}," +
            "{\"name\":\"Total Commits\",\"value\":\"%d\",\"inline\":true}," +
            "{\"name\":\"Repos\",\"value\":\"%d repos\",\"inline\":true}," +
            "{\"name\":\"Duration\",\"value\":\"%ds\",\"inline\":true}" +
            "]}]}",
            emoji, color, status, commits, successRepos, durSec
        );

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(DISCORD_WEBHOOK_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            System.out.println("📨 Discord notif sent (HTTP " + resp.statusCode() + ")");
        } catch (Exception e) {
            System.err.println("⚠️  Discord notif gagal: " + e.getMessage());
        }
    }

    // ================================================================
    // WINDOWS TOAST NOTIFICATION
    // ================================================================
    private static void sendWindowsToast(String status, int commits, int repos) {
        String emoji = "SUCCESS".equals(status) ? "✅" : "⚠️";
        String msg   = emoji + " GitHell " + status + ": " + commits + " commits, " + repos + " repo(s)";
        String ps    = String.format(
            "[Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType=WindowsRuntime] | Out-Null;" +
            "$t = [Windows.UI.Notifications.ToastTemplateType]::ToastText01;" +
            "$x = [Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent($t);" +
            "$x.GetElementsByTagName('text').Item(0).AppendChild($x.CreateTextNode('%s')) | Out-Null;" +
            "$n = [Windows.UI.Notifications.ToastNotification]::new($x);" +
            "[Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('GitHell').Show($n);",
            msg.replace("'", "''")
        );
        try {
            new ProcessBuilder("powershell", "-NoProfile", "-Command", ps).start().waitFor();
            System.out.println("🔔 Windows Toast sent.");
        } catch (Exception e) {
            System.err.println("⚠️  Toast gagal: " + e.getMessage());
        }
    }
}
