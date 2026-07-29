<?php
$BASE = dirname(__DIR__);

// Read stats
$statsFile = $BASE . '/stats.csv';
$stats = [];
if (file_exists($statsFile)) {
    $rows = array_filter(array_map('str_getcsv', file($statsFile)));
    $header = array_shift($rows);
    foreach ($rows as $r) {
        if (count($r) >= 4) {
            $stats[] = array_combine(array_slice($header, 0, count($r)), $r);
        }
    }
    $stats = array_reverse($stats);
}

$totalCommits = array_sum(array_column($stats, 'commits'));
$totalRuns = count($stats);
$successRuns = count(array_filter($stats, fn($s) => $s['status'] === 'SUCCESS'));
$todayRow = $stats[0] ?? null;
$todayCommits = $todayRow ? (int)$todayRow['commits'] : 0;
$todayStatus = $todayRow ? $todayRow['status'] : 'N/A';

// Read config
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

// Read today's log
$today = date('Y-m-d');
$logFile = $BASE . '/logs/daily_' . $today . '.log';
$logContent = file_exists($logFile) ? file_get_contents($logFile) : 'Log not available for today.';

// Next run calculation
$nextRun = new DateTime();
[$h, $m] = explode(':', $scheduledTime . ':00');
$nextRun->setTime((int)$h, (int)$m, 0);
if ($nextRun < new DateTime()) $nextRun->modify('+1 day');
$diff = $nextRun->diff(new DateTime());
$countdown = sprintf('%02d:%02d:%02d', $diff->h + ($diff->days * 24), $diff->i, $diff->s);
?>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Commit Automation Dashboard</title>
<link rel="stylesheet" href="style.css">
</head>
<body>

<header class="header">
  <div class="header-container">
    <div class="brand">Commit Automation</div>
    <div class="time"><?= date('Y-m-d H:i:s') ?></div>
  </div>
</header>

