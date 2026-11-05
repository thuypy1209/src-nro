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
 * Sự kiện Halloween (Boss Bí Ma, Ma Trơi, Đổi Kẹo & Hộp Quà Ma Quỷ)
 */
public class HalloweenEvent extends Event {

    @Override
    public int getId() {
        return ConstEvent.SU_KIEN_HALLOWEEN;
    }

    @Override
    public String getName() {
        return "Sự kiện Halloween";
    }

    @Override
    public void init() {
        EventManager.HALLOWEEN = true;

        // Sinh Boss Halloween
        EventManager.gI().createBoss(BossFactory.BI_MA);
        EventManager.gI().createBoss(BossFactory.MA_TROI);

        System.out.println("[SỰ KIỆN] Đã khởi tạo Sự kiện Halloween (Bí Ma, Ma Trơi).");
    }

    @Override
    public void destroy() {
        EventManager.HALLOWEEN = false;

        EventManager.gI().removeBoss(BossFactory.BI_MA);
        EventManager.gI().removeBoss(BossFactory.MA_TROI);

        System.out.println("[SỰ KIỆN] Đã kết thúc Sự kiện Halloween.");
    }

    @Override
    public void dropItem(Player player, Mob mob, List<ItemMap> list, int x, int yEnd) {
        if (mob == null || mob.zone == null) {
            return;
        }
        // Tỉ lệ 15% rơi Kẹo, Bánh quy, Bí ngô Halloween
        if (Util.isTrue(15, 100)) {
            int[] items = {ConstItem.KEO_HALLOWEEN, ConstItem.BANH_QUY_HALLOWEEN, ConstItem.BI_NGO_HALLOWEEN};
            int itemID = items[Util.nextInt(0, items.length - 1)];
            list.add(new ItemMap(mob.zone, itemID, 1, x, yEnd, player.id));
        }
        // Tỉ lệ 3% rơi Vé đổi kẹo
        if (Util.isTrue(3, 100)) {
            list.add(new ItemMap(mob.zone, ConstItem.VE_DOI_KEO, 1, x, yEnd, player.id));
        }
    }

    @Override
    public boolean useItem(Player player, Item item) {
        int itemID = item.template.id;
        switch (itemID) {
            case ConstItem.HOP_QUA_HALLOWEEN:
            case ConstItem.HOP_MA_QUY:
            case ConstItem.GIO_KEO_BI_NGO:
                openHopQuaHalloween(player, item);
                return true;
            default:
                return false;
        }
    }

    private void openHopQuaHalloween(Player player, Item item) {
        if (InventoryService.gI().getCountEmptyBag(player) <= 0) {
            Service.getInstance().sendThongBao(player, "Hành trang đã đầy.");
            return;
        }
        InventoryService.gI().subQuantityItemsBag(player, item, 1);
        int rand = Util.nextInt(0, 3);
        switch (rand) {
            case 0:
                Item nr = ItemService.gI().createNewItem((short) Util.nextInt(ConstItem.NGOC_RONG_4_SAO, ConstItem.NGOC_RONG_6_SAO), 1);
                InventoryService.gI().addItemBag(player, nr, 99);
                Service.getInstance().sendThongBao(player, "Bạn nhận được " + nr.template.name + "!");
                break;
            case 1:
                int ruby = Util.nextInt(15, 35);
                player.inventory.ruby += ruby;
                PlayerService.gI().sendInfoHpMpMoney(player);
                Service.getInstance().sendThongBao(player, "Bạn nhận được " + ruby + " Hồng ngọc!");
                break;
            case 2:
                Item da = ItemService.gI().createNewItem((short) Util.nextInt(ConstItem.DA_NANG_CAP_CAP_1, ConstItem.DA_NANG_CAP_CAP_3), 1);
                InventoryService.gI().addItemBag(player, da, 99);
                Service.getInstance().sendThongBao(player, "Bạn nhận được Đá nâng cấp!");
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
