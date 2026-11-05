package nro.models.npc_list;

import nro.consts.*;
import nro.models.item.Item;
import nro.models.item.ItemOption;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.services.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * NPC BoMong - Hybrid Merge (By Mr Blue & KhanhDTK)
 */
public class BoMong extends Npc {

    public BoMong(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (this.mapId == 47 || this.mapId == 84) {
                this.createOtherMenu(player, ConstNpc.BASE_MENU, "Xin chào, cậu muốn tôi giúp gì?",
                        "Nhiệm vụ\nhàng ngày", "Nhận ngọc\nmiễn phí", "Xem Top\nnăng Động", "Điểm danh");
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 47 || this.mapId == 84) {
                if (player.iDMark.isBaseMenu()) {
                    switch (select) {
                        case 0:
                            if (player.playerTask.sideTask.template != null) {
                                String npcSay = "Nhiệm vụ hiện tại: "
                                        + player.playerTask.sideTask.getName() + " ("
                                        + player.playerTask.sideTask.getLevel() + ")"
                                        + "\nHiện tại đã hoàn thành: "
                                        + player.playerTask.sideTask.count + "/"
                                        + player.playerTask.sideTask.maxCount + " ("
                                        + player.playerTask.sideTask.getPercentProcess()
                                        + "%)\nSố nhiệm vụ còn lại trong ngày: "
                                        + player.playerTask.sideTask.leftTask + "/"
                                        + ConstTask.MAX_SIDE_TASK;
                                this.createOtherMenu(player, ConstNpc.MENU_OPTION_PAY_SIDE_TASK,
                                        npcSay, "Trả nhiệm\nvụ", "Hủy nhiệm\nvụ");
                            } else {
                                this.createOtherMenu(player, ConstNpc.MENU_OPTION_LEVEL_SIDE_TASK,
                                        "Tôi có vài nhiệm vụ theo cấp bậc, "
                                                + "sức cậu có thể làm được cái nào?",
                                        "Dễ", "Bình thường", "Khó", "Siêu khó", "Từ chối");
                            }
                            break;
                        case 1:
                            TaskService.gI().checkDoneAchivements(player);
                            TaskService.gI().sendAchivement(player);
                            break;
                        case 2:
                            Service.getInstance().showTopNangDong(player);
                            break;
                        case 3:
                            if (player.lastCheckIn != null) {
                                LocalDate last = player.lastCheckIn.toLocalDate();
                                LocalDate today = LocalDate.now();
                                if (last.isEqual(today)) {
                                    Service.getInstance().sendThongBao(player, "Bạn đã điểm danh hôm nay rồi!");
                                    return;
                                }
                            }
                            player.lastCheckIn = LocalDateTime.now();
                            player.inventory.gem += 10000;
                            Item item457 = ItemService.gI().createNewItem((short) 457);
                            item457.quantity = 100;
                            item457.itemOptions.add(new ItemOption(30, 0));
                            InventoryService.gI().addItemBag(player, item457, 0);
                            PlayerService.gI().sendInfoHpMpMoney(player);
                            InventoryService.gI().sendItemBags(player);

                            Service.getInstance().sendThongBao(player, "Điểm danh thành công! Bạn nhận được 10.000 ngọc và 100 thỏi vàng.");
                            break;
                    }
                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_OPTION_LEVEL_SIDE_TASK) {
                    switch (select) {
                        case 0:
                            Service.getInstance().sendThongBao(player,
                                    "Hiện tại chỉ có thể thực hiện nhiệm vụ siêu khó !");
                            break;
                        case 1:
                            Service.getInstance().sendThongBao(player,
                                    "Hiện tại chỉ có thể thực hiện nhiệm vụ siêu khó !");
                            break;
                        case 2:
                            Service.getInstance().sendThongBao(player,
                                    "Hiện tại chỉ có thể thực hiện nhiệm vụ siêu khó !");
                            break;
                        case 3:
                            TaskService.gI().changeSideTask(player, (byte) select);
                            break;
                    }
                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_OPTION_PAY_SIDE_TASK) {
                    switch (select) {
                        case 0:
                            TaskService.gI().paySideTask(player);
                            break;
                        case 1:
                            TaskService.gI().removeSideTask(player);
                            break;
                    }
                }
            }
        }
    }
}
