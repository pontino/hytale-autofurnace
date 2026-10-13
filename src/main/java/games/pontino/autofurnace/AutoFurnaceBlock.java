package games.pontino.autofurnace;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;

/**
 * Flag "accensione automatica" salvato sul block entity della fornace.
 * Viene persistito col chunk, quindi sopravvive a riavvii e scaricamenti.
 */
public class AutoFurnaceBlock implements Component<ChunkStore> {

    @Nonnull
    public static final BuilderCodec<AutoFurnaceBlock> CODEC = BuilderCodec.builder(AutoFurnaceBlock.class, AutoFurnaceBlock::new)
        .append(
            new KeyedCodec<>("Enabled", Codec.BOOLEAN),
            (state, b) -> state.enabled = b,
            state -> Boolean.valueOf(state.enabled)
        )
        .add()
        .build();

    private boolean enabled;

    public static ComponentType<ChunkStore, AutoFurnaceBlock> getComponentType() {
        return AutoFurnacePlugin.get().getAutoFurnaceBlockComponentType();
    }

    public AutoFurnaceBlock() {
    }

    public AutoFurnaceBlock(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Component<ChunkStore> clone() {
        return new AutoFurnaceBlock(enabled);
    }
}
