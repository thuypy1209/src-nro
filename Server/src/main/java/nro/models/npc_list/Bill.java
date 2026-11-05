package nro.models.npc_list;

import nro.consts.*;
import nro.models.item.Item;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.services.*;
import nro.services.func.ChangeMapService;

import java.util.ArrayList;
import java.util.List;

/**
 * NPC Bill - Hybrid Merge (By Mr Blue & KhanhDTK)
 */
public class Bill extends Npc {

    public Bill(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (this.mapId == 48) {
                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Đói bụng quá...ngươi mang cho ta 200 phần đồ ăn,\n"
                                + "ta sẽ cho ngươi một món đồ Hủy Diệt.\n"
                                + "Nếu tâm trạng ta vui ngươi có thể nhận được trang bị tăng đến 15%",
                        "Nói\nchuyện", "Hướng\ndẫn\nthêm", "Đổi thức ăn\nlấy phiếu ăn", "Từ chối");
            } else if (this.mapId == 154) {
                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                        "...",
                        "Về\nthánh địa\nKaio", "Từ chối");
            } else {
                super.openBaseMenu(player);
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }

        switch (this.mapId) {
            case 48 -> {
                if (player.iDMark.isBaseMenu()) {
                    switch (select) {
                        case 0 -> {
                            if (player.setClothes.godClothes) {
                                Service.getInstance().sendThongBao(player, "Shop Đang Bảo Trì");
                            } else {
                                Service.getInstance().sendThongBao(player,
                                        "Ngươi trang bị đủ bộ 5 món trang bị Thần\n"
                                                + "và mang 200 phần đồ ăn tới đây...\nrồi ta nói chuyện tiếp.");
                            }
                        }
                        case 1 -> NpcService.gI().createTutorial(player, tempId, this.avartar,
                                ConstNpc.HUONG_DAN_BILL);
                        case 2 -> {
                            this.createOtherMenu(player, 10, "Ngươi có muốn đổi thức ăn thành phiếu ăn nhanh không?",
                                    "Đồng ý", "Từ chối");
                        }
                    }
                } else if (player.iDMark.getIndexMenu() == 10) {
                    if (select == 0) {
                        doiNhanhItem(player);
                    }
                }
            }
            case 154 -> {
                if (select == 0) {
                    ChangeMapService.gI().changeMap(player, 50, -1, 318, 336);
                }
            }
            default -> {}
        }
    }

    private void doiNhanhItem(Player player) {
        int[] itemIds = {1798, 1799, 1800, 1801, 1802};
        int total1805 = 0;

        for (int id : itemIds) {
            int count = 0;
            List<Item> itemsToSubtract = new ArrayList<>();

            for (Item item : player.inventory.itemsBag) {
                if (item != null && item.template != null && item.template.id == id) {
                    count += item.quantity;
                    itemsToSubtract.add(item);
                }
            }

            int soLanDoi = count / 99;
            int soLuongTru = soLanDoi * 99;

            if (soLanDoi > 0) {
                total1805 += soLanDoi;
                int remaining = soLuongTru;
                for (Item item : itemsToSubtract) {
                    if (remaining <= 0) {
                        break;
                    }
                    int toSub = Math.min(remaining, item.quantity);
                    InventoryService.gI().subQuantityItemsBag(player, item, toSub);
                    remaining -= toSub;
                }
            }
        }

        if (total1805 > 0) {
            Item item1805 = ItemService.gI().createNewItem((short) 1805);
            item1805.quantity = total1805;
            InventoryService.gI().addItemBag(player, item1805, 0);

            PlayerService.gI().sendInfoHpMpMoney(player);
            InventoryService.gI().sendItemBags(player);
            Service.getInstance().sendThongBao(player, "Bạn đã đổi thành công " + total1805 + " x Phiếu ăn.");
        } else {
            Service.getInstance().sendThongBao(player, "Bạn không có đủ 99 phần của bất kỳ vật phẩm nào để đổi.");
        }
    }
}
