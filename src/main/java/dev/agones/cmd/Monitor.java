package dev.agones.cmd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import br.com.azalim.mcserverping.MCPing;
import br.com.azalim.mcserverping.MCPingOptions;
import br.com.azalim.mcserverping.MCPingResponse;
import br.com.azalim.mcserverping.MCPingResponse.Description;
import br.com.azalim.mcserverping.MCPingResponse.Player;
import br.com.azalim.mcserverping.MCPingResponse.Players;
import br.com.azalim.mcserverping.MCPingResponse.Version;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public final class Monitor {

    //private final AlphaSdk alphaSdk = new AlphaSdk(this);
    //private final BetaSdk betaSdk = new BetaSdk(this);
    MCPingResponse reply;
    MCPingOptions options;

    public Monitor() {
        this(Integer.parseInt(System.getenv().getOrDefault("AGONES_SDK_HTTP_PORT", "9358")));
    }

    public Monitor(int port) {
        /*this.retrofit = new Retrofit.Builder()
                .baseUrl(new HttpUrl.Builder().scheme("http").host("localhost").port(port).build())
                .addCallAdapterFactory(SynchronousCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();*/

        this.options = MCPingOptions.builder()
                .hostname("localhost")
                .port(port)
                .build();
    }

    public void runMonitor() {
        try {
            reply = MCPing.getPing(this.options);
        } catch (IOException ex) {
            System.out.println(this.options.getHostname() + " is down or unreachable.");
            return;
        }

        System.out.println(String.format("Full response from %s:", this.options.getHostname()));
        System.out.println();

        Description description = this.reply.getDescription();

        System.out.println("Description:");
        System.out.println("    Raw: " + description.getText());
        System.out.println("    No color codes: " + description.getStrippedText());
        System.out.println();

        Players players = this.reply.getPlayers();

        System.out.println("Players: ");
        System.out.println("    Online count: " + players.getOnline());
        System.out.println("    Max players: " + players.getMax());
        System.out.println();

        // Can be null depending on the server
        List<Player> sample = players.getSample();

        if (sample != null) {
            System.out.println("    Players: " + players.getSample().stream()
                    .map(player -> String.format("%s@%s", player.getName(), player.getId()))
                    .collect(Collectors.joining(", "))
            );
            System.out.println();
        }

        Version version = reply.getVersion();

        System.out.println("Version: ");

        // The protocol is the version number: http://wiki.vg/Protocol_version_numbers
        System.out.println("    Protocol: " + version.getProtocol());
        System.out.println("    Name: " + version.getName());
        System.out.println();

        System.out.println(String.format("Favicon: %s", reply.getFavicon()));
    }

    /*public void RunMonitor(cmd *cobra.Command, args []string) {
        cfg := config.NewMonitorConfig();

        // Create new timed pinger
        pinger, err := ping.NewTimed(cfg.GetHost(), uint16(cfg.GetPort()), cfg.GetTimeout(), cfg.GetEdition());

        if err != nil {
            logger.Fatal("error creating ping client", zap.Error(err));
        }

        // Startup delay before the first ping (initial-delay)
        logger.Info("Starting up...");
        time.Sleep(cfg.GetInitialDelay());

        stop := signal.SetupSignalHandler(logger);

        // Ping server until startup
        err = pingUntilStartup(cfg.GetAttempts(), cfg.GetInterval(), pinger, stop);

        // Exit in case of unsuccessful startup
        if err != nil {
            if errors.Is(err, &ProcessStopped{}) {
                os.Exit(0);
            }
            logger.Fatal("fatal Mincraft server. exiting...", zap.Error(err));
        }

        // delay before next ping cycle
        time.Sleep(cfg.GetInterval());

        // Ping infinitely or until after a series of unsuccessful pings
        err = pingUntilFatal(cfg.GetAttempts(), cfg.GetInterval(), pinger, stop);

        // Exit in case of fatal server
        if err != nil {
            if errors.Is(err, &ProcessStopped{}) {
                os.Exit(0);
            }
            logger.Fatal("fatal Mincraft server. exiting...", zap.Error(err));
        }
    }*/

    public static void main (String[] args) {
        System.out.println("My Java Hello World!");
        new Monitor().runMonitor();

    }

}