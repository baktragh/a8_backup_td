
package udman.tapeimage;

public class FileFormatException extends Exception {

    private final String message;
    
    public FileFormatException(String message) {
        this.message=message;
    }
    
    @Override
    public String getMessage() {
        return message;
    }

}
