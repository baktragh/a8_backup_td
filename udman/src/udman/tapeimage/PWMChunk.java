package udman.tapeimage;

import java.io.InputStream;


/**
 *
 * @author  
 */
public class PWMChunk implements TapeImageChunk {

    /**
     *
     * @param sampleRate
     * @return
     */
    public static PWMChunk createDummyPWMS(int sampleRate) {
        PWMChunk c = new PWMChunk("pwms", null);
        c.length = 0x02;
        c.auxLo = 0x05;
        c.auxHi = 0x00;
        c.aux = 0x05;
        c.data = new int[]{sampleRate % 256, sampleRate / 256};
        return c;
    }

    /**
     * Chunk type
     */
    private final String type;
    /**
     * Chunk length
     */
    private int length;
    /**
     * Chunk data
     */
    private int[] data;
    /**
     * Parent chunk
     */
    private final TapeImageChunk parent;
    /**
     * Aux bytes
     */
    private int auxLo;
    private int auxHi;
    private int aux;

    /**
     *
     * @param type
     * @param parent
     */
    public PWMChunk(String type, TapeImageChunk parent) {

        this.type = type;
        this.parent = parent;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public int[] getData() {
        return data;
    }

    @Override
    public boolean isGeneretedUsingParent() {
        return false;
    }

    /**
     *
     * @return
     */
    public int getAux() {
        return aux;
    }

    /**
     *
     * @return
     */
    public int getAuxLo() {
        return auxLo;
    }

    /**
     *
     * @return
     */
    public int getAuxHi() {
        return auxHi;
    }

    /**
     *
     * @param s
     * @throws Exception
     */
    @Override
    public void readFromStream(InputStream s) throws Exception {

        /*Read length and baud rate*/
        int lengthLo = s.read();
        int lengthHi = s.read();
        auxLo = s.read();
        auxHi = s.read();

        if (lengthLo == -1 || lengthHi == -1 || auxLo == -1 || auxHi == -1) {
            throw new FileFormatException("Truncated pwmX chunk header");
        }
        length = lengthLo + (256 * lengthHi);
        aux = auxLo + (256 * auxHi);

        /*Read data*/
        data = new int[length];
        for (int i = 0; i < length; i++) {
            int b = s.read();
            if (b == -1) {
                throw new FileFormatException("Truncated pwmX chunk data");
            }
            data[i] = b;
        }
    }

    /**
     *
     * @param s
     * @throws Exception
     */
    @Override
    public void writeToStream(java.io.DataOutputStream s) throws Exception {
        s.writeBytes(type);
        s.write(length % 256);
        s.write(length / 256);
        s.write(aux % 256);
        s.write(aux / 256);
        for (int i = 0; i < data.length; i++) {
            s.write(data[i]);
        }
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        String head = String.format("%s (%05d)",type,length);
        return head +" ["+getPWMDetailString() +"]" ;
    }
    
    public String getPWMDetailString() {
        switch (type) {
            case "pwms": return toStringPWMS();
            case "pwmd": return toStringPWMD();
            case "pwml": return toStringPWML();
            case "pwmc": return toStringPWMC();
        }
        return "";
        
    }
    
    
    private String toStringPWMS() {
        
        String miniInfo="";
        
        if ((auxLo & 0x03) == 0x02) miniInfo="HL";
        if ((auxLo & 0x03) == 0x01) miniInfo="LH";
        
        if ((auxLo & 0x04) == 0x04) {
            miniInfo=miniInfo+" ML";
        }
        else {
            miniInfo=miniInfo+" LM";
        }
        return String.format("$%02X,$%02X,%05d Hz (%s)",auxLo,auxHi,(data[0]+256*data[1]),miniInfo);
        
    }
    private String toStringPWMD() {
        return String.format("0:%03d,1:%03d",auxLo,auxHi);
    }
    
    private String toStringPWML() {
        return String.format("IRG:%05d",aux);
    }
    private String toStringPWMC() {
        return String.format("IRG:%05d",aux);
    }

    @Override
    public TapeImageChunk getParent() {
        return parent;
    }

    /**
     *
     * @param newAuxValue
     */
    @Override
    public void setAux(int newAuxValue) {

    }


}
