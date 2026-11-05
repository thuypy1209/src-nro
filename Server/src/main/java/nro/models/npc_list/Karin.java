package nro.models.npc_list;

import nro.consts.*;
import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossManager;
import nro.models.boss.mapoffline.Boss_ThanMeo;
import nro.models.map.dungeon.zones.ZSnakeRoad;
import nro.models.npc.Npc;
import nro.models.npc.NpcFactory;
import nro.models.player.Player;
import nro.services.*;
import nro.utils.Util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * NPC Karin - Hybrid Merge (By Mr Blue & KhanhDTK)
 */
public class Karin extends Npc {

    public Karin(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (mapId == ConstMap.THAP_KARIN) {
                player.thachDauNPC = 0;
                if (player.zone instanceof ZSnakeRoad) {
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Hãy cầm lấy hai hạt đậu cuối cùng ở đây\nCố giữ mình nhé "
                                    + player.name,
                            "Cảm ơn\nsư phụ");
                } else if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                    if (player.doneThachDauThanMeo == 0) {
                        this.createOtherMenu(player, ConstNpc.THACH_DAU_THAN_MEO,
                                "Muốn chiến thắng Tàu Pảy Pảy phải đánh bại được ta đã",
                                "Đăng ký\n tập\n tự động", "Nhiệm vụ", "Tập luyện\n với\n Thần Mèo",
                                "Thách đấu\nThần Mèo");
                    } else {
                        if (player.doneThachDauYanjiro == 1) {
                            this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                    "Con hãy bay theo cây Gậy Như Ý trên đỉnh tháp để đến Thần Điện gặp Thượng đế\nCon rất xứng đáng để làm đệ tử ông ấy.",
                                    "Đăng ký\n tập\n tự động", "Tập luyện\n với\n Thần Mèo",
                                    "Tập luyện\n với\n Yajiro");
                        } else {
                            this.createOtherMenu(player, ConstNpc.THACH_DAU_YAJIRO,
                                    "Từ giờ Yajirô sẽ luyện tập cùng ngươi. Yajirô đã từng lên đây tập luyện và bây giờ hắn mạnh hơn ta đấy",
                                    "Đăng ký\n tập\n tự động", "Tập luyện\n với\n Yajirô",
                                    "Thách đấu\nYajirô");
                        }
                    }
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            Boss boss = BossManager.gI().getBossById((int) ((int) -251003 - player.id) - 300000);
            if (mapId == ConstMap.THAP_KARIN) {
                switch (player.iDMark.getIndexMenu()) {
                    case ConstNpc.BASE_MENU:
                        if (player.zone instanceof ZSnakeRoad) {
                            switch (select) {
                                case 0:
                                    player.setInteractWithKarin(true);
                                    Service.getInstance().sendThongBao(player,
                                            "Hãy mau bay xuống chân tháp Karin");
                                    break;
                            }
                        } else {
                            switch (select) {
                                case 0:
                                    handleAutoTrainingMenu(player);
                                    break;
                                case 1: // tập luyện với Thần mèo
                                    createOtherMenu(player, ConstNpc.COFIRM_LUYEN_TAP_THAN_MEO,
                                            "Con có chắc chắn muốn tập luyện ?\nTập luyện với ta sẽ tăng 20 sức mạnh mỗi phút",
                                            "Đồng ý\nluyện tập", "Không\nđồng ý");
                                    break;
                                case 2: // tập luyện với yajirô
                                    createOtherMenu(player, ConstNpc.COFIRM_LUYEN_TAP_YAJIRO,
                                            "Con có chắc chắn muốn tập luyện ?\nTập luyện với Yajirô sẽ tăng 40 sức mạnh mỗi phút",
                                            "Đồng ý\nluyện tập", "Không\nđồng ý");
                                    break;
                            }
                        }
                        break;
                    case ConstNpc.COFIRM_LUYEN_TAP_THAN_MEO:
                        switch (select) {
                            case 0:
                                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                                Runnable task = () -> {
                                    hide_npc(player, 18, 0);
                                    scheduler.shutdown();
                                };
                                scheduler.schedule(task, 1, TimeUnit.SECONDS);

                                try {
                                    Boss_ThanMeo dt = new Boss_ThanMeo(
                                            Util.createIdDuongTank((int) ((byte) player.id)),
                                            BossData.THAN_MEO, player.zone, this.cx, this.cy,
                                            (int) player.id);
                                } catch (Exception ex) {
                                    Logger.getLogger(NpcFactory.class.getName()).log(Level.SEVERE,
                                            null, ex);
                                }
                                PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_PVP);
                                player.zone.load_Me_To_Another(player);
                                break;
                            default:
                                break;
                        }
                        break;
                    case ConstNpc.COFIRM_LUYEN_TAP_YAJIRO:
                        switch (select) {
                            case 0:
                                player.activeYajiro = 1;
                                PlayerService.gI().changeAndSendTypePK(boss, ConstPlayer.PK_PVP);
                                PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_PVP);
                                break;
                            default:
                                break;
                        }
                        break;
                    case ConstNpc.THACH_DAU_THAN_MEO:
                        switch (select) {
                            case 0:
                                handleAutoTrainingMenu(player);
                                break;
                            case 1:
                                if (player.playerTask != null && player.playerTask.taskMain != null
                                        && player.playerTask.taskMain.index < player.playerTask.taskMain.subTasks.size()) {
                                    NpcService.gI().createTutorial(player, tempId, avartar,
                                            player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).name);
                                }
                                break;
                            case 2: // luyện tập với thần mèo
                                createOtherMenu(player, ConstNpc.COFIRM_LUYEN_TAP_THAN_MEO,
                                            "Con có chắc chắn muốn tập luyện ?\nTập luyện với ta sẽ tăng 20 sức mạnh mỗi phút",
                                            "Đồng ý\nluyện tập", "Không\nđồng ý");
                                break;
                            case 3:
                                createOtherMenu(player, ConstNpc.COFIRM_THACH_DAU_THAN_MEO,
                                        "Con có chắc chắn muốn thách đấu ?\n"
                                                + "Nếu thắng ta sẽ được tập với Yajirô, tăng 40 sức mạnh mỗi phút",
                                        "Đồng ý\ngiao đấu", "Không\nđồng ý");
                                break;
                        }
                        break;
                    case ConstNpc.COFIRM_THACH_DAU_THAN_MEO:
                        switch (select) {
                            case 0:
                                try {
                                    Boss_ThanMeo dt = new Boss_ThanMeo(
                                            Util.createIdDuongTank((int) ((byte) player.id)),
                                            BossData.THAN_MEO, player.zone, this.cx, this.cy,
                                            (int) player.id);
                                } catch (Exception ex) {
                                    Logger.getLogger(NpcFactory.class.getName()).log(Level.SEVERE,
                                            null, ex);
                                }

                                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                                Runnable task = () -> {
                                    hide_npc(player, 18, 0);
                                    scheduler.shutdown();
                                };
                                scheduler.schedule(task, 1, TimeUnit.SECONDS);

                                PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_PVP);
                                player.thachDauNPC = 1;
                                player.zone.load_Me_To_Another(player);
                                break;
                            default:
                                break;
                        }
                        break;
                    case ConstNpc.THACH_DAU_YAJIRO:
                        switch (select) {
                            case 0:
                                handleAutoTrainingMenu(player);
                                break;
                            case 1:// luyện tập với Yajirô
                                createOtherMenu(player, ConstNpc.COFIRM_LUYEN_TAP_YAJIRO,
                                        "Con có chắc chắn muốn tập luyện ?\nTập luyện với Yajirô sẽ tăng 40 sức mạnh mỗi phút",
                                        "Đồng ý\nluyện tập", "Không\nđồng ý");
                                break;
                            case 2:// thách đấu với Yajirô
                                createOtherMenu(player, ConstNpc.COFIRM_THACH_DAU_YAJIRO,
                                        "Con có chắc chắn muốn thách đấu ?\n"
                                                + "Nếu thắng được Yajirô, con sẽ được học võ với người mạnh hơn để tăng đến 80 sức mạnh mỗi phút",
                                        "Đồng ý\ngiao đấu", "Không\nđồng ý");
                                break;
                        }
                        break;
                    case ConstNpc.COFIRM_THACH_DAU_YAJIRO:
                        switch (select) {
                            case 0:
                                player.activeYajiro = 1;
                                PlayerService.gI().changeAndSendTypePK(boss, ConstPlayer.PK_PVP);
                                PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_PVP);
                                player.thachDauNPC = 1;
                                break;
                            default:
                                break;
                        }
                        break;
                    case 2001:
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
                        break;
                }
            }
        }
    }

    private void handleAutoTrainingMenu(Player player) {
        if (player.dangKyTapTuDong) {
            player.dangKyTapTuDong = false;
            NpcService.gI().createTutorial(player, tempId, avartar,
                    "Con đã hủy thành công đăng ký tập tự động\ntừ giờ con muốn tập Offline hãy tự đến đây trước");
        } else {
            this.createOtherMenu(player, 2001,
                    "Đăng ký để mỗi khi Offline quá 30 phút, con sẽ được tự động luyện tập với tốc độ 1280 sức mạnh mỗi phút",
                    "Hướng\ndẫn\nthêm", "Đồng ý\n1 ngọc\nmỗi lần", "Không\nđồng ý");
        }
    }
}
