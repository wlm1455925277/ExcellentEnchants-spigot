package su.nightexpress.excellentenchants.config;

import su.nightexpress.nightcore.locale.LangContainer;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;

import static su.nightexpress.excellentenchants.EnchantsPlaceholders.*;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

public class Lang implements LangContainer {

    public static final TextLocale COMMAND_ARGUMENT_NAME_LEVEL = LangEntry.builder("Command.Argument.Name.Level").text("等级");
    public static final TextLocale COMMAND_ARGUMENT_NAME_SLOT  = LangEntry.builder("Command.Argument.Name.Slot").text("槽位");

    public static final TextLocale COMMAND_LIST_DESC        = LangEntry.builder("Command.List.Desc").text("查看自定义附魔列表。");
    public static final TextLocale COMMAND_ENCHANT_DESC     = LangEntry.builder("Command.Enchant.Desc").text("为指定槽位的物品添加附魔。");
    public static final TextLocale COMMAND_DISENCHANT_DESC  = LangEntry.builder("Command.Disenchant.Desc").text("为指定槽位的物品移除附魔。");
    public static final TextLocale COMMAND_BOOK_DESC        = LangEntry.builder("Command.Book.Desc").text("给予一本带有指定附魔的附魔书。");
    public static final TextLocale COMMAND_RANDOM_BOOK_DESC = LangEntry.builder("Command.RandomBook.Desc").text("给予一本随机附魔的附魔书。");
    public static final TextLocale COMMAND_GIVE_FUEL_DESC   = LangEntry.builder("Command.GiveFuel.Desc").text("给予附魔充能燃料物品。");

    public static final MessageLocale COMMAND_LIST_DONE_OTHERS = LangEntry.builder("Command.List.DoneOthers").chatMessage(
            GRAY.wrap("已为 " + SOFT_YELLOW.wrap(PLAYER_NAME) + " 打开附魔 GUI。")
    );

    public static final MessageLocale COMMAND_ENCHANT_DONE_SELF = LangEntry.builder("Command.Enchant.Done.Self").chatMessage(
            GRAY.wrap("已为 " + SOFT_YELLOW.wrap(GENERIC_ITEM) + " 添加附魔 " + SOFT_YELLOW.wrap(GENERIC_ENCHANT + " " + GENERIC_LEVEL) + "！")
    );

    public static final MessageLocale COMMAND_ENCHANT_DONE_OTHERS = LangEntry.builder("Command.Enchant.Done.Others").chatMessage(
            GRAY.wrap("已为 " + SOFT_YELLOW.wrap(PLAYER_DISPLAY_NAME) + " 的 " + SOFT_YELLOW.wrap(GENERIC_ITEM) + " 添加附魔 " + SOFT_YELLOW.wrap(GENERIC_ENCHANT + " " + GENERIC_LEVEL) + "！")
    );

    public static final MessageLocale COMMAND_DISENCHANT_DONE_SELF = LangEntry.builder("Command.Disenchant.Done.Self").chatMessage(
            GRAY.wrap("已从 " + SOFT_YELLOW.wrap(GENERIC_ITEM) + " 移除附魔 " + SOFT_YELLOW.wrap(GENERIC_ENCHANT) + "！")
    );

    public static final MessageLocale COMMAND_DISENCHANT_DONE_OTHERS = LangEntry.builder("Command.Disenchant.Done.Others").chatMessage(
            GRAY.wrap("已从 " + SOFT_YELLOW.wrap(PLAYER_DISPLAY_NAME) + " 的 " + SOFT_YELLOW.wrap(GENERIC_ITEM) + " 移除附魔 " + SOFT_YELLOW.wrap(GENERIC_ENCHANT) + "！")
    );

    public static final MessageLocale COMMAND_ENCHANT_ERROR_NO_ITEM = LangEntry.builder("Command.Enchant.Error.NoItem").chatMessage(
            SOFT_RED.wrap("没有可供附魔的物品！")
    );

    public static final MessageLocale ENCHANTED_BOOK_GAVE = LangEntry.builder("Command.Book.Done").chatMessage(
            GRAY.wrap("你已将 " + SOFT_YELLOW.wrap(GENERIC_ENCHANT + " " + GENERIC_LEVEL) + " 附魔书给予 " + SOFT_YELLOW.wrap(PLAYER_DISPLAY_NAME) + "。")
    );

    public static final MessageLocale CHARGES_FUEL_GAVE = LangEntry.builder("Charges.Fuel.Gave").chatMessage(
            GRAY.wrap("你已将 " + SOFT_YELLOW.wrap("x" + GENERIC_AMOUNT + " " + GENERIC_NAME) + " 作为燃料给予 " + SOFT_YELLOW.wrap(PLAYER_NAME) + "。")
    );

    public static final MessageLocale CHARGES_FUEL_BAD_ENCHANTMENT = LangEntry.builder("Charges.Fuel.BadEnchantment").chatMessage(
            GRAY.wrap("附魔 " + SOFT_RED.wrap(GENERIC_NAME) + " 无法进行充能。")
    );

    public static final MessageLocale COMMAND_SYNTAX_INVALID_SLOT = LangEntry.builder("Command.Syntax.InvalidSlot").chatMessage(
            GRAY.wrap(SOFT_RED.wrap(GENERIC_INPUT) + " 不是有效的槽位！")
    );
}
