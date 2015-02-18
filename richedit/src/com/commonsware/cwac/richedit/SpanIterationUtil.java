package com.commonsware.cwac.richedit;

import android.text.Spannable;

import static com.commonsware.cwac.richedit.SpannableUtil.setSpan;

public final class SpanIterationUtil {
  public static final class SpanIterationReduction<R> {
    public R reduction;
  }

  public interface ReducingSpanIteration<R, S extends Spannable, T> {
    boolean iterate(S str, T span, SpanIterationReduction<R> spanIterationReduction);
  }

  public interface SpanIteration<S extends Spannable, T> {
    boolean iterate(S str, T span);
  }

  public interface SpanPredicate<T> {
    boolean select(T span);
  }

  public interface SpanValueProvider<R, T> {
    R getValue(T span);
  }

  public interface SpanProvider<R, T> {
    T provideSpan(R value);
  }

  private static final class ApplyReduction<R> {
    private static final int UNSET_PROLOGUE_START=Integer.MAX_VALUE;
    private static final int UNSET_EPILOGUE_END=-1;

    R prologue;
    int prologueStart=UNSET_PROLOGUE_START;
    R epilogue;
    int epilogueEnd=UNSET_EPILOGUE_END;

    public <S extends Spannable, T> void maybeApplyPrologue(S str, SpanProvider<R, T> spanProvider, Selection selection) {
      if (prologueStart < UNSET_PROLOGUE_START) {
        setSpan(str, spanProvider.provideSpan(prologue), prologueStart, selection.start);
      }
    }

    public <S extends Spannable, T> void maybeApplyEpilogue(S str, SpanProvider<R, T> spanProvider, Selection selection) {
      if (epilogueEnd > UNSET_EPILOGUE_END) {
        setSpan(str, spanProvider.provideSpan(epilogue), selection.end, epilogueEnd);
      }
    }
  }

  private static final class ApplyReducingSpanIteration<R, S extends Spannable, T> implements ReducingSpanIteration<ApplyReduction<R>, S, T> {
    private final Selection selection;
    private final SpanValueProvider<R, T> spanValueProvider;

    public ApplyReducingSpanIteration(Selection selection, SpanValueProvider<R, T> spanValueProvider) {
      this.selection=selection;
      this.spanValueProvider = spanValueProvider;
    }

    @Override
    public boolean iterate(S str, T span, SpanIterationReduction<ApplyReduction<R>> spanIterationReduction) {
      int spanStart=str.getSpanStart(span);

      if (spanStart < selection.start) {
        spanIterationReduction.reduction.prologueStart=Math.min(spanIterationReduction.reduction.prologueStart, spanStart);
        spanIterationReduction.reduction.prologue= spanValueProvider.getValue(span);
      }

      int spanEnd=str.getSpanEnd(span);

      if (spanEnd > selection.end) {
        spanIterationReduction.reduction.epilogueEnd=Math.max(spanIterationReduction.reduction.epilogueEnd, spanEnd);
        spanIterationReduction.reduction.epilogue= spanValueProvider.getValue(span);
      }

      str.removeSpan(span);
      return true;
    }
  }

  public static <R, S extends Spannable, T> void applySpansToSpannable(S str, Selection selection, Class<T> spanClass,
                                                                       SpanPredicate<T> predicate,
                                                                       SpanValueProvider<R, T> spanValueProvider,
                                                                       SpanProvider<R, T> spanProvider,
                                                                       R newValue) {
      ApplyReduction<R> applyReduction = iterateSpans(str, selection, spanClass,
          predicate,
          new ApplyReducingSpanIteration<R, S, T>(selection, spanValueProvider),
          new ApplyReduction<R>());
      if (newValue != null) {
        setSpan(str, spanProvider.provideSpan(newValue), selection.start, selection.end);
      }

      applyReduction.maybeApplyPrologue(str, spanProvider, selection);
      applyReduction.maybeApplyEpilogue(str, spanProvider, selection);
  }

  public static <S extends Spannable, T> void iterateSpans(S str, Selection selection, Class<T> spanClass,
                                                           SpanPredicate<T> predicate,
                                                           SpanIteration<S, T> iteration) {
    T[] spans = str.getSpans(selection.start, selection.end, spanClass);
    for (T span : spans) {
      if (predicate.select(span)) {
        boolean shouldContinue = iteration.iterate(str, span);
        if (!shouldContinue) {
            break;
        }
      }
    }
  }

  public static <R, S extends Spannable, T> R iterateSpans(S str, Selection selection, Class<T> spanClass,
                                                           SpanPredicate<T> predicate,
                                                           ReducingSpanIteration<R, S, T> iteration,
                                                           R zeroReduction) {
    T[] spans = str.getSpans(selection.start, selection.end, spanClass);
    if (spans.length > 0) {
      SpanIterationReduction<R> spanIterationReduction = new SpanIterationReduction<R>();
      spanIterationReduction.reduction = zeroReduction;
      for (T span : spans) {
        if (predicate.select(span)) {
          boolean shouldContinue = iteration.iterate(str, span, spanIterationReduction);
          if (!shouldContinue) {
              break;
          }
        }
      }
      return spanIterationReduction.reduction;
    } else {
      return zeroReduction;
    }
  }
}
