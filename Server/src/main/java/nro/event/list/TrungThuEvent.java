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
 * Sự kiện Tết Trung Thu (Boss Khỉ Đột, Nguyệt Thần, Bánh Trung Thu, Lồng Đèn)
 */
public class TrungThuEvent extends Event {

    @Override
    public int getId() {
        return ConstEvent.SU_KIEN_TRUNG_THU;
    }

    @Override
    public String getName() {
        return "Sự kiện Trung Thu";
    }

    @Override
    public void init() {
        EventManager.TRUNG_THU = true;

        // Sinh Boss Trung Thu
        EventManager.gI().createBoss(BossFactory.KHIDOT);
        EventManager.gI().createBoss(BossFactory.NGUYETTHAN);

        System.out.println("[SỰ KIỆN] Đã khởi tạo Sự kiện Trung Thu (Khỉ Đột, Nguyệt Thần).");
    }

    @Override
    public void destroy() {
        EventManager.TRUNG_THU = false;

        EventManager.gI().removeBoss(BossFactory.KHIDOT);
        EventManager.gI().removeBoss(BossFactory.NGUYETTHAN);

        System.out.println("[SỰ KIỆN] Đã kết thúc Sự kiện Trung Thu.");
    }

    @Override
    public void dropItem(Player player, Mob mob, List<ItemMap> list, int x, int yEnd) {
        if (mob == null || mob.zone == null) {
            return;
        }
        // Tỉ lệ 15% rơi Bánh Trung Thu 1 trứng
        if (Util.isTrue(15, 100)) {
            list.add(new ItemMap(mob.zone, ConstItem.BANH_TRUNG_THU_1_TRUNG, 1, x, yEnd, player.id));
        }
        // Tỉ lệ 3% rơi Lồng đèn kéo quân
        if (Util.isTrue(3, 100)) {
            list.add(new ItemMap(mob.zone, ConstItem.LONG_DEN_KEO_QUAN, 1, x, yEnd, player.id));
        }
    }

    @Override
    public boolean useItem(Player player, Item item) {
        int itemID = item.template.id;
        switch (itemID) {
            case ConstItem.BANH_TRUNG_THU_1_TRUNG:
            case ConstItem.BANH_TRUNG_THU_2_TRUNG:
            case ConstItem.BANH_TRUNG_THU_DAC_BIET:
                useBanhTrungThu(player, item);
                return true;
            case ConstItem.HOP_BANH_TRUNG_THU:
                openHopBanhTrungThu(player, item);
                return true;
            default:
                return false;
        }
    }

    private void useBanhTrungThu(Player player, Item item) {
        InventoryService.gI().subQuantityItemsBag(player, item, 1);
        player.nPoint.setHp(player.nPoint.hpMax);
        player.nPoint.setMp(player.nPoint.mpMax);
        PlayerService.gI().sendInfoHpMp(player);
        Service.getInstance().sendThongBao(player, "Bạn ăn " + item.template.name + " và được hồi phục sinh lực dồi dào!");
    }

    private void openHopBanhTrungThu(Player player, Item item) {
        if (InventoryService.gI().getCountEmptyBag(player) <= 0) {
            Service.getInstance().sendThongBao(player, "Hành trang đã đầy.");
            return;
        }
        InventoryService.gI().subQuantityItemsBag(player, item, 1);
        int rand = Util.nextInt(0, 3);
        switch (rand) {
            case 0:
                Item nr = ItemService.gI().createNewItem((short) Util.nextInt(ConstItem.NGOC_RONG_4_SAO, ConstItem.NGOC_RONG_5_SAO), 1);
                InventoryService.gI().addItemBag(player, nr, 99);
                Service.getInstance().sendThongBao(player, "Bạn nhận được " + nr.template.name + "!");
                break;
            case 1:
                int ruby = Util.nextInt(25, 60);
                player.inventory.ruby += ruby;
                PlayerService.gI().sendInfoHpMpMoney(player);
                Service.getInstance().sendThongBao(player, "Bạn nhận được " + ruby + " Hồng ngọc!");
                break;
            case 2:
                Item banhDacBiet = ItemService.gI().createNewItem((short) ConstItem.BANH_TRUNG_THU_DAC_BIET, 3);
                InventoryService.gI().addItemBag(player, banhDacBiet, 99);
                Service.getInstance().sendThongBao(player, "Bạn nhận được 3 Bánh Trung Thu Đặc Biệt!");
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
