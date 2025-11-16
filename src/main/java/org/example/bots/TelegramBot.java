package org.example.bots;

import org.example.logic.BotLogic;
import org.example.logic.BotLogic.State;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Telegram-бот, реализующий логику приёма обновлений (updates) и отправки ответов
 * через библиотеку telegrambots. Оборачивает и использует {@link BotLogic} для
 * обработки пользовательских команд и управления состояниями диалога.
 * <p>Класс регистрирует себя в {@code TelegramBotsApi} при вызове {@link #start()}
 * и обрабатывает входящие обновления в {@link #onUpdateReceived(Update)}.</p>
 * <p>Экземпляр класса хранит внутренний объект {@link BotLogic} и предоставляет
 * удобные методы для настройки клавиатуры ответов.</p>
 */
public class TelegramBot extends TelegramLongPollingBot {


    private final BotLogic logicBot = new BotLogic();
/**
 * Инициализирует и регистрирует бота в Telegram API.
 * <p>Метод создаёт экземпляр {@link TelegramBotsApi} и регистрирует текущий бот. Для удержания
 * потока работы приложения используется {@link java.util.concurrent.CountDownLatch},
 * который ожидает бесконечно, пока не будет прерван.</p>
 * @throws Exception если регистрация бота в TelegramBotsApi не удалась
 */
    public void start() throws Exception {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(this);
        CountDownLatch latch = new CountDownLatch(1);
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    /**
     * Обрабатывает входящее обновление (update) от Telegram.
     * <p>Поведение:</p>
     * <ul>
     * <li>Проверяет, что {@code update}, {@code update.getMessage()} и
     * {@code update.getMessage().getText()} не равны {@code null}; в противном случае — ничего не делает.</li>
     * <li>Извлекает {@code chatId}, {@code userId} и текст сообщения.</li>
     * <li>Передаёт текст и идентификатор пользователя в {@link BotLogic#handleCommand(long, String)} и получает ответную строку.</li>
     * <li>Формирует {@link SendMessage} с ответом, вызывает {@link #setButtons(SendMessage, long)}
     * для прикрепления клавиатуры, затем пытается выполнить отправку через
     * {@link #execute(org.telegram.telegrambots.meta.api.methods.BotApiMethod)}.</li>
     * <li>Все исключения логируются через {@code e.printStackTrace()} — метод не пробрасывает исключений наружу.</li>
     * </ul>
     * @param update входящее обновление от Telegram (может быть {@code null}, в этом случае метод ничего не делает).
     */
    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update == null || update.getMessage() == null || update.getMessage().getText() == null) return;
            String chatId = String.valueOf(update.getMessage().getChatId());
            long userId = Long.parseLong(String.valueOf(update.getMessage().getFrom().getId()));
            String text = update.getMessage().getText();
            String response = logicBot.handleCommand(userId, text);
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(response);
            setButtons(message, userId);
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * Устанавливает ReplyKeyboardMarkup (кнопочную клавиатуру) для сообщения
     * {@code sendMessage} в зависимости от состояния пользователя.
     * <p>Поведение:</p>
     * <ul>
     * <li>Создаёт {@link ReplyKeyboardMarkup} с resize/ selective флагами.</li>
     * <li>Опрашивает текущее состояние пользователя через {@link BotLogic#getUserState(long)}.</li>
     * <li>В зависимости от состояния ({@code AWAITING_ACTION_ON_NOTE},
     * {@code AWAITING_NOTE_TEXT} или другое) формирует соответствующие строки
     * клавиатуры с кнопками из {@link BotLogic.ButtonLabels}.</li>
     * <li>Устанавливает сформированную клавиатуру в {@code sendMessage}.</li>
     * </ul>
     * <p>Метод объявлен {@code synchronized} для обеспечения потокобезопасного
     * доступа при параллельной обработке update'ов и корректного чтения/использования состояния пользователя.</p>
     * @param sendMessage объект сообщения, к которому будет прикреплена клавиатура; не должен быть {@code null}.
     * @param userId идентификатор пользователя (используется для получения состояния).
     */
    public synchronized void setButtons(SendMessage sendMessage, long userId) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        markup.setSelective(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        State userState = logicBot.getUserState(userId);

        if (userState == State.AWAITING_ACTION_ON_NOTE) {
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(BotLogic.ButtonLabels.DELETE_NOTE));
            row.add(new KeyboardButton(BotLogic.ButtonLabels.CANCEL));
            keyboard.add(row);
        } else if (userState == State.AWAITING_NOTE_TEXT) {
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(BotLogic.ButtonLabels.CANCEL));
            keyboard.add(row);
        } else {
            KeyboardRow row1 = new KeyboardRow();
            row1.add(new KeyboardButton(BotLogic.ButtonLabels.NEW_NOTE));
            row1.add(new KeyboardButton(BotLogic.ButtonLabels.NOTES_LIST));
            KeyboardRow row2 = new KeyboardRow();
            row2.add(new KeyboardButton(BotLogic.ButtonLabels.FILTER_BY_TAG));
            row2.add(new KeyboardButton(BotLogic.ButtonLabels.EDIT_NOTE));
            keyboard.add(row1);
            keyboard.add(row2);
        }

        markup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(markup);
    }
    /**
     * Возвращает имя пользователя бота, используемое при регистрации.
     */
    @Override
    public String getBotUsername() {
        return "JavaVoice";
    }
    /**
     * Возвращает токен пользователя бота.
     */
    @Override
    public String getBotToken() {
        return System.getProperty("TOKEN_TELEGRAM");
    }
}

