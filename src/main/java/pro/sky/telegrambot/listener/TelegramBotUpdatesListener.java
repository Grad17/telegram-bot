package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.MessageUser;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    @Autowired
    private TelegramBot telegramBot;

    private NotificationTaskRepository notificationTaskRepository;

    public TelegramBotUpdatesListener(NotificationTaskRepository notificationTaskRepository){
        this.notificationTaskRepository = notificationTaskRepository;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            String messageText = update.message().text();
            Long chatId = update.message().chat().id();
            if (messageText.equals(MessageUser.start)){
                SendMessage message = new SendMessage(chatId, "Привет!");
                SendResponse response = telegramBot.execute(message);
            }
            Pattern pattern = Pattern.compile("([0-9\\.\\:\\s]{16})(\\s)([\\W+]+)");
            Matcher matcher = pattern.matcher(messageText);
            if (matcher.matches()) {
                String date = matcher.group(1);
                String notification = matcher.group(3);
                LocalDateTime dateTime = LocalDateTime.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                NotificationTask notificationTask = new NotificationTask(chatId, notification, dateTime);
                notificationTaskRepository.save(notificationTask);
                SendMessage message = new SendMessage(chatId, "Запись добавлена!");
                SendResponse response = telegramBot.execute(message);
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }
    @Scheduled(cron = "0 0/1 * * * *")
    public void run(){
        List<NotificationTask> notificationTask = notificationTaskRepository.findNotificationTaskByDateTime(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
        if (notificationTask != null){
            for (int i = 0; i < notificationTask.size(); i++) {
                NotificationTask notification = notificationTask.get(i);
                SendMessage message = new SendMessage(notification.getChatId(), notification.getNotification());
                telegramBot.execute(message);
            }
        }
    }
}
