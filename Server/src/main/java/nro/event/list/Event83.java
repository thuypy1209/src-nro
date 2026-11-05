package nro.event.list;

import nro.consts.ConstEvent;
import nro.consts.ConstItem;
import nro.event.Event;
import nro.event.EventManager;
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
 * Sự kiện Quốc tế Phụ Nữ 8/3 (Tặng hoa, Capsule Hồng, Pet Hoàng Thượng)
 */
public class Event83 extends Event {

    @Override
    public int getId() {
        return ConstEvent.SU_KIEN_8_3;
    }

    @Override
    public String getName() {
        return "Sự kiện 8/3 (Quốc Tế Phụ Nữ)";
    }

    @Override
    public void init() {
        EventManager.INTERNATIONAL_WOMENS_DAY = true;
        System.out.println("[SỰ KIỆN] Đã khởi tạo Sự kiện 8/3 (Tặng Hoa, Capsule Hồng).");
    }

    @Override
    public void destroy() {
        EventManager.INTERNATIONAL_WOMENS_DAY = false;
        System.out.println("[SỰ KIỆN] Đã kết thúc Sự kiện 8/3.");
    }

    @Override
    public void dropItem(Player player, Mob mob, List<ItemMap> list, int x, int yEnd) {
        if (mob == null || mob.zone == null) {
            return;
        }
        // Tỉ lệ 15% rơi Bông hoa hoặc Bó hoa hồng
        if (Util.isTrue(15, 100)) {
            int itemID = Util.isTrue(80, 100) ? ConstItem.BONG_HOA : ConstItem.BO_HOA_HONG;
            list.add(new ItemMap(mob.zone, itemID, 1, x, yEnd, player.id));
        }
    }

    @Override
    public boolean useItem(Player player, Item item) {
        int itemID = item.template.id;
        if (itemID == 2052) { // Capsule Hồng
            openCapsuleHong(player, item);
            return true;
        }
        return false;
    }

    private void openCapsuleHong(Player player, Item item) {
        if (InventoryService.gI().getCountEmptyBag(player) <= 0) {
            Service.getInstance().sendThongBao(player, "Hành trang đã đầy.");
            return;
        }
        InventoryService.gI().subQuantityItemsBag(player, item, 1);
        int rand = Util.nextInt(0, 3);
        switch (rand) {
            case 0:
                Item hoa = ItemService.gI().createNewItem((short) ConstItem.HOA_HONG_XANH, 1);
                hoa.itemOptions.add(new ItemOption(50, 10)); // Sức đánh
                hoa.itemOptions.add(new ItemOption(77, 10)); // HP
                hoa.itemOptions.add(new ItemOption(103, 10)); // KI
                hoa.itemOptions.add(new ItemOption(93, Util.nextInt(7, 30))); // HSD
                InventoryService.gI().addItemBag(player, hoa, 1);
                Service.getInstance().sendThongBao(player, "Bạn nhận được Cây Hoa Hồng Xanh!");
                break;
            case 1:
                int ruby = Util.nextInt(30, 80);
                player.inventory.ruby += ruby;
                PlayerService.gI().sendInfoHpMpMoney(player);
                Service.getInstance().sendThongBao(player, "Bạn nhận được " + ruby + " Hồng ngọc!");
                break;
            case 2:
                Item nr = ItemService.gI().createNewItem((short) ConstItem.NGOC_RONG_3_SAO, 1);
                InventoryService.gI().addItemBag(player, nr, 99);
                Service.getInstance().sendThongBao(player, "Bạn nhận được " + nr.template.name + "!");
                break;
            default:
                Item thoiVang = ItemService.gI().createNewItem((short) ConstItem.THOI_VANG, 2);
                InventoryService.gI().addItemBag(player, thoiVang, 99999);
                Service.getInstance().sendThongBao(player, "Bạn nhận được 2 Thỏi vàng!");
                break;
        }
        InventoryService.gI().sendItemBags(player);
    }
}
