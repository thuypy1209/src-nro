package nro.models.boss.event;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossFactory;
import nro.models.item.ItemOption;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.services.Service;
import nro.utils.Util;

public class ThuyTinh extends Boss {

    public ThuyTinh() {
        super(BossFactory.THUY_TINH, BossData.THUY_TINH);
    }

    @Override
    protected boolean useSpecialSkill() {
        return false;
    }

    @Override
    public void rewards(Player pl) {
        ItemMap it = new ItemMap(this.zone, 422, 1, this.location.x,
                this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), pl.id);
        it.options.add(new ItemOption(77, Util.nextInt(15, 20)));
        it.options.add(new ItemOption(103, Util.nextInt(15, 20)));
        it.options.add(new ItemOption(50, Util.nextInt(15, 20)));
        it.options.add(new ItemOption(94, Util.nextInt(1, 10)));
        it.options.add(new ItemOption(14, Util.nextInt(2, 5)));
        it.options.add(new ItemOption(154, 0));
        it.options.add(new ItemOption(93, Util.nextInt(1, 15)));
        Service.getInstance().dropItemMap(this.zone, it);
    }

    @Override
    public void checkPlayerDie(Player pl) {
    }

    @Override
    public void initTalk() {
        this.textTalkMidle = new String[] { "Các ngươi không thoát được đâu!", "Ha ha ha!" };
    }
}
