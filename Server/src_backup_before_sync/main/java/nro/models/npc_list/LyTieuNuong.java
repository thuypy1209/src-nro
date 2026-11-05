package nro.models.npc_list;

import nro.consts.ConstNpc;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.services.Service;
import nro.services.func.LuckyRoundService;

public class LyTieuNuong extends Npc {

    public LyTieuNuong(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            createOtherMenu(player, ConstNpc.BASE_MENU, "Ch?o b?n, t?i l? L? Ti?u N??ng qu?n l? tr? ch?i may m?n.",
                    "V?ng xoay\nv?ng",
                    "V?ng xoay\nng?c",
                    "??ng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.iDMark.isBaseMenu()) {
                switch (select) {
                    case 0 -> LuckyRoundService.gI().openCrackBallUI(player, LuckyRoundService.USING_GOLD);
                    case 1 -> LuckyRoundService.gI().openCrackBallUI(player, LuckyRoundService.USING_GEM);
                }
            }
        }
    }
}
