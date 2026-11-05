package nro.models.npc_list;

import nro.consts.*;
import nro.models.clan.Clan;
import nro.models.item.Item;
import nro.models.item.ItemOption;
import nro.models.item.ItemTemplate;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.skill.Skill;
import nro.models.skill.SkillTemplate;
import nro.server.io.Message;
import nro.services.*;
import nro.services.func.ChangeMapService;
import nro.services.func.ShopService;
import nro.utils.Log;
import nro.utils.SkillUtil;
import nro.utils.TimeUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NPC QuyLaoKame - Hybrid Merge (By Mr Blue & KhanhDTK)
 */
public class QuyLaoKame extends Npc {

    public static final int KOL_QUEST_MENU = 1007;
    public static final int KOL_VIP_REWARD_MENU = 1008;
    public static final int KOL_QUEST_TYPE_ITEM_COLLECTION = 1009;
    public static final int KOL_QUEST_TYPE_DUNGEON_COMPLETION = 1010;
    public static final int KOL_QUEST_TYPE_PVP_WINS = 1011;
    public static final int KOL_QUEST_TYPE_DAILY_QUEST_COMPLETION = 1012;
    public static final int KOL_QUEST_TYPE_BOSS_DEFEAT_PARTICIPATION = 1013;
    public static final int KOL_QUEST_TYPE_MONSTER_KILL_COUNT = 1014;

    private static class RewardItem {
        int itemId;
        int quantity;

        public RewardItem(int itemId, int quantity) {
            this.itemId = itemId;
            this.quantity = quantity;
        }
    }

    private static class KOLQuestData {
        int questType;
        int itemId;
        int requiredQuantity;
        List<RewardItem> rewards;
        String description;

        public KOLQuestData(int questType, int itemId, int requiredQuantity, List<RewardItem> rewards, String description) {
            this.questType = questType;
            this.itemId = itemId;
            this.requiredQuantity = requiredQuantity;
            this.rewards = rewards;
            this.description = description;
        }
    }

    private static final Map<Integer, KOLQuestData> KOL_QUESTS = new HashMap<>();
    private static final Map<Integer, KOLQuestData> KOL_VIP_QUESTS = new HashMap<>();

