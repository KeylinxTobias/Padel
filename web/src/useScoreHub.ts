import { useEffect, useState, useRef } from "react";
import * as signalR from "@microsoft/signalr";
import type { ScoreSnapshot, ScoreUpdatedEvent } from "./types";

const HUB_URL = import.meta.env.VITE_HUB_URL ?? "http://localhost:5000/hubs/score";

export function useScoreHub(matchId: string) {
  const [score, setScore] = useState<ScoreSnapshot | null>(null);
  const [connected, setConnected] = useState(false);
  const connectionRef = useRef<signalR.HubConnection | null>(null);

  useEffect(() => {
    if (!matchId) return;

    const connection = new signalR.HubConnectionBuilder()
      .withUrl(HUB_URL)
      .withAutomaticReconnect()
      .build();

    connectionRef.current = connection;

    connection.on("ScoreUpdated", (event: ScoreUpdatedEvent) => {
      try {
        const snapshot: ScoreSnapshot = JSON.parse(event.scoreSnapshot);
        setScore(snapshot);
      } catch {
        console.error("Failed to parse score snapshot", event.scoreSnapshot);
      }
    });

    connection.on("MatchEnded", () => {
      setScore((prev) => prev ? { ...prev, matchEnded: true } : prev);
    });

    connection.onreconnected(() => {
      connection.invoke("JoinMatch", matchId);
      setConnected(true);
    });

    connection.onclose(() => setConnected(false));

    connection.start().then(() => {
      connection.invoke("JoinMatch", matchId);
      setConnected(true);
    });

    return () => {
      connection.stop();
    };
  }, [matchId]);

  return { score, connected };
}
