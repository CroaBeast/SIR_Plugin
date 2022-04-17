package me.croabeast.sirplugin.objects;

import com.loohp.interactivechat.api.*;
import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.utilities.*;
import net.md_5.bungee.api.chat.*;
import org.bukkit.entity.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.regex.*;

import static me.croabeast.iridiumapi.IridiumAPI.*;
import static me.croabeast.sirplugin.utilities.TextUtils.*;
import static net.md_5.bungee.api.chat.ClickEvent.Action.*;

@SuppressWarnings("deprecation")
public class JsonMsg {

    /**
     * The player to parse placeholders.
     */
    private final Player player;
    /**
     * The line to initialize.
     */
    private final String line;

    /**
     * The result chat components.
     */
    private BaseComponent[] baseComponents;

    /**
     * A prefix used in the main pattern to identify the event.
     */
    final static String EVENT_ID = "(hover|run|suggest|url)=\\[(.+?)]";

    /**
     * The main pattern to identify the JSON message.
     */
    final static Pattern PATTERN =
            Pattern.compile("(?i)<" + EVENT_ID + "(\\|" + EVENT_ID + ")?>(.+?)</text>");

    /**
     * Basic JSON message constructor.
     * @param player the player for parse placeholders.
     * @param line the line to parse the message.
     */
    public JsonMsg(@NotNull Player player, String line) {
        line = parseInteractiveChat(player, line);
        this.player = player;
        this.line = centeredText(player, line);
        registerComponents();
    }

    /**
     * Custom JSON message constructor.
     * You can define a hover and click inputs instead using the json format.
     * @param player the player for parse placeholders.
     * @param line the line to parse the message.
     * @param click the click input line for click event
     * @param hover the hover list for the hover event
     */
    public JsonMsg(@NotNull Player player, String line, @Nullable String click, List<String> hover) {
        if (isValidJson(line)) line = stripJson(line);
        line = parseInteractiveChat(player, line);

        this.player = player;
        this.line = centeredText(player, line);

        List<BaseComponent> components = new ArrayList<>();
        final TextComponent comp = toComponent(this.line);

        if (!hover.isEmpty()) addHover(comp, hover);
        if (click != null) {
            String[] input = click.split(":", 2);
            addClick(comp, input[0], input[1]);
        }

        components.add(comp);
        baseComponents = components.toArray(new BaseComponent[0]);
    }

    /**
     * Parse InteractiveChat placeholders for using it the Json message.
     * @param player the requested player
     * @param line the line to parse
     * @return the line with the parsed placeholders.
     */
    private String parseInteractiveChat(Player player, String line) {
        if (Initializer.hasIntChat())
            try {
                return InteractiveChatAPI.markSender(line, player.getUniqueId());
            }
            catch (Exception e) {
                LogUtils.doLog(
                        "&cError parsing InteractiveChat placeholders.",
                        "&eUpdate to the latest version of InteractiveChat."
                );
                return line;
            }
        else return line;
    }

    /**
     * Check if the line has a valid json format.
     * @param line the input line to check.
     * @return if the line is a valid json message.
     */
    public static boolean isValidJson(String line) {
        return PATTERN.matcher(line).find();
    }

    /**
     * Strips the JSON format from a line.
     * @param line the line to strip.
     * @return the stripped line.
     */
    public static String stripJson(String line) {
        return line.replaceAll("(?i)<[/]?(text|hover|run|suggest|url)" +
                "(=\\[(.+?)](\\|(hover|run|suggest|url)=\\[(.+?)])?)?>", "");
    }

    /**
     * Converts a string to a TextComponent.
     * @param line the line to convert.
     * @return the requested component.
     */
    private TextComponent toComponent(String line) {
        return new TextComponent(TextComponent.fromLegacyText(line));
    }

    /**
     * Add a click event to a component.
     * @param comp the component to add the event
     * @param type the click event type
     * @param input the input line for the click event
     */
    private void addClick(TextComponent comp, String type, String input) {
        ClickEvent.Action action = null;
        if (type.matches("(?i)run")) action = RUN_COMMAND;
        else if (type.matches("(?i)suggest")) action = SUGGEST_COMMAND;
        else if (type.matches("(?i)url")) action = OPEN_URL;
        if (action != null) comp.setClickEvent(new ClickEvent(action, input));
    }

