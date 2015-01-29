package com.commonsware.cwac.richedit;

import android.text.Annotation;

public enum AnnotationManager {
  IMAGE_SPAN_KEY,
  LINK;

  public boolean matches(Annotation annotation) {
    return name().equals(annotation.getKey());
  }

  public Annotation createAnnotation(String value) {
    return new Annotation(name(), value);
  }
}
