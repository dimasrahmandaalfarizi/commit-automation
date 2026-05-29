<?php
/**
 * GitHell v2.0 Dashboard
 * Akses via: http://localhost/commit-automation-project/dashboard/
 */

$BASE = dirname(__DIR__);

// ── Baca stats.csv ───────────────────────────────────────────────
$statsFile = $BASE . '/stats.csv';
$stats = [];
if (file_exists($statsFile)) {
    $rows = array_filter(array_map('str_getcsv', file($statsFile)));
    $header = array_shift($rows);
    foreach ($rows as $r) {
        if (count($r) >= 4)
            $stats[] = array_combine(array_slice($header, 0, count($r)), $r);
    }
    $stats = array_reverse($stats); // latest first
}

// ── Totals ───────────────────────────────────────────────────────
$totalCommits  = array_sum(array_column($stats, 'commits'));
$totalRuns     = count($stats);
$successRuns   = count(array_filter($stats, fn($s) => $s['status'] === 'SUCCESS'));
$todayRow      = $stats[0] ?? null;
$todayCommits  = $todayRow ? (int)$todayRow['commits'] : 0;
$todayStatus   = $todayRow ? $todayRow['status'] : 'N/A';

// ── Baca config.properties ───────────────────────────────────────
$configFile = $BASE . '/config.properties';
$config = [];
if (file_exists($configFile)) {
    foreach (file($configFile) as $line) {
        $line = trim($line);
        if ($line === '' || $line[0] === '#') continue;
        [$k, $v] = array_pad(explode('=', $line, 2), 2, '');
        $config[trim($k)] = trim($v);
    }
}
$scheduledTime = $config['scheduled.time'] ?? '09:00';
$dailyTarget   = $config['daily.target']   ?? '20';
$branches      = $config['branches']       ?? 'main,abbys';
$discordOn     = ($config['discord.enabled'] ?? 'false') === 'true';
$randomMsg     = ($config['commit.message.style'] ?? 'random') === 'random';
$retryCount    = $config['retry.count']    ?? '3';

// ── Baca log hari ini ─────────────────────────────────────────────
$today   = date('Y-m-d');
$logFile = $BASE . '/logs/daily_' . $today . '.log';
$logContent = file_exists($logFile) ? htmlspecialchars(file_get_contents($logFile)) : 'Log hari ini belum tersedia.';

// ── Build heatmap data (12 minggu terakhir) ───────────────────────
$heatmapData = [];
foreach ($stats as $s) {
    $heatmapData[$s['date']] = (int)$s['commits'];
}

// ── Waktu run berikutnya ──────────────────────────────────────────
$nextRun = new DateTime();
[$h, $m] = explode(':', $scheduledTime . ':00');
$nextRun->setTime((int)$h, (int)$m, 0);
if ($nextRun < new DateTime()) $nextRun->modify('+1 day');
$diff = $nextRun->diff(new DateTime());
$countdown = sprintf('%02d:%02d:%02d', $diff->h + ($diff->days * 24), $diff->i, $diff->s);
?>
<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>GitHell v2.0 — Dashboard</title>
<link rel="stylesheet" href="style.css">
</head>
<body>

<!-- HEADER -->
<header class="header">
  <div class="header-brand">
    <div class="logo">🌑</div>
    <h1>Git<span>Hell</span> <small style="font-weight:300;opacity:.6">v2.0</small></h1>
  </div>
  <div class="header-right">
    📅 <?= date('D, d M Y H:i:s') ?> &nbsp;|&nbsp; WIB
  </div>
</header>

