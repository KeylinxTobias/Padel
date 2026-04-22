import { useEffect, useState } from "react";
import { Scoreboard } from "./Scoreboard";

const API_URL = import.meta.env.VITE_API_URL ?? "http://localhost:5000";

type FetchState = "loading" | "found" | "not_found" | "error";

function App() {
  const [state, setState] = useState<FetchState>("loading");
  const [matchId, setMatchId] = useState<string | null>(null);

  const fetchActive = () => {
    setState("loading");
    fetch(`${API_URL}/api/matches/active`)
      .then((res) => {
        if (res.status === 404) { setState("not_found"); return null; }
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
      })
      .then((data) => {
        if (data) { setMatchId(data.id); setState("found"); }
      })
      .catch(() => setState("error"));
  };

  useEffect(() => { fetchActive(); }, []);

  if (state === "found" && matchId) {
    return <Scoreboard matchId={matchId} />;
  }

  const message =
    state === "loading" ? "Söker aktiv match…" :
    state === "not_found" ? "Ingen aktiv match pågår." :
    "Kunde inte nå backend.";

  return (
    <div style={styles.root}>
      <h1 style={styles.title}>Padel Live</h1>
      <p style={styles.sub}>{message}</p>
      {state !== "loading" && (
        <button style={styles.button} onClick={fetchActive}>
          Försök igen
        </button>
      )}
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  root: {
    minHeight: "100vh",
    background: "#111",
    color: "#fff",
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    justifyContent: "center",
    gap: 20,
    fontFamily: "'Helvetica Neue', Arial, sans-serif",
  },
  title: { fontSize: 36, margin: 0 },
  sub: { color: "#888", margin: 0, fontSize: 18 },
  button: {
    padding: "12px 32px",
    fontSize: 18,
    borderRadius: 8,
    background: "#1976d2",
    color: "#fff",
    border: "none",
    cursor: "pointer",
  },
};

export default App;
