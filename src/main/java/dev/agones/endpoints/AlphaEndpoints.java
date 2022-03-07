package dev.agones.endpoints;

import dev.agones.model.request.BoolResponse;
import dev.agones.model.request.PlayerCount;
import dev.agones.model.request.PlayerInfo;
import dev.agones.model.request.PlayerList;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.PUT;

public interface AlphaEndpoints extends Endpoints {

    @POST("/alpha/player/connect")
    public BoolResponse playerConnect(@Body PlayerInfo info);

    @POST("/alpha/player/disconnect")
    public BoolResponse playerDisconnect(@Body PlayerInfo info);

    @GET("/alpha/player/connected")
    public PlayerList getConnectedPlayers();

    @GET("/alpha/player/connected/{player}")
    public BoolResponse isPlayerConnected(@Path("player") String playerId);

    @GET("/alpha/player/count")
    public PlayerCount getPlayerCount();

    @GET("/alpha/player/capacity")
    public PlayerCount getPlayerCapacity();

    @PUT("/alpha/player/capacity")
    public Void setPlayerCapacity(@Body PlayerCount count);

}