    static {
        KOL_QUESTS.put(1, new KOLQuestData(KOL_QUEST_TYPE_ITEM_COLLECTION, 1778, 100, Arrays.asList(new RewardItem(1821, 5)), "Nhiệm vụ 1:\nThu thập 100 cuốn chả giò (Quái doanh trại)"));
        KOL_QUESTS.put(2, new KOLQuestData(KOL_QUEST_TYPE_ITEM_COLLECTION, 1824, 10, Arrays.asList(new RewardItem(1592, 5), new RewardItem(1757, 5)), "Nhiệm vụ 2:\nThu thập 10 chai cuke 2 lít (Boss doanh trại)"));
        KOL_QUESTS.put(3, new KOLQuestData(KOL_QUEST_TYPE_DUNGEON_COMPLETION, -1, 20, Arrays.asList(new RewardItem(1360, 1)), "Nhiệm vụ 3:\nHoàn thành phó bản Destron Gas cấp 70 trên 20 lần"));
        KOL_QUESTS.put(4, new KOLQuestData(KOL_QUEST_TYPE_PVP_WINS, -1, 10, Arrays.asList(new RewardItem(1654, 1)), "Nhiệm vụ 4:\nĐánh bại 10 người trong đại hội võ thuật"));
        KOL_QUESTS.put(5, new KOLQuestData(KOL_QUEST_TYPE_DAILY_QUEST_COMPLETION, -1, 30, Arrays.asList(new RewardItem(1822, 10)), "Nhiệm vụ 5:\nHoàn thành 30 nhiệm vụ siêu khó hàng ngày"));
        KOL_QUESTS.put(6, new KOLQuestData(KOL_QUEST_TYPE_BOSS_DEFEAT_PARTICIPATION, -1, 5, Arrays.asList(new RewardItem(1797, 1), new RewardItem(1592, 5), new RewardItem(1757, 5)), "Nhiệm vụ 6:\nTham gia hạ gục boss baby 5 lần"));
        KOL_QUESTS.put(7, new KOLQuestData(KOL_QUEST_TYPE_MONSTER_KILL_COUNT, -1, 100000, Arrays.asList(new RewardItem(1592, 10), new RewardItem(664, 9999), new RewardItem(1757, 5)), "Nhiệm vụ 7:\nHạ 100.000 quái (dùng tự động luyện tập)"));

        KOL_VIP_QUESTS.put(1, new KOLQuestData(KOL_QUEST_TYPE_ITEM_COLLECTION, 1778, 100, Arrays.asList(new RewardItem(1821, 10)), "Nhiệm vụ 1:\nThu thập 100 cuốn chả giò (Quái doanh trại)"));
        KOL_VIP_QUESTS.put(2, new KOLQuestData(KOL_QUEST_TYPE_ITEM_COLLECTION, 1824, 10, Arrays.asList(new RewardItem(1592, 10), new RewardItem(1757, 10)), "Nhiệm vụ 2:\nThu thập 10 chai cuke 2 lít (Boss doanh trại)"));
        KOL_VIP_QUESTS.put(3, new KOLQuestData(KOL_QUEST_TYPE_DUNGEON_COMPLETION, -1, 20, Arrays.asList(new RewardItem(1360, 1)), "Nhiệm vụ 3:\nHoàn thành phó bản Destron Gas cấp 70 trên 20 lần"));
        KOL_VIP_QUESTS.put(4, new KOLQuestData(KOL_QUEST_TYPE_PVP_WINS, -1, 10, Arrays.asList(new RewardItem(1654, 1)), "Nhiệm vụ 4:\nĐánh bại 10 người trong đại hội võ thuật"));
        KOL_VIP_QUESTS.put(5, new KOLQuestData(KOL_QUEST_TYPE_DAILY_QUEST_COMPLETION, -1, 30, Arrays.asList(new RewardItem(1822, 20)), "Nhiệm vụ 5:\nHoàn thành 30 nhiệm vụ siêu khó hàng ngày"));
        KOL_VIP_QUESTS.put(6, new KOLQuestData(KOL_QUEST_TYPE_BOSS_DEFEAT_PARTICIPATION, -1, 5, Arrays.asList(new RewardItem(1797, 1), new RewardItem(1592, 10), new RewardItem(1757, 10)), "Nhiệm vụ 6:\nTham gia hạ gục boss baby 5 lần"));
        KOL_VIP_QUESTS.put(7, new KOLQuestData(KOL_QUEST_TYPE_MONSTER_KILL_COUNT, -1, 100000, Arrays.asList(new RewardItem(1592, 20), new RewardItem(1757, 10)), "Nhiệm vụ 7:\nHạ 100.000 quái (dùng tự động luyện tập)"));
    }

