package craftedcart.gcisomanager.util;

/**
 * @author CraftedCart
 *         Created on 26/11/2016 (DD/MM/YYYY)
 */
public class MathUtils {

    public static String prettifyByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.2f %sB", bytes / Math.pow(unit, exp), pre);
    }

}
