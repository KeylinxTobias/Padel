using Microsoft.EntityFrameworkCore;
using PadelApi.Models;

namespace PadelApi.Data;

public class PadelDbContext(DbContextOptions<PadelDbContext> options) : DbContext(options)
{
    public DbSet<Match> Matches => Set<Match>();
    public DbSet<PointEvent> PointEvents => Set<PointEvent>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<PointEvent>()
            .HasIndex(e => e.ClientEventId)
            .IsUnique(); // idempotency — prevent duplicate events from watch

        modelBuilder.Entity<PointEvent>()
            .HasOne(e => e.Match)
            .WithMany(m => m.Events)
            .HasForeignKey(e => e.MatchId)
            .OnDelete(DeleteBehavior.Cascade);
    }
}
