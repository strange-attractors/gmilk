package milky.menu;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.KeyStroke;

import org.jdom.Element;

import milky.MenuSettings;
import processing.core.PApplet;

/**
 * 
 * 
 * 
 * @author Felix Woitzel, Feb 2011
 * 
 */
public abstract class InteractiveMenuComponent implements KeyListener, ClipboardOwner {

	public static MenuSettings settings = new MenuSettings();
	private static KeyListener p5defaultKeyListener; // override the window close on 'Esc'
	public static Clipboard clipboard;

	public static void init(PApplet context) {
		p5defaultKeyListener = context.getKeyListeners()[0];
		context.removeKeyListener(p5defaultKeyListener);
		clipboard = context.getToolkit().getSystemClipboard();

	}

	protected String prefix = "";
	protected String suffix = "";
	private String label = "_label_";
	private String tooltip = "";
	
	private static String status;

	public void setLabel(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	protected static void setStatus(String status) {
		// XXX: activate status for a certain time, then fade out
		System.out.println("status: " + status);
		InteractiveMenuComponent.status = status;
	}

	public static String getStatus() {
		return status;
	}

	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}

	public String getTooltip() {
		return tooltip;
	}

	private InteractiveMenuComponent parent;

	public void setParent(InteractiveMenuComponent parent) {
		this.parent = parent;
	}

	public InteractiveMenuComponent getParent() {
		return parent;
	}

	public abstract void setXML(Element node);

	public abstract Element getXML();

	public static InteractiveMenuComponent buildComponent(Element node) {
		if (node.getName() == "int") {
			return buildInt(node);
		} else if (node.getName() == "menu") {
			return buildMenu(node);
		} else if (node.getName() == "text") {
			return buildTextInput(node);
		} else if (node.getName() == "javascript") {
			return buildJavascriptEditor(node);
		} else if (node.getName() == "toggle") {
			return buildToggle(node);
		} else if (node.getName() == "trigger") {
			return buildTrigger(node);
		}
		return null;
	}

	private static JavascriptEditor buildJavascriptEditor(Element node) {
		JavascriptEditor javascriptEditor = new JavascriptEditor(node.getAttributeValue("label"));
		javascriptEditor.setXML(node);
		return javascriptEditor;
	}

	private static Trigger buildTrigger(Element node) {
		Trigger trigger = new Trigger(node.getAttributeValue("label"));
		trigger.setXML(node);
		return trigger;
	}

	private static Toggle buildToggle(Element node) {
		return new Toggle(node.getAttributeValue("label"), Boolean.getBoolean(node.getAttributeValue("state")));
	}

	private static TextInput buildTextInput(Element node) {
		TextInput textInput = new TextInput(node.getAttributeValue("label"));
		textInput.setXML(node);
		return textInput;
	}

	private static Menu buildMenu(Element node) {
		Menu menu = new Menu(node.getAttributeValue("label"));
		menu.setXML(node);
		return menu;
	}

	private static IntegerInput buildInt(Element node) {
		IntegerInput integerInput = new IntegerInput(node.getAttributeValue("varName"), Integer.parseInt(node.getAttributeValue("min")), Integer
				.parseInt(node.getAttributeValue("max")));
		integerInput.setXML(node);
		return integerInput;
	}

	protected static final void register(InteractiveMenuComponent node) {
		components.add(node);
	}

	public boolean isRoot() {
		return getParent() == null;
	}

	protected abstract void draw(PApplet context, int x, int y);

	protected void drawBackground(PApplet context, int x, int y, int h, int w) {
		context.noStroke();
		context.noSmooth();
		context.fill(settings.backgroundColor);
		context.rect(x, y, h, w);
	}

	public static final void displayMenu(PApplet context, int x, int y) {
		if (activeComponent != null) {
			if (activeComponent.getTooltip() != null && !activeComponent.getTooltip().equals("")) {
				context.noStroke();
				context.noSmooth();
				context.fill(settings.backgroundColor2);
				context.rect(x, y - settings.margin, settings.fontWidth * activeComponent.tooltip.length() + settings.margin * 2, settings.fontHeight
						+ settings.margin * 2);

				context.fill(settings.fontColor);
				context.text(activeComponent.getTooltip(), x + settings.margin, y + settings.fontHeight - settings.margin);
				activeComponent.draw(context, x, y + settings.fontHeight + settings.margin);
			} else {
				activeComponent.draw(context, x, y);
			}
		}
	}

	private static final ArrayList<InteractiveMenuComponent> components = new ArrayList<InteractiveMenuComponent>();
	private static InteractiveMenuComponent activeComponent;
	private static HashMap<Integer, InteractiveMenuComponent> menuEntryPoints = new HashMap<Integer, InteractiveMenuComponent>();

