package nro.models.event_list;

import nro.models.boss.BossID;
import nro.models.consts.ConstNpc;
import nro.models.event.Event;

public class LunarNewYear extends Event {

    @Override
    public void npc() {
        createNpc(5, ConstNpc.NOI_BANH, 650, 408);
        createNpc(0, ConstNpc.NOI_BANH, 770, 432);
        createNpc(7, ConstNpc.NOI_BANH, 770, 432);
        createNpc(14, ConstNpc.NOI_BANH, 770, 432);
    }

    @Override
    public void boss() {
        createBoss(BossID.LAN_CON, 10);
    }
}
