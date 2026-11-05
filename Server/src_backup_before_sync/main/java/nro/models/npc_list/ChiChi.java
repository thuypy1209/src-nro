package nro.models.npc_list;

import nro.consts.ConstNpc;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.services.Service;

public class ChiChi extends Npc {

    public ChiChi(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Ch?o b?n, t?i l? ChiChi. B?n c?n gi?p vi?c g??", "Xem th?ng tin", "??ng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.iDMark.isBaseMenu()) {
                if (select == 0) {
                    Service.getInstance().sendThongBao(player, "Ch?c b?n ch?i game vui v?!");
                }
            }
        }
    }
}
