package com.commonsware.cwac.richedit.demo;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ImageCapturer implements Parcelable {
    public static final Parcelable.Creator<ImageCapturer> CREATOR =
            new Parcelable.Creator<ImageCapturer>() {
                public ImageCapturer createFromParcel(Parcel in) {
                    ImageCapturer imageCapturer = new ImageCapturer();
                    String absolutePath = in.readString();
                    // Android Studio stupidly thinks absolutePath can't be null. It is wrong.
                    //noinspection ConstantConditions
                    if (absolutePath != null) {
                        imageCapturer.imageTemporaryFile = new File(absolutePath);
                    }
                    imageCapturer.requestCode = in.readInt();
                    return imageCapturer;
                }

                public ImageCapturer[] newArray(int size) {
                    return new ImageCapturer[size];
                }
            };

    private File imageTemporaryFile;
    private int requestCode = -1;
    private DecodeReceivedImageAsyncTask decodeReceivedImageAsyncTask;

    /**
     * Start a new {@link android.app.Activity} to choose an image.
     *
     * @param activity    the current {@link android.app.Activity}
     * @param requestCode the requestCode you want Android to pass back when it calls
     *                    <code>activity</code>'s {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     * @throws IOException
     */
    public void awaitImageCapture(Activity activity, int requestCode) throws IOException {
        assertOnMainThread();

        if (requestCode < 1) {
            throw new IllegalArgumentException("requestCode must be greater than zero: " + requestCode);
        }

        if (imageTemporaryFile != null) {
            throw new IllegalStateException("Can only capture one file at a time. Previous file: " + imageTemporaryFile);
        }

        PackageManager packageManager = activity.getPackageManager();
        String title = activity.getString(R.string.select_image_source);

        String tempFileName = "cwac-richedit-demo-capture-" + UUID.randomUUID().toString() + ".jpg";
        File tempDir = new File(Environment.getExternalStorageDirectory(), "cwac-richedit-demo");
        if (!tempDir.exists()) {
            if (!tempDir.mkdir()) {
                throw new IOException("Couldn't create temporary image storage directory: " + tempDir);
            }
        }
        imageTemporaryFile = new File(tempDir, tempFileName);

        final List<Intent> galleryIntents = new ArrayList<Intent>();
        final Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");

        final List<ResolveInfo> listGalleryListeners = packageManager.queryIntentActivities(galleryIntent, 0);
        for (ResolveInfo res : listGalleryListeners) {
            final String packageName = res.activityInfo.packageName;
            final Intent intent = new Intent(galleryIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(packageName);
            galleryIntents.add(intent);
        }

        // camera intent
        final Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageTemporaryFile));

        final Intent chooserIntent = Intent.createChooser(cameraIntent, title);

        // Add the gallery options to the menu.
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, galleryIntents.toArray(new Parcelable[galleryIntents.size()]));
        this.requestCode = requestCode;
        activity.startActivityForResult(chooserIntent, requestCode);
    }

    /**
     * Call this from {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}.
     * <p/>
     * It is safe to call {@link #awaitImageCapture(android.app.Activity, int)} after this call.
     *
     * @param activity              your {@link android.app.Activity}
     * @param requestCode           from {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}.
     * @param resultCode            from {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}.
     * @param data                  from {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}.
     * @param imageCapturedCallback called when background processing finishes.
     * @return the <code>resultCode</code> from processing this result or <code>null</code> if the
     * <code>requestCode</code> didn't match the one passed to {@link #awaitImageCapture(android.app.Activity, int)}
     */
    public Integer onActivityResult(Activity activity, int requestCode, int resultCode, Intent data, ImageCapturedCallback imageCapturedCallback) {
        assertOnMainThread();

        if (imageTemporaryFile == null) {
            throw new IllegalStateException("awaitImageCapture wasn't called first");
        }

        if (this.requestCode == requestCode) {
            if (resultCode == Activity.RESULT_OK) {
                decodeReceivedImageAsyncTask = new DecodeReceivedImageAsyncTask(imageTemporaryFile, activity, data, imageCapturedCallback);
                decodeReceivedImageAsyncTask.execute((Void[]) null);
            }

            imageTemporaryFile = null;
            this.requestCode = -1;

            return resultCode;
        }

        return null;
    }

    /**
     * Ignore results from prior {@link #awaitImageCapture(android.app.Activity, int)} calls.
     * <p/>
     * It is safe to call {@link #awaitImageCapture(android.app.Activity, int)} after this call.
     */
    public void cancelAwaitImageCapture() {
        assertOnMainThread();

        imageTemporaryFile = null;
        requestCode = -1;
    }

    /**
     * Stop background processing from prior {@link #onActivityResult(android.app.Activity, int, int, android.content.Intent, com.commonsware.cwac.richedit.demo.ImageCapturer.ImageCapturedCallback)} calls.
     * <p/>
     * It is safe to call {@link #awaitImageCapture(android.app.Activity, int)} after this call.
     */
    public void cancelBackgroundProcessing() {
        assertOnMainThread();

        if (decodeReceivedImageAsyncTask != null) {
            decodeReceivedImageAsyncTask.cancel(true);
            decodeReceivedImageAsyncTask = null;
        }
    }

    private void assertOnMainThread() {
        Thread currentThread = Thread.currentThread();
        Thread mainThread = Looper.getMainLooper().getThread();
        if (mainThread != currentThread) {
            throw new IllegalStateException("ImageCapturer can only be used from the main thread. Main thread: " + mainThread + "Current thread: " + currentThread);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        String absolutePath;
        if (imageTemporaryFile == null) {
            absolutePath = null;
        } else {
            absolutePath = imageTemporaryFile.getAbsolutePath();
        }
        dest.writeString(absolutePath);
        dest.writeInt(requestCode);
    }

    public interface ImageCapturedCallback {
        /**
         * Only one of <code>imageLocation</code> or <code>imageFile</code> will be non-<code>null</code>.
         * This returns both a {@link android.net.Uri} and a {@link java.io.File} instead of just
         * a <code>Uri</code> because when possible, you might want to delete the <code>File</code>
         * when the bitmap is returned, but the <code>Uri</code> may be from a {@link android.content.ContentProvider},
         * which may not support deleting an item at that <code>Uri</code>.
         *
         * @param bitmap           A {@link android.graphics.Bitmap} of the captured image
         * @param imageUri The {@link android.net.Uri} of the captured image or <code>null</code>.
         * @param imageFile        The {@link java.io.File} of the captured image or <code>null</code>.
         */
        void onImageCaptured(Bitmap bitmap, Uri imageUri, File imageFile);

        void onImageCaptureFailed(IOException e);

        /**
         * @param outOfMemoryError The {@link java.lang.OutOfMemoryError}. May be <code>null</code>.
         * @param npe              A {@link java.lang.NullPointerException} which usually occurs when there is an underlying {@link java.lang.OutOfMemoryError}. May be <code>null</code>.
         */
        void onImageCaptureOutOfMemory(OutOfMemoryError outOfMemoryError, NullPointerException npe);
    }

    private static class DecodeReceivedImageTaskResult {
        public final Bitmap bitmap;
        public final Uri imageLocationUri;
        public final IOException ioException;
        public final OutOfMemoryError outOfMemoryError;
        public final NullPointerException nullPointerException;

        public DecodeReceivedImageTaskResult(Bitmap bitmap, Uri imageLocationUri, IOException ioException, OutOfMemoryError outOfMemoryError, NullPointerException nullPointerException) {
            this.bitmap = bitmap;
            this.imageLocationUri = imageLocationUri;
            this.ioException = ioException;
            this.outOfMemoryError = outOfMemoryError;
            this.nullPointerException = nullPointerException;
        }
    }

    private class DecodeReceivedImageAsyncTask extends AsyncTask<Void, Void, DecodeReceivedImageTaskResult> {
        private final File imageTemporaryFile;
        private final Activity activity;
        private final Intent intent;
        private final ImageCapturedCallback imageCapturedCallback;

        private DecodeReceivedImageAsyncTask(File imageTemporaryFile, Activity activity, Intent intent, ImageCapturedCallback imageCapturedCallback) {
            this.imageTemporaryFile = imageTemporaryFile;
            this.activity = activity;
            this.intent = intent;
            this.imageCapturedCallback = imageCapturedCallback;
        }

        @Override
        protected DecodeReceivedImageTaskResult doInBackground(Void... unused) {
            Bitmap bitmap;
            Uri imageUri;

            try {
                if (intent == null) {
                    bitmap = BitmapFactory.decodeFile(imageTemporaryFile.getAbsolutePath());
                    imageUri = null;
                } else {
                    imageUri = intent.getData();
                    ContentResolver contentResolver = activity.getContentResolver();
                    if (contentResolver == null) {
                        throw new IOException("Decoding image file failed because content resolver was null");
                    }
                    AssetFileDescriptor assetFileDescriptor = contentResolver.openAssetFileDescriptor(imageUri, "r");
                    bitmap = BitmapFactory.decodeFileDescriptor(assetFileDescriptor.getFileDescriptor());
                    try {
                        assetFileDescriptor.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }

                if (bitmap == null) {
                    throw new IOException("Decoding image file failed for unknown reason");
                }
            } catch (IOException e) {
                return new DecodeReceivedImageTaskResult(null, null, e, null, null);
            } catch (OutOfMemoryError e) {
                return new DecodeReceivedImageTaskResult(null, null, null, e, null);
            } catch (NullPointerException e) {
                // This NPE seems to be OOM related
                return new DecodeReceivedImageTaskResult(null, null, null, null, e);
            }

            return new DecodeReceivedImageTaskResult(bitmap, imageUri, null, null, null);
        }

        @Override
        protected void onPostExecute(DecodeReceivedImageTaskResult result) {
            decodeReceivedImageAsyncTask = null;
            if (!isCancelled()) {
                if (result.bitmap != null) {
                    File imageFile = imageTemporaryFile.exists() ? imageTemporaryFile : null;
                    imageCapturedCallback.onImageCaptured(result.bitmap, result.imageLocationUri, imageFile);
                } else if (result.ioException != null) {
                    imageCapturedCallback.onImageCaptureFailed(result.ioException);
                } else {
                    imageCapturedCallback.onImageCaptureOutOfMemory(result.outOfMemoryError, result.nullPointerException);
                }
            }
        }
    }
}
