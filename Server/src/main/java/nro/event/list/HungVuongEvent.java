package nro.event.list;

import nro.consts.ConstEvent;
import nro.consts.ConstItem;
import nro.consts.ConstNpc;
import nro.event.Event;
import nro.event.EventManager;
import nro.models.boss.BossFactory;
import nro.models.item.Item;
import nro.models.map.ItemMap;
import nro.models.mob.Mob;
import nro.models.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.PlayerService;
import nro.services.Service;
import nro.utils.Util;

import java.util.List;

/**
 * Sự kiện Giỗ Tổ Hùng Vương (Vua Hùng, Nồi Bánh, Boss Sơn Tinh, Boss Thủy Tinh)
 */
public class HungVuongEvent extends Event {

    @Override
    public int getId() {
        return ConstEvent.SU_KIEN_HUNG_VUONG;
    }

    @Override
    public String getName() {
        return "Sự kiện Giỗ Tổ Hùng Vương";
    }

    @Override
    public void init() {
        EventManager.HUNG_VUONG = true;

        // Sinh NPC Vua Hùng & Nồi Bánh tại 3 làng chính
        int[] villages = {0, 7, 14};
        for (int mapId : villages) {
            EventManager.gI().createNpc(mapId, ConstNpc.HUNG_VUONG, 720, 432);
            EventManager.gI().createNpc(mapId, ConstNpc.NOI_BANH, 770, 432);
        }

        // Sinh Boss Sơn Tinh & Thủy Tinh
        EventManager.gI().createBoss(BossFactory.THUY_TINH);
        EventManager.gI().createBoss(BossFactory.SON_TINH);

        System.out.println("[SỰ KIỆN] Đã khởi tạo Sự kiện Giỗ Tổ Hùng Vương (Sơn Tinh, Thủy Tinh, Vua Hùng, Nồi Bánh).");
    }

    @Override
    public void destroy() {
        EventManager.HUNG_VUONG = false;

        int[] villages = {0, 7, 14};
        for (int mapId : villages) {
            EventManager.gI().removeNpc(mapId, ConstNpc.HUNG_VUONG);
            EventManager.gI().removeNpc(mapId, ConstNpc.NOI_BANH);
        }

        EventManager.gI().removeBoss(BossFactory.THUY_TINH);
        EventManager.gI().removeBoss(BossFactory.SON_TINH);
        System.out.println("[SỰ KIỆN] Đã kết thúc Sự kiện Giỗ Tổ Hùng Vương.");
    }

    @Override
    public void dropItem(Player player, Mob mob, List<ItemMap> list, int x, int yEnd) {
        if (mob == null || mob.zone == null) {
            return;
        }
        // Tỉ lệ 15% rơi Dưa hấu hoặc Bánh dâng Vua Hùng
        if (Util.isTrue(15, 100)) {
            int[] items = {ConstItem.DUA_HAU, ConstItem.LA_DONG_2023, ConstItem.THIT_HEO_2023};
            int itemID = items[Util.nextInt(0, items.length - 1)];
            list.add(new ItemMap(mob.zone, itemID, 1, x, yEnd, player.id));
        }
    }

    @Override
    public boolean useItem(Player player, Item item) {
        int itemID = item.template.id;
        if (itemID == ConstItem.DUA_HAU) {
            InventoryService.gI().subQuantityItemsBag(player, item, 1);
            player.nPoint.setHp(player.nPoint.hpMax);
            player.nPoint.setMp(player.nPoint.mpMax);
            PlayerService.gI().sendInfoHpMp(player);
            Service.getInstance().sendThongBao(player, "Bạn ăn Dưa hấu Mai An Tiêm và cảm thấy tràn đầy sinh lực!");
            return true;
        }
        return false;
    }
}