    /**
     *
     * @param comp the component to add the event
     * @param hover the list to add as a hover
     */
    private void addHover(TextComponent comp, List<String> hover) {
        BaseComponent[] array = new BaseComponent[hover.size()];
        List<String> list = new ArrayList<>(hover);

        for (int i = 0; i < list.size(); i++) {
            String end = i == list.size() - 1 ? "" : "\n";
            array[i] = toComponent(colorize(player, list.get(i)) + end);
        }

        comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, array));
    }

    /**
     * Add the event found in the formatted line.
     * @param comp the component to add the event.
     * @param type the event's type.
     * @param input the input line for the event.
     */
    private void addEvent(TextComponent comp, String type, String input) {
        if (type.matches("(?i)run|suggest|url")) addClick(comp, type, input);
        else if (type.matches("(?i)hover"))
            addHover(comp, Arrays.asList(input.split(lineSplitter())));
    }

    /**
     * It registers all the components of the formatted line in the basic constructor.
     */
    private void registerComponents() {
        List<BaseComponent> components = new ArrayList<>();
        final Matcher match = PATTERN.matcher(line);
        int lastEnd = 0;

        while (match.find()) {
            final String type = match.group(1);
            final String input = match.group(2);

            final String extra = match.group(3);
            final String type2 = match.group(4);
            final String input2 = match.group(5);

            boolean isExtra = extra != null && extra.matches("(?i)\\|" + EVENT_ID);

            final String text = match.group(6);
            final String before = line.substring(lastEnd, match.start());

            components.addAll(Arrays.asList(TextComponent.fromLegacyText(before)));
            final TextComponent comp = toComponent(text);

            addEvent(comp, type, input);
            if (isExtra) addEvent(comp, type2, input2);

            components.add(comp);
            lastEnd = match.end();
        }

        if (lastEnd < (line.length() - 1)) {
            final String after = line.substring(lastEnd);
            components.addAll(Arrays.asList(TextComponent.fromLegacyText(after)));
        }

        baseComponents = components.toArray(new BaseComponent[0]);
    }

    /**
     * Gets the formatted component with all the added events.
     * @return the formatted component.
     */
    public BaseComponent[] build() {
        return baseComponents;
    }

    /**
     * Creates a centered chat message.
     * @param player a player to parse placeholders.
     * @param message the input message.
     * @return the centered chat message.
     */
    public static String centerMessage(@Nullable Player player, String message) {
        String initial = colorize(player, stripJson(message));

        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;

        for (char c : initial.toCharArray()) {
            if (c == '§') previousCode = true;
            else if (previousCode) {
                previousCode = false;
                isBold = c == 'l' || c == 'L';
            } else {
                FontInfo dFI = FontInfo.getDefaultFontInfo(c);
                messagePxSize += isBold ?
                        dFI.getBoldLength() : dFI.getLength();
                messagePxSize++;
            }
        }

        int halvedMessageSize = messagePxSize / 2;
        int toCompensate = 154 - halvedMessageSize;
        int spaceLength = FontInfo.SPACE.getLength() + 1;
        int compensated = 0;

        StringBuilder sb = new StringBuilder();
        while (compensated < toCompensate) {
            sb.append(" ");
            compensated += spaceLength;
        }

        return sb + colorize(player, message);
    }

    /**
     * Defines a string if is centered or not.
     * @param player a player to parse placeholders.
     * @param line the input line.
     * @return the result string.
     */
    public static String centeredText(@Nullable Player player, String line) {
        return line.startsWith(centerPrefix()) ?
                centerMessage(player, line.replace(centerPrefix(), "")) :
                colorize(player, line);
    }

    /**
     * The enum class to manage the length of every char.
     */
    public enum FontInfo {
        A('A', 5),
        a('a', 5),
        B('B', 5),
        b('b', 5),
        C('C', 5),
        c('c', 5),
        D('D', 5),
        d('d', 5),
        E('E', 5),
        e('e', 5),
        F('F', 5),
        f('f', 4),
        G('G', 5),
        g('g', 5),
        H('H', 5),
        h('h', 5),
        I('I', 3),
        i('i', 1),
        J('J', 5),
        j('j', 5),
        K('K', 5),
        k('k', 4),
        L('L', 5),
        l('l', 1),
        M('M', 5),
        m('m', 5),
        N('N', 5),
        n('n', 5),
        O('O', 5),
        o('o', 5),
        P('P', 5),
        p('p', 5),
        Q('Q', 5),
        q('q', 5),
        R('R', 5),
        r('r', 5),
        S('S', 5),
        s('s', 5),
        T('T', 5),
        t('t', 4),
        U('U', 5),
        u('u', 5),
        V('V', 5),
        v('v', 5),
        W('W', 5),
        w('w', 5),
        X('X', 5),
        x('x', 5),
        Y('Y', 5),
        y('y', 5),
        Z('Z', 5),
        z('z', 5),

        NUM_1('1', 5),
        NUM_2('2', 5),
        NUM_3('3', 5),
        NUM_4('4', 5),
        NUM_5('5', 5),
        NUM_6('6', 5),
        NUM_7('7', 5),
        NUM_8('8', 5),
        NUM_9('9', 5),
        NUM_0('0', 5),

        EXCLAMATION_POINT('!', 1),
        AT_SYMBOL('@', 6),
        NUM_SIGN('#', 5),
        DOLLAR_SIGN('$', 5),
        PERCENT('%', 5),
        UP_ARROW('^', 5),
        AMPERSAND('&', 5),
        ASTERISK('*', 5),

        LEFT_PARENTHESIS('(', 4),
        RIGHT_PARENTHESIS(')', 4),
        MINUS('-', 5),
        UNDERSCORE('_', 5),
        PLUS_SIGN('+', 5),
        EQUALS_SIGN('=', 5),
        LEFT_CURL_BRACE('{', 4),
        RIGHT_CURL_BRACE('}', 4),
        LEFT_BRACKET('[', 3),
        RIGHT_BRACKET(']', 3),

        COLON(':', 1),
        SEMI_COLON(';', 1),
        DOUBLE_QUOTE('"', 3),
        SINGLE_QUOTE('\'', 1),
        LEFT_ARROW('<', 4),
        RIGHT_ARROW('>', 4),
        QUESTION_MARK('?', 5),
        SLASH('/', 5),
        BACK_SLASH('\\', 5),
        LINE('|', 1),
        TILDE('~', 5),
        TICK('`', 2),
        PERIOD('.', 1),
        COMMA(',', 1),
        SPACE(' ', 3),
        DEFAULT('a', 4);

        private final char character;
        private final int length;

        FontInfo(char character, int length) {
            this.character = character;
            this.length = length;
        }

        private char getCharacter() {
            return character;
        }

        public int getLength() {
            return length;
        }

        public int getBoldLength() {
            if (this == SPACE) return getLength();
            return this.length + 1;
        }

        public static FontInfo getDefaultFontInfo(char c) {
            for (FontInfo dFI : values())
                if (dFI.getCharacter() == c) return dFI;
            return DEFAULT;
        }
    }
}
