package udman.tapeimage;

import java.io.*;
import java.util.*;


/**
 *
 * @author
 */
public class TapeImage {

    private final ArrayList<TapeImageChunk> chunkList;

    public TapeImage() {
        chunkList = new ArrayList<>();

    }

    /**
     *
     * @param filespec
     * @throws Exception
     */
    public void parse(String filespec) throws Exception {

        char[] chunkTypeChars = new char[4];
        int[] chunkTypeInts = new int[4];

        try (FileInputStream fis = new FileInputStream(filespec);
                BufferedInputStream bis = new BufferedInputStream(fis);) {

            boolean isFujiRead = false;
            TapeImageChunk lastBaudOrTrChunk = null;
            TapeImageChunk lastPWMSChunk = null;

            READLOOP:
            while (true) {

                /*Read next chunk while checking for EOF*/
                for (int i = 0; i < 4; i++) {

                    chunkTypeInts[i] = bis.read();

                    if (chunkTypeInts[i] == -1) {
                        if (i == 0) {
                            break READLOOP;
                        }
                        else {
                            throw new FileFormatException("Trailing bytes in tape image");
                        }
                    }
                }

                /*Determine chunk type*/
                for (int i = 0; i < 4; i++) {
                    chunkTypeChars[i] = (char) chunkTypeInts[i];
                }
                String chunkType = new String(chunkTypeChars);


                /*Create new instance of appropriate chunks*/

 /*FUJI chunk*/
                if (chunkType.equals("FUJI")) {
                    FujiChunk fujiChunk = new FujiChunk();
                    fujiChunk.readFromStream(bis);
                    chunkList.add(fujiChunk);
                    isFujiRead = true;
                    continue;
                }

                /*Other chunks only after FUJI chunk*/
                if (isFujiRead == false) {
                    throw new FileFormatException("FUJI chunk not found in tape image");
                }

                /*Baud chunk*/
                if (chunkType.equals("baud")) {
                    BaudChunk baudChunk = new BaudChunk();
                    baudChunk.readFromStream(bis);
                    chunkList.add(baudChunk);
                    lastBaudOrTrChunk = baudChunk;

                }

                else if (chunkType.equals("fsk ")) {
                    FSKChunk fskChunk = new FSKChunk();
                    fskChunk.readFromStream(bis);
                    chunkList.add(fskChunk);

                }

                /*Data chunk*/
                else if (chunkType.equals("data")) {

                    TapeImageChunk parent;

                    /*If there is no preceding chunk, then we must create virtual parent*/
                    if (lastBaudOrTrChunk == null) {
                        parent = new BaudChunk(600);
                    }
                    else {
                        parent = lastBaudOrTrChunk;
                    }

                    DataChunk dataChunk = new DataChunk(parent);
                    dataChunk.readFromStream(bis);
                    chunkList.add(dataChunk);
                }

                else if (chunkType.equals("pwms")) {
                    PWMChunk pwmChunk = new PWMChunk(chunkType, null);
                    pwmChunk.readFromStream(bis);
                    lastPWMSChunk = pwmChunk;
                    chunkList.add(pwmChunk);

                }

                else if (chunkType.equals("pwmc") || chunkType.equals("pwmd") || chunkType.equals("pwml")) {

                    TapeImageChunk parent = lastPWMSChunk;

                    if (parent == null) {
                        parent = PWMChunk.createDummyPWMS(44_100);
                    }

                    PWMChunk pwmChunk = new PWMChunk(chunkType, parent);
                    pwmChunk.readFromStream(bis);
                    chunkList.add(pwmChunk);
                }
                else {


                    /*Other chunks - they must be considered independent chunks*/
                    UnknownChunk unknownChunk = new UnknownChunk(chunkType);
                    unknownChunk.readFromStream(bis);
                    chunkList.add(unknownChunk);
                    lastBaudOrTrChunk = unknownChunk;
                }

            }
        }
    }

    /**
     *
     * @return
     */
    public int getChunkCount() {
        return chunkList.size();
    }

    /**
     *
     * @param index
     * @return
     */
    public TapeImageChunk getChunkAt(int index) {
        return chunkList.get(index);
    }

    /**
     *
     * @return
     */
    public String[] getListing() {

        int l = getChunkCount();
        String[] listing = new String[l];

        for (int i = 0; i < l; i++) {
            listing[i] = getChunkAt(i).toString();
        }

        return listing;
    }
}
