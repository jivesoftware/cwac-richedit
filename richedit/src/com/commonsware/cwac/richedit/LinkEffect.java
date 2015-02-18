package com.commonsware.cwac.richedit;

import android.text.Annotation;
import android.text.Spannable;

import static com.commonsware.cwac.richedit.SpanIterationUtil.applySpansToSpannable;
import static com.commonsware.cwac.richedit.SpanIterationUtil.iterateSpans;

public class LinkEffect extends Effect<String> {
  private static final SpanIterationUtil.SpanPredicate<Annotation> LINK_ANNOTATION_SPAN_PREDICATE=
      new SpanIterationUtil.SpanPredicate<Annotation>() {
        @Override
        public boolean select(Annotation span) {
          return AnnotationManager.LINK.matches(span);
        }
      };

  @Override
  public boolean existsInSelection(RichEditText editor) {
    return(valueInSelection(editor) != null);
  }

  @Override
  public String valueInSelection(RichEditText editor) {
    Selection selection=new Selection(editor);
    Spannable str=editor.getText();
    return iterateSpans(str, selection, Annotation.class,
            LINK_ANNOTATION_SPAN_PREDICATE,
            new SpanIterationUtil.ReducingSpanIteration<String, Spannable, Annotation>() {
      @Override
      public boolean iterate(Spannable str, Annotation span, SpanIterationUtil.SpanIterationReduction<String> spanIterationReduction) {
        spanIterationReduction.reduction = span.getValue();
        return false;
      }
    },
            null);
  }

  @Override
  public void applyToSelection(RichEditText editor, String value) {
    applyToSpannable(editor.getText(), new Selection(editor), value);
  }

  void applyToSpannable(Spannable str, Selection selection,
                        String link) {
    applySpansToSpannable(str, selection, Annotation.class,
        LINK_ANNOTATION_SPAN_PREDICATE,
        new SpanIterationUtil.SpanValueProvider<String, Annotation>() {
            @Override
            public String getValue(Annotation span) {
                return span.getValue();
            }
        },
        new SpanIterationUtil.SpanProvider<String, Annotation>() {
            @Override
            public Annotation provideSpan(String value) {
                return AnnotationManager.LINK.createAnnotation(value);
            }
        },
        link);
  }
}
