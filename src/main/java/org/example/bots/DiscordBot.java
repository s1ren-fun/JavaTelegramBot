package org.example.bots;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import org.example.logic.BotLogic;

import java.util.EnumSet;
import java.util.concurrent.CountDownLatch;
/**
 * Простой Discord-бот на JDA, использующий {@link org.example.logic.BotLogic}
 * для обработки сообщений и slash-команд.
 */
public class DiscordBot extends ListenerAdapter {
    private final String token;
    private final BotLogic logicBot = new BotLogic();

    public DiscordBot() {
        this.token =  System.getProperty("TOKEN_DISCORD");
    }

    /**
     * Запускает JDA и регистрирует slash-команды.
     * Проверяет токен, настраивает intents, строит клиент, ждёт готовности и
     * блокирует поток, чтобы процесс не завершился.
     * @throws Exception при ошибках инициализации JDA
     * @throws IllegalStateException если токен не задан
     */
    public void start() throws Exception {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Discord token не задан.");
        }
        EnumSet<GatewayIntent> intents = EnumSet.of(
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.DIRECT_MESSAGES
        );
        JDA jda = JDABuilder.createDefault(token, intents)
                .disableCache(CacheFlag.VOICE_STATE, CacheFlag.SCHEDULED_EVENTS, CacheFlag.EMOJI, CacheFlag.STICKER)
                .addEventListeners(this)
                .build();
        jda.updateCommands().addCommands(
                Commands.slash("start", "Start the bot"),
                Commands.slash("new_note", "Создание заметка"),
                Commands.slash("all_note", "Показать все ваши заметки"),
                Commands.slash("edit_note", "Изменение заметки"),
                Commands.slash("filter_tag", "Фильтрация по вашим тегам")
        ).queue();
        jda.awaitReady();
        CountDownLatch latch = new CountDownLatch(1);
        latch.await();
    }

    /**
     * Обрабатывает входящее текстовое сообщение.
     * Игнорирует сообщения от ботов и пустые сообщения, вызывает {@link org.example.logic.BotLogic#handleCommand(long, String)}
     * и отправляет ответ в тот же канал.
     * @param event событие получения сообщения
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        try {
            String content = event.getMessage() != null ? event.getMessage().getContentRaw() : null;

            if (event.getAuthor() == null || event.getAuthor().isBot()) return;
            if (content == null) return;

            String userIdStr = event.getAuthor().getId();
            long userIdLong = 0;
            try { userIdLong = Long.parseLong(userIdStr); } catch (Exception ignored) {}

            MessageChannel channel = event.getChannel();

            String response;
            response = logicBot.handleCommand(userIdLong, content);

            BotLogic.State userState = logicBot.getUserState(userIdLong);
            StringBuilder sb = new StringBuilder();
            if (userState == BotLogic.State.AWAITING_ACTION_ON_NOTE) {
                sb.append("- ").append(BotLogic.ButtonLabels.DELETE_NOTE).append("\n");
                sb.append("- ").append(BotLogic.ButtonLabels.CANCEL).append("\n");
            } else if (userState == BotLogic.State.AWAITING_NOTE_TEXT) {
                sb.append("- ").append(BotLogic.ButtonLabels.CANCEL).append("\n");
            } else {
                sb.append("- ").append(BotLogic.ButtonLabels.NEW_NOTE).append("\n");
                sb.append("- ").append(BotLogic.ButtonLabels.NOTES_LIST).append("\n");
                sb.append("- ").append(BotLogic.ButtonLabels.FILTER_BY_TAG).append("\n");
                sb.append("- ").append(BotLogic.ButtonLabels.EDIT_NOTE).append("\n");
            }

            String toSend = response + "\n\n" + sb;

            channel.sendMessage(toSend).queue();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * Обрабатывает slash-команду.
     * Передаёт имя команды в {@link org.example.logic.BotLogic#handleCommand(long, String)}
     * и отвечает пользователю.
     * @param event событие slash-команды
     */
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        try {
            String userIdStr = event.getUser().getId();
            long userIdLong = 0;
            try { userIdLong = Long.parseLong(userIdStr); } catch (Exception ignored) {}

            String response;
            response = logicBot.handleCommand(userIdLong, "/" + event.getName());


            event.reply(response).setEphemeral(false).queue();

        } catch (Exception e) {
            e.printStackTrace();
            try { event.reply("Ошибка: " + e.getMessage()).setEphemeral(true).queue(); }
            catch (Exception ignored) {}
        }
    }
}
