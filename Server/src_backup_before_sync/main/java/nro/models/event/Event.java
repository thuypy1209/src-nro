package nro.models.event;

import nro.models.boss.Boss_Manager.BossManager;
import nro.models.ievent.IEvent;
import nro.models.map.Map;
import nro.models.map.Zone;
import nro.models.map.service.MapService;
import nro.models.npc.Npc;
import nro.models.npc.NpcFactory;
import nro.models.player.Player;
import nro.models.utils.Logger;

public abstract class Event implements IEvent {

    @Override
    public void init() {
        npc();
        boss();
        itemMap();
        itemBoss();
    }

    @Override
    public void npc() {

    }

    @Override
    public void createNpc(int mapId, int npcId, int x, int y) {
        try {
            Map map = MapService.gI().getMapById(mapId);
            if (map != null && map.npcs != null) {
                map.npcs.removeIf(n -> n != null && n.tempId == npcId);
                Npc npc = NpcFactory.createNPC(mapId, 1, x, y, npcId);
                if (npc != null) {
                    map.npcs.add(npc);
                    if (map.zones != null) {
                        for (Zone z : map.zones) {
                            if (z != null) {
                                for (Player pl : z.getPlayers()) {
                                    if (pl != null && pl.getSession() != null) {
                                        z.mapInfo(pl);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.error("Lỗi createNpc: " + e.getMessage() + "\n");
        }
    }

    @Override
    public void boss() {

    }

    @Override
    public void createBoss(int bossId, int... total) {
        int len = 1;
        if (total.length > 0) {
            len = total[0];
        }
        try {
            for (int i = 0; i < len; i++) {
                BossManager.gI().createBoss(bossId);
            }
        } catch (Exception e) {
            Logger.error(e + "\n");
        }
    }

    @Override
    public void itemMap() {

    }

    @Override
    public void itemBoss() {

    }
}
