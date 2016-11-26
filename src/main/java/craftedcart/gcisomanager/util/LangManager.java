package craftedcart.gcisomanager.util;

import java.util.ResourceBundle;

/**
 * @author CraftedCart
 *         Created on 23/10/2016 (DD/MM/YYYY)
 */
public class LangManager {

    public static String getItem(String key) {
        return ResourceBundle.getBundle("lang").getString(key);
    }

}
