package nro.event.list;

import nro.consts.ConstEvent;
import nro.consts.ConstItem;
import nro.consts.ConstNpc;
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
 * Sự kiện Tết Nguyên Đán (NPC Vua Hùng, Nồi Bánh Chưng, Boss Lân Con)
 */
public class TetEvent extends Event {

    @Override
    public int getId() {
        return ConstEvent.SU_KIEN_TET;
    }

    @Override
    public String getName() {
        return "Sự kiện Tết Nguyên Đán";
    }

    @Override
    public void init() {
        EventManager.LUNAR_NEW_YEAR = true;

        // Sinh NPC Vua Hùng tại 3 làng chính (Aru, Mori, Kakarot)
        EventManager.gI().createNpc(0, ConstNpc.HUNG_VUONG, 720, 432);
        EventManager.gI().createNpc(7, ConstNpc.HUNG_VUONG, 720, 432);
        EventManager.gI().createNpc(14, ConstNpc.HUNG_VUONG, 720, 432);

        // Sinh NPC Nồi Bánh Chưng tại 3 làng chính
        EventManager.gI().createNpc(0, ConstNpc.NOI_BANH, 770, 432);
        EventManager.gI().createNpc(7, ConstNpc.NOI_BANH, 770, 432);
        EventManager.gI().createNpc(14, ConstNpc.NOI_BANH, 770, 432);

        // Sinh Boss Lân Con
        EventManager.gI().createBoss(BossFactory.LAN_CON);

        System.out.println("[SỰ KIỆN] Đã khởi tạo Sự kiện Tết (Vua Hùng, Nồi Bánh, Boss Lân Con).");
    }

    @Override
    public void destroy() {
        EventManager.LUNAR_NEW_YEAR = false;

        int[] villages = {0, 7, 14};
        for (int mapId : villages) {
            EventManager.gI().removeNpc(mapId, ConstNpc.HUNG_VUONG);
            EventManager.gI().removeNpc(mapId, ConstNpc.NOI_BANH);
        }

        EventManager.gI().removeBoss(BossFactory.LAN_CON);
        System.out.println("[SỰ KIỆN] Đã kết thúc Sự kiện Tết Nguyên Đán.");
    }

    @Override
    public void dropItem(Player player, Mob mob, List<ItemMap> list, int x, int yEnd) {
        if (mob == null || mob.zone == null) {
            return;
        }
        // Tỉ lệ 15% rơi nguyên liệu nấu bánh ngày Tết
        if (Util.isTrue(15, 100)) {
            int[] nls = {ConstItem.LA_DONG_2023, ConstItem.LA_CHUOI, ConstItem.THIT_HEO_2023, ConstItem.THIT_BA_CHI};
            int itemID = nls[Util.nextInt(0, nls.length - 1)];
            list.add(new ItemMap(mob.zone, itemID, 1, x, yEnd, player.id));
        }
    }

    @Override
    public boolean useItem(Player player, Item item) {
        int itemID = item.template.id;
        switch (itemID) {
            case ConstItem.BANH_CHUNG_CHIN:
            case ConstItem.BANH_TET_CHIN:
            case ConstItem.BANH_CHUNG_2023:
            case ConstItem.BANH_TET_2023:
                useBanhTet(player, item);
                return true;
            case ConstItem.HOP_QUA_TET_2023:
            case ConstItem.HOP_QUA_TET:
            case ConstItem.HOP_QUA_TET_398:
                openHopQuaTet(player, item);
                return true;
            default:
                return false;
        }
    }

    private void useBanhTet(Player player, Item item) {
        InventoryService.gI().subQuantityItemsBag(player, item, 1);
        player.nPoint.setHp(player.nPoint.hpMax);
        player.nPoint.setMp(player.nPoint.mpMax);
        PlayerService.gI().sendInfoHpMp(player);
        Service.getInstance().sendThongBao(player, "Bạn đã ăn " + item.template.name + " và được hồi phục toàn bộ thể lực!");
    }

    private void openHopQuaTet(Player player, Item item) {
        if (InventoryService.gI().getCountEmptyBag(player) <= 0) {
            Service.getInstance().sendThongBao(player, "Hành trang đã đầy.");
            return;
        }
        InventoryService.gI().subQuantityItemsBag(player, item, 1);
        int rand = Util.nextInt(0, 3);
        switch (rand) {
            case 0:
                Item thoiVang = ItemService.gI().createNewItem((short) ConstItem.THOI_VANG, Util.nextInt(1, 3));
                InventoryService.gI().addItemBag(player, thoiVang, 99999);
                Service.getInstance().sendThongBao(player, "Bạn nhận được Thỏi vàng may mắn!");
                break;
            case 1:
                int ruby = Util.nextInt(20, 50);
                player.inventory.ruby += ruby;
                PlayerService.gI().sendInfoHpMpMoney(player);
                Service.getInstance().sendThongBao(player, "Bạn nhận được " + ruby + " Hồng ngọc!");
                break;
            case 2:
                Item nr = ItemService.gI().createNewItem((short) Util.nextInt(ConstItem.NGOC_RONG_4_SAO, ConstItem.NGOC_RONG_7_SAO), 1);
                InventoryService.gI().addItemBag(player, nr, 99);
                Service.getInstance().sendThongBao(player, "Bạn nhận được " + nr.template.name + "!");
                break;
            default:
                Item xiMuoi = ItemService.gI().createNewItem((short) ConstItem.XI_MUOI_HOA_MAI, 1);
                InventoryService.gI().addItemBag(player, xiMuoi, 99);
                Service.getInstance().sendThongBao(player, "Bạn nhận được Xí muội hoa mai!");
                break;
        }
        InventoryService.gI().sendItemBags(player);
    }
}
