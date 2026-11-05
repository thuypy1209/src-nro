package nro.event;

import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.services.TaskService;

public class NoiBanh1 extends Npc {

    public NoiBanh1(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        createOtherMenu(player, 0, "Xin chào " + player.name + "\nTôi là nồi nấu bánh.\nBạn cần gì?",
                "Tự nấu\nbánh",
                "Từ chối");
    }

    @Override
    public void confirmMenu(Player pl, int select) {
        if (canOpenNpc(pl)) {
            switch (pl.idMark.getIndexMenu()) {
                case 0 -> {
                    switch (select) {
                        case 0 ->
                            createOtherMenu(pl, 1, "Chọn loại bánh muốn nấu:",
                                    "Nấu\nBánh Dầy",
                                    "Nấu\nBánh Chưng",
                                    "Từ chối");
                        default -> {
                        }
                    }
                }
                case 1 -> {
                    switch (select) {
                        case 0:// banh day
                            if (InventoryService.gI().getCountEmptyBag(pl) <= 0) {
                                Service.gI().sendThongBao(pl, "Hành trang đã đầy, cần ít nhất 1 ô trống");
                                return;
                            }
                            Item comnep = InventoryService.gI().findItemBag(pl, 1546);
                            Item botgao = InventoryService.gI().findItemBag(pl, 1547);// ga
                            Item muoitieu = InventoryService.gI().findItemBag(pl, 1545);// trung
                            Item chalua = InventoryService.gI().findItemBag(pl, 1544);// botmy
                            if (comnep != null && comnep.quantity >= 99
                                    && botgao != null && botgao.quantity >= 10
                                    && muoitieu != null && muoitieu.quantity >= 10
                                    && chalua != null && chalua.quantity >= 1) {
                                InventoryService.gI().subQuantityItemsBag(pl, comnep, 99);
                                InventoryService.gI().subQuantityItemsBag(pl, botgao, 10);
                                InventoryService.gI().subQuantityItemsBag(pl, muoitieu, 10);
                                InventoryService.gI().subQuantityItemsBag(pl, chalua, 1);
                                Item banh_day = ItemService.gI().createNewItem((short) 1542);
                                banh_day.itemOptions.add(new ItemOption(93, 30));
                                InventoryService.gI().addItemBag(pl, banh_day);
                                InventoryService.gI().sendItemBags(pl);
                                Service.gI().sendThongBao(pl, "Bạn nhận được Bánh dày");
                            } else {
                                Service.gI().sendThongBao(pl, "Cần x99 Cơm nếp, x10 Bột gạo, x10 Muối tiêu, x1 Chả lụa để nấu Bánh dầy");
                            }
                            break;
                        case 1:// banh chung
                            if (InventoryService.gI().getCountEmptyBag(pl) <= 0) {
                                Service.gI().sendThongBao(pl, "Hành trang đã đầy, cần ít nhất 1 ô trống");
                                return;
                            }
                            Item comnep1 = InventoryService.gI().findItemBag(pl, 1546);
                            Item dauxanh = InventoryService.gI().findItemBag(pl, 1548);// ga
                            Item thittuoi = InventoryService.gI().findItemBag(pl, 1549);// trung
                            if (comnep1 != null && comnep1.quantity >= 99
                                    && dauxanh != null && dauxanh.quantity >= 99
                                    && thittuoi != null && thittuoi.quantity >= 99) {
                                InventoryService.gI().subQuantityItemsBag(pl, comnep1, 99);
                                InventoryService.gI().subQuantityItemsBag(pl, dauxanh, 99);
                                InventoryService.gI().subQuantityItemsBag(pl, thittuoi, 99);
                                Item banh_chung = ItemService.gI().createNewItem((short) 1556);
                                banh_chung.itemOptions.add(new ItemOption(93, 30));
                                InventoryService.gI().addItemBag(pl, banh_chung);
                                InventoryService.gI().sendItemBags(pl);
                                Service.gI().sendThongBao(pl, "Bạn nhận được Bánh chưng");
                            } else {
                                Service.gI().sendThongBao(pl, "Cần x99 Cơm nếp, x99 Đậu xanh, x99 Thịt tươi để nấu Bánh chưng");
                            }
                            break;
                    }
                }
            }
        }
    }
}
