package me.croabeast.sir.plugin.file;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface CacheManageable {

    /**
     * Indicates if the "load" and/or "save" methods have higher or lower priority.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Priority {
        /**
         * The level of priority, can be negative.
         * @return priority level
         */
        int value() default 0;
    }
}
