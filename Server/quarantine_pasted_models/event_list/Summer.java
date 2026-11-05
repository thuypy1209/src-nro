package nro.models.event_list;

import nro.models.consts.ConstNpc;
import nro.models.event.Event;

public class Summer extends Event {

    @Override
    public void npc() {
        createNpc(5, ConstNpc.XE_NUOC_MIA, 750, 408);
        createNpc(0, ConstNpc.XE_NUOC_MIA, 650, 432);
        createNpc(7, ConstNpc.XE_NUOC_MIA, 650, 432);
        createNpc(14, ConstNpc.XE_NUOC_MIA, 650, 432);
    }
}
