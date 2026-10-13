package games.pontino.autofurnace;

import com.hypixel.hytale.builtin.crafting.component.BenchBlock;
import com.hypixel.hytale.builtin.crafting.component.ProcessingBenchBlock;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;

/**
 * Entry point: registra il componente persistente {@link AutoFurnaceBlock} e il sistema
 * che riconosce Accendi/Spegni e riaccende le fornaci in automatico.
 */
public class AutoFurnacePlugin extends JavaPlugin {
    private static AutoFurnacePlugin INSTANCE;

    private ComponentType<ChunkStore, AutoFurnaceBlock> autoFurnaceBlockComponentType;

    public static AutoFurnacePlugin get() {
        return INSTANCE;
    }

    public AutoFurnacePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        INSTANCE = this;
    }

    @Override
    protected void setup() {
        autoFurnaceBlockComponentType = getChunkStoreRegistry().registerComponent(AutoFurnaceBlock.class, "AutoFurnaceBlock", AutoFurnaceBlock.CODEC);

        getChunkStoreRegistry().registerSystem(new AutoIgniteSystem(
            autoFurnaceBlockComponentType,
            ProcessingBenchBlock.getComponentType(),
            BenchBlock.getComponentType()
        ));
    }

    public ComponentType<ChunkStore, AutoFurnaceBlock> getAutoFurnaceBlockComponentType() {
        return autoFurnaceBlockComponentType;
    }
}
