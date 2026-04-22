using Microsoft.AspNetCore.SignalR;
using Microsoft.EntityFrameworkCore;
using PadelApi.Data;
using PadelApi.Hubs;
using PadelApi.Models;

namespace PadelApi.Endpoints;

public static class MatchEndpoints
{
    public static void MapMatchEndpoints(this WebApplication app)
    {
        var group = app.MapGroup("/api/matches");

        // POST /api/matches — start a new match
        group.MapPost("/", async (StartMatchRequest req, PadelDbContext db) =>
        {
            var match = new Match
            {
                PointSystem = req.PointSystem,
                ServingTeam = req.ServingTeam,
            };
            db.Matches.Add(match);
            await db.SaveChangesAsync();
            return Results.Created($"/api/matches/{match.Id}", match.ToDto());
        });

        // GET /api/matches/active — senaste pågående match (ingen EndedAt)
        group.MapGet("/active", async (PadelDbContext db) =>
        {
            var match = await db.Matches
                .Include(m => m.Events.OrderBy(e => e.Timestamp))
                .Where(m => m.EndedAt == null)
                .OrderByDescending(m => m.StartedAt)
                .FirstOrDefaultAsync();

            return match is null ? Results.NotFound() : Results.Ok(match.ToDetailDto());
        });

        // GET /api/matches — list all matches (newest first)
        group.MapGet("/", async (PadelDbContext db) =>
        {
            var matches = await db.Matches
                .OrderByDescending(m => m.StartedAt)
                .Select(m => m.ToDto())
                .ToListAsync();
            return Results.Ok(matches);
        });

        // GET /api/matches/{id} — get match with latest score snapshot
        group.MapGet("/{id:guid}", async (Guid id, PadelDbContext db) =>
        {
            var match = await db.Matches
                .Include(m => m.Events.OrderBy(e => e.Timestamp))
                .FirstOrDefaultAsync(m => m.Id == id);

            if (match is null) return Results.NotFound();
            return Results.Ok(match.ToDetailDto());
        });

        // POST /api/matches/{id}/events — add a point event (idempotent)
        group.MapPost("/{id:guid}/events", async (
            Guid id,
            AddEventRequest req,
            PadelDbContext db,
            IHubContext<ScoreHub> hub) =>
        {
            var match = await db.Matches.FindAsync(id);
            if (match is null) return Results.NotFound();
            if (match.EndedAt.HasValue) return Results.Conflict("Match already ended.");

            // Idempotency: ignore duplicate events from watch
            if (await db.PointEvents.AnyAsync(e => e.ClientEventId == req.ClientEventId))
                return Results.Ok("duplicate");

            var ev = new PointEvent
            {
                ClientEventId = req.ClientEventId,
                MatchId = id,
                Timestamp = req.Timestamp.ToUniversalTime(),
                Team = req.Team,
                ScoreSnapshot = req.ScoreSnapshot,
            };
            db.PointEvents.Add(ev);
            await db.SaveChangesAsync();

            // Push live update to all iPad/web clients watching this match
            await hub.Clients.Group($"match-{id}").SendAsync("ScoreUpdated", new
            {
                matchId = id,
                ev.Team,
                ev.Timestamp,
                scoreSnapshot = req.ScoreSnapshot,
            });

            return Results.Created($"/api/matches/{id}/events/{ev.Id}", ev);
        });

        // PATCH /api/matches/{id}/end — mark match as ended
        group.MapPatch("/{id:guid}/end", async (Guid id, PadelDbContext db, IHubContext<ScoreHub> hub) =>
        {
            var match = await db.Matches.FindAsync(id);
            if (match is null) return Results.NotFound();

            match.EndedAt = DateTime.UtcNow;
            await db.SaveChangesAsync();

            await hub.Clients.Group($"match-{id}").SendAsync("MatchEnded", new { matchId = id });

            return Results.Ok(match.ToDto());
        });
    }
}

// ── DTOs & request models ─────────────────────────────────────────────────────

public record StartMatchRequest(string PointSystem, string ServingTeam);

public record AddEventRequest(
    Guid ClientEventId,
    string Team,
    DateTime Timestamp,
    string ScoreSnapshot);

// Extension methods to map models to DTOs
file static class MappingExtensions
{
    public static object ToDto(this Match m) => new
    {
        m.Id,
        m.StartedAt,
        m.EndedAt,
        m.PointSystem,
        m.ServingTeam,
    };

    public static object ToDetailDto(this Match m) => new
    {
        m.Id,
        m.StartedAt,
        m.EndedAt,
        m.PointSystem,
        m.ServingTeam,
        LatestScore = m.Events.LastOrDefault()?.ScoreSnapshot,
        EventCount = m.Events.Count,
    };
}
