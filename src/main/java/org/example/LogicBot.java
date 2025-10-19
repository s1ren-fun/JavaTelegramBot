package org.example;

/**
 * Класс основной логики работы бота.
 * <p>Отвечает за обработку входящих сообщений от пользователя и формирование ответа в зависимости от введённой команды.</p>
 * <p>Поддерживаемые команды:</p>
 * <ul>
 *    <li><b>/start</b> — приветственное сообщение.</li>
 *    <li><b>/help</b> — выводит справку о возможностях бота.</li>
 *    <li><i>любое другое сообщение</i> — бот повторяет введённый текст.</li>
 * </ul>
 * @since 1.0
 */
public class LogicBot implements Logic {
    /**
     *  Обрабатывает входящее сообщение и возвращает ответ.
     *  @param text входной текст, полученный от пользователя
     *  @return сформированный ответ бота
     */
    @Override
    public String handleCommand(String text){
        String response;

        switch (text){
            case "/start":
            case "/help":
                response = "Привет! Я твой Java-бот 🤖. Я готов повторять за тобой";
                break;
            default:
                response = "Ты написал: " + text;
        }
        return response;
    }
}
