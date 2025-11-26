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

import org.example.entity.Platform;
import org.example.logic.BotLogic;
import org.example.scheduler.NotificationSender;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.concurrent.CountDownLatch;

/**
 * Простой Discord-бот на JDA, использующий {@link org.example.logic.BotLogic}
 * для обработки сообщений и slash-команд.
 */
public class DiscordBot extends ListenerAdapter implements NotificationSender {
    private final String token;
    private final BotLogic logicBot = new BotLogic();
    private JDA jda;

    /**
     * Конструктор класса {@code DiscordBot}.
     * Извлекает токен бота из системного свойства {@code TOKEN_DISCORD}.
     * Если токен не задан, методы запуска бота выбросят исключение.
     */
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
        jda = JDABuilder.createDefault(token, intents)
                .disableCache(CacheFlag.VOICE_STATE, CacheFlag.SCHEDULED_EVENTS, CacheFlag.EMOJI, CacheFlag.STICKER)
                .addEventListeners(this)
                .build();
        jda.updateCommands().addCommands(
                Commands.slash("start", "Start the bot"),
                Commands.slash("new_note", "Создание заметки"),
                Commands.slash("all_note", "Показать все ваши заметки"),
                Commands.slash("edit_note", "Изменение заметки"),
                Commands.slash("filter_tag", "Фильтрация по вашим тегам"),
                Commands.slash("new_reminder", "Создать напоминание"),
                Commands.slash("my_reminders", "Показать мои напоминания")
        ).queue();
        jda.awaitReady();
        CountDownLatch latch = new CountDownLatch(1);
        latch.await();
    }

    /**
     * Обрабатывает входящее текстовое сообщение.
     * Игнорирует сообщения от ботов и пустые сообщения, вызывает {@link org.example.logic.BotLogic#handleCommand(long, String,Platform)}
     * и отправляет ответ в тот же канал.
     * @param event событие получения сообщения
     */
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        try {
            event.getMessage();
            String content = event.getMessage().getContentRaw();

            event.getAuthor();
            if (event.getAuthor().isBot()) return;

            String userIdStr = event.getAuthor().getId();
            long userIdLong = 0;
            try { userIdLong = Long.parseLong(userIdStr); } catch (Exception ignored) {}

            MessageChannel channel = event.getChannel();

            String response;
            response = logicBot.handleCommand(userIdLong, content, Platform.DISCORD);

            BotLogic.State userState = logicBot.getUserState(userIdLong);
            StringBuilder sb = new StringBuilder();

            switch (userState) {
                case AWAITING_LOGIN:
                    sb.append("- Введите ваш логин\n");
                    sb.append("- ").append(BotLogic.ButtonLabels.CANCEL).append("\n");
                    break;
                case AWAITING_NOTE_TEXT:
                    sb.append("- Введите текст заметки\n");
                    sb.append("- ").append(BotLogic.ButtonLabels.CANCEL).append("\n");
                    break;
                case AWAITING_REMINDER_TEXT:
                    sb.append("- Введите текст напоминания\n");
                    sb.append("- ").append(BotLogic.ButtonLabels.CANCEL).append("\n");
                    break;
                case AWAITING_REMINDER_TIME, AWAITING_REMINDER_TIME_FROM_NOTE:
                    sb.append("- Введите дату и время (ДД.ММ.ГГГГ ЧЧ:ММ)\n");
                    sb.append("- ").append(BotLogic.ButtonLabels.CANCEL).append("\n");
                    break;
                case AWAITING_REMINDER_ID_FOR_ACTION:
                    sb.append("- Введите номер напоминания для действия (изменить/удалить)\n");
                    sb.append("- ").append(BotLogic.ButtonLabels.CANCEL).append("\n");
                    break;
                case AWAITING_REMINDER_EDIT_ACTION:
                    sb.append("- Выберите действие: 'Изменить текст', 'Изменить дату/время', 'Удалить'\n");
                    sb.append("- ").append(BotLogic.ButtonLabels.CANCEL).append("\n");
                    break;
                case AWAITING_REMINDER_EDIT_TIME:
                    sb.append("- Введите новое время (ДД.ММ.ГГГГ ЧЧ:ММ)\n");
                    sb.append("- ").append(BotLogic.ButtonLabels.CANCEL).append("\n");
                    break;
                case AWAITING_REMINDER_EDIT_TEXT:
                    sb.append("- Введите новый текст напоминания\n");
                    sb.append("- ").append(BotLogic.ButtonLabels.CANCEL).append("\n");
                    break;
                case AWAITING_REMINDER_DELETE_CONFIRMATION:
                    sb.append("- Ответьте 'да' или 'нет'\n");
                    sb.append("- ").append(BotLogic.ButtonLabels.CANCEL).append("\n");
                    break;
                case AWAITING_ACTION_ON_NOTE:
                    sb.append("- ").append(BotLogic.ButtonLabels.DELETE_NOTE).append("\n");
                    sb.append("- ").append(BotLogic.ButtonLabels.CONVERT_TO_REMINDER).append("\n");
                    sb.append("- ").append(BotLogic.ButtonLabels.CANCEL).append("\n");
                    break;
                case AWAITING_NOTE_ID_FOR_EDIT:
                    sb.append("- Введите номер заметки для редактирования\n");
                    sb.append("- ").append(BotLogic.ButtonLabels.CANCEL).append("\n");
                    break;
                case AWAITING_NEW_TEXT_FOR_EDIT:
                    sb.append("- Введите новый текст заметки\n");
                    sb.append("- ").append(BotLogic.ButtonLabels.CANCEL).append("\n");
                    break;
                case AWAITING_DELETE_CONFIRMATION:
                    sb.append("- Ответьте 'да' или 'нет'\n");
                    sb.append("- ").append(BotLogic.ButtonLabels.CANCEL).append("\n");
                    break;
                case AWAITING_TAG_FOR_FILTER:
                    sb.append("- Выберите тег из предложенного списка или 'Все заметки'\n");
                    sb.append("- ").append(BotLogic.ButtonLabels.CANCEL).append("\n");
                    break;
                case AWAITING_NOTE_ID_FOR_TAG_EDIT:
                    sb.append("- Введите номер заметки для редактирования тегов\n");
                    sb.append("- ").append(BotLogic.ButtonLabels.CANCEL).append("\n");
                    break;
                case AWAITING_NEW_TAGS_INPUT:
                    sb.append("- Введите новые теги или оставьте пустым для удаления\n");
                    sb.append("- ").append(BotLogic.ButtonLabels.CANCEL).append("\n");
                    break;
                case AWAITING_NOTE_ID_FOR_REMINDER:
                    sb.append("- Введите номер заметки для преобразования\n");
                    sb.append("- ").append(BotLogic.ButtonLabels.CANCEL).append("\n");
                    break;
                case AWAITING_NOTE_ID_FOR_DELETE:
                    sb.append("- Введите номер заметки для удаления\n");
                    sb.append("- ").append(BotLogic.ButtonLabels.CANCEL).append("\n");
                    break;
                default:
                    sb.append("- ").append(BotLogic.ButtonLabels.NEW_NOTE).append("\n");
                    sb.append("- ").append(BotLogic.ButtonLabels.NOTES_LIST).append("\n");
                    sb.append("- ").append(BotLogic.ButtonLabels.FILTER_BY_TAG).append("\n");
                    sb.append("- ").append(BotLogic.ButtonLabels.EDIT_NOTE).append("\n");
                    sb.append("- ").append(BotLogic.ButtonLabels.NEW_REMINDER).append("\n");
                    sb.append("- ").append(BotLogic.ButtonLabels.MY_REMINDERS).append("\n");
                    sb.append("- ").append(BotLogic.ButtonLabels.CONVERT_TO_REMINDER).append("\n");
                    sb.append("- ").append(BotLogic.ButtonLabels.CANCEL).append("\n");
                    break;
            }

            String toSend = response + "\n\n" + sb;

            channel.sendMessage(toSend).queue();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * Обрабатывает slash-команду.
     * Передаёт имя команды в {@link org.example.logic.BotLogic#handleCommand(long, String,Platform)}
     * и отвечает пользователю.
     * @param event событие slash-команды
     */
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        new Thread(() -> {
            try {
                String userIdStr = event.getUser().getId();
                long userIdLong = 0;
                try { userIdLong = Long.parseLong(userIdStr); } catch (Exception ignored) {}

                String response;
                response = logicBot.handleCommand(userIdLong, "/" + event.getName(), Platform.DISCORD);

                event.getHook().sendMessage(response).queue();

            } catch (Exception e) {
                e.printStackTrace();
                try {
                    event.getHook().sendMessage("Ошибка: " + e.getMessage()).queue();
                } catch (Exception ignored) {
                }
            }
        }).start();
    }
    @Override
    public void sendDiscordNotification(Long userId, String message) {
        if (jda == null) {
            System.err.println("[DiscordBot] JDA не инициализирован, невозможно отправить сообщение пользователю: " + userId);
            return;
        }
        if (userId == null) {
            System.err.println("[DiscordBot] ID пользователя null, невозможно отправить сообщение.");
            return;
        }

        System.out.println("[DiscordBot] Попытка отправить DM пользователю ID: " + userId);

        jda.retrieveUserById(userId).queue(
                user -> {
                    System.out.println("[DiscordBot] Найден пользователь: " + user.getName() + " (" + user.getId() + ")");
                    user.openPrivateChannel().queue(
                            channel -> {
                                System.out.println("[DiscordBot] Открыт DM канал для пользователя " + user.getName() + ", отправляем сообщение...");
                                channel.sendMessage(message).queue(
                                        msg -> System.out.println("[DiscordBot] Сообщение успешно отправлено в DM пользователю " + user.getName()),
                                        failure -> System.err.println("[DiscordBot] Ошибка отправки сообщения в DM пользователю " + user.getName() + ": " + failure.getMessage())
                                );
                            },
                            channelFailure -> System.err.println("[DiscordBot] Ошибка открытия DM канала для пользователя " + user.getName() + ": " + channelFailure.getMessage())
                    );
                },
                userFailure -> System.err.println("[DiscordBot] Ошибка поиска пользователя по ID " + userId + ": " + userFailure.getMessage())
        );
    }

    @Override
    public void sendTelegramNotification(Long userId, String message) {
        throw new UnsupportedOperationException("DiscordBot не может отправлять уведомления в Telegram.");
    }

    /**
     * Отправляет личное сообщение пользователю в Discord.
     *
     * @param userId идентификатор пользователя в Discord
     * @param message текст сообщения
     */
    public void sendPrivateMessage(Long userId, String message) {
        if (jda == null) {
            System.err.println("[DiscordBot] JDA не инициализирован, невозможно отправить сообщение пользователю: " + userId);
            return;
        }
        if (userId == null) {
            System.err.println("[DiscordBot] ID пользователя null, невозможно отправить сообщение.");
            return;
        }

        System.out.println("[DiscordBot] Попытка отправить DM пользователю ID: " + userId);

        jda.retrieveUserById(userId).queue(
                user -> {
                    System.out.println("[DiscordBot] Найден пользователь: " + user.getName() + " (" + user.getId() + ")");
                    user.openPrivateChannel().queue(
                            channel -> {
                                System.out.println("[DiscordBot] Открыт DM канал для пользователя " + user.getName() + ", отправляем сообщение...");
                                channel.sendMessage(message).queue(
                                        msg -> System.out.println("[DiscordBot] Сообщение успешно отправлено в DM пользователю " + user.getName()),
                                        failure -> System.err.println("[DiscordBot] Ошибка отправки сообщения в DM пользователю " + user.getName() + ": " + failure.getMessage())
                                );
                            },
                            channelFailure -> System.err.println("[DiscordBot] Ошибка открытия DM канала для пользователя " + user.getName() + ": " + channelFailure.getMessage())
                    );
                },
                userFailure -> System.err.println("[DiscordBot] Ошибка поиска пользователя по ID " + userId + ": " + userFailure.getMessage())
        );
    }
}