    public QuyLaoKame(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                Item ruacon = InventoryService.gI().findItemBag(player, 874);
                ArrayList<String> menu = new ArrayList<>();
                menu.add("Nói\nchuyện");
                menu.add("Học\nKỹ năng");
                menu.add("Nhận quà\nKOL");
                menu.add("Nhận quà\nKOL VIP");
                menu.add("Tặng\nCapsule bang");
                if (ruacon != null && ruacon.quantity >= 1) {
                    menu.add("Giao\nRùa con");
                }
                menu.add("Đổi tên");
                String[] menus = menu.toArray(String[]::new);

                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                        "|7|Người thành công là người làm những điều người khác không muốn làm.\n Áp Lực Tạo Lên Kim Cương.\n"
                                + "|7|-----YTB KhanhDTK-----",
                        menus);
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }

        if (player.iDMark.isBaseMenu()) {
            Item ruacon = InventoryService.gI().findItemBag(player, 874);
            boolean hasRuacon = (ruacon != null && ruacon.quantity >= 1);

            switch (select) {
                case 0 -> { // Nói chuyện
                    if (player.clan != null) {
                        if (player.clan.isLeader(player)) {
                            this.createOtherMenu(player, ConstNpc.MENU_NOI_CHUYEN,
                                    "|7|Đừng sợ bắt đầu lại. Đôi khi, đó là cơ hội để xây lại tốt hơn.\n"
                                            + "|7|-----YTB KhanhDTK-----",
                                    "Nhiệm vụ\n", "Về khu\nvực bang\n",
                                    "Giải tán\nBang hội", "Kho báu\ndưới biển");
                        } else {
                            this.createOtherMenu(player, ConstNpc.MENU_NOI_CHUYEN,
                                    "Chào con, ta rất vui khi gặp con\nCon muốn làm gì nào ?\n",
                                    "Nhiệm vụ\n", "Về khu\nvực bang\n", "Kho báu\ndưới biển");
                        }
                    } else {
                        this.createOtherMenu(player, ConstNpc.MENU_NOI_CHUYEN,
                                "|7|Đừng sợ bắt đầu lại. Đôi khi, đó là cơ hội để xây lại tốt hơn.\n"
                                        + "|7|-----YTB KhanhDTK-----",
                                "Nhiệm vụ\n", "Kho báu\ndưới biển");
                    }
                }
                case 1 -> { // Học Kỹ năng
                    if (player.gender != ConstPlayer.TRAI_DAT) {
                        NpcService.gI().createTutorial(player, tempId, avartar, "Con hãy về hành tinh của mình mà thể hiện");
                        return;
                    }
                    handleSkillLearningMenu(player);
                }
                case 2 -> handleKOLQuest(player, false); // Quà KOL
                case 3 -> handleKOLQuest(player, true);  // Quà KOL VIP
                case 4 -> { // Tặng Capsule bang
                    this.createOtherMenu(player, ConstNpc.MENU_TANG_CAPSULE,
                            "|7|Đừng sợ bắt đầu lại. Đôi khi, đó là cơ hội để xây lại tốt hơn.\n"
                                    + "|7|-----YTB KhanhDTK-----",
                            "Đóng góp capsule", "Đóng");
                }
                default -> {
                    if (hasRuacon && select == 5) {
                        handleTradeRuacon(player);
                    } else {
                        Service.getInstance().sendThongBaoOK(player, "Liên Hệ KhanhDTK để đổi Tên giá chỉ bằng ly coffe");
                    }
                }
            }
        } else if (player.iDMark.getIndexMenu() == 1) { // Giao rùa con nhận quà
            if (select == 0) {
                Item ruacon = InventoryService.gI().findItemBag(player, 874);
                if (ruacon != null && ruacon.quantity >= 1) {
                    if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                        InventoryService.gI().subQuantityItemsBag(player, ruacon, 1);
                        int[] items = {734, 920, 849, 743, 733};
                        Item item = ItemService.gI().createNewItem((short) items[nro.utils.Util.nextInt(items.length)]);
                        InventoryService.gI().addItemBag(player, item, 0);
                        InventoryService.gI().sendItemBags(player);
                        Service.getInstance().sendThongBao(player, "Bạn nhận được " + item.template.name);
                    } else {
                        Service.getInstance().sendThongBao(player, "Hành trang không đủ ô trống");
                    }
                }
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
        } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_TANG_CAPSULE) {
            if (select == 0) {
                if (player.clan == null) {
                    Service.getInstance().sendThongBao(player, "Chưa có bang hội");
                    return;
                }
                Item capsulebang = InventoryService.gI().findItemBagByTemp(player, (short) 2137);
                Clan clan = player.clan;

                if (capsulebang == null || capsulebang.quantity < 100) {
                    Service.getInstance().sendThongBao(player, "Không đủ capsule");
                    return;
                }
                if (clan.level >= 5) {
                    Service.getInstance().sendThongBao(player, "Bang hội của bạn đã đạt cấp tối đa");
                    return;
                }
                InventoryService.gI().subQuantityItemsBag(player, capsulebang, 100);
                InventoryService.gI().sendItemBags(player);
                clan.level++;
                clan.maxMember++;
                Service.getInstance().sendLogout(player);
            }
        } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_NOI_CHUYEN) {
            if (player.clan != null) {
                if (player.clan.isLeader(player)) {
                    switch (select) {
                        case 0 -> NpcService.gI().createTutorial(player, avartar,
                                "Nhiệm vụ hiện tại của con: " + player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).name);
                        case 1 -> ChangeMapService.gI().changeMap(player, player.clan.getClanArea(), 910, 190);
                        case 2 -> this.createOtherMenu(player, ConstNpc.ACP_GIAI_TAN_BANG,
                                "Con có chắc chắn giải tán bang hội không?", "Đồng ý", "Từ chối");
                        case 3 -> handleTreasureMap(player);
                    }
                } else {
                    switch (select) {
                        case 0 -> NpcService.gI().createTutorial(player, avartar,
                                "Nhiệm vụ hiện tại của con: " + player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).name);
                        case 1 -> Service.getInstance().sendThongBao(player, "Chức năng tạm đóng");
                        case 2 -> handleTreasureMap(player);
                    }
                }
            } else {
                switch (select) {
                    case 0 -> NpcService.gI().createTutorial(player, avartar, player.playerTask.taskMain.name);
                    case 1 -> handleTreasureMap(player);
                }
            }
        } else if (player.iDMark.getIndexMenu() == ConstNpc.ACP_GIAI_TAN_BANG) {
            if (select == 0 && player.clan != null && player.clan.isLeader(player)) {
                ClanService.gI().dissolveClan(player);
            }
        } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_OPENED_DBKB) {
            if (select == 0) {
                if (player.clan != null && player.clan.banDoKhoBau != null) {
                    ChangeMapService.gI().goToDBKB(player);
                } else {
                    this.npcChat(player, "Bang hội của con hiện không có hang kho báu đang hoạt động.");
                }
            }
        } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_OPEN_DBKB) {
            switch (select) {
                case 0 -> Service.getInstance().showTopClanBDKB(player);
                case 1 -> Service.getInstance().showTopClanBDKB(player);
                case 2 -> {
                    if (player.clan == null) {
                        Service.getInstance().sendThongBao(player, "Hãy vào bang hội trước");
                        return;
                    }
                    if (player.isAdmin() || player.nPoint.power >= nro.models.map.phoban.BanDoKhoBau.POWER_CAN_GO_TO_DBKB) {
                        nro.services.func.Input.gI().createFormChooseLevelBDKB(player);
                    } else {
                        this.npcChat(player, "Yêu cầu sức mạnh lớn hơn " + nro.utils.Util.numberToMoney(nro.models.map.phoban.BanDoKhoBau.POWER_CAN_GO_TO_DBKB));
                    }
                }
            }
        } else if (player.iDMark.getIndexMenu() == KOL_QUEST_MENU) {
            handleKOLQuestRewardConfirm(player, select, false);
        } else if (player.iDMark.getIndexMenu() == KOL_VIP_REWARD_MENU) {
            handleKOLQuestRewardConfirm(player, select, true);
        }
    }

    private void handleTreasureMap(Player player) {
        if (player.clan != null) {
            if (player.clan.banDoKhoBau != null) {
                this.createOtherMenu(player, ConstNpc.MENU_OPENED_DBKB,
                        "Bang hội của con đang đi tìm kho báu dưới biển cấp độ "
                                + player.clan.banDoKhoBau.level
                                + "\nCon có muốn đi theo không?",
                        "Đồng ý", "Từ chối");
            } else {
                this.createOtherMenu(player, ConstNpc.MENU_OPEN_DBKB,
                        "Đây là bản đồ kho báu hải tặc tí hon\nCác con cứ yên tâm lên đường\n"
                                + "Ở đây có ta lo\nNhớ chọn cấp độ vừa sức mình nhé",
                        "Top\nBang hội", "Thành tích\nBang", "Chọn\ncấp độ", "Từ chối");
            }
        } else {
            this.npcChat(player, "Con phải có bang hội ta mới có thể cho con đi");
        }
    }

    private void handleTradeRuacon(Player player) {
        Item ruacon = InventoryService.gI().findItemBag(player, 874);
        if (ruacon != null && ruacon.quantity >= 1) {
            this.createOtherMenu(player, 1, "Cảm ơn cậu đã cứu con rùa của ta\nĐể cảm ơn ta sẽ tặng cậu món quà.", "Nhận quà", "Đóng");
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
            Log.error(QuyLaoKame.class, e);
        }
    }

    private void handleKOLQuest(Player player, boolean isVIP) {
        int currentStage;
        Map<Integer, KOLQuestData> questsMap;
        int menuType;

        if (isVIP) {
            currentStage = player.kolVIPQuestStage;
            questsMap = KOL_VIP_QUESTS;
            menuType = KOL_VIP_REWARD_MENU;
        } else {
            currentStage = player.kolQuestStage;
            questsMap = KOL_QUESTS;
            menuType = KOL_QUEST_MENU;
        }

        if (currentStage < 1) {
            currentStage = 1;
            if (isVIP) {
                player.kolVIPQuestStage = 1;
            } else {
                player.kolQuestStage = 1;
            }
        }

        KOLQuestData questData = questsMap.get(currentStage);

        if (questData == null) {
            this.createOtherMenu(player, menuType, "Con đã hoàn thành tất cả nhiệm vụ " + (isVIP ? "KOL VIP" : "KOL") + " rồi! Chúc mừng con!", "Đóng");
            return;
        }

        long currentProgress = 0;
        switch (questData.questType) {
            case KOL_QUEST_TYPE_ITEM_COLLECTION:
                Item requiredItem = InventoryService.gI().findItemBag(player, questData.itemId);
                currentProgress = requiredItem != null ? requiredItem.quantity : 0;
                break;
            case KOL_QUEST_TYPE_DUNGEON_COMPLETION:
                currentProgress = player.destronGas70CompletionCount;
                break;
            case KOL_QUEST_TYPE_PVP_WINS:
                currentProgress = player.martialArtsTournamentWins;
                break;
            case KOL_QUEST_TYPE_DAILY_QUEST_COMPLETION:
                currentProgress = player.dailySuperHardQuestCompletionCount;
                break;
            case KOL_QUEST_TYPE_BOSS_DEFEAT_PARTICIPATION:
                currentProgress = player.bossBabyDefeatParticipationCount;
                break;
            case KOL_QUEST_TYPE_MONSTER_KILL_COUNT:
                currentProgress = player.monsterKillCountAutoTrain;
                break;
            default:
                Service.getInstance().sendThongBao(player, "Lỗi: Loại nhiệm vụ không xác định.");
                return;
        }

        int percent = (int) ((currentProgress * 100.0) / questData.requiredQuantity);
        if (percent > 100) {
            percent = 100;
        }

        String rewardDetails = formatRewardDetails(questData.rewards);
        String npcText = questData.description + "\nPhần thưởng: " + rewardDetails + "\nHoàn thành: "
                + currentProgress + "/" + questData.requiredQuantity + " (" + percent + "%)";

        if (currentProgress >= questData.requiredQuantity) {
            this.createOtherMenu(player, menuType, npcText, "Nhận thưởng", "Đóng");
        } else {
            this.createOtherMenu(player, menuType, npcText, "Đóng");
        }
    }

    private void handleKOLQuestRewardConfirm(Player player, int select, boolean isVIP) {
        if (select == 0) {
            int currentStage;
            Map<Integer, KOLQuestData> questsMap;

            if (isVIP) {
                Item vipTicket = InventoryService.gI().findItemBag(player, 1825);
                if (vipTicket == null || vipTicket.quantity < 1) {
                    Service.getInstance().sendThongBao(player, "Bạn cần có Vé Nhiệm Vụ VIP để nhận thưởng KOL VIP.");
                    openBaseMenu(player);
                    return;
                }
                currentStage = player.kolVIPQuestStage;
                questsMap = KOL_VIP_QUESTS;
            } else {
                currentStage = player.kolQuestStage;
                questsMap = KOL_QUESTS;
            }

            if (currentStage < 1) {
                currentStage = 1;
                if (isVIP) {
                    player.kolVIPQuestStage = 1;
                } else {
                    player.kolQuestStage = 1;
                }
            }

            KOLQuestData questData = questsMap.get(currentStage);

            if (questData == null) {
                Service.getInstance().sendThongBao(player, "Không tìm thấy nhiệm vụ hiện tại.");
                return;
            }

            long currentProgress = 0;
            switch (questData.questType) {
                case KOL_QUEST_TYPE_ITEM_COLLECTION:
                    Item requiredItem = InventoryService.gI().findItemBag(player, questData.itemId);
                    currentProgress = requiredItem != null ? requiredItem.quantity : 0;
                    break;
                case KOL_QUEST_TYPE_DUNGEON_COMPLETION:
                    currentProgress = player.destronGas70CompletionCount;
                    break;
                case KOL_QUEST_TYPE_PVP_WINS:
                    currentProgress = player.martialArtsTournamentWins;
                    break;
                case KOL_QUEST_TYPE_DAILY_QUEST_COMPLETION:
                    currentProgress = player.dailySuperHardQuestCompletionCount;
                    break;
                case KOL_QUEST_TYPE_BOSS_DEFEAT_PARTICIPATION:
                    currentProgress = player.bossBabyDefeatParticipationCount;
                    break;
                case KOL_QUEST_TYPE_MONSTER_KILL_COUNT:
                    currentProgress = player.monsterKillCountAutoTrain;
                    break;
                default:
                    Service.getInstance().sendThongBao(player, "Lỗi: Loại nhiệm vụ không xác định.");
                    return;
            }

            if (currentProgress >= questData.requiredQuantity) {
                if (questData.questType == KOL_QUEST_TYPE_ITEM_COLLECTION) {
                    Item requiredItem = InventoryService.gI().findItemBag(player, questData.itemId);
                    if (requiredItem != null) {
                        InventoryService.gI().subQuantityItemsBag(player, requiredItem, questData.requiredQuantity);
                    }
                }

                for (RewardItem rewardData : questData.rewards) {
                    Item rewardItem = ItemService.gI().createNewItem((short) rewardData.itemId);
                    rewardItem.quantity = rewardData.quantity;

                    List<ItemOption> options = getPetBackpackOptions(rewardData.itemId);
                    if (options != null) {
                        rewardItem.itemOptions.addAll(options);
                    }

                    InventoryService.gI().addItemBag(player, rewardItem, 0);
                }
                InventoryService.gI().sendItemBags(player);

                Service.getInstance().sendThongBao(player, "Bạn đã nhận phần thưởng nhiệm vụ " + (isVIP ? "KOL VIP" : "KOL") + " cấp " + currentStage + "!");

                if (isVIP) {
                    player.kolVIPQuestStage++;
                } else {
                    player.kolQuestStage++;
                }

                openBaseMenu(player);
            } else {
                Service.getInstance().sendThongBao(player, "Bạn không đủ điều kiện hoàn thành nhiệm vụ!");
                openBaseMenu(player);
            }
        } else {
            openBaseMenu(player);
        }
    }

    private List<ItemOption> getPetBackpackOptions(int itemId) {
        List<ItemOption> options = new ArrayList<>();
        options.add(new ItemOption(73, 0));

        switch (itemId) {
            case 1360:
                options.add(new ItemOption(77, 13));
                options.add(new ItemOption(103, 13));
                options.add(new ItemOption(50, 13));
                options.add(new ItemOption(101, 20));
                options.add(new ItemOption(30, 1));
                options.add(new ItemOption(93, 90));
                break;
            case 1654:
                options.add(new ItemOption(50, 16));
                options.add(new ItemOption(77, 15));
                options.add(new ItemOption(103, 15));
                options.add(new ItemOption(106, 0));
                options.add(new ItemOption(30, 1));
                options.add(new ItemOption(93, 30));
                break;
            case 1797:
                options.add(new ItemOption(50, 25));
                options.add(new ItemOption(103, 30));
                options.add(new ItemOption(30, 1));
                options.add(new ItemOption(93, 90));
                break;
            default:
                return null;
        }
        return options;
    }

    private String formatRewardDetails(List<RewardItem> rewards) {
        StringBuilder details = new StringBuilder();
        for (int i = 0; i < rewards.size(); i++) {
            RewardItem rewardData = rewards.get(i);
            ItemTemplate template = ItemService.gI().getTemplate(rewardData.itemId);
            String rewardName = template != null ? template.name : ("Vật phẩm " + rewardData.itemId);
            details.append(rewardData.quantity).append(" ").append(rewardName);
            if (i < rewards.size() - 1) {
                details.append(", ");
            }
        }
        return details.toString();
    }
}
