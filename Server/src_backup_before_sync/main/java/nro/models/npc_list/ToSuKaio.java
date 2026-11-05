package nro.models.npc_list;

import nro.consts.*;
import nro.models.boss.mapoffline.NPC_ToSuKaio;
import nro.models.npc.Npc;
import nro.models.player.NPoint;
import nro.models.player.Player;
import nro.services.*;
import nro.utils.Util;

/**
 * NPC ToSuKaio - Hybrid Merge (By Mr Blue & KhanhDTK)
 */
public class ToSuKaio extends Npc {

    public ToSuKaio(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (this.mapId == 50) {
                String autoTrainingOption = player.dangKyTapTuDong ? "Hủy đăng\nký tập\ntự động" : "Đăng ký\ntập\ntự động";
                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Ta có thể giúp gì cho ngươi ?",
                        "Cắc cùm cum", autoTrainingOption, "Nâng\nGiới hạn\nSức mạnh", "Từ chối");
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

        if (this.mapId == 50) {
            int menuId = player.iDMark.getIndexMenu();

            if (menuId == ConstNpc.BASE_MENU) {
                switch (select) {
                    case 0 -> {
                        NPC_ToSuKaio toSukaio = new NPC_ToSuKaio();
                        toSukaio.NPC_ToSuKaio(player);
                        hide_npc(player, 43, 0);
                    }
                    case 1 -> handleAutoTrainingMenu(player);
                    case 2 -> showLimitPowerMenu(player);
                }
            } else if (menuId == ConstNpc.MENU_NANG_GIOI_HAN) {
                switch (select) {
                    case 0 -> showLimitPowerMyself(player);
                    case 1 -> handleLimitPowerPet(player, 0);
                    default -> this.npcChat(player, "Khi nào con cần thì quay lại gặp ta!");
                }
            } else if (menuId == ConstNpc.OPEN_POWER_MYSEFT) {
                handleLimitPowerMyselfOptions(player, select);
            } else if (menuId == ConstNpc.OPEN_POWER_PET) {
                handleLimitPowerPet(player, select);
            } else if (menuId == 2001) {
                handleAutoTrainingRegistration(player, select);
            }
        }
    }

    private void showLimitPowerMenu(Player player) {
        this.createOtherMenu(player, ConstNpc.MENU_NANG_GIOI_HAN,
                "Con muốn nâng giới hạn sức mạnh cho bản thân hay đệ tử?",
                "Bản thân", "Đệ tử", "Từ chối");
    }

    private void showLimitPowerMyself(Player player) {
        if (player.nPoint.limitPower < NPoint.MAX_LIMIT) {
            if (player.nPoint.limitPower >= 5 && player.nPoint.limitPower < 9) {
                this.createOtherMenu(player, ConstNpc.OPEN_POWER_MYSEFT,
                        "Ta sẽ truyền năng lượng giúp con mở giới hạn sức mạnh của bản thân lên "
                                + Util.numberToMoney(player.nPoint.getPowerNextLimit()),
                        "Nâng\ngiới hạn\nsức mạnh",
                        "Nâng ngay\n" + Util.numberToMoney(OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER) + " vàng",
                        "Đóng");
            } else {
                this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Yêu cầu đạt 50 tỷ sức mạnh", "Đóng");
            }
        } else {
            this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Sức mạnh của con đã đạt tới giới hạn",
                    "Đóng");
        }
    }

    private void handleLimitPowerMyselfOptions(Player player, int select) {
        if (player.nPoint.limitPower < 5 || player.nPoint.limitPower >= 9) {
            Service.getInstance().sendThongBao(player, "Sức mạnh của ngươi không đủ 50 tỷ!");
            return;
        }

        switch (select) {
            case 0 -> OpenPowerService.gI().openPowerBasic(player);
            case 1 -> {
                if (player.inventory.gold >= OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER) {
                    if (OpenPowerService.gI().openPowerSpeed(player)) {
                        player.inventory.subGold(OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER);
                        Service.getInstance().sendMoney(player);
                    }
                } else {
                    Service.getInstance().sendThongBao(player, "Bạn không đủ vàng để mở, còn thiếu "
                            + Util.numberToMoney(OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER - player.inventory.gold) + " vàng");
                }
            }
        }
    }

    private void handleLimitPowerPet(Player player, int select) {
        if (select != 0) {
            return;
        }

        if (player.pet == null) {
            Service.getInstance().sendThongBao(player, "Không thể thực hiện");
            return;
        }

        if (player.pet.nPoint.limitPower < 5 || player.pet.nPoint.limitPower >= 9) {
            this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Đệ tử của bạn cần đạt 50 tỷ sức mạnh!", "Đóng");
            return;
        }

        if (player.inventory.gold >= OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER) {
            if (OpenPowerService.gI().openPowerSpeed(player.pet)) {
                player.inventory.subGold(OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER);
                Service.getInstance().sendMoney(player);
            }
        } else {
            Service.getInstance().sendThongBao(player, "Bạn không đủ vàng để mở, còn thiếu "
                    + Util.numberToMoney(OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER - player.inventory.gold) + " vàng");
        }
    }

    private void handleAutoTrainingMenu(Player player) {
        if (player.dangKyTapTuDong) {
            player.dangKyTapTuDong = false;
            NpcService.gI().createTutorial(player, tempId, avartar,
                    "Con đã hủy thành công đăng ký tập tự động\nTừ giờ con muốn tập Offline hãy tự đến đây trước");
        } else {
            this.createOtherMenu(player, 2001,
                    "Đăng ký để mỗi khi Offline quá 30 phút, con sẽ được tự động luyện tập với tốc độ 1280 sức mạnh mỗi phút",
                    "Hướng\ndẫn\nthêm", "Đồng ý\n1 vàng\nmỗi lần", "Không\nđồng ý");
        }
    }

    private void handleAutoTrainingRegistration(Player player, int select) {
        switch (select) {
            case 0 -> NpcService.gI().createTutorial(player, tempId, avartar,
                    "Tập tự động: Khi Offline quá 30 phút, nhân vật sẽ tự động nhận thêm tiềm năng và sức mạnh.");
            case 1 -> {
                player.mapIdDangTapTuDong = mapId;
                player.dangKyTapTuDong = true;
                NpcService.gI().createTutorial(player, tempId, avartar,
                        "Từ giờ, quá 30 phút Offline con sẽ được tự động luyện tập");
            }
        }
    }
}
