package com.godofthings.mept.client;

import com.godofthings.mept.WandMenu;
import net.minecraft.client.gui.GuiGraphics;
import com.godofthings.mept.WandMenu;
import net.minecraft.client.gui.components.Button;
import com.godofthings.mept.WandMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.godofthings.mept.WandMenu;
import net.minecraft.network.chat.Component;
import com.godofthings.mept.WandMenu;
import net.minecraft.resources.ResourceLocation;
import com.godofthings.mept.WandMenu;
import net.minecraft.world.entity.player.Inventory;
import com.godofthings.mept.WandMenu;
import net.neoforged.neoforge.network.PacketDistributor;

import com.godofthings.mept.WandMenu;
import com.godofthings.mept.network.SyncPagePayload;

/**
 * Screen for the ME Placement Tool configuration menu
 */
public class MeptWandScreen extends AbstractContainerScreen<WandMenu> {
    // Custom toolbox background texture
    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath(com.godofthings.Godofthings.MODID, "textures/gui/mept/toolbox.png");
    // Page button textures
    private static final ResourceLocation PREV_PAGE = ResourceLocation.fromNamespaceAndPath(com.godofthings.Godofthings.MODID, "textures/gui/mept/prev_page.png");
    private static final ResourceLocation NEXT_PAGE = ResourceLocation.fromNamespaceAndPath(com.godofthings.Godofthings.MODID, "textures/gui/mept/next_page.png");
    
    private Button prevButton;
    private Button nextButton;
    
    // Button dimensions (should match the actual texture size)
    private static final int BTN_WIDTH = 16;
    private static final int BTN_HEIGHT = 16;

    public MeptWandScreen(WandMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        // New GUI dimensions: 175x167
        this.imageWidth = 175;
        this.imageHeight = 167;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        // Inventory label position - just above player inventory at y=84
        this.inventoryLabelY = 73;
    }

    @Override
    protected void init() {
        super.init();
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;

        // Previous page button - positioned to the left of the 3x3 grid
        this.prevButton = Button.builder(Component.empty(), button -> {
            int currentPage = this.menu.getCurrentPage();
            if (currentPage > 0) {
                this.menu.setCurrentPage(currentPage - 1);
                PacketDistributor.sendToServer(new SyncPagePayload(currentPage - 1));
                updateButtonVisibility();
            }
        }).bounds(relX + 44, relY + 35, BTN_WIDTH, BTN_HEIGHT).build();

        // Next page button - positioned to the right of the 3x3 grid
        this.nextButton = Button.builder(Component.empty(), button -> {
            int currentPage = this.menu.getCurrentPage();
            if (currentPage < WandMenu.MAX_PAGES - 1) {
                this.menu.setCurrentPage(currentPage + 1);
                PacketDistributor.sendToServer(new SyncPagePayload(currentPage + 1));
                updateButtonVisibility();
            }
        }).bounds(relX + 117, relY + 35, BTN_WIDTH, BTN_HEIGHT).build();

        this.addRenderableWidget(prevButton);
        this.addRenderableWidget(nextButton);
        updateButtonVisibility();
    }

    private void updateButtonVisibility() {
        int currentPage = this.menu.getCurrentPage();
        this.prevButton.visible = currentPage > 0;
        this.nextButton.visible = currentPage < WandMenu.MAX_PAGES - 1;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        
        // Render custom button textures (override default button rendering)
        renderPageButtons(guiGraphics);
        
        // Ghost slot items are now rendered by vanilla's slot rendering system
        // via GhostSlot.getItem() -> displayStackSupplier, ensuring proper Z-ordering
        // with JEI/REI drag overlays
        
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Draw page indicator above the 3x3 grid area
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        int currentPage = this.menu.getCurrentPage();
        String pageText = (currentPage + 1) + "/" + WandMenu.MAX_PAGES;
        int textWidth = this.font.width(pageText);
        // Position centered above the 3x3 grid
        guiGraphics.drawString(this.font, pageText, relX + 88 - textWidth / 2, relY + 8, 0x404040, false);
    }
    
    /**
     * Render custom page button textures over the invisible buttons.
     */
    private void renderPageButtons(GuiGraphics guiGraphics) {
        if (prevButton.visible) {
            guiGraphics.blit(PREV_PAGE, prevButton.getX(), prevButton.getY(), 0, 0, BTN_WIDTH, BTN_HEIGHT, BTN_WIDTH, BTN_HEIGHT);
        }
        if (nextButton.visible) {
            guiGraphics.blit(NEXT_PAGE, nextButton.getX(), nextButton.getY(), 0, 0, BTN_WIDTH, BTN_HEIGHT, BTN_WIDTH, BTN_HEIGHT);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Push pose and translate Z to render tooltip above REI's drag highlight overlay
        // REI's overlay typically renders at Z around 300-400, so we use 500 to ensure
        // our tooltip appears above it
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 500);
        
        // Vanilla's renderTooltip will now correctly handle ghost slots
        // since GhostSlot.getItem() returns the actual display item
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        
        guiGraphics.pose().popPose();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int x, int y) {
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(BG, relX, relY, 0, 0, this.imageWidth, this.imageHeight);
    }
}
