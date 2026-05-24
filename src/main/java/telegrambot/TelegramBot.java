package telegrambot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.*;
import java.util.*;

public class TelegramBot extends TelegramLongPollingBot {

    private static final String TOKEN = "8618505453:AAG9_QxcCwEXm5U-ZS2fsfrvGUSswrwnUgU";
    private static final String BOT_USERNAME = "ideal_kafel_bot";
    private static final long ADMIN_ID = 8017207855L;
    private static final String MAHSULOTLAR_FILE = "mahsulotlar.json";

    private Map<Long, String> adminHolat = new HashMap<>();

    @Override
    public String getBotToken() { return TOKEN; }

    @Override
    public String getBotUsername() { return BOT_USERNAME; }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            callbackQayta(chatId, data);
            return;
        }

        if (update.hasMessage() && update.getMessage().hasText()) {
            String matn = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String ism = update.getMessage().getFrom().getFirstName();

            // Admin rejimi
            if (adminHolat.containsKey(chatId)) {
                adminInputQayta(chatId, matn);
                return;
            }

            switch (matn) {
                case "/start":
                    boshSahifa(chatId, ism);
                    break;
                case "/admin":
                    if (chatId == ADMIN_ID) {
                        adminPanel(chatId);
                    } else {
                        xabarYubor(chatId, "❌ Siz admin emassiz!");
                    }
                    break;
                default:
                    boshSahifa(chatId, ism);
                    break;
            }
        }
    }

    private void callbackQayta(long chatId, String data) {
        switch (data) {
            case "mahsulotlar":
                mahsulotlarniKorsat(chatId);
                break;
            case "narxlar":
                narxlarniKorsat(chatId);
                break;
            case "boglanish":
                xabarYuborInline(chatId,
                    "📞 *Bog'lanish:*\n\n" +
                    "📱 Telefon: +998 90 123 45 67\n" +
                    "📍 Manzil: Jizzax shahri\n" +
                    "🕐 Ish vaqti: 9:00 - 18:00",
                    ortugaQayt());
                break;
            case "haqida":
                xabarYuborInline(chatId,
                    "ℹ️ *Ideal Kafel haqida:*\n\n" +
                    "🏆 10 yildan ortiq tajriba\n" +
                    "✅ Sifatli mahsulotlar\n" +
                    "🚚 Yetkazib berish xizmati\n" +
                    "💯 Kafolat beramiz",
                    ortugaQayt());
                break;
            case "bosh":
                boshSahifa(chatId, "");
                break;
            case "admin_qosh":
                if (chatId == ADMIN_ID) {
                    adminHolat.put(chatId, "nom_kutilmoqda");
                    xabarYubor(chatId, "📦 Yangi mahsulot nomi yozing:");
                }
                break;
            case "admin_kor":
                if (chatId == ADMIN_ID) {
                    adminMahsulotlar(chatId);
                }
                break;
            case "admin_panel":
                if (chatId == ADMIN_ID) {
                    adminPanel(chatId);
                }
                break;
        }

        // O'chirish
        if (data.startsWith("ochir_") && chatId == ADMIN_ID) {
            int index = Integer.parseInt(data.replace("ochir_", ""));
            mahsulotOchir(chatId, index);
        }
    }

    private void adminInputQayta(long chatId, String matn) {
        String holat = adminHolat.get(chatId);

        if (holat.equals("nom_kutilmoqda")) {
            adminHolat.put(chatId, "narx_kutilmoqda:" + matn);
            xabarYubor(chatId, "💰 Narxini yozing (so'mda):");
        } else if (holat.startsWith("narx_kutilmoqda:")) {
            String nom = holat.replace("narx_kutilmoqda:", "");
            adminHolat.put(chatId, "emoji_kutilmoqda:" + nom + ":" + matn);
            xabarYubor(chatId, "🎨 Emoji tanlang (masalan: 🪨 🧱 💎):");
        } else if (holat.startsWith("emoji_kutilmoqda:")) {
            String[] parts = holat.replace("emoji_kutilmoqda:", "").split(":");
            String nom = parts[0];
            String narx = parts[1];
            String emoji = matn;

            mahsulotQosh(nom, narx, emoji);
            adminHolat.remove(chatId);

            xabarYuborInline(chatId,
                "✅ *Mahsulot qo'shildi!*\n\n" +
                emoji + " " + nom + "\n💰 " + narx + " so'm/kv.m",
                adminTugmalar());
        }
    }

    private void boshSahifa(long chatId, String ism) {
        String salom = ism.isEmpty() ? "" : "Salom " + ism + "! 👋\n\n";
        xabarYuborInline(chatId,
            salom +
            "🏪 *Ideal Kafel* ga xush kelibsiz!\n\n" +
            "🪨 Sifatli kafellar eng yaxshi narxlarda!\n\n" +
            "Quyidagi bo'limlardan birini tanlang:",
            asosiyTugmalar());
    }

    private void mahsulotlarniKorsat(long chatId) {
        JSONArray mahsulotlar = mahsulotlarniOl();
        if (mahsulotlar.isEmpty()) {
            xabarYuborInline(chatId, "📦 Hozircha mahsulot yo'q!", ortugaQayt());
            return;
        }

        StringBuilder sb = new StringBuilder("🛒 *Mahsulotlar:*\n\n");
        for (Object obj : mahsulotlar) {
            JSONObject m = (JSONObject) obj;
            sb.append(m.get("emoji")).append(" *")
              .append(m.get("nom")).append("*\n")
              .append("💰 ").append(m.get("narx")).append(" so'm/kv.m\n\n");
        }
        xabarYuborInline(chatId, sb.toString(), ortugaQayt());
    }

    private void narxlarniKorsat(long chatId) {
        mahsulotlarniKorsat(chatId);
    }

    private void adminPanel(long chatId) {
        xabarYuborInline(chatId,
            "⚙️ *Admin panel*\n\nNimani qilmoqchisiz?",
            adminTugmalar());
    }

    private void adminMahsulotlar(long chatId) {
        JSONArray mahsulotlar = mahsulotlarniOl();
        if (mahsulotlar.isEmpty()) {
            xabarYuborInline(chatId, "📦 Hozircha mahsulot yo'q!", adminTugmalar());
            return;
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (int i = 0; i < mahsulotlar.size(); i++) {
            JSONObject m = (JSONObject) mahsulotlar.get(i);
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(tugma(m.get("emoji") + " " + m.get("nom") + " — o'chirish ❌", "ochir_" + i));
            rows.add(row);
        }

        List<InlineKeyboardButton> orqaRow = new ArrayList<>();
        orqaRow.add(tugma("⬅️ Orqaga", "admin_panel"));
        rows.add(orqaRow);

        markup.setKeyboard(rows);
        xabarYuborInline(chatId, "📦 *Mahsulotlar ro'yxati:*\nO'chirish uchun bosing:", markup);
    }

    private void mahsulotOchir(long chatId, int index) {
        JSONArray mahsulotlar = mahsulotlarniOl();
        if (index >= 0 && index < mahsulotlar.size()) {
            JSONObject o = (JSONObject) mahsulotlar.get(index);
            mahsulotlar.remove(index);
            mahsulotlarniSaqla(mahsulotlar);
            xabarYuborInline(chatId,
                "✅ *" + o.get("nom") + "* o'chirildi!",
                adminTugmalar());
        }
    }

    @SuppressWarnings("unchecked")
    private void mahsulotQosh(String nom, String narx, String emoji) {
        JSONArray mahsulotlar = mahsulotlarniOl();
        JSONObject yangi = new JSONObject();
        yangi.put("nom", nom);
        yangi.put("narx", narx);
        yangi.put("emoji", emoji);
        mahsulotlar.add(yangi);
        mahsulotlarniSaqla(mahsulotlar);
    }

    private JSONArray mahsulotlarniOl() {
        try {
            File file = new File(MAHSULOTLAR_FILE);
            if (!file.exists()) return new JSONArray();
            JSONParser parser = new JSONParser();
            return (JSONArray) parser.parse(new FileReader(file));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private void mahsulotlarniSaqla(JSONArray mahsulotlar) {
        try (FileWriter fw = new FileWriter(MAHSULOTLAR_FILE)) {
            fw.write(mahsulotlar.toJSONString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Tugmalar
    private InlineKeyboardMarkup asosiyTugmalar() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(tugma("🛒 Mahsulotlar", "mahsulotlar"));
        row1.add(tugma("💰 Narxlar", "narxlar"));
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(tugma("📞 Bog'lanish", "boglanish"));
        row2.add(tugma("ℹ️ Haqida", "haqida"));
        rows.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(tugmaUrl("🌐 Veb-sayt", "https://youtube000999-cpu.github.io/Ideal-Kafel/"));
        rows.add(row3);

        markup.setKeyboard(rows);
        return markup;
    }

    private InlineKeyboardMarkup adminTugmalar() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(tugma("➕ Mahsulot qo'shish", "admin_qosh"));
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(tugma("📋 Mahsulotlar", "admin_kor"));
        rows.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(tugma("🏠 Bosh sahifa", "bosh"));
        rows.add(row3);

        markup.setKeyboard(rows);
        return markup;
    }

    private InlineKeyboardMarkup ortugaQayt() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(tugma("⬅️ Orqaga", "bosh"));
        rows.add(row);
        markup.setKeyboard(rows);
        return markup;
    }

    private InlineKeyboardButton tugma(String matn, String data) {
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText(matn);
        btn.setCallbackData(data);
        return btn;
    }

    private InlineKeyboardButton tugmaUrl(String matn, String url) {
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText(matn);
        btn.setUrl(url);
        return btn;
    }

    private void xabarYubor(long chatId, String matn) {
        SendMessage xabar = new SendMessage();
        xabar.setChatId(String.valueOf(chatId));
        xabar.setText(matn);
        xabar.setParseMode("Markdown");
        try { execute(xabar); } catch (TelegramApiException e) { e.printStackTrace(); }
    }

    private void xabarYuborInline(long chatId, String matn, InlineKeyboardMarkup markup) {
        SendMessage xabar = new SendMessage();
        xabar.setChatId(String.valueOf(chatId));
        xabar.setText(matn);
        xabar.setParseMode("Markdown");
        xabar.setReplyMarkup(markup);
        try { execute(xabar); } catch (TelegramApiException e) { e.printStackTrace(); }
    }

    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new TelegramBot());
            System.out.println("Bot ishga tushdi! ✅");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
