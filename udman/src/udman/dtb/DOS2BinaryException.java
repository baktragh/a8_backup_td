package udman.dtb;

import java.io.File;

/**
 * Incorrect DOS 2 Binary format
 */
public class DOS2BinaryException extends Exception {

    private final String message;
    private final int offset;
    private final String filename;

    public DOS2BinaryException(String filename, String message, int offset) {
        this.message = message;
        this.offset = offset;
        File f = new File(filename);
        this.filename = f.getName();
    }

    @Override
    public String getMessage() {
        return getMessageString();
    }

    @Override
    public String toString() {
        return getClass().getName() + " " + getMessageString();
    }

    private String getMessageString() {
        StringBuilder sb = new StringBuilder();
        sb.append(filename!=null?filename:"<No file>");
        sb.append(": ");
        sb.append(message);
        if (!message.endsWith(".")) {
            sb.append('.');
        }
        sb.append(" Offset: ");
        sb.append(String.format("%05d",offset));
        sb.append(" [0x");
        sb.append(String.format("%04X",offset));
        sb.append(']');
        return sb.toString();
    }

    public String getFormattedMessage() {
        return getFormattedMessage(null);
    }

    public String getFormattedMessage(String extraHeader) {
        StringBuilder sb = new StringBuilder();
        sb.append("<HTML>");

        if (extraHeader != null) {
            sb.append("<b>");
            sb.append(extraHeader);
            sb.append("</b><BR>");
        }
        
        sb.append(getClass().getName());
        sb.append(": ");
        sb.append(filename!=null?filename:"<No file>");
        sb.append("<BR>");
        sb.append(message);
        if (!message.endsWith(".")) {
            sb.append('.');
        }
        sb.append("<BR>");
        sb.append(" Offset: ");
        sb.append(String.format("%05d",offset));
        sb.append(" [0x");
        sb.append(String.format("%04X",offset));
        sb.append(']');
        sb.append("</HTML>");
        return sb.toString();
    }
}
