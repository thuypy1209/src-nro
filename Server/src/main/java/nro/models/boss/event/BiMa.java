package nro.models.boss.event;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossFactory;
import nro.models.item.ItemOption;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.services.Service;

public class BiMa extends Boss {

    public BiMa() {
        super(BossFactory.BI_MA, BossData.BI_MA);
    }

    @Override
    protected boolean useSpecialSkill() {
        return false;
    }

    @Override
    public void rewards(Player pl) {
        ItemMap it = new ItemMap(this.zone, 585, 1, this.location.x,
                this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), pl.id);
        it.options.add(new ItemOption(73, 0));
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