<main class="container">

  <!-- STAT CARDS -->
  <div class="stats-grid">
    <div class="stat-card primary">
      <div class="icon">🔢</div>
      <div class="value"><?= number_format($totalCommits) ?></div>
      <div class="label">Total Commits All-Time</div>
    </div>
    <div class="stat-card <?= $todayStatus === 'SUCCESS' ? 'success' : ($todayStatus === 'FAILED' ? 'danger' : 'warning') ?>">
      <div class="icon">📅</div>
      <div class="value"><?= $todayCommits ?></div>
      <div class="label">Commits Hari Ini (<?= $todayStatus ?>)</div>
    </div>
    <div class="stat-card success">
      <div class="icon">✅</div>
      <div class="value"><?= $successRuns ?></div>
      <div class="label">Run Berhasil</div>
    </div>
    <div class="stat-card">
      <div class="icon">📊</div>
      <div class="value"><?= $totalRuns ?></div>
      <div class="label">Total Run</div>
    </div>
  </div>

  <!-- ROW: SCHEDULE + CONFIG -->
  <div style="display:grid;grid-template-columns:1fr 2fr;gap:1rem;margin-bottom:2rem;">
    <div>
      <p class="section-title">⏰ Jadwal</p>
      <div class="schedule-card">
        <div class="time"><?= $scheduledTime ?></div>
        <div class="label">WIB · Setiap Hari</div>
        <div class="next">⏳ Run berikutnya dalam <strong id="countdown"><?= $countdown ?></strong></div>
      </div>
    </div>
    <div>
      <p class="section-title">⚙️ Konfigurasi Aktif</p>
      <div class="config-grid">
        <div class="config-item">
          <span class="config-key">daily.target</span>
          <span class="config-val"><?= $dailyTarget ?> commits</span>
        </div>
        <div class="config-item">
          <span class="config-key">branches</span>
          <span class="config-val"><?= $branches ?></span>
        </div>
        <div class="config-item">
          <span class="config-key">retry.count</span>
          <span class="config-val"><?= $retryCount ?>x</span>
        </div>
        <div class="config-item">
          <span class="config-key">commit.message.style</span>
          <span class="config-val"><?= $randomMsg ? '🎲 random' : 'thematic' ?></span>
        </div>
        <div class="config-item">
          <span class="config-key">discord.enabled</span>
          <span class="config-val"><?= $discordOn ? '✅ yes' : '❌ no' ?></span>
        </div>
        <div class="config-item">
          <span class="config-key">randomize.delays</span>
          <span class="config-val"><?= ($config['randomize.delays'] ?? 'true') === 'true' ? '✅ yes' : '❌ no' ?></span>
        </div>
      </div>
    </div>
  </div>

  <!-- HEATMAP -->
  <p class="section-title">🟩 Contribution Heatmap (12 Weeks)</p>
  <div class="heatmap-wrap">
    <div class="heatmap" id="heatmap"></div>
    <div class="heatmap-legend">
      Less
      <div class="heatmap-cell"></div>
      <div class="heatmap-cell" data-level="1"></div>
      <div class="heatmap-cell" data-level="2"></div>
      <div class="heatmap-cell" data-level="3"></div>
      <div class="heatmap-cell" data-level="4"></div>
      More
    </div>
  </div>

  <!-- HISTORY TABLE -->
  <p class="section-title" style="margin-top:2rem;">📋 Run History</p>
  <div class="card">
    <table class="log-table">
      <thead>
        <tr>
          <th>#</th>
          <th>Tanggal</th>
          <th>Commits</th>
          <th>Durasi</th>
          <th>Status</th>
        </tr>
      </thead>
      <tbody>
        <?php if (empty($stats)): ?>
        <tr><td colspan="5" style="text-align:center;color:var(--text-muted);padding:2rem;">Belum ada data. Run GitHell --daily untuk memulai.</td></tr>
        <?php else: ?>
        <?php foreach (array_slice($stats, 0, 30) as $i => $s): ?>
        <tr>
          <td class="num"><?= $i + 1 ?></td>
          <td><?= htmlspecialchars($s['date']) ?></td>
          <td class="num"><?= htmlspecialchars($s['commits']) ?></td>
          <td class="dur"><?= isset($s['duration_sec']) ? $s['duration_sec'] . 's' : '-' ?></td>
          <td>
            <?php
              $st = $s['status'] ?? 'N/A';
              $cls = $st === 'SUCCESS' ? 'success' : ($st === 'FAILED' ? 'failed' : 'partial');
              $ico = $st === 'SUCCESS' ? '✅' : ($st === 'FAILED' ? '❌' : '⚠️');
              echo "<span class='badge $cls'>$ico $st</span>";
            ?>
          </td>
        </tr>
        <?php endforeach; ?>
        <?php endif; ?>
      </tbody>
    </table>
  </div>

  <!-- LOG VIEWER -->
  <p class="section-title" style="margin-top:2rem;">📄 Log Hari Ini (<?= $today ?>)</p>
  <div class="log-viewer" id="logViewer"><?php
    $lines = explode("\n", $logContent);
    foreach ($lines as $line) {
        $cls = '';
        if (stripos($line, 'error') !== false || stripos($line, 'failed') !== false || stripos($line, '❌') !== false) $cls = 'log-err';
        elseif (stripos($line, 'warning') !== false || stripos($line, '⚠') !== false) $cls = 'log-warn';
        elseif (stripos($line, '✅') !== false || stripos($line, 'success') !== false || stripos($line, 'selesai') !== false) $cls = 'log-ok';
        elseif (stripos($line, 'start') !== false || stripos($line, 'end') !== false) $cls = 'log-info';
        echo "<span class='$cls'>" . htmlspecialchars($line) . "</span>\n";
    }
  ?></div>

