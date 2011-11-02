/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.seam.faces.view.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

import org.jboss.solder.logging.Logger;

/**
 * Extension that scans enums for view specific configuration
 * 
 * @author stuart
 * @author <a href="mailto:bleathem@gmail.com">Brian Leathem</a>
 */
public class ViewConfigExtension implements Extension {

    private transient final Logger log = Logger.getLogger(ViewConfigExtension.class);

    private final Map<String, ViewConfigDescriptor> data = new HashMap<String, ViewConfigDescriptor>();

    public <T> void processAnnotatedType(@Observes ProcessAnnotatedType<T> event) {
        AnnotatedType<T> tp = event.getAnnotatedType();
        if (log.isTraceEnabled()) {
            log.tracef("Annotated Type: %s", tp.getJavaClass().getName());
            for (Annotation annotation : tp.getAnnotations()) {
                log.tracef("|-- Annotation: %s", annotation.annotationType().getName());
                for (Annotation qualifier : annotation.getClass().getAnnotations()) {
                    log.tracef("    |-- Qualifier: %s", qualifier.annotationType().getName());
                }
            }
        }
        if (tp.isAnnotationPresent(ViewConfig.class)) {
            if (!tp.getJavaClass().isInterface()) {
                log.warn("ViewConfig annotation should only be applied to interfaces, and [" + tp.getJavaClass()
                        + "] is not an interface.");
            } else {
                for (Class<?> clazz : tp.getJavaClass().getClasses()) {
                    for (Field enumm : clazz.getFields())
                        if (enumm.isAnnotationPresent(ViewPattern.class)) {
                            ViewPattern viewConfig = enumm.getAnnotation(ViewPattern.class);
                            String viewId = viewConfig.value();
                            ViewConfigDescriptor viewConfigDescriptor = data.get(viewId);
                            if (viewConfigDescriptor == null) {
                                viewConfigDescriptor = new ViewConfigDescriptor(viewId, getViewFieldValue(enumm));
                                data.put(viewId, viewConfigDescriptor);
                            }
                            for (Annotation a : enumm.getAnnotations()) {
                                if (a.annotationType() != ViewPattern.class) {
                                    viewConfigDescriptor.addMetaData(a);
                                }
                            }
                        }
                }
            }
        }
    }

    /**
     * Returns the value of a view field.
     * 
     * @throws IllegalArgumentException if an error happens
     */
    private Object getViewFieldValue(Field enumm) {
        try {
            return enumm.get(null);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid view field " + enumm + " - error getting value " + e.toString(), e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Invalid view field " + enumm + " - error getting value " + e.toString(), e);
        }
    }

    public Map<String, ViewConfigDescriptor> getData() {
        return Collections.unmodifiableMap(data);
    }
}
