package org.moshang.fantasystructure.client.widget;

import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector4f;

@Accessors(chain = true)
public class FixedScrollableWidget extends DraggableScrollableWidgetGroup {
    @Getter @Setter
    protected int xBarWidth;
    @Getter @Setter
    protected int yBarHeight;

    public FixedScrollableWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.draggable = false;
    }

    @Override
    public FixedScrollableWidget setDraggable(boolean draggable) {
        return this;
    }

    @Override
    public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        drawBackgroundTexture(graphics, mouseX, mouseY);
        int x = getPosition().x;
        int y = getPosition().y;
        int width = getSize().width;
        int height = getSize().height;
        if (useScissor) {
            var trans = graphics.pose().last().pose();
            var realPos = trans.transform(new Vector4f(x, y, 0, 1));
            var realPos2 = trans.transform(new Vector4f(x + width, y + height, 0, 1));
            graphics.enableScissor((int) realPos.x, (int) realPos.y, (int) realPos2.x, (int) realPos2.y);
            if(!hookDrawInBackground(graphics, mouseX, mouseY, partialTicks)) {
                drawWidgetsBackground(graphics, mouseX, mouseY, partialTicks);
            }
            graphics.disableScissor();
        } else {
            if(!hookDrawInBackground(graphics, mouseX, mouseY, partialTicks)) {
                drawWidgetsBackground(graphics, mouseX, mouseY, partialTicks);
            }
        }

        if (xBarHeight > 0) {
            if (xBarB != null) {
                xBarB.draw(graphics, mouseX, mouseY, x, y + height - xBarHeight, width, xBarHeight);
            }
            if (xBarF != null) {
                int scrollableWidth = getMaxWidth() - width;
                int barStartX = scrollableWidth <= 0 ? 0 : (int) ((float) scrollXOffset / scrollableWidth * (width - xBarWidth));
                xBarF.draw(graphics, mouseX, mouseY, x + barStartX, y + height - xBarHeight, xBarWidth, xBarHeight);
            }
        }
        if (yBarWidth > 0) {
            if (yBarB != null) {
                yBarB.draw(graphics, mouseX, mouseY, x + width  - yBarWidth, y, yBarWidth, height);
            }
            if (yBarF != null) {
                int scrollableHeight = getMaxHeight() - height;
                int barStartY = scrollableHeight <= 0 ? 0 : (int) ((float) scrollYOffset / scrollableHeight * (height - yBarHeight));
                yBarF.draw(graphics, mouseX, mouseY, x + width  - yBarWidth, y + barStartY, yBarWidth, yBarHeight);
            }
        }
    }
}
