package me.croabeast.sir.api.file.update;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang.StringUtils;

@UtilityClass
class KeyUtils {

    boolean isSubKeyOf(String parentKey, String subKey, char separator) {
        if (StringUtils.isEmpty(parentKey)) return false;

        return subKey.startsWith(parentKey)
                && subKey.substring(parentKey.length()).startsWith(separator + "");
    }

    String getIndents(String key) {
        String[] splitKey = key.split("[" + '.' + "]");
        StringBuilder builder = new StringBuilder();

        for (int i = 1; i < splitKey.length; i++) builder.append("  ");
        return builder.toString();
    }
}
