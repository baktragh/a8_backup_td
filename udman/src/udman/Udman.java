package udman;

import java.io.IOException;
import javax.swing.SwingUtilities;

public class Udman {
    
    private static UtilityDisk ud;

    public static void main(String[] args) throws Exception {
        
        ud=null;
        
        if (args.length>0) {
            try {
                ud = new UtilityDisk(args[0]);
            }
            catch (IOException ioe) {
                
            }
        }
        else {
            ud=null;
        }

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                UdManFrame f = new UdManFrame();
                if (ud!=null) f.setDisk(ud);
                f.setVisible(true);
            }
        });

    }

}
