package org.mineacademy.bfo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.mineacademy.bfo.ChatUtil;
import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.PlayerUtil;
import org.mineacademy.bfo.SerializeUtil;
import org.mineacademy.bfo.Valid;
import org.mineacademy.bfo.collection.SerializedMap;
import org.mineacademy.bfo.remain.CompChatColor;
import org.mineacademy.bfo.remain.Remain;

import lombok.NonNull;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * A very simple way of sending interactive chat messages
 */
public final class SimpleComponent implements ConfigSerializable {

	/**
	 * Prevent oversized JSON from kicking players by removing interactive elements from it?
	 */
	public static boolean STRIP_OVERSIZED_COMPONENTS = true;

	/**
	 * The pattern to match URL addresses when parsing text
	 */
	private static final Pattern URL_PATTERN = Pattern.compile("^(?:(https?)://)?([-\\w_\\.]{2,}\\.[a-z]{2,4})(/\\S*)?$");

	/**
	 * The past components
	 */
	private final List<Part> pastComponents = new ArrayList<>();

	/**
	 * The current component being created
	 */
	private Part currentComponent;

	/**
	 * Create a new interactive chat component
	 *
	 * @param text
	 */
	private SimpleComponent(String text) {

		// Inject the center element here already
		if (Common.stripColors(text).startsWith("<center>"))
			text = ChatUtil.center(text.replace("<center>", "").trim());

		this.currentComponent = new Part(text);
	}

	/**
	 * Private constructor used when deserializing
	 */
	private SimpleComponent() {
	}

	// --------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------

	/**
	 * Add a show text event
	 *
	 * @param texts
	 * @return
	 */
	public SimpleComponent onHover(Collection<String> texts) {
		return this.onHover(Common.toArray(texts));
	}

	/**
	 * Add a show text hover event
	 *
	 * @param lines
	 * @return
	 */
	public SimpleComponent onHover(String... lines) {
		// I don't know why we have to wrap this inside new text component but we do this
		// to properly reset bold and other decoration colors
		final String joined = Common.colorize(String.join("\n", lines));
		this.currentComponent.hoverEvent = HoverEvent.showText(LegacyComponentSerializer.legacySection().deserialize(joined));

		return this;
	}

	/**
	 * Set view permission for last component part
	 *
	 * @param viewPermission
	 * @return
	 */
	public SimpleComponent viewPermission(String viewPermission) {
		this.currentComponent.viewPermission = viewPermission;

		return this;
	}

	/**
	 * Set view permission for last component part
	 *
	 * @param viewCondition
	 * @return
	 */
	public SimpleComponent viewCondition(String viewCondition) {
		this.currentComponent.viewCondition = viewCondition;

		return this;
	}

	/**
	 * Add a run command event
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent onClickRunCmd(String text) {
		return this.onClick(ClickEvent.runCommand(text));
	}

	/**
	 * Add a suggest command event
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent onClickSuggestCmd(String text) {
		return this.onClick(ClickEvent.suggestCommand(text));
	}

	/**
	 * Open the given URL
	 *
	 * @param url
	 * @return
	 */
	public SimpleComponent onClickOpenUrl(String url) {
		return this.onClick(ClickEvent.openUrl(url));
	}

	/**
	 * Add a command event
	 *
	 * @param event
	 * @return
	 */
	public SimpleComponent onClick(ClickEvent event) {
		this.currentComponent.clickEvent = event;

		return this;
	}

	/**
	 * Invoke insertion
	 *
	 * @param insertion
	 * @return
	 */
	public SimpleComponent onClickInsert(String insertion) {
		this.currentComponent.insertion = insertion;

		return this;
	}

	// --------------------------------------------------------------------
	// Building
	// --------------------------------------------------------------------

	/**
	 * Append new component at the beginning of all components
	 *
	 * @param component
	 * @return
	 */
	public SimpleComponent appendFirst(SimpleComponent component) {
		this.pastComponents.add(0, component.currentComponent);
		this.pastComponents.addAll(0, component.pastComponents);

		return this;
	}

