package jpeg;

import Jama.Matrix;
import jpeg.Quality;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class QualityTest {
    static double[][] original = {{146.6, 107.41, 82.88, 252.82, 101.66, 90.58, 163.7, 177.97, 206.2, 188.24, 153.19, 53.8, 188.39, 12.18, 161.93, 168.6, 152.36, 186.78, 177.58, 119.06, 150.81, 130.89, 95.25, 221.07}, {53.04, 179.98, 175.4, 130.02, 251.4, 33.49, 82.62, 1.65, 231.33, 29.58, 70.09, 234.54, 58.79, 137.36, 224.38, 69.6, 89.52, 224.9, 5.06, 73.02, 81.68, 195.07, 201.96, 183.97}, {252.21, 125.37, 86.1, 230.97, 88.32, 187.72, 157.28, 61.14, 132.27, 154.19, 212.64, 190.16, 17.26, 245.26, 231.23, 2.36, 172.59, 23.32, 214.15, 174.41, 14.12, 120.7, 29.17, 148.98}, {229.94, 178.33, 61.5, 69.68, 17.58, 83.88, 110.3, 247.09, 166.14, 200.93, 241.88, 137.11, 146.79, 64.27, 11.33, 81.91, 105.62, 210.32, 154.85, 141.12, 146.0, 55.9, 128.8, 41.15}, {177.02, 148.51, 100.49, 208.6, 30.9, 16.51, 58.13, 54.98, 130.19, 58.39, 254.65, 168.95, 65.25, 106.44, 75.98, 69.52, 49.09, 192.06, 34.95, 53.53, 44.61, 136.76, 21.69, 173.99}, {129.19, 105.86, 49.9, 161.58, 223.22, 219.86, 96.91, 95.76, 102.38, 41.97, 217.29, 183.84, 163.51, 89.4, 6.51, 136.14, 230.71, 55.57, 196.84, 253.45, 11.62, 230.24, 67.54, 73.54}, {244.2, 60.03, 185.62, 227.67, 96.0, 117.93, 71.76, 19.33, 56.73, 201.4, 188.71, 197.7, 165.0, 150.83, 166.02, 135.59, 37.62, 17.36, 21.35, 187.57, 152.87, 151.76, 173.53, 12.57}, {210.51, 254.38, 67.27, 126.8, 92.71, 130.03, 217.81, 97.49, 109.62, 60.6, 77.08, 225.46, 173.39, 133.11, 219.73, 235.08, 67.43, 185.88, 238.91, 158.69, 216.14, 161.22, 237.15, 133.92}, {181.52, 188.94, 147.65, 121.88, 205.65, 161.76, 109.95, 6.55, 156.88, 155.44, 32.57, 251.04, 3.85, 184.19, 252.34, 204.81, 224.14, 154.59, 251.93, 67.06, 8.44, 74.94, 142.99, 11.02}, {82.53, 160.59, 227.2, 175.95, 247.16, 195.39, 52.24, 143.85, 20.23, 168.94, 92.73, 106.18, 52.24, 119.93, 134.35, 86.95, 13.77, 46.38, 241.07, 252.72, 13.47, 239.78, 1.7, 26.93}, {66.57, 247.19, 127.4, 151.47, 66.9, 41.95, 176.33, 165.9, 10.5, 1.9, 55.54, 161.11, 135.09, 156.58, 248.22, 182.99, 118.44, 103.75, 41.7, 178.59, 108.4, 29.62, 108.2, 152.38}, {113.98, 141.45, 115.81, 42.72, 100.81, 226.43, 232.43, 209.76, 93.75, 236.01, 109.23, 224.75, 192.18, 75.82, 118.86, 147.39, 56.17, 62.78, 101.02, 192.75, 207.95, 42.67, 159.93, 181.93}, {149.42, 243.91, 209.15, 61.15, 150.72, 176.1, 59.66, 123.03, 245.05, 39.91, 6.35, 234.93, 177.23, 68.89, 226.83, 99.12, 57.52, 122.92, 199.48, 197.14, 97.63, 217.42, 144.69, 195.87}, {73.47, 153.05, 94.52, 201.87, 223.02, 145.62, 21.72, 251.44, 48.67, 67.75, 243.69, 8.73, 95.86, 194.53, 106.16, 96.37, 53.93, 33.12, 99.24, 78.54, 116.43, 173.38, 16.37, 79.74}, {190.2, 189.56, 197.9, 3.81, 177.63, 116.59, 247.35, 1.98, 1.96, 113.35, 103.09, 231.15, 193.84, 39.72, 172.79, 248.43, 211.47, 18.84, 226.3, 191.69, 78.4, 241.54, 185.63, 56.48}, {25.4, 180.46, 157.33, 44.19, 103.23, 192.72, 129.02, 153.1, 159.05, 216.73, 222.22, 39.4, 87.44, 235.64, 2.99, 232.83, 112.18, 197.93, 46.13, 247.9, 5.95, 183.5, 71.34, 170.61}, {224.05, 247.17, 202.42, 142.3, 82.63, 190.71, 106.65, 157.26, 229.21, 213.91, 87.77, 138.81, 26.29, 150.12, 125.19, 109.04, 124.28, 42.36, 17.04, 126.37, 191.73, 56.67, 182.28, 42.02}, {39.61, 246.13, 40.24, 20.82, 116.46, 15.51, 177.44, 105.02, 208.58, 179.1, 32.76, 49.93, 147.93, 166.68, 172.18, 49.24, 151.04, 16.47, 63.79, 69.25, 209.44, 96.82, 2.47, 16.05}, {75.96, 183.4, 111.59, 76.99, 16.89, 177.59, 91.12, 253.45, 177.5, 192.61, 42.92, 80.48, 22.21, 193.76, 74.59, 240.15, 191.92, 191.02, 72.92, 79.97, 128.0, 123.06, 82.15, 55.29}, {117.11, 164.23, 200.25, 234.6, 166.82, 88.4, 134.16, 10.39, 254.6, 226.31, 236.17, 64.15, 82.16, 136.89, 75.03, 233.8, 73.41, 131.37, 69.93, 193.55, 106.87, 53.92, 225.15, 176.93}, {48.08, 31.84, 131.7, 11.45, 23.79, 249.1, 194.73, 114.96, 232.63, 237.3, 37.86, 232.34, 226.79, 224.87, 251.45, 148.85, 105.5, 86.46, 97.35, 35.92, 224.37, 127.17, 7.39, 183.79}, {204.49, 188.53, 221.46, 195.77, 236.62, 231.32, 219.45, 88.44, 14.41, 114.12, 23.2, 213.54, 125.18, 165.07, 46.44, 67.65, 148.95, 254.39, 131.25, 233.18, 159.52, 244.55, 19.48, 70.39}, {101.85, 56.09, 216.96, 213.83, 26.58, 222.43, 136.28, 178.21, 246.51, 203.74, 7.0, 123.7, 204.73, 246.65, 148.59, 158.36, 251.65, 64.01, 182.63, 135.67, 196.47, 221.47, 250.91, 254.49}, {169.46, 131.2, 130.7, 124.64, 163.4, 201.63, 187.04, 251.46, 61.57, 151.22, 29.76, 124.95, 75.36, 179.11, 210.25, 34.68, 211.67, 240.53, 29.02, 155.21, 239.75, 235.45, 1.16, 83.15}};
    static double[][] modified = {{146.6, 146.6, 82.88, 82.88, 101.66, 101.66, 163.7, 163.7, 206.2, 206.2, 153.19, 153.19, 188.39, 188.39, 161.93, 161.93, 152.36, 152.36, 177.58, 177.58, 150.81, 150.81, 95.25, 95.25}, {146.6, 146.6, 82.88, 82.88, 101.66, 101.66, 163.7, 163.7, 206.2, 206.2, 153.19, 153.19, 188.39, 188.39, 161.93, 161.93, 152.36, 152.36, 177.58, 177.58, 150.81, 150.81, 95.25, 95.25}, {252.21, 252.21, 86.1, 86.1, 88.32, 88.32, 157.28, 157.28, 132.27, 132.27, 212.64, 212.64, 17.26, 17.26, 231.23, 231.23, 172.59, 172.59, 214.15, 214.15, 14.12, 14.12, 29.17, 29.17}, {252.21, 252.21, 86.1, 86.1, 88.32, 88.32, 157.28, 157.28, 132.27, 132.27, 212.64, 212.64, 17.26, 17.26, 231.23, 231.23, 172.59, 172.59, 214.15, 214.15, 14.12, 14.12, 29.17, 29.17}, {177.02, 177.02, 100.49, 100.49, 30.9, 30.9, 58.13, 58.13, 130.19, 130.19, 254.65, 254.65, 65.25, 65.25, 75.98, 75.98, 49.09, 49.09, 34.95, 34.95, 44.61, 44.61, 21.69, 21.69}, {177.02, 177.02, 100.49, 100.49, 30.9, 30.9, 58.13, 58.13, 130.19, 130.19, 254.65, 254.65, 65.25, 65.25, 75.98, 75.98, 49.09, 49.09, 34.95, 34.95, 44.61, 44.61, 21.69, 21.69}, {244.2, 244.2, 185.62, 185.62, 96.0, 96.0, 71.76, 71.76, 56.73, 56.73, 188.71, 188.71, 165.0, 165.0, 166.02, 166.02, 37.62, 37.62, 21.35, 21.35, 152.87, 152.87, 173.53, 173.53}, {244.2, 244.2, 185.62, 185.62, 96.0, 96.0, 71.76, 71.76, 56.73, 56.73, 188.71, 188.71, 165.0, 165.0, 166.02, 166.02, 37.62, 37.62, 21.35, 21.35, 152.87, 152.87, 173.53, 173.53}, {181.52, 181.52, 147.65, 147.65, 205.65, 205.65, 109.95, 109.95, 156.88, 156.88, 32.57, 32.57, 3.85, 3.85, 252.34, 252.34, 224.14, 224.14, 251.93, 251.93, 8.44, 8.44, 142.99, 142.99}, {181.52, 181.52, 147.65, 147.65, 205.65, 205.65, 109.95, 109.95, 156.88, 156.88, 32.57, 32.57, 3.85, 3.85, 252.34, 252.34, 224.14, 224.14, 251.93, 251.93, 8.44, 8.44, 142.99, 142.99}, {66.57, 66.57, 127.4, 127.4, 66.9, 66.9, 176.33, 176.33, 10.5, 10.5, 55.54, 55.54, 135.09, 135.09, 248.22, 248.22, 118.44, 118.44, 41.7, 41.7, 108.4, 108.4, 108.2, 108.2}, {66.57, 66.57, 127.4, 127.4, 66.9, 66.9, 176.33, 176.33, 10.5, 10.5, 55.54, 55.54, 135.09, 135.09, 248.22, 248.22, 118.44, 118.44, 41.7, 41.7, 108.4, 108.4, 108.2, 108.2}, {149.42, 149.42, 209.15, 209.15, 150.72, 150.72, 59.66, 59.66, 245.05, 245.05, 6.35, 6.35, 177.23, 177.23, 226.83, 226.83, 57.52, 57.52, 199.48, 199.48, 97.63, 97.63, 144.69, 144.69}, {149.42, 149.42, 209.15, 209.15, 150.72, 150.72, 59.66, 59.66, 245.05, 245.05, 6.35, 6.35, 177.23, 177.23, 226.83, 226.83, 57.52, 57.52, 199.48, 199.48, 97.63, 97.63, 144.69, 144.69}, {190.2, 190.2, 197.9, 197.9, 177.63, 177.63, 247.35, 247.35, 1.96, 1.96, 103.09, 103.09, 193.84, 193.84, 172.79, 172.79, 211.47, 211.47, 226.3, 226.3, 78.4, 78.4, 185.63, 185.63}, {190.2, 190.2, 197.9, 197.9, 177.63, 177.63, 247.35, 247.35, 1.96, 1.96, 103.09, 103.09, 193.84, 193.84, 172.79, 172.79, 211.47, 211.47, 226.3, 226.3, 78.4, 78.4, 185.63, 185.63}, {224.05, 224.05, 202.42, 202.42, 82.63, 82.63, 106.65, 106.65, 229.21, 229.21, 87.77, 87.77, 26.29, 26.29, 125.19, 125.19, 124.28, 124.28, 17.04, 17.04, 191.73, 191.73, 182.28, 182.28}, {224.05, 224.05, 202.42, 202.42, 82.63, 82.63, 106.65, 106.65, 229.21, 229.21, 87.77, 87.77, 26.29, 26.29, 125.19, 125.19, 124.28, 124.28, 17.04, 17.04, 191.73, 191.73, 182.28, 182.28}, {75.96, 75.96, 111.59, 111.59, 16.89, 16.89, 91.12, 91.12, 177.5, 177.5, 42.92, 42.92, 22.21, 22.21, 74.59, 74.59, 191.92, 191.92, 72.92, 72.92, 128.0, 128.0, 82.15, 82.15}, {75.96, 75.96, 111.59, 111.59, 16.89, 16.89, 91.12, 91.12, 177.5, 177.5, 42.92, 42.92, 22.21, 22.21, 74.59, 74.59, 191.92, 191.92, 72.92, 72.92, 128.0, 128.0, 82.15, 82.15}, {48.08, 48.08, 131.7, 131.7, 23.79, 23.79, 194.73, 194.73, 232.63, 232.63, 37.86, 37.86, 226.79, 226.79, 251.45, 251.45, 105.5, 105.5, 97.35, 97.35, 224.37, 224.37, 7.39, 7.39}, {48.08, 48.08, 131.7, 131.7, 23.79, 23.79, 194.73, 194.73, 232.63, 232.63, 37.86, 37.86, 226.79, 226.79, 251.45, 251.45, 105.5, 105.5, 97.35, 97.35, 224.37, 224.37, 7.39, 7.39}, {101.85, 101.85, 216.96, 216.96, 26.58, 26.58, 136.28, 136.28, 246.51, 246.51, 7.0, 7.0, 204.73, 204.73, 148.59, 148.59, 251.65, 251.65, 182.63, 182.63, 196.47, 196.47, 250.91, 250.91}, {101.85, 101.85, 216.96, 216.96, 26.58, 26.58, 136.28, 136.28, 246.51, 246.51, 7.0, 7.0, 204.73, 204.73, 148.59, 148.59, 251.65, 251.65, 182.63, 182.63, 196.47, 196.47, 250.91, 250.91}};

    static double resultMSE = 8142.3184;
    static double resultMAE = 63.0651;
    static double resultSAE = 36325.4900;
    static double resultPSNR = 9.0233;
    static double resultPSNRrgb = 29.1180;
    static double resultSSIM = 0.265384;
    static double resultMSSIM = 0.265600;

    @Test
    void qualityTest() {
        double mse = Quality.countMSE(original, modified);
        assertEquals(resultMSE, round(mse, 4), "Wrong MSE calculation");
        System.out.println("Test MSE: OK");

        double mae = Quality.countMAE(original, modified);
        assertEquals(resultMAE, round(mae, 4), "Wrong MAE calculation");
        System.out.println("Test MAE: OK");

        double sae = Quality.countSAE(original, modified);
        assertEquals(resultSAE, round(sae, 4), "Wrong SAE calculation");
        System.out.println("Test SAE: OK");

        double psnr = Quality.countPSNR(mse);
        assertEquals(resultPSNR, round(psnr, 4), "Wrong PSNR calculation");
        System.out.println("Test PSNR: OK");

        double psnrRGB = Quality.countPSNRforRGB(64, 85, 90);
        assertEquals(resultPSNRrgb, round(psnrRGB, 4), "Wrong PSNR calculation for RGB");
        System.out.println("Test RGB: OK");

        // Test SSIM
        ssimTest();
    }

    void ssimTest() {
        try {
            double ssim = Quality.countSSIM(new Matrix(original), new Matrix(modified));
            assertEquals(resultSSIM, round(ssim, 6), "Wrong result for SSIM");
            System.out.println("Test SSIM: OK");
        } catch (Exception e) {
            System.out.println("SSIM not implemented");
        }

        try {
            double mssim = Quality.countMSSIM(new Matrix(original), new Matrix(modified));
            assertEquals(resultMSSIM, round(mssim, 6), "Wrong result for MSSIM");
            System.out.println("Test MSSIM: OK");
        } catch (Exception e) {
            System.out.println("MSSIM not implemented");
            return;
        }
        System.out.println("All test passed successfully");
    }

    private static double round(double input, int decimals) {
        double scale = Math.pow(10, decimals);
        return Math.round(input * scale) / scale;
    }
}
