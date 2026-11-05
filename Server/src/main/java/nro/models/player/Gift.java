package nro.models.player;

/**
 *
 * @author 💖 YTB KhanhDTK 💖
 * 
 *
 */
public class Gift {

    private Player player;

    public Gift(Player player) {
        this.player = player;
    }

    public boolean goldTanThu;
    public boolean gemTanThu;

    public void dispose() {
        this.player = null;
    }

}
