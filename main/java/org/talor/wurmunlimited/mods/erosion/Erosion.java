package org.talor.wurmunlimited.mods.erosion;

import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.Players;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.Terraforming;
import com.wurmonline.server.sounds.SoundPlayer;
import com.wurmonline.server.structures.Fence;
import com.wurmonline.server.zones.NoSuchZoneException;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zone;
import com.wurmonline.server.zones.Zones;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;
import org.gotti.wurmunlimited.modloader.interfaces.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Erosion implements WurmServerMod, Configurable, ServerStartedListener, Initable, PreInitable {

    private static Logger logger = Logger.getLogger(Erosion.class.getName());

    // Configuration default values
    private boolean enableErosion = true;

    @Override
	public void onServerStarted() {
	}

	@Override
	public void configure(Properties properties) {
        // Check .properties file for configuration values
        enableErosion = Boolean.parseBoolean(properties.getProperty("enableErosion", Boolean.toString(enableErosion)));
	}

	@Override
	public void preInit() {
	}

	@Override
	public void init() {



        HookManager.getInstance().registerHook("com.wurmonline.server.zones.TilePoller", "checkEffects", "(IIIBB)V", new InvocationHandlerFactory() {

            @Override
            public InvocationHandler createInvocationHandler() {
                return new InvocationHandler() {

                    @Override
                    public Object invoke(Object object, Method method, Object[] args) throws Throwable {

                        if (enableErosion) {

                            int tilex = (int) args[1];
                            int tiley = (int) args[2];

                            short gravityMod = 40;

                            int centralTile = Server.surfaceMesh.getTile(tilex,tiley);
                            short centralTileHeight = Tiles.decodeHeight(centralTile);

                            for (int x = -1; x <= 1; x++) {
                                for (int y = -1; y <= 1; y++) {
                                    if (!isBlocked(tilex + x, tiley + y))
                                    {
                                        int tile = Server.surfaceMesh.getTile(tilex + x, tiley + y);
                                        byte oldType = Tiles.decodeType(tile);
                                        int rocktile = Server.rockMesh.getTile(tilex + x, tiley + y);
                                        short rockheight = Tiles.decodeHeight(rocktile);


                                        short newHeight = 0;
                                        short thisTileheight = Tiles.decodeHeight(tile);
                                        short mod = (short) (2 * Math.random() * Math.ceil(Math.abs((thisTileheight - centralTileHeight)/gravityMod)));

                                        if(centralTileHeight > thisTileheight + gravityMod ) {
                                            newHeight = (short)Math.max(rockheight, thisTileheight + mod);
                                        } else {
                                            newHeight = (short)Math.max(rockheight, thisTileheight - mod);
                                        }

                                        byte type = Tiles.Tile.TILE_DIRT.id;
                                        if (oldType == Tiles.Tile.TILE_SAND.id) {
                                            type = oldType;
                                        } else if ((oldType == Tiles.Tile.TILE_CLAY.id) || (oldType == Tiles.Tile.TILE_TAR.id) || (oldType == Tiles.Tile.TILE_PEAT.id)) {
                                            type = oldType;
                                        } else if (oldType == Tiles.Tile.TILE_MOSS.id) {
                                            type = oldType;
                                        } else if (oldType == Tiles.Tile.TILE_MARSH.id) {
                                            type = oldType;
                                        }
                                        if (Terraforming.allCornersAtRockLevel(tilex + x, tiley + y, Server.surfaceMesh)) {
                                            type = Tiles.Tile.TILE_ROCK.id;
                                        }
                                        Server.setSurfaceTile(tilex + x, tiley + y, newHeight, type, (byte)0);
                                        Players.getInstance().sendChangedTile(tilex + x, tiley + y, true, true);
                                    }
                                }
                            }
                        }

                        return method.invoke(object, args);

                    }
                };
            }
        });


    }

    private static final boolean isBlocked(int tx, int ty)
    {

        if ( tx <= 0 || ty <= 0 || tx >= Server.surfaceMesh.getSize() - 1 || ty >= Server.surfaceMesh.getSize() - 1) {
            return true;
        }

        int otile = Server.surfaceMesh.getTile(tx, ty);

        int diff = Math.abs(Terraforming.getMaxSurfaceDifference(otile, tx, ty));
        if (diff < 40)
        {
            return true;
        }


        for (int x = 0; x >= -1; x--) {
            for (int y = 0; y >= -1; y--) {
                try
                {
                    int tile = Server.surfaceMesh.getTile(tx + x, ty + y);
                    byte type = Tiles.decodeType(tile);
                    if (Terraforming.isNonDiggableTile(type))
                    {
                        return true;
                    }
                    if (Terraforming.isRoad(type))
                    {
                        return true;
                    }
                    if ((type == Tiles.Tile.TILE_CLAY.id) || (type == Tiles.Tile.TILE_TAR.id) || (type == Tiles.Tile.TILE_PEAT.id)) {
                        return true;
                    }

                    try {
                        Zone zone = Zones.getZone(tx + x, ty + y, true);

                        VolaTile vtile = zone.getTileOrNull(tx + x, ty + y);

                        if (vtile != null)
                        {
                            if (vtile.getStructure() != null)
                            {
                                return true;
                            }
                            Fence[] fences = vtile.getFencesForLevel(0);
                            if (fences.length > 0)
                            {
                                if ((x == 0) && (y == 0))
                                {
                                  return true;
                                }
                                if ((x == -1) && (y == 0)) {
                                    for (Fence f : fences) {
                                        if (f.isHorizontal())
                                        {
                                            return true;
                                        }
                                    }
                                } else if ((y == -1) && (x == 0)) {
                                    for (Fence f : fences) {
                                        if (!f.isHorizontal())
                                        {
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (NoSuchZoneException nsze) {
                        return true;
                    }
                }
                catch (Exception e)
                {
                    return true;
                }
            }
        }
        return false;
    }
}

