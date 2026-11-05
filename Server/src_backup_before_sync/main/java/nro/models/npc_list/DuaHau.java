package nro.models.npc_list;

import nro.consts.ConstNpc;
import nro.models.npc.DuaHauEgg;
import nro.models.npc.Npc;
import nro.models.player.Player;

public class DuaHau extends Npc {

    public static final int CHUA_THE_THU_HOACH = 500;
    public static final int CO_THE_THU_HOACH = 501;

    public DuaHau(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (player.DuaHauEgg == null) {
                DuaHauEgg.createDuaHauEgg(player);
            }
            if (player.DuaHauEgg.getSecondDone() <= 0) {
                this.createOtherMenu(player, CO_THE_THU_HOACH,
                        "Dưa hấu đã chín, bạn có thể thu hoạch!", "Thu hoạch", "Từ chối");
            } else {
                int timeLeft = player.DuaHauEgg.getSecondDone();
                int hours = timeLeft / 3600;
                int minutes = (timeLeft % 3600) / 60;
                int seconds = timeLeft % 60;
                String timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                this.createOtherMenu(player, CHUA_THE_THU_HOACH,
                        "Dưa hấu đang lớn.\nCòn khoảng " + timeFormatted + " nữa sẽ chín.", "Đóng");
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player) && player.DuaHauEgg != null) {
            int menuIndex = player.iDMark.getIndexMenu();
            if (menuIndex == CO_THE_THU_HOACH && select == 0) {
                player.DuaHauEgg.openEgg();
            }
        }
    }
}
