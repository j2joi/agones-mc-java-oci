package dev.agones.cmd;


import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


import br.com.azalim.mcserverping.MCPing;
import br.com.azalim.mcserverping.MCPingOptions;
import br.com.azalim.mcserverping.MCPingResponse;
import br.com.azalim.mcserverping.MCPingResponse.Description;
import br.com.azalim.mcserverping.MCPingResponse.Player;
import br.com.azalim.mcserverping.MCPingResponse.Players;
import br.com.azalim.mcserverping.MCPingResponse.Version;
import dev.agones.AgonesSDK;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public final class Monitor {
    private static int attempts_left=20;
    private static final int max_failed_healthcheck=5;
    private static int current_failed_healthcheck=0;

    private AgonesSDK sdk;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

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
        int timeout = Integer.parseInt(System.getenv().getOrDefault("MC_TIMEOUT", "2"));
        this.sdk = new AgonesSDK(port);
        this.options = MCPingOptions.builder()
                .hostname("localhost")
                .port(port)
                .build();
    }

    public Monitor(int mc_server_port,int timeout) {
        this.sdk = new AgonesSDK();
        this.options = MCPingOptions.builder()
                .hostname("localhost")
                .port(mc_server_port)
                .timeout(timeout)
                .build();
    }

    public void runMonitor(int ping_interval, int gs_warmup_time,int attempts) {
        //initial attempt
        if(PingUntilStartup(ping_interval,attempts,gs_warmup_time)){
            System.out.println("GameServer failed to startup.  Exiting...");
            System.exit(1);
        }
        //notify Agones GameServer is in Ready state.
        //this.sdk.ready();
        System.out.println("GameServer Started. Triggering Health check");

        //Proceed with continuous healthChecks until Shutdown
        gsHealthCheck(attempts);

        /*Description description = this.reply.getDescription();

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

        System.out.println(String.format("Favicon: %s", reply.getFavicon()));*/
        System.out.println("We sure did attempt to monitor your GameServer");
    }

    @SneakyThrows
    private boolean PingUntilStartup(int ping_interval,int attempts,int warmup_time){
        boolean isUp = false;
        int startup_attempts = attempts;
        //wait for GS to be fully up and running.
        //Thread.sleep(warmup_time);
        do {
            try {
                reply = MCPing.getPing(this.options);
                if (reply != null){
                    if (reply.getPlayers().getMax() != 0) {
                        System.out.println("Initial Ping succeeded" + reply.getPlayers().getMax());
                        this.sdk.alpha().setCapacity(reply.getPlayers().getMax());
                        this.sdk.ready();
                        isUp=true;
                        break;
                    }
                }
            } catch (Exception catchAll) {
                System.out.println(this.options.getHostname() + " is down or unreachable.... ");
                System.out.println("Initial Ping: Attempts left : "+startup_attempts);
            }
            startup_attempts--;
            Thread.sleep(warmup_time);
        }while(startup_attempts > 0);
        return isUp;
    }
    @SneakyThrows
    private void gsHealthCheck(int attempts_health){
        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        Runnable task1 = () -> {
            //count++;
            System.out.println("Running...Ping HealthCheck Task - attempts left : " + attempts_left);
            try {
                reply = MCPing.getPing(this.options);
                if (reply !=null && reply.getPlayers().getMax() >0){
                    this.sdk.health();
                    if(reply.getPlayers().getOnline()==reply.getPlayers().getMax()){
                        //auto scale.   For now do do nothing.
                        System.out.println("Need to prepare for scaling out.  Treggering autoscale.");
                    }
                }
                //reply.getPlayers().getSample()
                //this.sdk.alpha().playerConnect();
                //reply.getPlayers().
                attempts_left=attempts_health;
            } catch (IOException ex) {
                System.out.println(this.options.getHostname() + " is down or unreachable.... ");
                System.out.println("Health Check failed: Attempts left : "+attempts_left);
                attempts_left--;
            }

        };

        // init Delay = 5, repeat the task every 1 second
        ScheduledFuture<?> scheduledFuture = ses.scheduleAtFixedRate(task1, 5, 1, TimeUnit.SECONDS);
        //scheduledFuture.
        while (true) {
            System.out.println("attempts left:" + attempts_left);
            Thread.sleep(1000);
            if (attempts_left <= 0) {
                System.out.println("No more attempts left, cancel healthcheck!");
                scheduledFuture.cancel(true);
                ses.shutdown();
                break;
            }
        }
    }


    private void ReadyPing() throws Exception {
        reply = MCPing.getPing(this.options);
    }


    public static void main (String[] args) {
        System.out.println("Minecraft Java Monitor Starting...");
        int mc_port = Integer.parseInt(System.getenv().getOrDefault("MC_PORT", "25565"));
        int ping_timeout = Integer.parseInt(System.getenv().getOrDefault("MC_TIMEOUT", "60000"));
        int interval = Integer.parseInt(System.getenv().getOrDefault("PING_INTERVAL", "5000"));
        int warmupTime = Integer.parseInt(System.getenv().getOrDefault("WARMUP_TIME", "30000"));
        int retries = Integer.parseInt(System.getenv().getOrDefault("MAX_ATTEMPTS", "10"));

        new Monitor(mc_port,ping_timeout).runMonitor(interval,warmupTime,retries);

    }
}