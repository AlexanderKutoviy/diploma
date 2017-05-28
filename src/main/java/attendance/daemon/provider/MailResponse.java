package attendance.daemon.provider;

public class MailResponse {

    public String messageId;
    public String responseBody;
    public String messageBody;
    public String state;
    public String error;

    @Override
    public String toString() {
        return "MailResponse{" +
                "messageId='" + messageId + '\'' +
                ", responseBody='" + responseBody + '\'' +
                ", messageBody='" + messageBody + '\'' +
                ", state='" + state + '\'' +
                ", error='" + error + '\'' +
                '}';
    }
}