	/**
	 * Append text to this simple component
	 *
	 * @param text
	 * @return
	 */
	public SimpleComponent append(String text) {
		return this.append(text, true);
	}

	/**
	 * Append text to this simple component
	 *
	 * @param text
	 * @param colorize
	 * @return
	 */
	public SimpleComponent append(String text, boolean colorize) {
		return this.append(text, null, colorize);
	}

	/**
	 * Create another component. The current is put in a list of past components
	 * so next time you use onClick or onHover, you will be added the event to the new one
	 * specified here
	 *
	 * @param text
	 * @param inheritFormatting
	 * @return
	 */
	public SimpleComponent append(String text, TextComponent inheritFormatting) {
		return this.append(text, inheritFormatting, true);
	}

	/**
	 * Create another component. The current is put in a list of past components
	 * so next time you use onClick or onHover, you will be added the event to the new one
	 * specified here
	 *
	 * @param text
	 * @param inheritFormatting
	 * @param colorize
	 * @return
	 */
	public SimpleComponent append(String text, TextComponent inheritFormatting, boolean colorize) {

		// Get the last extra
		TextComponent inherit = inheritFormatting != null ? inheritFormatting : this.currentComponent.toTextComponent(false, null);

		if (inherit != null && !inherit.children().isEmpty())
			inherit = (TextComponent) inherit.children().get(inherit.children().size() - 1);

		// Center text for each line separately if replacing colors
		if (colorize) {
			final List<String> formatContents = Arrays.asList(text.split("\n"));

			for (int i = 0; i < formatContents.size(); i++) {
				final String line = formatContents.get(i);

				if (Common.stripColors(line).startsWith("<center>"))
					formatContents.set(i, ChatUtil.center(line.replace("<center>", "")));
			}

			text = String.join("\n", formatContents);
		}

		this.pastComponents.add(this.currentComponent);

		this.currentComponent = new Part(colorize ? Common.colorize(text) : text);
		this.currentComponent.inheritFormatting = inherit;

		return this;
	}

	/**
	 * Append a new component on the end of this one
	 *
	 * @param component
	 * @return
	 */
	public SimpleComponent append(SimpleComponent component) {
		this.pastComponents.add(this.currentComponent);
		this.pastComponents.addAll(component.pastComponents);

		// Get the last extra
		TextComponent inherit = Common.getOrDefault(component.currentComponent.inheritFormatting, this.currentComponent.toTextComponent(false, null));

		if (inherit != null && !inherit.children().isEmpty())
			inherit = (TextComponent) inherit.children().get(inherit.children().size() - 1);

		this.currentComponent = component.currentComponent;
		this.currentComponent.inheritFormatting = inherit;

		return this;
	}

	/**
	 * Return the plain colorized message combining all components into one
	 * without click/hover events
	 *
	 * @return
	 */
	public String getPlainMessage() {
		return Remain.toLegacyText(this.build(null));
	}

	/**
	 * Builds the component and its past components into a {@link TextComponent}
	 *
	 * @return
	 */
	public TextComponent getTextComponent() {
		return this.build(null);
	}

	/**
	 * Builds the component and its past components into a {@link TextComponent}
	 *
	 * @param receiver
	 * @return
	 */
	public TextComponent build(Audience receiver) {
		final List<Component> children = new ArrayList<>();

		for (final Part part : this.pastComponents) {
			final TextComponent component = part.toTextComponent(true, receiver);

			if (component != null)
				children.add(component);
		}

		final TextComponent currentComponent = this.currentComponent.toTextComponent(true, receiver);

		if (currentComponent != null)
			children.add(currentComponent);

		return Component.textOfChildren(children.toArray(new Component[children.size()]));
	}

