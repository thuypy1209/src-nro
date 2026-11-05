package nro.models.npc_list;

import nro.consts.ConstNpc;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.services.TaskService;

public class DrMyuu extends Npc {

    public DrMyuu(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                        "N?m 740, ta t?m th?y c?c k? sinh tr?ng c?a King Tuffle,\nsau ?? ta ?? nghi?n c?u v? ch? t?o k? sinh tr?ng Baby.\nBaby c? kh? n?ng b?m v?o c? th? ng??i kh?c,\nki?m so?t s?c m?nh c?a h? v? l?m vi?c theo ? c?a ta.\nTuy nhi?n ta ?? m?t ki?m so?t n? ho?n to?n...\n Ng??i c? th? gi?p ta ch? ng? n? kh?ng ?",
                        "??ng ?",
                        "T? ch?i");
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.iDMark.isBaseMenu()) {
                switch (select) {
                    case 0 -> {
                    }
                }
            }
        }
    }
}
