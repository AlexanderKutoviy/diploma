package attendance.daemon.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class MailMessage {
    public String from;
    public List<String> recipients = new ArrayList<>();
    public String subject;
    public String contentText;
    public String contentHtml;
    public HashMap<String, String> headers = new HashMap<>();
    public ArrayList<Attachment> attachments = new ArrayList<>();

    public class Attachment {
        public byte[] data;
        public String filename;
        public String cid;
        public String mimeType;
        /**
         * true: related recipients html
         * false: mixed
         */
        public boolean isRelated = false;
        public HashMap<String, String> headers = new HashMap<>();

        public String ensureCid() {
            if (cid == null) {
                Random r = new Random();
                for (int i = 0; i < 100; ++i) {
                    cid = Long.toHexString(r.nextLong()) + "@message";
                    if (!attachments.stream()
                            .filter(a -> a != this && cid.equals(a.cid))
                            .findAny()
                            .isPresent())
                        break;
                }
                if (cid == null)
                    throw new RuntimeException("Cannot generate cid");
            }
            return cid;
        }
    }

    public Attachment addAttachment() {
        Attachment res = new Attachment();
        attachments.add(res);
        return res;
    }

    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }

    public void addRecipient(String recipient) {
        this.recipients.add(recipient);
    }

    @Override
    public String toString() {
        return "MailMessage{" +
                "from='" + from + '\'' +
                ", recipients=" + recipients +
                ", subject='" + subject + '\'' +
                ", contentText='" + contentText + '\'' +
                ", contentHtml='" + contentHtml + '\'' +
                ", headers=" + headers +
                ", attachments=" + attachments +
                '}';
    }
}