	/**
	 * Quickly replaces an object in all parts of this component
	 *
	 * @param variable the factual variable - you must supply brackets
	 * @param value
	 * @return
	 */
	public SimpleComponent replace(String variable, Object value) {
		final String serialized = SerializeUtil.serialize(value).toString();

		for (final Part part : this.pastComponents) {
			Valid.checkNotNull(part.text);

			part.text = part.text.replace(variable, serialized);
		}

		Valid.checkNotNull(this.currentComponent.text);
		this.currentComponent.text = this.currentComponent.text.replace(variable, serialized);

		return this;
	}

	// --------------------------------------------------------------------
	// Sending
	// --------------------------------------------------------------------

	/**
	 * Attempts to send the complete {@link SimpleComponent} to the given
	 * command senders. If they are players, we send them interactive elements.
	 * <p>
	 * If they are console, they receive a plain text message.
	 * @param receivers
	 * @param <T>
	 */
	public <T extends Audience> void send(T... receivers) {
		this.send(Arrays.asList(receivers));
	}

	/**
	 * Attempts to send the complete {@link SimpleComponent} to the given
	 * command senders. If they are players, we send them interactive elements.
	 * <p>
	 * If they are console, they receive a plain text message.
	 *
	 * @param <T>
	 * @param receivers
	 */
	public <T extends Audience> void send(Iterable<T> receivers) {
		this.sendAs(null, receivers);
	}

	/**
	 * Attempts to send the complete {@link SimpleComponent} to the given
	 * command senders. If they are players, we send them interactive elements.
	 * <p>
	 * If they are console, they receive a plain text message.
	 *
	 * We will also replace relation placeholders if the sender is set and is player.
	 *
	 * @param <T>
	 * @param sender
	 * @param receivers
	 */
	public <T extends Audience> void sendAs(Audience sender, Iterable<T> receivers) {
		for (final Audience receiver : receivers) {
			final TextComponent component = this.build(receiver);

			// Prevent clients being kicked out, so we just send plain message instead
			if (STRIP_OVERSIZED_COMPONENTS && Remain.toJson(component).length() + 1 >= Short.MAX_VALUE) {
				final String legacy = Remain.toLegacyText(component);

				if (legacy.length() + 1 >= Short.MAX_VALUE)
					Common.warning("JSON Message to " + receiver + " was too large and could not be sent: '" + legacy + "'");

				else {
					Common.warning("JSON Message to " + receiver + " was too large, removing interactive elements to avoid kick. Sending plain: '" + legacy + "'");

					receiver.sendMessage(Remain.toComponentLegacy(legacy));
				}

			} else
				receiver.sendMessage(component);
		}
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.serialize().toStringFormatted();
	}

	// --------------------------------------------------------------------
	// Serialize
	// --------------------------------------------------------------------

	/**
	 * @see org.mineacademy.bfo.model.ConfigSerializable#serialize()
	 */
	@Override
	public SerializedMap serialize() {
		final SerializedMap map = new SerializedMap();

		map.putIf("Current_Component", this.currentComponent);
		map.put("Past_Components", this.pastComponents);

		return map;
	}

	/**
	 * Create a {@link SimpleComponent} from the serialized map
	 *
	 * @param map
	 * @return
	 */
	public static SimpleComponent deserialize(SerializedMap map) {
		final SimpleComponent component = new SimpleComponent();

		component.currentComponent = map.get("Current_Component", Part.class);
		component.pastComponents.addAll(map.getList("Past_Components", Part.class));

		return component;
	}

	// --------------------------------------------------------------------
	// Static
	// --------------------------------------------------------------------

