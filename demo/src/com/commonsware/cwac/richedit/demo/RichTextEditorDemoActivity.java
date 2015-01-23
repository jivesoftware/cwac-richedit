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
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.commonsware.cwac.richedit.RichEditText;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RichTextEditorDemoActivity extends Activity {
    private static final String TAG = "cwac-richedit-demo";
    RichEditText editor;
    ImageButton insertImageImageButton;
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
            editor.insertImage(new ImageSpan(RichTextEditorDemoActivity.this, bitmap), key);
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
    }

    @SuppressWarnings("unchecked")
    private void setImageUrisByKeyWithSavedInstanceState(Bundle savedInstanceState) {
        imageUrisByKey = (HashMap<String, Uri>) savedInstanceState.getSerializable("imageUrisByKey");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        HashMap<String, ImageSpan> imageSpansByKey = new HashMap<String, ImageSpan>(imageUrisByKey.size());
        for (Map.Entry<String, Uri> imageUriByKey : imageUrisByKey.entrySet()) {
            imageSpansByKey.put(imageUriByKey.getKey(), new ImageSpan(this, imageUriByKey.getValue()));
        }

        // don't do this inside of #onCreate because RichEditText's text won't be restored
        // until after super#onRestoreInstanceState completes.
        editor.restoreImages(imageSpansByKey);
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
