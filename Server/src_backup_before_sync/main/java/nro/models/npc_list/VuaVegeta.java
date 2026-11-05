package nro.models.npc_list;

import nro.consts.*;
import nro.models.item.ItemTemplate;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.skill.Skill;
import nro.models.skill.SkillTemplate;
import nro.server.io.Message;
import nro.services.*;
import nro.services.func.ShopService;
import nro.utils.Log;
import nro.utils.SkillUtil;
import nro.utils.TimeUtil;

public class VuaVegeta extends Npc {

    public VuaVegeta(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                if (player.gender != ConstPlayer.XAYDA) {
                    NpcService.gI().createTutorial(player, tempId, avartar, "Con hãy về hành tinh của mình mà thể hiện");
                    return;
                }
                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Chào con, ta rất vui khi gặp được con\nCon muốn làm gì nào ?",
                        "Nhiệm vụ", "Học\nKỹ năng");
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.iDMark.isBaseMenu()) {
                switch (select) {
                    case 0 -> {
                        if (player.playerTask != null && player.playerTask.taskMain != null
                                && player.playerTask.taskMain.index < player.playerTask.taskMain.subTasks.size()) {
                            NpcService.gI().createTutorial(player, tempId, avartar,
                                    player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).name);
                        }
                    }
                    case 1 -> handleSkillLearningMenu(player);
                }
            } else if (player.iDMark.getIndexMenu() == 12) {
                handleSkillLearning(player, select);
            } else if (player.iDMark.getIndexMenu() == 13) {
                if (select == 0) {
                    player.nPoint.tiemNang += (player.LearnSkill.Potential / 2);
                    player.LearnSkill.Time = -1;
                    Service.getInstance().point(player);
                    Service.getInstance().sendThongBao(player, "Đã huỷ học kỹ năng thành công.");
                }
            }
        }
    }

    private void handleSkillLearningMenu(Player player) {
        if (player.LearnSkill != null && player.LearnSkill.Time != -1) {
            long ngoc = 5;
            long time = player.LearnSkill.Time - System.currentTimeMillis();
            if (time > 0) {
                if (time / 600_000 >= 2) {
                    ngoc += (time / 600_000);
                }
                ItemTemplate template = ItemService.gI().getTemplate(player.LearnSkill.ItemTemplateSkillId);
                String name = template != null ? template.name : "Kỹ năng";
                String[] subName = name.split(" ");
                byte level = 1;
                try {
                    level = Byte.parseByte(subName[subName.length - 1]);
                } catch (Exception ignored) {}
                SkillTemplate st = SkillUtil.findSkillTemplate(SkillUtil.getTempSkillSkillByItemID(player.LearnSkill.ItemTemplateSkillId));
                String skillName = st != null ? st.name : name;
                createOtherMenu(player, 12,
                        "Con đang học kỹ năng\n" + skillName
                        + " cấp " + level + "\nThời gian còn lại " + TimeUtil.getTime(time),
                        "Học Cấp tốc " + ngoc + " ngọc", "Huỷ", "Bỏ qua");
                return;
            } else {
                player.LearnSkill.Time = -1;
                learnSkill(player);
                return;
            }
        } else {
            ShopService.gI().openShopNormal(player, this, 0, 0, player.gender);
        }
    }

    private void handleSkillLearning(Player player, int select) {
        switch (select) {
            case 0 -> {
                long time = player.LearnSkill.Time - System.currentTimeMillis();
                long ngoc = 5;
                if (time / 600_000 >= 2) {
                    ngoc += (time / 600_000);
                }
                if (player.inventory.gem < ngoc) {
                    Service.getInstance().sendThongBao(player, "Bạn không có đủ ngọc");
                    return;
                }
                player.inventory.subGem((int) ngoc);
                Service.getInstance().sendMoney(player);
                player.LearnSkill.Time = -1;
                learnSkill(player);
            }
            case 1 ->
                createOtherMenu(player, 13, "Con có muốn huỷ học kỹ năng này và nhận lại 50% số tiềm năng không ?", "Ok", "Đóng");
        }
    }

    private void learnSkill(Player player) {
        try {
            ItemTemplate template = ItemService.gI().getTemplate(player.LearnSkill.ItemTemplateSkillId);
            String name = template != null ? template.name : "";
            String[] subName = name.split(" ");
            byte level = 1;
            try {
                level = Byte.parseByte(subName[subName.length - 1]);
            } catch (Exception ignored) {}
            byte skillTempId = SkillUtil.getTempSkillSkillByItemID(player.LearnSkill.ItemTemplateSkillId);
            Skill curSkill = SkillUtil.createSkill(skillTempId, level);

            player.BoughtSkill.add((int) player.LearnSkill.ItemTemplateSkillId);
            SkillUtil.setSkill(player, curSkill);
            Message msg = Service.getInstance().messageSubCommand((byte) 62);
            msg.writer().writeShort(curSkill.skillId);
            player.sendMessage(msg);
            msg.cleanup();
            PlayerService.gI().sendInfoHpMpMoney(player);
            Service.getInstance().sendThongBao(player, "Bạn đã học thành công kỹ năng cấp " + level);
        } catch (Exception e) {
            Log.error(VuaVegeta.class, e);
        }
    }
}
