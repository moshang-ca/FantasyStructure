package org.moshang.fantasystructure.client.widget;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.function.Consumer;

@Accessors(chain = true)
public class PageScrollableWidget extends WidgetGroup {
    @Getter @Setter
    private boolean scrollable = true;
    @Getter @Setter
    private int currentPage = 0;
    @Setter
    private Consumer<Integer> onScroll;

    public PageScrollableWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
        if(onScroll == null) return false;
        if (this.isMouseOverElement(mouseX, mouseY)) {
            if (super.mouseWheelMove(mouseX, mouseY, wheelDelta)) {
                setFocus(true);
                return true;
            }
            if (scrollable) {
                setFocus(true);
                if (isFocus()) {
                    if(Math.abs(wheelDelta) > .1f) {
                        int pageChange = (int) Math.signum(-wheelDelta);
                        onScroll.accept(pageChange);
                    }
                }
            }
            return true;
        }
        setFocus(false);
        return false;
    }

    @Override
    public PageScrollableWidget setBackground(IGuiTexture... backgroundTexture) {
        super.setBackground(backgroundTexture);
        return this;
    }
}
