public class RegistrationResult {
    private boolean success;
    private String message;

    public RegistrationResult(boolean success, String message){
        this.message = message;
        this.success = success;
    }

    public boolean isSuccess(){
        return success;
    }

    public String getMessage(){
        return message;
    }
}
