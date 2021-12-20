package me.croabeast.sircore.objects;

import me.croabeast.sircore.*;
import me.croabeast.sircore.utilities.*;
import net.md_5.bungee.api.chat.*;
import org.bukkit.entity.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static net.md_5.bungee.api.chat.HoverEvent.Action.*;
import static net.md_5.bungee.api.chat.ClickEvent.Action.*;

@SuppressWarnings("deprecation")
public class Message {

    private final TextUtils text;

    private final Player player;
    private final String format;

    BaseComponent[] hoverText = null;
    String suggest = null;
    String execute = null;
    String openURL = null;

    public Message(Application main, Player player, @Nullable String format) {
        this.text = main.getTextUtils();
        this.player = player;
        this.format = format;
    }

    @Nullable
    public String getFormat() { return this.format; }

    public Message setHover(@Nullable List<String> list) {
        if (list == null) return this;
        hoverText = new BaseComponent[list.size()];

        for (int i = 0; i < list.size(); i++) {
            String line = text.parse(player, list.get(i)) + (i == list.size() - 1 ? "" : "\n");
            hoverText[i] = new TextComponent(TextComponent.fromLegacyText(line));
        }
        return this;
    }

    public Message setSuggestion(@Nullable String suggest) {
        this.suggest = suggest;
        return this;
    }

    public Message setExecutor(@Nullable String execute) {
        this.execute = execute;
        return this;
    }

    public Message setURL(@Nullable String openURL) {
        this.openURL = openURL;
        return this;
    }

    @Nullable
    public TextComponent getBuilder() {
        if (getFormat() == null) return null;
        TextComponent component = new TextComponent(TextComponent.fromLegacyText(getFormat()));

        if (hoverText != null) component.setHoverEvent(new HoverEvent(SHOW_TEXT, this.hoverText));
        if (execute != null) component.setClickEvent(new ClickEvent(RUN_COMMAND, this.execute));
        if (suggest != null) component.setClickEvent(new ClickEvent(SUGGEST_COMMAND, this.suggest));
        if (openURL != null) component.setClickEvent(new ClickEvent(OPEN_URL, this.openURL));

        return component;
    }
}
