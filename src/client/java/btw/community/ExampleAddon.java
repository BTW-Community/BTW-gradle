package btw.community;

import net.minecraft.src.FCAddOn;

public class ExampleAddon extends FCAddOn {
    public static ExampleAddon instance = new ExampleAddon();

    private ExampleAddon() {
        super("Example Name", "0.1.0", "Ex");
    }

    @Override
    public void Initialize() {

    }
}
