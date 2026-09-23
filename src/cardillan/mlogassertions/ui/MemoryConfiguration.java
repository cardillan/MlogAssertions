package cardillan.mlogassertions.ui;

import arc.math.Interp;
import arc.scene.Group;
import arc.scene.actions.Actions;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Log;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.input.InputHandler;
import mindustry.ui.Styles;
import mindustry.ui.fragments.BlockConfigFragment;
import mindustry.world.blocks.logic.MemoryBlock;
import mindustry.world.blocks.logic.MemoryBlock.MemoryBuild;

import java.lang.reflect.Field;

import static mindustry.Vars.player;

public class MemoryConfiguration {

    // BlockConfigFragment private fields
    private static Field configTable;
    private static Field configSelected;

    public static void init() {
        InputHandler input = Vars.control.input;

        try {
            // BlockConfigFragment private fields
            configTable = BlockConfigFragment.class.getDeclaredField("table");
            configTable.setAccessible(true);
            configSelected = BlockConfigFragment.class.getDeclaredField("selected");
            configSelected.setAccessible(true);

            Field field = InputHandler.class.getDeclaredField("config");
            field.setAccessible(true);
            field.set(input, new MemoryBrowserConfigFragment(input.config));

            Vars.content.blocks().each(b -> b instanceof MemoryBlock, b -> b.configurable = true);
        } catch (ReflectiveOperationException e) {
            Log.err("[MlogAssertions] Failed to replace InputHandler.config", e);
        }
    }

    private static class MemoryBrowserConfigFragment extends BlockConfigFragment {
        private final BlockConfigFragment delegate;
        private final Table table;

        public MemoryBrowserConfigFragment(BlockConfigFragment delegate) {
            this.delegate = delegate;
            try {
                table = (Table) configTable.get(delegate);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to replace InputHandler.config", e);
            }
        }

        private Building selected() {
            try {
                return (Building) configSelected.get(delegate);
            } catch (IllegalAccessException e) {
                Log.err("[MlogAssertions] Failed to access BlockConfigFragment private fields", e);
                return null;
            }
        }

        @Override
        public void build(Group parent) {
            delegate.build(parent);
        }

        @Override
        public void forceHide() {
            delegate.forceHide();
        }

        @Override
        public boolean isShown() {
            return delegate.isShown();
        }

        @Override
        public Building getSelected() {
            return delegate.getSelected();
        }

        @Override
        public void showConfig(Building tile) {
            if (tile instanceof MemoryBuild memory) {
                try {
                    if (configSelected.get(delegate) instanceof Building bld) {
                        bld.onConfigureClosed();
                    }
                    if (tile.configTapped()) {
                        configSelected.set(delegate, tile);

                        table.visible = true;
                        table.clear();
                        table.background(null);
                        table.button(Icon.zoom, Styles.cleari, () -> new VarsDialog(new MemoryVars(memory)).show()).size(40);
                        table.pack();
                        table.setTransform(true);
                        table.actions(Actions.scaleTo(0f, 1f), Actions.visible(true),
                                Actions.scaleTo(1f, 1f, 0.07f, Interp.pow3Out));

                        table.update(() -> {
                            Building selected = selected();
                            if (selected != null && selected.shouldHideConfigure(player)) {
                                hideConfig();
                                return;
                            }

                            table.setOrigin(Align.center);
                            if (selected == null || selected.block == Blocks.air || !selected.isValid()) {
                                hideConfig();
                            } else {
                                selected.updateTableAlign(table);
                            }
                        });
                    }
                } catch (IllegalAccessException e) {
                    Log.err("[MlogAssertions] Failed to access BlockConfigFragment private fields", e);
                }
            } else {
                delegate.showConfig(tile);
            }
        }

        @Override
        public boolean hasConfigMouse() {
            return delegate.hasConfigMouse();
        }

        @Override
        public void hideConfig() {
            delegate.hideConfig();
        }
    }
}
