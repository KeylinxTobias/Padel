namespace PadelApi.Models;

public class Match
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public DateTime StartedAt { get; set; } = DateTime.UtcNow;
    public DateTime? EndedAt { get; set; }
    public string PointSystem { get; set; } = "GOLDEN"; // GOLDEN | ADVANTAGE
    public string ServingTeam { get; set; } = "US";     // US | THEM

    public ICollection<PointEvent> Events { get; set; } = [];
}
