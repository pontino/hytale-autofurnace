package games.pontino.autofurnace;

import com.hypixel.hytale.builtin.crafting.component.BenchBlock;
import com.hypixel.hytale.builtin.crafting.component.CraftingManager;
import com.hypixel.hytale.builtin.crafting.component.ProcessingBenchBlock;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Gestisce l'accensione automatica delle fornaci (banchi di lavorazione a combustibile).
 *
 * <p>Il pulsante Accendi/Spegni della finestra vanilla viene "letto" osservando lo stato della fornace
 * mentre qualcuno ha la finestra aperta:
 * <ul>
 *   <li>spenta → accesa, non per opera nostra: il giocatore ha premuto Accendi → automatico ON;</li>
 *   <li>accesa → spenta mentre poteva ancora lavorare: il giocatore ha premuto Spegni → automatico OFF.
 *       Il gioco spegne da solo la fornace solo quando non può lavorare, quindi i due casi non si confondono.</li>
 * </ul>
 * Con l'automatico ON la fornace viene riaccesa ogni volta che il tick vanilla
 * (BenchSystems.ProcessingBenchTick) potrebbe lavorare.
 */
public class AutoIgniteSystem extends EntityTickingSystem<ChunkStore> {
    private static final String COLOR_ON = "#f2a541";
    private static final String COLOR_OFF = "#9aa7b3";

    private final ComponentType<ChunkStore, AutoFurnaceBlock> autoFurnaceComponentType;
    private final ComponentType<ChunkStore, ProcessingBenchBlock> processingBenchComponentType;
    private final ComponentType<ChunkStore, BenchBlock> benchBlockComponentType;
    private final ComponentType<ChunkStore, BlockModule.BlockStateInfo> blockStateInfoComponentType = BlockModule.BlockStateInfo.getComponentType();
    private final Query<ChunkStore> query;

    // Stato "acceso" visto al tick precedente, per riconoscere i click su Accendi/Spegni
    private final Map<ProcessingBenchBlock, Boolean> lastActive = new WeakHashMap<>();

    public AutoIgniteSystem(@Nonnull ComponentType<ChunkStore, AutoFurnaceBlock> autoFurnaceComponentType,
                            @Nonnull ComponentType<ChunkStore, ProcessingBenchBlock> processingBenchComponentType,
                            @Nonnull ComponentType<ChunkStore, BenchBlock> benchBlockComponentType) {
        this.autoFurnaceComponentType = autoFurnaceComponentType;
        this.processingBenchComponentType = processingBenchComponentType;
        this.benchBlockComponentType = benchBlockComponentType;
        this.query = Query.and(processingBenchComponentType, benchBlockComponentType);
    }

    @Override
    public Query<ChunkStore> getQuery() {
        return query;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        var processingBenchBlock = archetypeChunk.getComponent(index, processingBenchComponentType);
        var benchBlock = archetypeChunk.getComponent(index, benchBlockComponentType);
        if (processingBenchBlock == null || benchBlock == null) return;

        // Solo banchi con combustibile: gli altri sono sempre attivi
        var processingBench = processingBenchBlock.getProcessingBench();
        if (processingBench == null || processingBench.getFuel() == null) return;

        var autoFurnace = archetypeChunk.getComponent(index, autoFurnaceComponentType);
        var blockStateInfo = archetypeChunk.getComponent(index, blockStateInfoComponentType);
        boolean autoEnabled = autoFurnace != null && autoFurnace.isEnabled();
        boolean active = processingBenchBlock.isActive();
        Boolean wasActive = lastActive.get(processingBenchBlock);

        if (wasActive != null && wasActive != active && !benchBlock.getWindows().isEmpty()) {
            if (active && !autoEnabled) {
                // Accendi premuto
                autoEnabled = true;
                if (autoFurnace == null) {
                    commandBuffer.addComponent(archetypeChunk.getReferenceTo(index), autoFurnaceComponentType, new AutoFurnaceBlock(true));
                } else {
                    autoFurnace.setEnabled(true);
                }
                if (blockStateInfo != null) blockStateInfo.markNeedsSaving();
                notifyViewers(benchBlock, Message.raw("Furnace set to auto mode: it will relight by itself").color(COLOR_ON));
            } else if (!active && autoEnabled && canAutoIgnite(processingBenchBlock, benchBlock)) {
                // Spegni premuto mentre poteva ancora lavorare
                autoEnabled = false;
                autoFurnace.setEnabled(false);
                if (blockStateInfo != null) blockStateInfo.markNeedsSaving();
                notifyViewers(benchBlock, Message.raw("Furnace auto mode disabled").color(COLOR_OFF));
            }
        }

        if (autoEnabled && canAutoIgnite(processingBenchBlock, benchBlock)) {
            processingBenchBlock.setActive(true, benchBlock, blockStateInfo);
        }

        lastActive.put(processingBenchBlock, processingBenchBlock.isActive());
    }

    private static void notifyViewers(@Nonnull BenchBlock benchBlock, @Nonnull Message message) {
        for (var uuid : benchBlock.getWindows().keySet()) {
            var playerRef = Universe.get().getPlayer(uuid);
            if (playerRef != null) playerRef.sendMessage(message);
        }
    }

    /**
     * Stesse condizioni con cui il tick vanilla tiene accesa la fornace, così non la
     * accendiamo per poi vederla spegnere al tick successivo.
     */
    static boolean canAutoIgnite(@Nonnull ProcessingBenchBlock processingBenchBlock, @Nonnull BenchBlock benchBlock) {
        if (processingBenchBlock.isActive()) return false;

        var processingBench = processingBenchBlock.getProcessingBench();
        if (processingBench == null || processingBench.getFuel() == null) return false;

        var fuelContainer = processingBenchBlock.getFuelContainer();
        if (fuelContainer == null || fuelContainer.isEmpty()) return false;

        processingBenchBlock.checkForRecipeUpdate(benchBlock);
        var recipe = processingBenchBlock.getRecipe();
        if (recipe == null) return false;

        if (!processingBenchBlock.getOutputContainer().canAddItemStacks(CraftingManager.getOutputItemStacks(recipe), false, false)) return false;

        return !processingBenchBlock.getInputContainer()
            .getSlotMaterialsToRemove(CraftingManager.getInputMaterials(recipe), ProcessingBenchBlock.EXACT_RESOURCE_AMOUNTS, true)
            .isEmpty();
    }
}
