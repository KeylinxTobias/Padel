using System.Text.Json;

namespace PadelApi.Models;

public class PointEvent
{
    public Guid Id { get; set; } = Guid.NewGuid();

    // GUID generated on the watch — used for idempotency
    public Guid ClientEventId { get; set; }

    public Guid MatchId { get; set; }
    public Match Match { get; set; } = null!;

    // Timestamp from the watch (may arrive out of order)
    public DateTime Timestamp { get; set; }

    public string Team { get; set; } = "US"; // US | THEM

    // Full score snapshot after this point (JSON)
    public string ScoreSnapshot { get; set; } = "{}";
}
