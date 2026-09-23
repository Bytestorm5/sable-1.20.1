package dev.ryanhcode.sable.index;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SableToasts {
    public static final SableToastId SUB_LEVEL_LOAD_FAILURE = new SableToastId();
    public static final SableToastId SUB_LEVEL_SAVE_FAILURE = new SableToastId();
    public static final SableToastId SUB_LEVEL_PHYSICS_FAILURE = new SableToastId();

    /**
     * Shows a system toast, or updates the one already showing for the same id.
     * 1.20.1's system toasts are keyed by a fixed enum, so Sable's toasts carry their own token instead.
     */
    public static void addOrUpdate(final ToastComponent toasts, final SableToastId id, final Component title, @Nullable final Component message) {
        final SableToast existing = toasts.getToast(SableToast.class, id);
        if (existing == null) {
            toasts.addToast(new SableToast(id, title, message));
        } else {
            existing.delegate.reset(title, message);
        }
    }

    public static final class SableToastId {
    }

    private static final class SableToast implements Toast {
        private final SableToastId id;
        private final SystemToast delegate;

        private SableToast(final SableToastId id, final Component title, @Nullable final Component message) {
            this.id = id;
            this.delegate = new SystemToast(SystemToast.SystemToastIds.PERIODIC_NOTIFICATION, title, message);
        }

        @Override
        public @NotNull Visibility render(final @NotNull GuiGraphics graphics, final @NotNull ToastComponent toastComponent, final long timeSinceLastVisible) {
            return this.delegate.render(graphics, toastComponent, timeSinceLastVisible);
        }

        @Override
        public @NotNull Object getToken() {
            return this.id;
        }

        @Override
        public int width() {
            return this.delegate.width();
        }

        @Override
        public int height() {
            return this.delegate.height();
        }
    }
}
