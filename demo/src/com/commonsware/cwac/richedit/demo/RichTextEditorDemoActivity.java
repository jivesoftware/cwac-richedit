/***
 Copyright (c) 2012 CommonsWare, LLC

 Licensed under the Apache License, Version 2.0 (the "License"); you may
 not use this file except in compliance with the License. You may obtain
 a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.commonsware.cwac.richedit.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.text.Annotation;
import android.text.Editable;
import android.text.NoCopySpan;
import android.text.SpanWatcher;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.commonsware.cwac.richedit.Effect;
import com.commonsware.cwac.richedit.LinkEffect;
import com.commonsware.cwac.richedit.RichEditText;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RichTextEditorDemoActivity extends Activity {
    private static final String TAG = "cwac-richedit-demo";
    RichEditText editor;
    ImageButton insertImageImageButton;
    ToggleButton linkToggleButton;
    RichEditText.OnSelectionChangedListener onSelectionChangedListener = new RichEditText.OnSelectionChangedListener() {
      @Override
      public void onSelectionChanged(int start, int end, List<Effect<?>> effects) {
        boolean containsLinkEffect = effects.contains(RichEditText.LINK);
        linkToggleButton.setChecked(containsLinkEffect);
      }
    };
    Button dumpSpansButton;

    ImageCapturer imageCapturer;
    ImageCapturer.ImageCapturedCallback imageCapturedCallback = new ImageCapturer.ImageCapturedCallback() {
        @Override
        public void onImageCaptured(Bitmap bitmap, Uri imageUri, File imageFile) {
            insertImageImageButton.setEnabled(true);
            editor.setEnabled(true);
            String key = Integer.toString(nextKey);
            nextKey++;
            if (imageUri != null) {
                imageUrisByKey.put(key, imageUri);
            } else {
                imageUrisByKey.put(key, Uri.fromFile(imageFile));
            }
            editor.insertImage(key, new ImageSpan(RichTextEditorDemoActivity.this, bitmap));
        }

        @Override
        public void onImageCaptureFailed(IOException e) {
            Toast.makeText(RichTextEditorDemoActivity.this, R.string.image_capture_failed_io, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Image capture failed (IO)", e);
            insertImageImageButton.setEnabled(true);
            editor.setEnabled(true);
        }

        @Override
        public void onImageCaptureOutOfMemory(OutOfMemoryError outOfMemoryError, NullPointerException npe) {
            Toast.makeText(RichTextEditorDemoActivity.this, R.string.image_capture_failed_oom, Toast.LENGTH_SHORT).show();
            Throwable t;
            if (outOfMemoryError != null) {
                t = outOfMemoryError;
            } else {
                t = npe;
            }
            Log.e(TAG, "Image capture failed (OOM)", t);
            insertImageImageButton.setEnabled(true);
            editor.setEnabled(true);
        }
    };

    int nextKey;
    HashMap<String, Uri> imageUrisByKey;
    RichEditText.ImageSpanRestorer imageSpanRestorer = new RichEditText.ImageSpanRestorer() {
        @Override
        public ImageSpan getImageSpanForKey(String key) {
            Uri uri = imageUrisByKey.get(key);
            if (uri == null) {
                return null;
            } else {
                return new ImageSpan(RichTextEditorDemoActivity.this, uri);
            }
        }
    };
    RichEditText.ImageSpanWatcher imageSpanWatcher = new RichEditText.ImageSpanWatcher() {
        @Override
        public void onImageSpanRemoved(String key) {
            imageUrisByKey.remove(key);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        if (savedInstanceState != null) {
            imageCapturer = savedInstanceState.getParcelable("imageCapturer");
            nextKey = savedInstanceState.getInt("nextKey");
            setImageUrisByKeyWithSavedInstanceState(savedInstanceState);
        }
        if (imageCapturer == null) {
            imageCapturer = new ImageCapturer();
        }
        if (nextKey == 0) {
            nextKey = 1;
        }
        if (imageUrisByKey == null) {
            imageUrisByKey = new HashMap<String, Uri>();
        }

        editor = (RichEditText) findViewById(R.id.editor);
        editor.enableActionModes(true);
        editor.setImageSpanRestorer(imageSpanRestorer);
        editor.setImageSpanWatcher(imageSpanWatcher);
        editor.setOnSelectionChangedListener(onSelectionChangedListener);

        insertImageImageButton = (ImageButton) findViewById(R.id.insertImageButton);
        insertImageImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    imageCapturer.awaitImageCapture(RichTextEditorDemoActivity.this, 1);
                } catch (IOException e) {
                    Toast.makeText(RichTextEditorDemoActivity.this,
                            R.string.await_image_capture_exception,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        linkToggleButton = (ToggleButton) findViewById(R.id.linkToggleButton);
        linkToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = editor.getEffectValue(RichEditText.LINK);
                if (value == null) {
                    linkToggleButton.setChecked(false);
                    AlertDialog.Builder alert = new AlertDialog.Builder(RichTextEditorDemoActivity.this);

                    alert.setTitle("Link Contents");
                    alert.setMessage("Enter Link Contents");
                    final EditText input = new EditText(RichTextEditorDemoActivity.this);
                    alert.setView(input);

                    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            int selectionStart = editor.getSelectionStart();
                            String link = input.getText().toString();
                            String text = "Link";
                            editor.getText().replace(
                                    selectionStart,
                                    editor.getSelectionEnd(),
                                    text);
                            editor.setSelection(selectionStart, selectionStart + text.length());
                            editor.applyEffect(RichEditText.LINK, link);
                            linkToggleButton.setChecked(true);
                        }
                    });

                    alert.setNegativeButton("Cancel", null);

                    alert.show();
                } else {
                  editor.applyEffect(RichEditText.LINK, null);
                }
            }
        });
        dumpSpansButton = (Button)findViewById(R.id.dumpSpansButton);
        dumpSpansButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Editable text = editor.getText();
                Object[] spans = text.getSpans(0, text.length(), Object.class);
                Arrays.sort(spans, new Comparator<Object>() {
                    @Override
                    public int compare(Object lhs, Object rhs) {
                        int lhsStart = text.getSpanStart(lhs);
                        int rhsStart = text.getSpanStart(rhs);
                        if (lhsStart == rhsStart) {
                            int lhsEnd = text.getSpanStart(lhs);
                            int rhsEnd = text.getSpanStart(rhs);
                            return lhsEnd - rhsEnd;
                        } else {
                            return lhsStart - rhsStart;
                        }
                    }
                });
                for (Object span : spans) {
                    if (!(span instanceof NoCopySpan)) {
                        int spanStart = text.getSpanStart(span);
                        int spanEnd = text.getSpanEnd(span);
                        CharSequence spanText = text.subSequence(spanStart, spanEnd);
                        if (span instanceof Annotation) {
                            Annotation annotation = (Annotation)span;
                            Log.i(TAG, "Span: " + annotation.getClass() + "[key=" + annotation.getKey() + ", value=" + annotation.getValue() + "], text: \"" + spanText + "\"");
                        } else {
                            Log.i(TAG, "Span: " + span + "m text: \"" + spanText + "\"");
                        }
                    }
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void setImageUrisByKeyWithSavedInstanceState(Bundle savedInstanceState) {
        imageUrisByKey = (HashMap<String, Uri>) savedInstanceState.getSerializable("imageUrisByKey");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("imageCapturer", imageCapturer);
        outState.putInt("nextKey", nextKey);
        outState.putSerializable("imageUrisByKey", imageUrisByKey);
    }

    @Override
    protected void onDestroy() {
        imageCapturer.cancelBackgroundProcessing();

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Integer imageCapturerResultCode = imageCapturer.onActivityResult(this, requestCode, resultCode, data, imageCapturedCallback);
        if (Integer.valueOf(1).equals(imageCapturerResultCode)) {
            insertImageImageButton.setEnabled(false);
            editor.setEnabled(false);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