	/**
	 * Compile the message into components, creating a new PermissibleComponent
	 * each time the message has a new & color/formatting, preserving
	 * the last color
	 *
	 * @param message
	 * @param inheritFormatting
	 * @param viewPermission
	 * @return
	 */
	private static TextComponent[] toComponent(@NonNull String message, TextComponent inheritFormatting) {
		final List<TextComponent> components = new ArrayList<>();

		// Plot the previous formatting manually before the message to retain it
		if (inheritFormatting != null) {

			if (inheritFormatting.hasDecoration(TextDecoration.BOLD))
				message = CompChatColor.BOLD + message;

			if (inheritFormatting.hasDecoration(TextDecoration.ITALIC))
				message = CompChatColor.ITALIC + message;

			if (inheritFormatting.hasDecoration(TextDecoration.OBFUSCATED))
				message = CompChatColor.MAGIC + message;

			if (inheritFormatting.hasDecoration(TextDecoration.STRIKETHROUGH))
				message = CompChatColor.STRIKETHROUGH + message;

			if (inheritFormatting.hasDecoration(TextDecoration.UNDERLINED))
				message = CompChatColor.UNDERLINE + message;

			message = Common.colorize(inheritFormatting.color().asHexString()) + message;
		}

		StringBuilder builder = new StringBuilder();
		TextComponent component = Component.text("");

		for (int index = 0; index < message.length(); index++) {
			char letter = message.charAt(index);

			if (letter == CompChatColor.COLOR_CHAR) {
				if (++index >= message.length())
					break;

				letter = message.charAt(index);

				if (letter >= 'A' && letter <= 'Z')
					letter += 32;

				CompChatColor format;

				if (letter == 'x' && index + 12 < message.length()) {
					final StringBuilder hex = new StringBuilder("#");

					for (int j = 0; j < 6; j++)
						hex.append(message.charAt(index + 2 + (j * 2)));

					try {
						format = CompChatColor.of(hex.toString());

					} catch (NoSuchMethodError | IllegalArgumentException ex) {
						format = null;
					}

					index += 12;

				} else
					format = CompChatColor.getByChar(letter);

				if (format == null)
					continue;

				if (builder.length() > 0) {
					final TextComponent old = component;

					component = Component.textOfChildren(old);
					old.content(builder.toString());

					builder = new StringBuilder();
					components.add(old);
				}

				if (format == CompChatColor.BOLD)
					component.decorate(TextDecoration.BOLD);

				else if (format == CompChatColor.ITALIC)
					component.decorate(TextDecoration.ITALIC);

				else if (format == CompChatColor.UNDERLINE)
					component.decorate(TextDecoration.UNDERLINED);

				else if (format == CompChatColor.STRIKETHROUGH)
					component.decorate(TextDecoration.STRIKETHROUGH);

				else if (format == CompChatColor.MAGIC)
					component.decorate(TextDecoration.OBFUSCATED);

				else if (format == CompChatColor.RESET) {
					format = CompChatColor.WHITE;

					component = Component.text("");
					component.color(NamedTextColor.WHITE);

				} else {
					component = Component.text("");

					component.color(format.isHex() ? TextColor.fromHexString(format.getName()) : NamedTextColor.NAMES.value(format.getName()));
				}

				continue;
			}

			int pos = message.indexOf(' ', index);

			if (pos == -1)
				pos = message.length();

			// Web link handling
			if (URL_PATTERN.matcher(message).region(index, pos).find()) {

				if (builder.length() > 0) {
					final TextComponent old = component;
					component = Component.textOfChildren(old);

					old.content(builder.toString());

					builder = new StringBuilder();
					components.add(old);
				}

				final TextComponent old = component;
				component = Component.textOfChildren(old);

				final String urlString = message.substring(index, pos);
				component.content(urlString);
				component.clickEvent(ClickEvent.openUrl(urlString.startsWith("http") ? urlString : "http://" + urlString));
				components.add(component);

				index += pos - index - 1;
				component = old;

				continue;
			}

			builder.append(letter);
		}

		component.content(builder.toString());
		components.add(component);

		return components.toArray(new TextComponent[components.size()]);

	}

	/**
	 * Create a new interactive chat component
	 * You can then build upon your text to add interactive elements
	 *
	 * @return
	 */
	public static SimpleComponent empty() {
		return of(true, "");
	}

	/**
	 * Create a new interactive chat component
	 * You can then build upon your text to add interactive elements
	 *
	 * @param text
	 * @return
	 */
	public static SimpleComponent of(String text) {
		return of(true, text);
	}

