package org.example.bots;

import org.example.logic.BotLogic;
import org.example.logic.BotLogic.State;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Discord-бот, реализующий ту же, что и Telegram-версия.
 * <p>
 * - Обрабатывает текстовые сообщения (пользователь вводит команды/тексты).
 * - Отправляет сообщения с набором кнопок (components) в зависимости от состояния пользователя.
 * - Обрабатывает нажатия на кнопки и делегирует всё в BotLogic.
 * </p>
 */
public class DiscordBot extends ListenerAdapter {
    private final BotLogic logicBot = new BotLogic();
    public void start() throws Exception {
        String token = System.getProperty("DiscordToken");
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("DiscordToken system property is not set. Запустите JVM с -DDiscordToken=YOUR_TOKEN");
        }
        System.out.println("Starting Discord bot...");
        net.dv8tion.jda.api.JDA jda = JDABuilder.createDefault(
                        token,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.DIRECT_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT
                )
                .setActivity(Activity.playing("with notes"))
                .addEventListeners(this)
                .build();
        jda.awaitReady();
        System.out.println("Discord bot ready.");
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down Discord bot...");
            try {
                jda.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        }));

        latch.await();
        System.out.println("Discord bot stopped.");
    }

    /**
     * Обрабатываем входящее текстовое сообщение от пользователя (в канале или DM).
     */
    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        // игнорируем сообщения от ботов (включая самого себя)
        if (event.getAuthor().isBot()) return;
        String text = event.getMessage().getContentRaw();
        long userId = event.getAuthor().getIdLong();
        String response = logicBot.handleCommand(userId, text == null ? "" : text);
        List<ActionRow> rows = buildActionRowsForUser(userId);
        if (rows.isEmpty()) {
            event.getChannel().sendMessage(response).queue();
        } else {
            event.getChannel().sendMessage(response).setActionRows(rows).queue();
        }
    }

    /**
     * Обрабатываем нажатия на кнопки (Button Interactions).
     * При нажатии мы вызываем BotLogic.handleCommand(userId, label).
     */
    @Override
    public void onButtonInteraction(@Nonnull ButtonInteractionEvent event) {
        User user = event.getUser();
        long userId = user.getIdLong();
        String componentId = event.getComponentId(); // мы используем componentId == label для простоты
        String response = logicBot.handleCommand(userId, componentId);
        event.reply(response).setActionRows(buildActionRowsForUser(userId)).queue();
    }

    /**
     * Строит список ActionRow (строк кнопок) в зависимости от состояния пользователя,
     * Мы используем Button.primary/secondary и устанавливаем componentId равным
     * тексу кнопки (чтобы BotLogic получал одинаковые метки).
     */
    private List<ActionRow> buildActionRowsForUser(long userId) {
        State userState = logicBot.getUserState(userId);
        List<ActionRow> rows = new ArrayList<>();

        if (userState == State.AWAITING_ACTION_ON_NOTE) {
            Button delete = Button.primary(BotLogic.ButtonLabels.DELETE_NOTE, BotLogic.ButtonLabels.DELETE_NOTE);
            Button editTags = Button.primary(BotLogic.ButtonLabels.EDIT_TAGS, BotLogic.ButtonLabels.EDIT_TAGS);
            Button editText = Button.primary("EDIT_TEXT", "Изменить текст"); // локальная метка
            Button cancel = Button.danger(BotLogic.ButtonLabels.CANCEL, BotLogic.ButtonLabels.CANCEL);

            rows.add(ActionRow.of(delete, editTags));
            rows.add(ActionRow.of(editText, cancel));
        } else if (userState == State.AWAITING_TAG_FOR_FILTER) {
            Button cancel = Button.danger(BotLogic.ButtonLabels.CANCEL, BotLogic.ButtonLabels.CANCEL);
            rows.add(ActionRow.of(cancel));
        } else if (userState == State.AWAITING_NOTE_TEXT) {
            Button cancel = Button.danger(BotLogic.ButtonLabels.CANCEL, BotLogic.ButtonLabels.CANCEL);
            rows.add(ActionRow.of(cancel));
        } else {
            Button newNote = Button.primary(BotLogic.ButtonLabels.NEW_NOTE, BotLogic.ButtonLabels.NEW_NOTE);
            Button notesList = Button.primary(BotLogic.ButtonLabels.NOTES_LIST, BotLogic.ButtonLabels.NOTES_LIST);
            Button filterByTag = Button.primary(BotLogic.ButtonLabels.FILTER_BY_TAG, BotLogic.ButtonLabels.FILTER_BY_TAG);
            Button editNote = Button.primary(BotLogic.ButtonLabels.EDIT_NOTE, BotLogic.ButtonLabels.EDIT_NOTE);

            rows.add(ActionRow.of(newNote, notesList));
            rows.add(ActionRow.of(filterByTag, editNote));
        }

        return rows;
    }
}

