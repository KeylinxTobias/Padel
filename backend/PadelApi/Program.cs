using Microsoft.EntityFrameworkCore;
using PadelApi.Data;
using PadelApi.Endpoints;
using PadelApi.Hubs;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddDbContext<PadelDbContext>(options =>
    options.UseSqlServer(builder.Configuration.GetConnectionString("DefaultConnection")));

builder.Services.AddSignalR();

builder.Services.AddCors(options =>
    options.AddDefaultPolicy(policy =>
        policy.WithOrigins(builder.Configuration.GetSection("AllowedOrigins").Get<string[]>() ?? ["http://localhost:5173"])
              .AllowAnyHeader()
              .AllowAnyMethod()
              .AllowCredentials())); // required for SignalR

builder.Services.AddOpenApi();

var app = builder.Build();

if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();

    // Auto-run migrations in dev
    using var scope = app.Services.CreateScope();
    var db = scope.ServiceProvider.GetRequiredService<PadelDbContext>();
    await db.Database.MigrateAsync();
}

app.UseCors();
app.MapHub<ScoreHub>("/hubs/score");
app.MapMatchEndpoints();

// Health check — används av mobilappen för att verifiera backend-anslutning
app.MapGet("/health", (PadelDbContext db) => new
{
    status = "ok",
    timestamp = DateTime.UtcNow,
    database = db.Database.CanConnect() ? "ok" : "unreachable",
})
.WithName("HealthCheck");

app.Run();
