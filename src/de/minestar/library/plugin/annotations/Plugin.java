package de.minestar.library.plugin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Plugin {

    public String name();

    public String version() default "UNKNOWN";

    public String[] softDepend() default "";

    public String[] hardDepend() default "";
}
