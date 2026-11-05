package nro.models.boss.event_noel;


import nro.models.boss.Boss;
import nro.models.boss.BossesData;
import nro.models.boss.BossID;
import nro.models.consts.BossStatus;
import nro.models.boss.Boss_Manager.BossManager;
import static nro.models.consts.BossType.CHRISTMAS_EVENT;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.map.service.ChangeMapService;
import nro.models.utils.Logger;
import nro.models.utils.Util;

public class OngGiaNoel extends Boss {

    private long lastTimeDrop;
    private long st;
    private int timeLeave;

    public OngGiaNoel() throws Exception {
        super(CHRISTMAS_EVENT, BossID.ONG_GIA_NOEL, BossesData.ONG_GIA_NOEL);
    }

    @Override
    public void joinMap() {
        if (zoneFinal != null) {
            joinMapByZone(zoneFinal);
            this.notifyJoinMap();
            this.changeStatus(BossStatus.CHAT_S);
            this.wakeupAnotherBossWhenAppear();
            return;
        }
        if (this.zone == null) {
            if (this.parentBoss != null) {
                this.zone = parentBoss.zone;
            } else if (this.lastZone == null) {
                this.zone = getMapJoin();
            } else {
                this.zone = this.lastZone;
            }
        }
        if (this.zone != null) {
            try {
                int zoneid = 0;
                // Check trong khu lớn hơn 10 người chuyển sang khu n + 1
                while (zoneid < this.zone.map.zones.size() && this.zone.map.zones.get(zoneid).getNumOfPlayers() > 10) {
                    zoneid++;
                }
                // Check trong khu có boss sẽ chuyển sang khu n + 1
                while (zoneid < this.zone.map.zones.size() && this.zone.map.zones.get(zoneid).getBosses().stream().anyMatch(b -> b.id == BossID.ONG_GIA_NOEL && !b.isDie())) {
                    zoneid++;
                }
                if (zoneid < this.zone.map.zones.size()) {
                    this.zone = this.zone.map.zones.get(zoneid);
                } else {
                    this.leaveMapNew();
                    return;
                }
                int x = Util.nextInt(100, Math.max(150, this.zone.map.mapWidth - 100));
                int y = this.zone.map.yPhysicInTop(x, 100);
                ChangeMapService.gI().changeMap(this, this.zone, x, y);
                Service.gI().sendFlagBag(this);
                this.changeStatus(BossStatus.CHAT_S);
                st = System.currentTimeMillis();
                timeLeave = Util.nextInt(100000, 300000);
            } catch (Exception e) {
                Logger.error(this.data[0].getName() + ": Lỗi đang tiến hành REST\n");
                this.changeStatus(BossStatus.REST);
            }
        } else {
            Logger.error(this.data[0].getName() + ": Lỗi map đang tiến hành RESPAWN\n");
            this.changeStatus(BossStatus.RESPAWN);
        }
    }

    @Override
    public void chatM() {
        if (this.data[this.currentLevel].getTextM().length == 0) {
            return;
        }
        if (!Util.canDoWithTime(this.lastTimeChatM, this.timeChatM)) {
            return;
        }
        String textChat = this.data[this.currentLevel].getTextM()[Util.nextInt(0, this.data[this.currentLevel].getTextM().length - 1)];
        int prefix = Integer.parseInt(textChat.substring(1, textChat.lastIndexOf("|")));
        textChat = textChat.substring(textChat.lastIndexOf("|") + 1);
        this.chat(prefix, textChat);
        this.lastTimeChatM = System.currentTimeMillis();
        this.timeChatM = Util.nextInt(3000, 20000);
    }

    private void giftBox() {
        if (Util.canDoWithTime(lastTimeDrop, 60000)) {
            this.chat("Hô hô hô, Giáng sinh vui vẻ!");
            int dropX = this.location.x;
            int dropY = this.zone.map.yPhysicInTop(dropX, 100);
            ItemMap item = new ItemMap(zone, 648, 1, dropX, dropY, -1);
            ItemMap item2 = new ItemMap(zone, 648, 1, dropX + Util.nextInt(20, 50), dropY, -1);
            ItemMap item3 = new ItemMap(zone, 648, 1, dropX - Util.nextInt(20, 50), dropY, -1);
            Service.gI().dropItemMap(this.zone, item);
            Service.gI().dropItemMap(this.zone, item2);
            Service.gI().dropItemMap(this.zone, item3);
            lastTimeDrop = System.currentTimeMillis();
        }
    }

    @Override
    public void active() {
        this.attack();
        this.giftBox();
    }

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, timeLeave)) {
            this.leaveMapNew();
        }
    }

    @Override
    public void leaveMap() {
        ChangeMapService.gI().exitMap(this);
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);
    }

    @Override
    public void attack() {
        if (Util.canDoWithTime(this.lastTimeAttack, 1000)) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getPlayerAttack();
                if (pl == null || pl.location == null) {
                    return;
                }
                int dis = Util.getDistance(this, pl);
                if (dis <= 60) {
                    this.giftBox();
                } else if (dis < 300) {
                    this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(20, 50)), pl.location.y);
                } else {
                    this.moveToPlayer(pl);
                }
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        return 0;
    }

    @Override
    public void reward(Player plKill) {
    }
}
