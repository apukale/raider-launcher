package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class BoxBlurTest {
    @Test(timeout = 5000) public void uniformImageStaysUniform() {
        int[] pixels = new int[35]; java.util.Arrays.fill(pixels, 0xFF445566);
        BoxBlur.blur(pixels, 7, 5, 2, 3);
        for (int pixel : pixels) assertEquals(0xFF445566, pixel);
    }
    @Test(timeout = 5000) public void brightPixelSpreadsAndBrightnessIsSimilar() {
        int[] pixels = new int[121]; pixels[60] = 0xFFFFFFFF;
        BoxBlur.blur(pixels, 11, 11, 2, 3);
        int sum=0, nonzero=0; for(int pixel:pixels){int light=pixel&255;sum+=light;if(light>0)nonzero++;}
        assertTrue(nonzero>1); assertTrue(sum>180&&sum<340);
    }
    @Test(timeout = 5000) public void tinyImagesAndEdgesAreSafe() {
        int[] one={0xFF123456}; BoxBlur.blur(one,1,1,12,3); assertEquals(0xFF123456,one[0]);
        int[] edge={0xFFFFFFFF,0xFF000000,0xFF000000,0xFF000000}; BoxBlur.blur(edge,2,2,1,3);
        for(int pixel:edge){assertTrue((pixel&255)>0);assertTrue((pixel&255)<255);}
    }
}

