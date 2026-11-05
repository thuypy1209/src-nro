package nro.event.list;

import nro.consts.ConstEvent;
import nro.consts.ConstItem;
import nro.event.Event;
import nro.event.EventManager;
import nro.models.boss.BossFactory;
import nro.models.item.Item;
import nro.models.item.ItemOption;
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
 * Sự kiện Giáng Sinh Noel (Ông già Noel, Tuần lộc, Tất vớ Giáng Sinh, Nón Noel)
 */
public class NoelEvent extends Event {

    @Override
    public int getId() {
        return ConstEvent.SU_KIEN_NOEL;
    }

    @Override
    public String getName() {
        return "Sự kiện Noel (Giáng Sinh)";
    }

    @Override
    public void init() {
        EventManager.CHRISTMAS = true;

        // Sinh Boss Giáng Sinh
        EventManager.gI().createBoss(BossFactory.ONG_GIA_NOEL);
        EventManager.gI().createBoss(BossFactory.TUAN_LOC_EVENT);

        System.out.println("[SỰ KIỆN] Đã khởi tạo Sự kiện Noel (Ông Già Noel, Tuần Lộc, Cây Thông, Tuyết Rơi).");
    }

    @Override
    public void destroy() {
        EventManager.CHRISTMAS = false;

        EventManager.gI().removeBoss(BossFactory.ONG_GIA_NOEL);
        EventManager.gI().removeBoss(BossFactory.TUAN_LOC_EVENT);

        System.out.println("[SỰ KIỆN] Đã kết thúc Sự kiện Noel.");
    }

    @Override
    public void dropItem(Player player, Mob mob, List<ItemMap> list, int x, int yEnd) {
        if (mob == null || mob.zone == null) {
            return;
        }
        // Tỉ lệ 15% rơi Kẹo Giáng Sinh hoặc Kẹo Người Tuyết
        if (Util.isTrue(15, 100)) {
            int itemID = Util.isTrue(50, 100) ? ConstItem.KEO_DUONG : ConstItem.KEO_NGUOI_TUYET;
            list.add(new ItemMap(mob.zone, itemID, 1, x, yEnd, player.id));
        }
    }

    @Override
    public boolean useItem(Player player, Item item) {
        int itemID = item.template.id;
        if (itemID == ConstItem.TAT_VO_GIANG_SINH) {
            openTatGiangSinh(player, item);
            return true;
        }
        return false;
    }

    private void openTatGiangSinh(Player player, Item item) {
        if (InventoryService.gI().getCountEmptyBag(player) <= 0) {
            Service.getInstance().sendThongBao(player, "Hành trang đã đầy.");
            return;
        }
        InventoryService.gI().subQuantityItemsBag(player, item, 1);
        int rand = Util.nextInt(0, 3);
        switch (rand) {
            case 0:
                // Nón Noel
                int[] nonNoel = {ConstItem.NON_NOEL_DO, ConstItem.NON_NOEL_XAM, ConstItem.NON_NOEL_XANH};
                Item non = ItemService.gI().createNewItem((short) nonNoel[Util.nextInt(0, nonNoel.length - 1)], 1);
                non.itemOptions.add(new ItemOption(50, Util.nextInt(10, 20))); // Sức đánh
                non.itemOptions.add(new ItemOption(77, Util.nextInt(10, 20))); // HP
                non.itemOptions.add(new ItemOption(103, Util.nextInt(10, 20))); // KI
                non.itemOptions.add(new ItemOption(93, Util.nextInt(3, 14))); // Hạn sử dụng
                InventoryService.gI().addItemBag(player, non, 1);
                Service.getInstance().sendThongBao(player, "Bạn nhận được " + non.template.name + "!");
                break;
            case 1:
                int ruby = Util.nextInt(20, 50);
                player.inventory.ruby += ruby;
                PlayerService.gI().sendInfoHpMpMoney(player);
                Service.getInstance().sendThongBao(player, "Bạn nhận được " + ruby + " Hồng ngọc!");
                break;
            case 2:
                Item nr = ItemService.gI().createNewItem((short) Util.nextInt(ConstItem.NGOC_RONG_4_SAO, ConstItem.NGOC_RONG_6_SAO), 1);
                InventoryService.gI().addItemBag(player, nr, 99);
                Service.getInstance().sendThongBao(player, "Bạn nhận được " + nr.template.name + "!");
                break;
            default:
                Item thoiVang = ItemService.gI().createNewItem((short) ConstItem.THOI_VANG, 1);
                InventoryService.gI().addItemBag(player, thoiVang, 99999);
                Service.getInstance().sendThongBao(player, "Bạn nhận được 1 Thỏi vàng!");
                break;
        }
        InventoryService.gI().sendItemBags(player);
    }
}
