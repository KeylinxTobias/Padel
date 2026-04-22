export interface ScoreSnapshot {
  pointsUs: number;
  pointsThem: number;
  pointLabelUs: string;
  pointLabelThem: string;
  gamesUs: number;
  gamesThem: number;
  setsUs: number;
  setsThem: number;
  servingTeam: "US" | "THEM";
  servingPlayer: 1 | 2;
  isTiebreak: boolean;
  matchEnded: boolean;
}

export interface ScoreUpdatedEvent {
  matchId: string;
  team: "US" | "THEM";
  timestamp: string;
  scoreSnapshot: string; // JSON string of ScoreSnapshot
}
