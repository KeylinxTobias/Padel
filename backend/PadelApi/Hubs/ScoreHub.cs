using Microsoft.AspNetCore.SignalR;

namespace PadelApi.Hubs;

/// <summary>
/// SignalR hub for live score streaming to web clients (iPad scoreboard etc.).
/// Clients join a match-specific group and receive ScoreUpdated events.
/// </summary>
public class ScoreHub : Hub
{
    /// <summary>Called by web clients to subscribe to a specific match.</summary>
    public async Task JoinMatch(string matchId)
    {
        await Groups.AddToGroupAsync(Context.ConnectionId, $"match-{matchId}");
    }

    public async Task LeaveMatch(string matchId)
    {
        await Groups.RemoveFromGroupAsync(Context.ConnectionId, $"match-{matchId}");
    }
}
