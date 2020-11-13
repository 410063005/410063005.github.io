package cn.edu.hust.cm.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.NinePatch;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        printChunk();

        System.out.println("isNinePatchChunk? "  + NinePatch.isNinePatchChunk(generateNinePatchChunk(new int[] {17, 55}, new int[] {6, 17})));
    }

    byte[] generateNinePatchChunk(int[] xRegions, int[] yRegions) {

        int NO_COLOR = 0x00000001;
        int colorSize = 9;
        int bufferSize = xRegions.length * 4 + yRegions.length * 4 + colorSize * 4 + 32;

        ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize).order(ByteOrder.nativeOrder());
        // 第一个byte，不为0
        byteBuffer.put((byte) 1);

        //mDivX length
        byteBuffer.put((byte) 2);
        //mDivY length
        byteBuffer.put((byte) 2);
        //mColors length
        byteBuffer.put((byte) colorSize);

        //skip
        byteBuffer.putInt(0);
        byteBuffer.putInt(0);

        //padding 设为0
        byteBuffer.putInt(0);
        byteBuffer.putInt(0);
        byteBuffer.putInt(0);
        byteBuffer.putInt(0);

        //skip
        byteBuffer.putInt(0);

        // mDivX
        byteBuffer.putInt(xRegions[0]);
        byteBuffer.putInt(xRegions[1]);

        // mDivY
        byteBuffer.putInt(yRegions[0]);
        byteBuffer.putInt(yRegions[1]);

        // mColors
        for (int i = 0; i < colorSize; i++) {
            byteBuffer.putInt(NO_COLOR);
        }

        return byteBuffer.array();
    }

    void printChunk() {
        byte[] chunk = getNinePatchChunk("search_bg.9.png");
//        byte[] chunk = getNinePatchChunk("btn_complete_pressed.9.png");
        System.out.println("isNinePatchChunk? " + NinePatch.isNinePatchChunk(chunk));

        if (chunk != null) {
            System.out.println("wasDeserialized " + chunk[0]);
            System.out.println("numXDivs " + chunk[1]);
            System.out.println("numYDivs " + chunk[2]);
            System.out.println("numColors  " + chunk[3]);
            System.out.println("*xDivsz " + TextUtils.join(",", get(chunk, 4, 4)));
            System.out.println("*yDivsz " + TextUtils.join(",", get(chunk, 8, 4)));
            System.out.println("paddingLeft " + TextUtils.join(",", get(chunk, 12, 4)));
            System.out.println("paddingRight " + TextUtils.join(",", get(chunk, 16, 4)));
            System.out.println("paddingTop " + TextUtils.join(",", get(chunk, 20, 4)));
            System.out.println("paddingBottom " + TextUtils.join(",", get(chunk, 24, 4)));
            System.out.println("*colors " + TextUtils.join(",", get(chunk, 28, 4)));

            // 第32个字节开始
            // 首先是xDivsz指针指向的内容，其大小为numXDivs个int32_t
            System.out.println("xDivsz " + TextUtils.join(",", get(chunk, 32, chunk[1] * 4)));
            // 然后是yDivs指针指向的内容，其大小为numYDivs个int32_t
            System.out.println("yDivsz " + TextUtils.join(",", get(chunk, 32 + chunk[1] * 4, chunk[2] * 4)));
            // 最后是colors指针指向的内容，其大小为numColors个int32_t（ARGB格式）
            System.out.println("colors " + TextUtils.join(",", get(chunk, 32 + chunk[1] * 4 + chunk[2] * 4, chunk[3] * 4)));
        }
    }

    byte[] getNinePatchChunk(String name) {
        InputStream in = null;
        try {
            in = getAssets().open(name);

            Bitmap bitmap = BitmapFactory.decodeStream(in);
            System.out.println("bitmap width " + bitmap.getWidth());
            System.out.println("bitmap height " + bitmap.getHeight());
            return bitmap.getNinePatchChunk();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    Byte[] get(byte[] chunk, int start, int offset) {
        Byte[] data = new Byte[offset];
        for (int i = 0; i < offset; i++) {
            data[i] = chunk[start + i];
        }
        return data;
    }


    boolean isNinePatchChunk(String name) {
        return NinePatch.isNinePatchChunk(getNinePatchChunk(name));
    }
}
