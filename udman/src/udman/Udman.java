package udman;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
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
        
        UdManFrame f = new UdManFrame();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                
                if (ud!=null) f.setDisk(ud);
                f.pack();
                centerContainer(f);
                f.setVisible(true);
                
            }
        });
        
        

    }
    
    public static void centerContainer(Container c) {
        Dimension containerDimension = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (containerDimension.width - c.getBounds().width) / 2;
        int y = (containerDimension.height - c.getBounds().height) / 2;
        c.setLocation(x, y);
    }

}
