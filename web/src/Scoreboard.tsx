import { useScoreHub } from "./useScoreHub";
import type { ScoreSnapshot } from "./types";

interface Props {
  matchId: string;
}

export function Scoreboard({ matchId }: Props) {
  const { score, connected } = useScoreHub(matchId);

  return (
    <div style={styles.root}>
      <ConnectionBadge connected={connected} />

      {score ? (
        <ScoreDisplay score={score} />
      ) : (
        <p style={styles.waiting}>Väntar på matchdata…</p>
      )}
    </div>
  );
}

function ScoreDisplay({ score }: { score: ScoreSnapshot }) {
  const serveLabel =
    `${score.servingTeam === "US" ? "Vi" : "Dom"} – spelare ${score.servingPlayer}` +
    (score.isTiebreak ? " (Tiebreak)" : "");

  return (
    <div style={styles.scoreWrapper}>
      {score.matchEnded && <div style={styles.matchEnded}>MATCH SLUT</div>}

      {/* Serve indicator */}
      <div style={styles.serveRow}>
        <span style={styles.serveDot}>●</span>
        <span style={styles.serveLabel}>{serveLabel} servar</span>
      </div>

      {/* Team labels */}
      <div style={styles.row}>
        <span style={styles.teamLabel}>VI</span>
        <span style={styles.separator} />
        <span style={styles.teamLabel}>DOM</span>
      </div>

      {/* Points */}
      <div style={styles.row}>
        <span style={styles.points}>{score.pointLabelUs}</span>
        <span style={styles.dash}>–</span>
        <span style={styles.points}>{score.pointLabelThem}</span>
      </div>

      {/* Games */}
      <div style={styles.row}>
        <span style={styles.games}>{score.gamesUs}</span>
        <span style={styles.subLabel}>games</span>
        <span style={styles.games}>{score.gamesThem}</span>
      </div>

      {/* Sets */}
      <div style={styles.row}>
        <span style={styles.sets}>{score.setsUs}</span>
        <span style={styles.subLabel}>set</span>
        <span style={styles.sets}>{score.setsThem}</span>
      </div>
    </div>
  );
}

function ConnectionBadge({ connected }: { connected: boolean }) {
  return (
    <div style={{ ...styles.badge, background: connected ? "#2e7d32" : "#b71c1c" }}>
      {connected ? "● Live" : "○ Återansluter…"}
    </div>
  );
}

// ── Styles ────────────────────────────────────────────────────────────────────

const styles: Record<string, React.CSSProperties> = {
  root: {
    minHeight: "100vh",
    background: "#111",
    color: "#fff",
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    justifyContent: "center",
    fontFamily: "'Helvetica Neue', Arial, sans-serif",
    userSelect: "none",
  },
  badge: {
    position: "fixed",
    top: 16,
    right: 20,
    padding: "4px 12px",
    borderRadius: 20,
    fontSize: 14,
    color: "#fff",
  },
  scoreWrapper: {
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    gap: 12,
    width: "100%",
    maxWidth: 600,
    padding: "0 24px",
  },
  matchEnded: {
    fontSize: 28,
    fontWeight: 700,
    color: "#f44336",
    letterSpacing: 4,
  },
  serveRow: {
    display: "flex",
    alignItems: "center",
    gap: 8,
    marginBottom: 8,
  },
  serveDot: {
    color: "#4caf50",
    fontSize: 18,
  },
  serveLabel: {
    fontSize: 20,
    color: "#aaa",
  },
  row: {
    display: "flex",
    width: "100%",
    alignItems: "center",
    justifyContent: "space-between",
  },
  teamLabel: {
    flex: 1,
    textAlign: "center",
    fontSize: 28,
    fontWeight: 600,
    letterSpacing: 4,
    color: "#ccc",
  },
  separator: { flex: 0.2 },
  points: {
    flex: 1,
    textAlign: "center",
    fontSize: 120,
    fontWeight: 900,
    lineHeight: 1,
  },
  dash: {
    fontSize: 60,
    color: "#555",
    flex: 0.2,
    textAlign: "center",
  },
  games: {
    flex: 1,
    textAlign: "center",
    fontSize: 64,
    fontWeight: 700,
  },
  sets: {
    flex: 1,
    textAlign: "center",
    fontSize: 40,
    fontWeight: 500,
    color: "#aaa",
  },
  subLabel: {
    flex: 0.3,
    textAlign: "center",
    fontSize: 16,
    color: "#555",
  },
  waiting: {
    fontSize: 24,
    color: "#666",
  },
};
