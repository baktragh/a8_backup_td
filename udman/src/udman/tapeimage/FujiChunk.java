package udman.tapeimage;

import java.io.InputStream;


/**
 *
 * @author  
 */
public class FujiChunk implements TapeImageChunk {

    private static final String TYPE = "FUJI";
    /**
     * Get default FUJI chunk
     *
     * @return Default FUJI chunk
     */
    public static FujiChunk getDefaultFujiChunk() {
        FujiChunk fc = new FujiChunk();
        fc.length = 0;
        fc.data = new int[0];
        fc.aux = 0;

        return fc;
    }
    private int length;
    private int[] data;
    private int aux;

    /**
     *
     * @return
     */
    public int getAux() {
        return aux;
    }

    @Override
    public String getType() {
        return TYPE;
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
     * @param s
     * @throws Exception
     */
    @Override
    public void readFromStream(InputStream s) throws Exception {

        /*Read length and AUX*/
        int lengthLo = s.read();
        int lengthHi = s.read();
        int auxLo = s.read();
        int auxHi = s.read();

        if (lengthLo == -1 || lengthHi == -1 || auxLo == -1 || auxHi == -1) {
            throw new FileFormatException("Truncated FUJI chunk header");
        }

        length = lengthLo + (256 * lengthHi);
        aux = auxLo + (256 * auxHi);

        /*Read data*/
        data = new int[length];
        for (int i = 0; i < length; i++) {
            int b = s.read();
            if (b == -1) {
                throw new FileFormatException("Truncated FUJI chunk data");
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
        s.writeBytes("FUJI");
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
        char[] chars = new char[data.length];
        for(int i=0;i<data.length;i++) {
            chars[i]=(char)data[i];
        }
        return String.format("FUJI (%05d) [%s]",length,new String(chars));
    }

    @Override
    public TapeImageChunk getParent() {
        return null;
    }

    /**
     *
     * @param newAuxValue
     */
    @Override
    public void setAux(int newAuxValue) {
        /*Do nothing*/
    }

}