<main class="container">
  <div class="stats-grid">
    <div class="stat-card">
      <div class="label">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 6.042A8.967 8.967 0 006 3.75c-1.052 0-2.062.18-3 .512v14.25A8.987 8.987 0 016 18c2.305 0 4.408.867 6 2.292m0-14.25a8.966 8.966 0 016-2.292c1.052 0 2.062.18 3 .512v14.25A8.987 8.987 0 0018 18a8.967 8.967 0 00-6 2.292m0-14.25v14.25" />
        </svg>
        Total Commits
      </div>
      <div class="value"><?= number_format($totalCommits) ?></div>
    </div>
    <div class="stat-card">
      <div class="label">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5" />
        </svg>
        Today's Commits
      </div>
      <div class="value"><?= $todayCommits ?></div>
    </div>
    <div class="stat-card">
      <div class="label">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        Successful Runs
      </div>
      <div class="value"><?= $successRuns ?> / <?= $totalRuns ?></div>
    </div>
    <div class="stat-card">
      <div class="label">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        Next Run
      </div>
      <div class="value" id="countdown"><?= $countdown ?></div>
    </div>
  </div>

  <div class="section-title">
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" style="width: 20px; height: 20px;">
      <path stroke-linecap="round" stroke-linejoin="round" d="M9.594 3.94c.09-.542.56-.94 1.11-.94h2.593c.55 0 1.02.398 1.11.94l.213 1.281c.063.374.313.686.645.87.074.04.147.083.22.127.324.196.72.257 1.075.124l1.217-.456a1.125 1.125 0 011.37.49l1.296 2.247a1.125 1.125 0 01-.26 1.431l-1.003.827c-.293.24-.438.613-.431.992a6.759 6.759 0 010 .255c-.007.378.138.75.43.99l1.005.828c.424.35.534.954.26 1.43l-1.298 2.247a1.125 1.125 0 01-1.369.491l-1.217-.456c-.355-.133-.75-.072-1.076.124a6.57 6.57 0 01-.22.128c-.331.183-.581.495-.644.869l-.213 1.28c-.09.543-.56.941-1.11.941h-2.594c-.55 0-1.02-.398-1.11-.94l-.213-1.281c-.062-.374-.312-.686-.644-.87a6.52 6.52 0 01-.22-.127c-.325-.196-.72-.257-1.076-.124l-1.217.456a1.125 1.125 0 01-1.369-.49l-1.297-2.247a1.125 1.125 0 01.26-1.431l1.004-.827c.292-.24.437-.613.43-.992a6.932 6.932 0 010-.255c.007-.378-.138-.75-.43-.99l-1.004-.828a1.125 1.125 0 01-.26-1.43l1.297-2.247a1.125 1.125 0 011.37-.491l1.216.456c.356.133.751.072 1.076-.124.072-.044.146-.087.22-.128.332-.183.582-.495.644-.869l.214-1.281z" />
      <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
    </svg>
    Configuration
  </div>
  <div class="config-grid">
    <?php foreach ($config as $k => $v): ?>
      <div class="config-item">
        <span class="config-key"><?= htmlspecialchars($k) ?></span>
        <span class="config-val"><?= htmlspecialchars($v) ?></span>
      </div>
    <?php endforeach; ?>
  </div>

  <div class="section-title">
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" style="width: 20px; height: 20px;">
      <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6A2.25 2.25 0 016 3.75h2.25A2.25 2.25 0 0110.5 6v2.25a2.25 2.25 0 01-2.25 2.25H6a2.25 2.25 0 01-2.25-2.25V6zM3.75 15.75A2.25 2.25 0 016 13.5h2.25a2.25 2.25 0 012.25 2.25V18a2.25 2.25 0 01-2.25 2.25H6A2.25 2.25 0 013.75 18v-2.25zM13.5 6a2.25 2.25 0 012.25-2.25H18A2.25 2.25 0 0120.25 6v2.25A2.25 2.25 0 0118 10.5h-2.25a2.25 2.25 0 01-2.25-2.25V6zM13.5 15.75a2.25 2.25 0 012.25-2.25H18a2.25 2.25 0 012.25 2.25V18A2.25 2.25 0 0118 20.25h-2.25A2.25 2.25 0 0113.5 18v-2.25z" />
    </svg>
    Contribution Heatmap
  </div>
  <div class="heatmap-container">
    <div class="heatmap" id="heatmap"></div>
  </div>

  <div class="section-title">
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" style="width: 20px; height: 20px;">
      <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 6.75h12M8.25 12h12m-12 5.25h12M3.75 6.75h.007v.008H3.75V6.75zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zM3.75 12h.007v.008H3.75V12zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm-.375 5.25h.007v.008H3.75v-.008zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0z" />
    </svg>
    Run History
  </div>
  <div class="data-table-container">
    <table class="data-table">
      <thead>
        <tr>
          <th>Date</th>
          <th>Commits</th>
          <th>Duration</th>
          <th>Status</th>
        </tr>
      </thead>
      <tbody>
        <?php if (empty($stats)): ?>
        <tr><td colspan="4" class="empty">No run history available.</td></tr>
        <?php else: ?>
        <?php foreach (array_slice($stats, 0, 10) as $s): ?>
        <tr>
          <td><?= htmlspecialchars($s['date']) ?></td>
          <td><?= htmlspecialchars($s['commits']) ?></td>
          <td><?= isset($s['duration_sec']) ? $s['duration_sec'] . 's' : '-' ?></td>
          <td>
            <span class="status-badge status-<?= strtolower($s['status'] ?? 'unknown') ?>">
              <?= htmlspecialchars($s['status'] ?? 'UNKNOWN') ?>
            </span>
          </td>
        </tr>
        <?php endforeach; ?>
        <?php endif; ?>
      </tbody>
    </table>
  </div>

  <div class="section-title">
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" style="width: 20px; height: 20px;">
      <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
    </svg>
    System Log
  </div>
  <div class="log-viewer" id="logViewer">
    <?php
    $lines = explode("\n", $logContent);
    foreach ($lines as $line) {
        $line = trim(htmlspecialchars($line));
        if ($line === '') continue;
        
        // Simple timestamp parser for aesthetic formatting (assuming typical format "[TIME] message")
        $time = '';
        if (preg_match('/^\[(.*?)\]\s*(.*)$/', $line, $matches)) {
            $time = '<span class="log-time">[' . $matches[1] . ']</span>';
            $line = $matches[2];
        }
        
        echo '<div class="log-line">' . $time . $line . '</div>';
    }
    ?>
  </div>

</main>

<script>
const statsData = <?= json_encode(array_combine(
    array_column($stats, 'date'),
    array_column($stats, 'commits')
)) ?>;

function buildHeatmap() {
    const hm = document.getElementById('heatmap');
    if (!hm) return;
    
    const today = new Date();
    const start = new Date(today);
    start.setDate(start.getDate() - 7 * 12 + 1);
    
    const dayOfWeek = (start.getDay() + 6) % 7;
    start.setDate(start.getDate() - dayOfWeek);

    const maxCommits = Math.max(...Object.values(statsData).map(Number), 1);
    let col = null;

    for (let d = new Date(start); d <= today; d.setDate(d.getDate() + 1)) {
        const dow = (d.getDay() + 6) % 7;
        if (dow === 0) {
            col = document.createElement('div');
            col.className = 'heatmap-col';
            hm.appendChild(col);
        }
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

function updateCountdown() {
    const el = document.getElementById('countdown');
    if (!el) return;
    const parts = el.textContent.split(':').map(Number);
    if (parts.length !== 3 || isNaN(parts[0])) return;
    
    let total = parts[0] * 3600 + parts[1] * 60 + parts[2];
    total = Math.max(0, total - 1);
    
    el.textContent = [
        Math.floor(total / 3600),
        Math.floor((total % 3600) / 60),
        total % 60
    ].map(v => String(v).padStart(2, '0')).join(':');
}
setInterval(updateCountdown, 1000);

const lv = document.getElementById('logViewer');
if (lv) lv.scrollTop = lv.scrollHeight;

setTimeout(() => location.reload(), 60000);
</script>
</body>
</html>