</main>

<script>
// ── Heatmap Builder ──────────────────────────────────────────────
const statsData = <?= json_encode(array_combine(
    array_column($stats, 'date'),
    array_column($stats, 'commits')
)) ?>;

function buildHeatmap() {
    const hm = document.getElementById('heatmap');
    const today = new Date();
    const start = new Date(today);
    start.setDate(start.getDate() - 7 * 12 + 1); // 12 weeks ago

    // Align to Monday
    const dayOfWeek = (start.getDay() + 6) % 7;
    start.setDate(start.getDate() - dayOfWeek);

    const maxCommits = Math.max(...Object.values(statsData).map(Number), 1);
    let col = null;

    for (let d = new Date(start); d <= today; d.setDate(d.getDate() + 1)) {
        const dow = (d.getDay() + 6) % 7; // Mon=0
        if (dow === 0) { col = document.createElement('div'); col.className = 'heatmap-col'; hm.appendChild(col); }
        if (!col) continue;

        const dateStr = d.toISOString().split('T')[0];
        const commits = parseInt(statsData[dateStr] || 0);
        const level = commits === 0 ? 0 : commits <= maxCommits * .25 ? 1 : commits <= maxCommits * .5 ? 2 : commits <= maxCommits * .75 ? 3 : 4;

        const cell = document.createElement('div');
        cell.className = 'heatmap-cell';
        if (level > 0) cell.setAttribute('data-level', level);
        cell.title = `${dateStr}: ${commits} commits`;
        col.appendChild(cell);
    }
}
buildHeatmap();

// ── Countdown Timer ──────────────────────────────────────────────
function updateCountdown() {
    const el = document.getElementById('countdown');
    if (!el) return;
    const parts = el.textContent.split(':').map(Number);
    let total = parts[0]*3600 + parts[1]*60 + parts[2];
    total = Math.max(0, total - 1);
    el.textContent = [
        Math.floor(total/3600),
        Math.floor((total%3600)/60),
        total%60
    ].map(v => String(v).padStart(2,'0')).join(':');
}
setInterval(updateCountdown, 1000);

// ── Auto-scroll log to bottom ────────────────────────────────────
const lv = document.getElementById('logViewer');
if (lv) lv.scrollTop = lv.scrollHeight;

// ── Auto-refresh setiap 60 detik ─────────────────────────────────
setTimeout(() => location.reload(), 60000);
</script>

</body>
</html>
