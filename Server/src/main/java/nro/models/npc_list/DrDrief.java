package nro.models.npc_list;

import nro.consts.*;
import nro.models.clan.Clan;
import nro.models.clan.ClanMember;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.services.*;
import nro.services.func.ChangeMapService;

/**
 * NPC DrDrief - Hybrid Merge (By Mr Blue & KhanhDTK)
 */
public class DrDrief extends Npc {

    public DrDrief(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player pl) {
        if (canOpenNpc(pl)) {
            if (this.mapId == 84) {
                this.createOtherMenu(pl, ConstNpc.BASE_MENU,
                        "Tàu Vũ Trụ của ta có thể đưa cậu đến hành tinh khác chỉ trong 3 giây. Cậu muốn đi đâu?",
                        pl.gender == ConstPlayer.TRAI_DAT ? "Đến\nTrái Đất"
                                : pl.gender == ConstPlayer.NAMEC ? "Đến\nNamếc" : "Đến\nXayda");
            } else if (this.mapId == 153) {
                Clan clan = pl.clan;
                ClanMember cm = pl.clanMember;
                if (clan != null && cm != null) {
                    if (cm.role == Clan.LEADER) {
                        this.createOtherMenu(pl, ConstNpc.BASE_MENU,
                                "Cần 1000 capsule bang [đang có " + clan.clanPoint
                                        + " capsule bang] để nâng cấp bang hội lên cấp "
                                        + (clan.level + 1) + "\n"
                                        + "+1 tối đa số lượng thành viên",
                                "Về\nĐảoKame", "Góp " + cm.memberPoint + " capsule", "Nâng cấp",
                                "Từ chối");
                    } else {
                        this.createOtherMenu(pl, ConstNpc.BASE_MENU, "Bạn đang có " + cm.memberPoint
                                + " capsule bang, bạn có muốn đóng góp toàn bộ cho bang hội của mình không ?",
                                "Về\nĐảoKame", "Đồng ý", "Từ chối");
                    }
                } else {
                    this.createOtherMenu(pl, ConstNpc.BASE_MENU, "Bạn cần vào bang hội trước", "Về\nĐảoKame", "Từ chối");
                }
            } else if (!TaskService.gI().checkDoneTaskTalkNpc(pl, this)) {
                if (pl.playerTask.taskMain.id == 7) {
                    NpcService.gI().createTutorial(pl, this.avartar,
                            "Hãy lên đường cứu đứa bé nhà tôi\n"
                                    + "Chắc bây giờ nó đang sợ hãi lắm rồi");
                } else {
                    this.createOtherMenu(pl, ConstNpc.BASE_MENU,
                            "Tàu Vũ Trụ của ta có thể đưa cậu đến hành tinh khác chỉ trong 3 giây. Cậu muốn đi đâu?",
                            "Đến\nNamếc", "Đến\nXayda", "Siêu thị");
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 84) {
                ChangeMapService.gI().changeMapBySpaceShip(player, player.gender + 24, -1, -1);
            } else if (mapId == 153) {
                if (select == 0) {
                    ChangeMapService.gI().changeMap(player, ConstMap.DAO_KAME, -1, 1059, 408);
                    return;
                }
                Clan clan = player.clan;
                ClanMember cm = player.clanMember;
                if (clan == null || cm == null) {
                    Service.getInstance().sendThongBao(player, "Bạn chưa có bang hội!");
                    return;
                }
                if (select == 1) {
                    player.clan.clanPoint += cm.memberPoint;
                    cm.clanPoint += cm.memberPoint;
                    cm.memberPoint = 0;
                    Service.getInstance().sendThongBao(player, "Đóng góp thành công");
                } else if (select == 2 && cm.role == Clan.LEADER) {
                    if (clan.level >= 5) {
                        Service.getInstance().sendThongBao(player,
                                "Bang hội của bạn đã đạt cấp tối đa");
                        return;
                    }
                    if (clan.clanPoint < 1000) {
                        Service.getInstance().sendThongBao(player, "Không đủ capsule");
                        return;
                    }
                    clan.level++;
                    clan.maxMember++;
                    clan.clanPoint -= 1000;
                    Service.getInstance().sendThongBao(player,
                            "Bang hội của bạn đã được nâng cấp lên cấp " + clan.level);
                }
            } else if (player.iDMark.isBaseMenu()) {
                switch (select) {
                    case 0:
                        ChangeMapService.gI().changeMapBySpaceShip(player, 25, -1, -1);
                        break;
                    case 1:
                        ChangeMapService.gI().changeMapBySpaceShip(player, 26, -1, -1);
                        break;
                    case 2:
                        ChangeMapService.gI().changeMapBySpaceShip(player, 84, -1, -1);
                        break;
                }
            }
        }
    }
}
