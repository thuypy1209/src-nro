package nro.event.list;

import nro.consts.ConstEvent;
import nro.consts.ConstItem;
import nro.event.Event;
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
 * Sự kiện Nhà giáo Việt Nam 20/11 (Tặng hoa tri ân sư phụ, Hộp quà Teacher Day)
 */
public class Event2011 extends Event {

    @Override
    public int getId() {
        return ConstEvent.SU_KIEN_20_11;
    }

    @Override
    public String getName() {
        return "Sự kiện 20/11 (Nhà Giáo VN)";
    }

    @Override
    public void init() {
        System.out.println("[SỰ KIỆN] Đã khởi tạo Sự kiện 20/11 (Tri Ân Sư Phụ, Hộp Quà Teacher Day).");
    }

    @Override
    public void destroy() {
        System.out.println("[SỰ KIỆN] Đã kết thúc Sự kiện 20/11.");
    }

    @Override
    public void dropItem(Player player, Mob mob, List<ItemMap> list, int x, int yEnd) {
        if (mob == null || mob.zone == null) {
            return;
        }
        // Tỉ lệ 15% rơi Bông hoa tặng thầy
        if (Util.isTrue(15, 100)) {
            int itemID = Util.isTrue(80, 100) ? ConstItem.BONG_HOA : ConstItem.BONG_HOA_610;
            list.add(new ItemMap(mob.zone, itemID, 1, x, yEnd, player.id));
        }
    }

    @Override
    public boolean useItem(Player player, Item item) {
        int itemID = item.template.id;
        if (itemID == ConstItem.HOP_QUA_TEACHER_DAY) {
            openHopQuaTeacherDay(player, item);
            return true;
        }
        return false;
    }

    private void openHopQuaTeacherDay(Player player, Item item) {
        if (InventoryService.gI().getCountEmptyBag(player) <= 0) {
            Service.getInstance().sendThongBao(player, "Hành trang đã đầy.");
            return;
        }
        InventoryService.gI().subQuantityItemsBag(player, item, 1);
        int rand = Util.nextInt(0, 3);
        switch (rand) {
            case 0:
                Item nr = ItemService.gI().createNewItem((short) Util.nextInt(ConstItem.NGOC_RONG_3_SAO, ConstItem.NGOC_RONG_5_SAO), 1);
                InventoryService.gI().addItemBag(player, nr, 99);
                Service.getInstance().sendThongBao(player, "Bạn nhận được " + nr.template.name + "!");
                break;
            case 1:
                int ruby = Util.nextInt(50, 100);
                player.inventory.ruby += ruby;
                PlayerService.gI().sendInfoHpMpMoney(player);
                Service.getInstance().sendThongBao(player, "Bạn nhận được " + ruby + " Hồng ngọc!");
                break;
            case 2:
                Item nr2 = ItemService.gI().createNewItem((short) ConstItem.NGOC_RONG_2_SAO, 1);
                InventoryService.gI().addItemBag(player, nr2, 99);
                Service.getInstance().sendThongBao(player, "Bạn nhận được Ngọc Rồng 2 Sao!");
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
