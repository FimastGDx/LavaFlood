package net.fimastgd.lavaflood.client.gui;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.Minecraft;

import net.fimastgd.lavaflood.world.inventory.StartGuiMenu;
import net.fimastgd.lavaflood.network.LavafloodModPackets;
import net.fimastgd.lavaflood.network.LavaFloodStartPacket;

import java.util.HashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;

public class StartGuiScreen extends AbstractContainerScreen<StartGuiMenu> {
	private final static HashMap<String, Object> guistate = StartGuiMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	EditBox Seconds;
	private String errorMessage = "";

	// GUI dimensions
	// imageWidth = 204, imageHeight = 120
	// EditBox: width=80 → x-offset = (204-80)/2 = 62
	// Button: width=100 → x-offset = (204-100)/2 = 52

	public StartGuiScreen(StartGuiMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 204;
		this.imageHeight = 120;
	}

	@Override
	public void render(PoseStack ms, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(ms);
		super.render(ms, mouseX, mouseY, partialTicks);
		this.renderTooltip(ms, mouseX, mouseY);
		Seconds.render(ms, mouseX, mouseY, partialTicks);
	}

	@Override
	protected void renderBg(PoseStack ms, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
	}

	@Override
	public boolean keyPressed(int key, int b, int c) {
		if (key == 256) {
			this.minecraft.player.closeContainer();
			return true;
		}
		if (Seconds.isFocused())
			return Seconds.keyPressed(key, b, c);
		return super.keyPressed(key, b, c);
	}

	@Override
	public void containerTick() {
		super.containerTick();
		Seconds.tick();
	}

	@Override
	protected void renderLabels(PoseStack ps, int mouseX, int mouseY) {
		// centered text labels (coords relative to leftPos / topPos)
		String line1 = "Lava rise interval (seconds):";
		String line2 = "Range: 1 – 1800";
		int cx1 = (imageWidth - font.width(line1)) / 2;
		int cx2 = (imageWidth - font.width(line2)) / 2;
		font.draw(ps, line1, cx1, 8, 0xFFFFFF);
		font.draw(ps, line2, cx2, 20, 0xAAAAAA);

		// error message (red, centered)
		if (!errorMessage.isEmpty()) {
			int ex = (imageWidth - font.width(errorMessage)) / 2;
			font.draw(ps, "§c" + errorMessage, ex, 72, 0xFFFFFF);
		}
	}

	@Override
	public void onClose() {
		super.onClose();
		Minecraft.getInstance().keyboardHandler.setSendRepeatsToGui(false);
	}

	@Override
	public void init() {
		super.init();
		this.minecraft.keyboardHandler.setSendRepeatsToGui(true);

		// EditBox (width 80, centered)
		int boxW = 80;
		int boxX = this.leftPos + (imageWidth - boxW) / 2; // leftPos + 62
		int boxY = this.topPos + 35;

		Seconds = new EditBox(this.font, boxX, boxY, boxW, 20, new TextComponent("60")) {
			{
				setSuggestion("60");
			}

			// Allow only digit characters
			@Override
			public boolean charTyped(char c, int mods) {
				if (Character.isDigit(c))
					return super.charTyped(c, mods);
				return false;
			}

			@Override
			public void insertText(String text) {
				super.insertText(text);
				setSuggestion(getValue().isEmpty() ? "60" : null);
			}

			@Override
			public void moveCursorTo(int pos) {
				super.moveCursorTo(pos);
				setSuggestion(getValue().isEmpty() ? "60" : null);
			}
		};
		Seconds.setMaxLength(4); // max "1800"
		guistate.put("text:Seconds", Seconds);
		this.addWidget(this.Seconds);

		// START button (width 100, centered)
		int btnW = 100;
		int btnX = this.leftPos + (imageWidth - btnW) / 2; // leftPos + 52
		int btnY = this.topPos + 62;

		this.addRenderableWidget(new Button(btnX, btnY, btnW, 20,
				new TextComponent("START"), e -> onStartPressed()));
	}

	private void onStartPressed() {
		String raw = Seconds.getValue().trim();
		if (raw.isEmpty())
			raw = "60";

		int seconds;
		try {
			seconds = Integer.parseInt(raw);
		} catch (NumberFormatException ex) {
			errorMessage = "Enter a valid number!";
			return;
		}

		if (seconds < 1 || seconds > 1800) {
			errorMessage = "Must be between 1 and 1800!";
			return;
		}

		errorMessage = "";

		// сообщение в чат (клиентская сторона, только этому игроку)
		String lang = this.minecraft.options.languageCode; // например "ru_ru"

		if (lang != null && lang.toLowerCase().startsWith("ru")) {
			// русский - две строки
			this.minecraft.player.sendMessage(
					new net.minecraft.network.chat.TextComponent(
							"§e[ВНИМАНИЕ] Будьте осторожны, лава уже поднимается!"),
					java.util.UUID.randomUUID());
			this.minecraft.player.sendMessage(
					new net.minecraft.network.chat.TextComponent(
							"§e§nБетонные блоки §d§lне заполняются лавой§e, "
									+ "вы можете построить куб из бетонных блоков, "
									+ "и внутри него лава вас не затопит."),
					java.util.UUID.randomUUID());
		} else {
			// английский и все прочие языки
			this.minecraft.player.sendMessage(
					new net.minecraft.network.chat.TextComponent(
							"§e[WARNING] Be careful, the lava is already rising!"),
					java.util.UUID.randomUUID());
			this.minecraft.player.sendMessage(
					new net.minecraft.network.chat.TextComponent(
							"§e§nConcrete blocks §d§lare NOT filled with lava§e. "
									+ "Build a closed concrete cube — "
									+ "lava will never flood the inside."),
					java.util.UUID.randomUUID());
		}

		// отправить пакет на сервер
		LavafloodModPackets.CHANNEL.sendToServer(new LavaFloodStartPacket(seconds));
		this.minecraft.player.closeContainer();
	}
}