	public static final void addEntryPoint(Integer keyCode, InteractiveMenuComponent menuComponent) {
		menuEntryPoints.put(keyCode, menuComponent);
	}

	public static final void removeEntryPoint(Integer keyCode) {
		menuEntryPoints.remove(keyCode);
	}

	public final void setActive(boolean activated) {
		if (activated && (activeComponent != this)) {
			if (activeComponent != null) {
				activeComponent.onClose();
			}
			activeComponent = this;
			onActivation();
		} else if (activeComponent == this) {
			if (getParent() != null) {
				onClose();
				getParent().setActive(true);
			} else {
				onClose();
				activeComponent = null;
			}
		}
	}

	protected boolean isActive() {
		return this == activeComponent;
	}

	public void close() {
		setActive(false);
	}

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable transferable) {
	}

	private static final TextInput error = new TextInput("error");

	protected void onError(Exception e) {
		nonUnicodesPressed.remove(KeyEvent.VK_ENTER);
		ArrayList<String> lines = new ArrayList<String>();
		lines.add("unexpected error: " + getLabel());
		lines.add("text: " + e.toString());
		lines.add("message: " + e.getMessage());
		error.setText(lines);
		error.setParent(this);
		error.setActive(true);
	}

	private boolean entryPointTriggered = false;

	@Override
	public void keyPressed(KeyEvent keyEvent) {
		KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(keyEvent);
		if (keyStroke == KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK)
				|| keyStroke == KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, ActionEvent.CTRL_MASK)) {
			if (activeComponent != null) {
				activeComponent.copy();
			}
			return;
		}
		if (keyStroke == KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK)
				|| keyStroke == KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, ActionEvent.SHIFT_MASK)) {
			if (activeComponent != null) {
				activeComponent.paste();
			}
			return;
		}
		if (activeComponent == null && menuEntryPoints.get(keyEvent.getKeyCode()) != null) {
			menuEntryPoints.get(keyEvent.getKeyCode()).setActive(true);
			entryPointTriggered = true;
			return;
		}
		char keyChar = keyEvent.getKeyChar();
		if (!validUnicode(keyChar)) {
			preprocessNonUnicode(keyEvent);
			if (activeComponent != null) {
				activeComponent.onNonUnicodeInput();
			}
		}
		if (keyChar != KeyEvent.VK_ESCAPE) { // override the P5 close command
			p5defaultKeyListener.keyPressed(keyEvent);
		}
	}

	abstract protected void paste();

	abstract protected void copy();

	@Override
	public void keyReleased(KeyEvent keyEvent) {
		nonUnicodesPressed.remove(keyEvent.getKeyCode());
		p5defaultKeyListener.keyReleased(keyEvent);
	}

	@Override
	public void keyTyped(KeyEvent keyEvent) {
		if (!entryPointTriggered) {
			// System.out.println("keyEvent: " + keyEvent);
			char keyChar = keyEvent.getKeyChar();
			if (validUnicode(keyChar) && activeComponent != null) {
				activeComponent.onUnicodeInput(keyChar);
			}
			if (keyChar != KeyEvent.VK_ESCAPE) {
				p5defaultKeyListener.keyTyped(keyEvent);
			}
		} else {
			entryPointTriggered = false;
		}
	}

	/**
	 * copied from [Interfascia ALPHA 002 -- http://superstable.net/interfascia/]
	 * 
	 * only change: removed cursor key char codes (0xFFFF)
	 * 
	 * @param b
	 * @return
	 */
	private static final boolean validUnicode(char b) {
		int c = (int) b;
		if (c == 0xFFFF) {
			return false;
		}
		return ((c >= 0x0020 && c <= 0x007E) || (c >= 0x00A1 && c <= 0x017F) || (c == 0x018F) || (c == 0x0192) || (c >= 0x01A0 && c <= 0x01A1)
				|| (c >= 0x01AF && c <= 0x01B0) || (c >= 0x01D0 && c <= 0x01DC) || (c >= 0x01FA && c <= 0x01FF) || (c >= 0x0218 && c <= 0x021B)
				|| (c >= 0x0250 && c <= 0x02A8) || (c >= 0x02B0 && c <= 0x02E9) || (c >= 0x0300 && c <= 0x0345) || (c >= 0x0374 && c <= 0x0375)
				|| (c == 0x037A) || (c == 0x037E) || (c >= 0x0384 && c <= 0x038A) || (c >= 0x038E && c <= 0x03A1) || (c >= 0x03A3 && c <= 0x03CE)
				|| (c >= 0x03D0 && c <= 0x03D6) || (c >= 0x03DA) || (c >= 0x03DC) || (c >= 0x03DE) || (c >= 0x03E0) || (c >= 0x03E2 && c <= 0x03F3)
				|| (c >= 0x0401 && c <= 0x044F) || (c >= 0x0451 && c <= 0x045C) || (c >= 0x045E && c <= 0x0486) || (c >= 0x0490 && c <= 0x04C4)
				|| (c >= 0x04C7 && c <= 0x04C9) || (c >= 0x04CB && c <= 0x04CC) || (c >= 0x04D0 && c <= 0x04EB) || (c >= 0x04EE && c <= 0x04F5)
				|| (c >= 0x04F8 && c <= 0x04F9) || (c >= 0x0591 && c <= 0x05A1) || (c >= 0x05A3 && c <= 0x05C4) || (c >= 0x05D0 && c <= 0x05EA)
				|| (c >= 0x05F0 && c <= 0x05F4) || (c >= 0x060C) || (c >= 0x061B) || (c >= 0x061F) || (c >= 0x0621 && c <= 0x063A)
				|| (c >= 0x0640 && c <= 0x0655) || (c >= 0x0660 && c <= 0x06EE) || (c >= 0x06F0 && c <= 0x06FE) || (c >= 0x0901 && c <= 0x0939)
				|| (c >= 0x093C && c <= 0x094D) || (c >= 0x0950 && c <= 0x0954) || (c >= 0x0958 && c <= 0x0970) || (c >= 0x0E01 && c <= 0x0E3A)
				|| (c >= 0x1E80 && c <= 0x1E85) || (c >= 0x1EA0 && c <= 0x1EF9) || (c >= 0x2000 && c <= 0x202E) || (c >= 0x2030 && c <= 0x2046)
				|| (c == 0x2070) || (c >= 0x2074 && c <= 0x208E) || (c == 0x2091) || (c >= 0x20A0 && c <= 0x20AC) || (c >= 0x2100 && c <= 0x2138)
				|| (c >= 0x2153 && c <= 0x2182) || (c >= 0x2190 && c <= 0x21EA) || (c >= 0x2190 && c <= 0x21EA) || (c >= 0x2000 && c <= 0x22F1)
				|| (c == 0x2302) || (c >= 0x2320 && c <= 0x2321) || (c >= 0x2460 && c <= 0x2469) || (c == 0x2500) || (c == 0x2502) || (c == 0x250C)
				|| (c == 0x2510) || (c == 0x2514) || (c == 0x2518) || (c == 0x251C) || (c == 0x2524) || (c == 0x252C) || (c == 0x2534)
				|| (c == 0x253C) || (c >= 0x2550 && c <= 0x256C) || (c == 0x2580) || (c == 0x2584) || (c == 0x2588) || (c == 0x258C)
				|| (c >= 0x2590 && c <= 0x2593) || (c == 0x25A0) || (c >= 0x25AA && c <= 0x25AC) || (c == 0x25B2) || (c == 0x25BA) || (c == 0x25BC)
				|| (c == 0x25C4) || (c == 0x25C6) || (c >= 0x25CA && c <= 0x25CC) || (c == 0x25CF) || (c >= 0x25D7 && c <= 0x25D9) || (c == 0x25E6)
				|| (c == 0x2605) || (c == 0x260E) || (c == 0x261B) || (c == 0x261E) || (c >= 0x263A && c <= 0x263C) || (c == 0x2640) || (c == 0x2642)
				|| (c == 0x2660) || (c == 0x2663) || (c == 0x2665) || (c == 0x2666) || (c == 0x266A) || (c == 0x266B) || (c >= 0x2701 && c <= 0x2709)
				|| (c >= 0x270C && c <= 0x2727) || (c >= 0x2729 && c <= 0x274B) || (c == 0x274D) || (c >= 0x274F && c <= 0x2752) || (c == 0x2756)
				|| (c >= 0x2758 && c <= 0x275E) || (c >= 0x2761 && c <= 0x2767) || (c >= 0x2776 && c <= 0x2794) || (c >= 0x2798 && c <= 0x27BE)
				|| (c >= 0xF001 && c <= 0xF002) || (c >= 0xF021 && c <= 0xF0FF) || (c >= 0xF601 && c <= 0xF605) || (c >= 0xF610 && c <= 0xF616)
				|| (c >= 0xF800 && c <= 0xF807) || (c >= 0xF80A && c <= 0xF80B) || (c >= 0xF80E && c <= 0xF811) || (c >= 0xF814 && c <= 0xF815)
				|| (c >= 0xF81F && c <= 0xF820) || (c >= 0xF81F && c <= 0xF820) || (c == 0xF833));
	}

	private static final void preprocessNonUnicode(KeyEvent keyEvent) {
		nonUnicodesPressed.add(keyEvent.getKeyCode());
	}

	protected static final HashSet<Integer> nonUnicodesPressed = new HashSet<Integer>();

	protected abstract void onUnicodeInput(char unicode);

	protected abstract void onNonUnicodeInput();

	protected void onActivation() {
	};

	protected void onClose() {
	}

	public void clear() {
		components.remove(this);
	};
}
