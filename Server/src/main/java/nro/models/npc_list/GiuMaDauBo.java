package nro.models.npc_list;

import nro.consts.ConstMob;
import nro.consts.ConstNpc;
import nro.models.clan.Clan;
import nro.models.clan.ClanMember;
import nro.models.map.Zone;
import nro.models.mob.GauTuongCuop;
import nro.models.mob.Mob;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.services.Service;
import nro.utils.Util;

public class GiuMaDauBo extends Npc {

    public GiuMaDauBo(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            this.createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Ng??i mu?n t?m m?nh v? v? m?nh h?n b?ng tai Porata trong truy?n thuy?t, ta s? ??a ng??i ??n ???",
                    "Khi?u chi?n\nBoss G?u", "T? ch?i");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            switch (select) {
                case 0 -> {
                    Zone zone = player.zone;
                    if (zone == null) {
                        return;
                    }
                    if (player.clan == null) {
                        Service.getInstance().sendThongBao(player, "B?n kh?ng c? trong bang h?i!");
                        return;
                    }
                    Clan clan = player.clan;
                    ClanMember member = clan.getClanMember((int) player.id);
                    if (member == null || member.role != Clan.LEADER) {
                        Service.getInstance().sendThongBao(player, "Ch? bang ch? m?i c? quy?n g?i G?u T??ng C??p!");
                        return;
                    }
                    long membersInZone = clan.membersInGame.stream()
                            .filter(p -> p.zone != null && p.zone.equals(zone))
                            .count();
                    if (membersInZone < 1) {
                        Service.getInstance().sendThongBao(player, "C?n c? th?nh vi?n bang h?i trong khu v?c!");
                        return;
                    }
                    Mob mobBase = new Mob();
                    mobBase.tempId = ConstMob.GAU_TUONG_CUOP;
                    mobBase.level = 1;
                    mobBase.location = new nro.models.player.Location();
                    mobBase.location.x = player.location.x + Util.nextInt(-30, 30);
                    mobBase.location.y = player.location.y;
                    mobBase.zone = zone;
                    mobBase.point.setHpFull(2_000_000_000);
                    mobBase.point.setHP(mobBase.point.getHpFull());
                    mobBase.pDame = (byte) 50;
                    mobBase.pTiemNang = 0;
                    mobBase.setTiemNang();
                    mobBase.status = 5;
                    GauTuongCuop gauTuong = new GauTuongCuop(mobBase);
                    zone.addMob(gauTuong);
                    for (Player pl : zone.getPlayers()) {
                        if (pl != null && pl.getSession() != null) {
                            zone.mapInfo(pl);
                        }
                    }

                    Service.getInstance().sendThongBao(player, "G?u T??ng C??p ?? xu?t hi?n!");
                }
            }
        }
    }
}