	/**
	 * Create a new interactive chat component
	 * You can then build upon your text to add interactive elements
	 *
	 * @param colorize
	 * @param text
	 * @return
	 */
	public static SimpleComponent of(boolean colorize, String text) {
		return new SimpleComponent(colorize ? Common.colorize(text) : text);
	}

	// --------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------

	/**
	 * The part that is being created
	 */
	static final class Part implements ConfigSerializable {

		/**
		 * The text
		 */
		private String text;

		/**
		 * The view permission
		 */

		private String viewPermission;

		/**
		 * The view JS condition
		 */

		private String viewCondition;

		/**
		 * The hover event
		 */
		@SuppressWarnings("rawtypes")
		private HoverEvent hoverEvent;

		/**
		 * The click event
		 */

		private ClickEvent clickEvent;

		/**
		 * The insertion
		 */

		private String insertion;

		/**
		 * What component to inherit colors/decoration from?
		 */

		private TextComponent inheritFormatting;

		/*
		 * Create a new part
		 */
		private Part(String text) {
			Valid.checkNotNull(text, "Part text cannot be null");

			this.text = text;
		}

		/**
		 * @see org.mineacademy.bfo.model.ConfigSerializable#serialize()
		 */
		@Override
		public SerializedMap serialize() {
			final SerializedMap map = new SerializedMap();

			map.put("Text", this.text);
			map.putIf("View_Permission", this.viewPermission);
			map.putIf("View_Condition", this.viewCondition);
			map.putIf("Hover_Event", this.hoverEvent);
			map.putIf("Click_Event", this.clickEvent);
			map.putIf("Insertion", this.insertion);
			map.putIf("Inherit_Formatting", this.inheritFormatting);

			return map;
		}

		/**
		 * Create a Part from the given serializedMap
		 *
		 * @param map
		 * @return
		 */
		public static Part deserialize(SerializedMap map) {
			final Part part = new Part(map.getString("Text"));

			part.viewPermission = map.getString("View_Permission");
			part.viewCondition = map.getString("View_Condition");
			part.hoverEvent = map.get("Hover_Event", HoverEvent.class);
			part.clickEvent = map.get("Click_Event", ClickEvent.class);
			part.insertion = map.getString("Insertion");
			part.inheritFormatting = map.get("Inherit_Formatting", TextComponent.class);

			return part;
		}

		/**
		 * Turn this part of the components into a {@link TextComponent}
		 * for the given receiver
		 *
		 * @param checkForReceiver
		 * @param receiver
		 * @return
		 */
		private TextComponent toTextComponent(boolean checkForReceiver, Audience receiver) {
			if ((checkForReceiver && !this.canSendTo(receiver)) || this.isEmpty())
				return null;

			final TextComponent[] base = toComponent(this.text, this.inheritFormatting);

			for (final TextComponent part : base) {
				if (this.hoverEvent != null)
					part.hoverEvent(this.hoverEvent);

				if (this.clickEvent != null)
					part.clickEvent(this.clickEvent);

				if (this.insertion != null)
					part.insertion(this.insertion);
			}

			return Component.textOfChildren(base);
		}

		/*
		 * Return if we're dealing with an empty format
		 */
		private boolean isEmpty() {
			return this.text.isEmpty() && this.hoverEvent == null && this.clickEvent == null && this.insertion == null;
		}

		/*
		 * Can this component be shown to the given sender?
		 */
		private boolean canSendTo(Audience receiver) {

			if (this.viewPermission != null && !this.viewPermission.isEmpty() && (receiver == null || !PlayerUtil.hasPerm(receiver, this.viewPermission)))
				return false;

			if (this.viewCondition != null && !this.viewCondition.isEmpty()) {
				if (receiver == null)
					return false;

				final Object result = JavaScriptExecutor.run(Variables.replace(this.viewCondition, receiver), receiver);

				if (result != null) {
					Valid.checkBoolean(result instanceof Boolean, "View condition must return Boolean not " + (result == null ? "null" : result.getClass()) + " for component: " + this);

					if (!((boolean) result))
						return false;
				}
			}

			return true;
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return this.serialize().toStringFormatted();
		}
	}
}
