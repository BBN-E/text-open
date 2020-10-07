package com.bbn.serif.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * This annotation marks items which should be moved into the JSerif repository. The most common use
 * of this annotation is to mark a class that has been temporarily placed in another repository but
 * should be moved when possible.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.METHOD,
    ElementType.PACKAGE, ElementType.TYPE})
public @interface MoveToJSerif {

}
