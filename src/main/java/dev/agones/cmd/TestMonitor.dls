package dev.agones.cmd;

import br.com.azalim.mcserverping.MCPing;
import br.com.azalim.mcserverping.MCPingOptions;
import br.com.azalim.mcserverping.MCPingResponse;
import dev.agones.AgonesSDK;

import java.io.IOException;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.List;

public class TestMonitor {

    public static void main(String[] args) {

        final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        MCPingResponse reply = null;
        MCPingOptions options;
        boolean isRunning = false;
        log("Minecraft Java Monitor Starting...:"+TestMonitor.class.getCanonicalName());
        int mc_port = Integer.parseInt(System.getenv().getOrDefault("MC_PORT", "25565"));
        int ping_timeout = Integer.parseInt(System.getenv().getOrDefault("MC_TIMEOUT", "5000"));
        int interval = Integer.parseInt(System.getenv().getOrDefault("PING_INTERVAL", "5000"));
        int warmupTime = Integer.parseInt(System.getenv().getOrDefault("WARMUP_TIME", "60"));
        int retries = Integer.parseInt(System.getenv().getOrDefault("MAX_ATTEMPTS", "5"));

        /// ENV  Vars validation.
        log("- AGONES_SDK_PORT: "+System.getenv().get("AGONES_SDK_HTTP_PORT"));
        log("- MC_PORT: "+ System.getenv().get("MC_PORT"));
        log("- MC_TIMEOUT: "+ System.getenv().get("MC_TIMEOUT"));
        log("- PING_INTERVAL: "+ System.getenv().get("PING_INTERVAL"));
        log("- WARMUP_TIME: "+ System.getenv().get("WARMUP_TIME"));
        log("- MAX_ATTEMPTS: "+ System.getenv().get("MAX_ATTEMPTS"));
        log("Init Agones SDK:");

/////////////// Steps /////////////
// 1.  Init Agones
// 2.  Build Minecraft Health check (MC Pinger)
// 3.  Trigger two types of Health checks
//        (a)  Ready ():  MC Container UP and Running  -- Mark Kubernetes Pod Status to Ready through Agones SDK ready()
//        (b)  Healh checks (): MC APP is healty  -->   Update Agones Service by running health checks  adn update health()
//              i.   Sync Players Minecraft and Agones Platform

////////////////////////////
//1, 2.  Init Agones SDK, Minecraft Pinger
////////////////////////////
        sdk = new AgonesSDK();
        log("Agones SDK Initialized");

        options = MCPingOptions.builder()
                .hostname("localhost")
                .port(mc_port)
                .timeout(ping_timeout)
                .build();
        log("GameServer Initiating...");
        waitforEvent(warmupTime);
        log("GameServer WarmUpTim - Completed");
///////////////////////////
// 3. (a)     Ready ():  MC Container UP and Running  -- Mark Kubernetes Pod Status to Ready through Agones SDK ready()
//////////////////////////

        for (int w=0; w <= retries; w++) {
            try {
                reply=MCPing.getPing(options);
                int maxPlayers=Optional.ofNullable(reply.getPlayers().getMax()).orElse(-1);
                if (maxPlayers > 0) {
                    isRunning = true;
                    log("[ Initial Ping succeeded: Max No. Players:  "+ maxPlayers);
                    sdk.alpha().setCapacity(maxPlayers);
                    sdk.ready();
                    w=retries+1;
                    break;
                }
            } catch (IOException e) {
                log("Warm Up Ping failed");
                isRunning = false;
            } catch (Exception catchAll){
                log("Something Unexpected happened."+ catchAll.getMessage());
                //catchAll.printStackTrace();
            }
            waitforEvent(interval);

        }
///////////////////////////
// 3. (b)   Healh checks (): MC APP is healty  -->   Update Agones Service by running health checks  adn update health()
//////////////////////////
        if (isRunning) {
            try {
                log("GameServer Descriptions: " + reply.getDescription());
                //sdk.ready();
                Runnable task1 = () -> {
                    try {
                        MCPingResponse info=MCPing.getPing(options);
                        sdk.health();
                        int mcConnectedPlayers=info.getPlayers().getOnline();
                        log("Minecraft No. connected Players : "+mcConnectedPlayers);
                        //Sync Players with Agones
                        if (!gameStarted && mcConnectedPlayers>0) {
                            //Mark game server as allocated.  Can't be autoscaled.
                            gameStarted=true;
                            sdk.allocate();
                        }
                        syncPlayers(sdk.alpha().getConnectedPlayers(),info.getPlayers().getSample());
                        log("Agones Reported Connected Players: "+Optional.ofNullable(sdk.alpha().getPlayerCount()).orElse(0));
                        current_failed_health_checks=0;
                    } catch (IOException e) {
                        log("Failed Health check");
                        current_failed_health_checks++;
                    } catch (NullPointerException notExpected){
                        log("Failed Health check due to NullPointer"+ notExpected.getMessage());
                        current_failed_health_checks++;
                    }catch (Exception unexpected){
                        log(":-(  crashed! "+ unexpected.getMessage());
                        unexpected.printStackTrace();

                    }
                };

////////////////////////////////////////////////////
//Keep watching for healthcheck results or shutdown
////////////////////////////////////////////////////
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
        }else{
            log("Server not Running..  Shutting down monitoring service...");
        }
    }
    /*****************
     ////////////////////////////////////////////////////////////
     //  UTILS
     // ////////////////////////////////////////////////////////
     */
    public static void syncPlayers(List<String> agonesSDKPlayers, List<MCPingResponse.Player> mcPlayers) throws IOException,NullPointerException{
        //agonesSDKPlayers=Optional.ofNullable(agonesSDKPlayers);
        agonesSDKPlayers=Optional.ofNullable (agonesSDKPlayers).orElse(Collections.emptyList());
        mcPlayers=Optional.ofNullable(mcPlayers).orElse(Collections.emptyList());

        //Remove metadata of all  Players in Agones
        log("about to remove all connected players from SDK");
        for (String player : agonesSDKPlayers)
            sdk.alpha().playerDisconnect(player);

        log("Players stats removed");
        //Update playersMetada with connected players in Minecraft
        for (MCPingResponse.Player player : mcPlayers) {
            sdk.alpha().playerConnect(player.getId());
            log("Player Metadata : " + player.getId() + " appended to Agones Players list.");
        }
    }
    public static void log(String message){
        System.out.println("[ "+clock.instant()+"] "+ message);
    }

    public static void waitforEvent(int delay_time){
        try {
            //log("before waitForEvent "+delay_time);
            TimeUnit.SECONDS.sleep(delay_time);
            //log("after waitForEvent ");
        } catch (InterruptedException ie) {
            log("Catch Exception "+ie.getMessage());
            Thread.currentThread().interrupt();
            log("Catch Exception after Interrupt ");
        }
    }

    ////////////////////////////////////////
    private static final int max_failed_health_checks=5;
    private static int current_failed_health_checks=0;
    private static Clock clock = Clock.systemDefaultZone();
    private static boolean gameStarted =false;
    private static AgonesSDK sdk;

}
