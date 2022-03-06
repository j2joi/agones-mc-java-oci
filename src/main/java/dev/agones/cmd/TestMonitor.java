package dev.agones.cmd;

import br.com.azalim.mcserverping.MCPing;
import br.com.azalim.mcserverping.MCPingOptions;
import br.com.azalim.mcserverping.MCPingResponse;
import dev.agones.AgonesSDK;

import java.io.IOException;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;

public class TestMonitor {
    private static final int max_failed_health_checks=5;
    private static int current_failed_health_checks=0;
    private static Clock clock = Clock.systemDefaultZone();

    public static void log(String message){

        System.out.println("[ "+clock.instant()+"] "+ message);
    }

    public static void waitforEvent(int delay_time){
        try {
            log("before waitForEvent "+delay_time);
            TimeUnit.SECONDS.sleep(delay_time);
            log("after waitForEvent ");
        } catch (InterruptedException ie) {
            log("Catch Exception "+ie.getMessage());
            Thread.currentThread().interrupt();
            log("Catch Exception after Interrupt ");
        }
    }

    public static void main(String[] args) {

        final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        log("Minecraft Java Monitor Starting...:"+TestMonitor.class.getCanonicalName());
        int mc_port = Integer.parseInt(System.getenv().getOrDefault("MC_PORT", "25565"));
        int ping_timeout = Integer.parseInt(System.getenv().getOrDefault("MC_TIMEOUT", "5000"));
        int interval = Integer.parseInt(System.getenv().getOrDefault("PING_INTERVAL", "5000"));
        int warmupTime = Integer.parseInt(System.getenv().getOrDefault("WARMUP_TIME", "60"));
        int retries = Integer.parseInt(System.getenv().getOrDefault("MAX_ATTEMPTS", "5"));
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss");
        log("- AGONES_SDK_PORT: "+System.getenv().get("AGONES_SDK_HTTP_PORT"));
        log("- MC_PORT: "+ System.getenv().get("MC_PORT"));
        log("- MC_TIMEOUT: "+ System.getenv().get("MC_TIMEOUT"));
        log("- PING_INTERVAL: "+ System.getenv().get("PING_INTERVAL"));
        log("- WARMUP_TIME: "+ System.getenv().get("WARMUP_TIME"));
        log("- MAX_ATTEMPTS: "+ System.getenv().get("MAX_ATTEMPTS"));
        log("Init Agones SDK:");
        AgonesSDK sdk = new AgonesSDK();
        log("Agones SDK Initialized");
        MCPingResponse reply = null;
        MCPingOptions options;
        options = MCPingOptions.builder()
                .hostname("localhost")
                .port(mc_port)
                .timeout(ping_timeout)
                .build();
        boolean isRunning = false;
        log("GameServer Initiating...");
        waitforEvent(warmupTime);
        log("GameServer WarmUpTim - Completed");
        for (int w=0; w <= retries; w++) {
            try {
                log("Warm Up Ping");
                reply = MCPing.getPing(options);
                if (reply != null && reply.getPlayers().getMax() > 0) {
                    isRunning = true;
                    log("[ Initial Ping succeeded: No: Players:  "+ reply.getPlayers().getMax());
                    sdk.ready();
                    break;
                }
            } catch (Exception e) {
                isRunning = false;
            } finally {
                waitforEvent(interval);
            }
        }
        if (isRunning) {
            try {
                log("GameServer Descriptions: " + reply.getDescription());
                sdk.ready();
                Runnable task1 = () -> {
                    log("Running...Ping HealthCheck Task");
                    try {
                        MCPing.getPing(options);
                        sdk.health();
                        current_failed_health_checks=0;
                    } catch (IOException e) {
                        log("Failed Health check");
                        current_failed_health_checks++;
                    }
                };
                ScheduledFuture<?> scheduledFuture = executorService.scheduleAtFixedRate(task1, 0, 2, TimeUnit.SECONDS);
                log("Health Started...");
                while(isRunning) {
                    if(current_failed_health_checks>max_failed_health_checks){
                        scheduledFuture.cancel(true);
                        executorService.shutdown();
                        log("Health Check stopped!");
                        log("Shutting down SDK...");
                        sdk.shutdown();
                        isRunning=false;
                    }
                    waitforEvent(2);
                }
                log("Done: ");

                //System.out.println("GameServer by SDK: " +sdk.gameServer().getObjectMeta().getAnnotations());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

