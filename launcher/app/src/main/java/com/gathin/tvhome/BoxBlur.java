package com.gathin.tvhome;

/** Three-pass separable box blur for ARGB pixel arrays. */
final class BoxBlur {
    private BoxBlur() { }
    static void blur(int[] pixels, int width, int height, int radius, int passes) {
        if (pixels == null || width <= 0 || height <= 0 || pixels.length < width * height) return;
        int r = Math.max(0, radius), count = Math.max(0, Math.min(3, passes));
        if (r == 0 || count == 0) return;
        int[] temp = new int[width * height];
        for (int pass = 0; pass < count; pass++) {
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) temp[y * width + x] = average(pixels, width, height, x, y, r, true);
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) pixels[y * width + x] = average(temp, width, height, x, y, r, false);
        }
    }
    private static int average(int[] p, int w, int h, int x, int y, int r, boolean horizontal) {
        long a=0, red=0, green=0, blue=0; int n=0;
        int from=horizontal?Math.max(0,x-r):Math.max(0,y-r), to=horizontal?Math.min(w-1,x+r):Math.min(h-1,y+r);
        for(int i=from;i<=to;i++){int c=p[horizontal?y*w+i:i*w+x];a+=(c>>>24)&255;red+=(c>>>16)&255;green+=(c>>>8)&255;blue+=c&255;n++;}
        int h2=n/2;return ((int)((a+h2)/n)<<24)|((int)((red+h2)/n)<<16)|((int)((green+h2)/n)<<8)|(int)((blue+h2)/n);
    }
}
