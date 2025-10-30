package com.example.demo.TelegramBot;

import com.example.demo.TelegramBot.TelegramBotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
@Slf4j
public class TelegramBotConfig {

    @Value("${telegram.bot.token:}")
    private String botToken;

    @Value("${telegram.bot.username:}")
    private String botUsername;

    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramBotService telegramBotService) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

            if (botToken != null && !botToken.isEmpty() &&
                    botUsername != null && !botUsername.isEmpty()) {
                botsApi.registerBot(telegramBotService);
                log.info("Telegram bot успешно зарегистрирован: {}", botUsername);
            } else {
                log.warn("Telegram bot токен или username не указаны. Бот не будет запущен.");
            }

            return botsApi;
        }catch (TelegramApiException e){
            log.error("Ошибка при регистрации Telegram бота", e);
            throw new RuntimeException("Не удалось зарегистрировать Telegram бота", e);
        }
    }
}