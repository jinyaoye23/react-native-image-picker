package com.mg.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;
import com.yalantis.ucrop.model.AspectRatio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;


import cn.finalteam.rxgalleryfinal.RxGalleryFinal;
import cn.finalteam.rxgalleryfinal.bean.ImageCropBean;
import cn.finalteam.rxgalleryfinal.bean.MediaBean;
import cn.finalteam.rxgalleryfinal.imageloader.ImageLoaderType;
import cn.finalteam.rxgalleryfinal.rxbus.RxBus;
import cn.finalteam.rxgalleryfinal.rxbus.RxBusResultDisposable;
import cn.finalteam.rxgalleryfinal.rxbus.event.ImageMultipleResultEvent;
import cn.finalteam.rxgalleryfinal.rxbus.event.ImageRadioResultEvent;
import cn.finalteam.rxgalleryfinal.ui.RxGalleryListener;
import cn.finalteam.rxgalleryfinal.ui.base.IRadioImageCheckedListener;
import cn.finalteam.rxgalleryfinal.utils.FileUtils;
import cn.finalteam.rxgalleryfinal.utils.Logger;
import cn.finalteam.rxgalleryfinal.utils.SimpleDateUtils;
import cn.finalteam.rxgalleryfinal.utils.ThemeUtils;

class PickerModule extends ReactContextBaseJavaModule implements cameraCallBack, ActivityEventListener {
    private static final String E_ACTIVITY_DOES_NOT_EXIST = "E_ACTIVITY_DOES_NOT_EXIST";

    private Promise mPickerPromise;
    private boolean watermark = false;
    private String address = "";


    private boolean cropping = false;
    private boolean multiple = false;
    private boolean isCamera = false;
    private boolean includeBase64 = false;
    private boolean openCameraOnStart = false;
    private boolean isVideo = false;
    private boolean isHidePreview = false;
    private boolean isPlayGif = false;
    private boolean isHideVideoPreview = false;
    private String title = null;
    private String imageLoader = null;
    //Light Blue 500
    private int width = 200;
    private int height = 200;
    private int maxSize = 9;
    private int imgmaxSize = 200;

    private int compressQuality = -1;
    private boolean returnAfterShot = false;
    private final ReactApplicationContext mReactContext;

    private Compression compression = new Compression();
    private ReadableMap options;

    PickerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mReactContext = reactContext;
        reactContext.addActivityEventListener(this);

    }

    @Override
    public String getName() {
        return "ImageCropPicker";
    }

    private void setConfiguration(final ReadableMap options) {
        watermark = options.hasKey("watermark") && options.getBoolean("watermark");
        address = options.hasKey("address") ? options.getString("address") : "";


        multiple = options.hasKey("multiple") && options.getBoolean("multiple");
        isCamera = options.hasKey("isCamera") && options.getBoolean("isCamera");
        openCameraOnStart = options.hasKey("openCameraOnStart") && options.getBoolean("openCameraOnStart");
        width = options.hasKey("width") ? options.getInt("width") : width;
        height = options.hasKey("height") ? options.getInt("height") : height;
        maxSize = options.hasKey("maxSize") ? options.getInt("maxSize") : maxSize;
        imgmaxSize = options.hasKey("imgmaxSize") ? options.getInt("imgmaxSize") : imgmaxSize;
        cropping = options.hasKey("cropping") ? options.getBoolean("cropping") : cropping;
        includeBase64 = options.hasKey("includeBase64") && options.getBoolean("includeBase64");
        compressQuality = options.hasKey("compressQuality") ? options.getInt("compressQuality") : compressQuality;
        title = options.hasKey("title") ? options.getString("title") : title;
        returnAfterShot = options.hasKey("returnAfterShot") && options.getBoolean("returnAfterShot");
        isVideo = options.hasKey("isVideo") && options.getBoolean("isVideo");
        isHidePreview = options.hasKey("isHidePreview") && options.getBoolean("isHidePreview");
        isHideVideoPreview = options.hasKey("isHideVideoPreview") && options.getBoolean("isHideVideoPreview");
        isPlayGif = options.hasKey("isPlayGif") && options.getBoolean("isPlayGif");

        imageLoader = options.hasKey("imageLoader") ? options.getString("imageLoader") : imageLoader;
        this.options = options;
    }

    private static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }

        return type;
    }

    private WritableMap getAsyncSelection(final Activity activity, String path) throws Exception {
        String mime = getMimeType(path);
        if (mime != null && mime.startsWith("video/")) {
            return getVideo(activity, path, mime);
        }

        return getImage(activity, path);
    }

    private WritableMap getAsyncSelection(final Activity activity, ImageCropBean result) throws Exception {

        String path = result.getOriginalPath();
        return getAsyncSelection(activity, path);
    }

    private WritableMap getAsyncSelection(final Activity activity, MediaBean result) throws Exception {

        String path = result.getOriginalPath();
        FileInputStream fis = new FileInputStream(path);
        Bitmap bitmap = BitmapFactory.decodeStream(fis);


        //质量处理
        ByteArrayInputStream isBm = compressImage(bitmap);
        //取代原照片
        setnewpath(isBm, path);
        return getAsyncSelection(activity, path);


    }

    private WritableMap getAsyncSelection2(final Activity activity, String path) throws Exception {
//        String path = result.getOriginalPath();
        return getImage(activity, path);
    }

    private WritableMap getVideo(Activity activity, String path, String mime) throws Exception {
        Bitmap bmp = validateVideo(path);

        WritableMap video = new WritableNativeMap();
        video.putInt("width", bmp.getWidth());
        video.putInt("height", bmp.getHeight());
        video.putString("mime", mime);
        video.putInt("size", (int) new File(path).length());
        video.putString("path", "file://" + path);

        return video;
    }

    private WritableMap getImage(final Activity activity, String path) throws Exception {
        WritableMap image = new WritableNativeMap();
        if (path.startsWith("http://") || path.startsWith("https://")) {
            throw new Exception("Cannot select remote files");
        }
        validateImage(path);

        // if compression options are provided image will be compressed. If none options is provided,
        // then original image will be returned
        File compressedImage = compression.compressImage(activity, options, path);
        String compressedImagePath = compressedImage.getPath();
        BitmapFactory.Options options = validateImage(compressedImagePath);

        image.putString("path", "file://" + path);
        image.putInt("width", options.outWidth);
        image.putInt("height", options.outHeight);
        image.putString("mime", options.outMimeType);
        image.putInt("size", (int) new File(compressedImagePath).length());
        Log.d("aaa", "---999");
        if (includeBase64) {
            image.putString("data", getBase64StringFromFile(compressedImagePath));
        }

        return image;
    }

    private WritableMap getImage(final Activity activity, ImageCropBean result) throws Exception {

        String path = result.getOriginalPath();
        return getImage(activity, path);
    }

    private WritableMap getImage(final Activity activity, MediaBean result) throws Exception {

        String path = result.getOriginalPath();
        return getImage(activity, path);
    }

    private void initImageLoader(Activity activity) {

        ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(activity);
        config.threadPriority(Thread.NORM_PRIORITY - 2);
        config.denyCacheImageMultipleSizesInMemory();
        config.diskCacheFileNameGenerator(new Md5FileNameGenerator());
        config.diskCacheSize(50 * 1024 * 1024); // 50 MiB
        config.tasksProcessingOrder(QueueProcessingType.LIFO);
        ImageLoader.getInstance().init(config.build());
    }

    ImageCropBean result;

    public void openCamera() {
//        rxGalleryFinal
//                .radio()
//                .subscribe(new RxBusResultDisposable<ImageRadioResultEvent>() {
//                    @Override
//                    protected void onEvent(ImageRadioResultEvent imageRadioResultEvent) throws Exception {
//                        //拍照水印处理
//                        result = imageRadioResultEvent.getResult();
//                    }
//                })
//                .openGallery();


        Uri myimageUri = Uri.parse("file://" + img_savepath + System.currentTimeMillis()
                + ".jpg");
        imageUri = myimageUri.toString();
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, myimageUri);
        activity.startActivityForResult(cameraIntent, TAKE_IMAGE_REQUEST_CODE);

    }

    String imageUri;
    private final int TAKE_IMAGE_REQUEST_CODE = 1001;
    private final int crop_REQUEST_CODE = 1002;

    public static final String SDCARD = Environment
            .getExternalStorageDirectory().toString();
    private String img_savepath = cacheDir + "/";
    public static final String appDirName = "mogu";
    public static final String cacheDir = SDCARD + "/" + appDirName + "/"
            + "CameraPic";


    private Bitmap createWatermark(Bitmap target, String mark) {
        int w = target.getWidth();
        int h = target.getHeight();

        Log.d("aaa", w + "----w-");
        Log.d("aaa", h + "----h-");

        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        Paint p = new Paint();

        // 水印的颜色
        p.setColor(Color.WHITE);

        // 水印的字体大小
        float texts = w;
        if (h < w) {
            texts = h;
        }
        p.setTextSize(texts / 20);

        p.setAntiAlias(true);// 去锯齿

        canvas.drawBitmap(target, 0, 0, p);

        // 在左边的中间位置开始添加水印
        float drawy = h - texts / 15;
        float drawx = w / 15;
//        if(h<w){
//            drawx=w/10;
//        }
        canvas.drawText(mark, drawx, (drawy - texts / 15), p);
        canvas.drawText(address, drawx, drawy, p);

        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();

        return bmp;
    }

    private void setnewpath(ByteArrayInputStream bis, String path) {

        File avaterFile = new File(path);//设置文件名称

        if (avaterFile.exists()) {
            avaterFile.delete();
        }
        try {
            avaterFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(avaterFile);


            byte[] buf = new byte[1024];
            long len = 0;
            long start = System.currentTimeMillis();
            while ((len = bis.read(buf, 0, buf.length)) != -1) {
                fos.write(buf);
            }
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void posturl(String path) {
        try {
            WritableArray resultArr = new WritableNativeArray();
            resultArr.pushMap(getAsyncSelection2(activity, path));
            mPickerPromise.resolve(resultArr);
        } catch (Exception e) {
        }


    }

    public void getBitmap(String path) {

        try {
            if (path.startsWith("file://")) {
                path = path.substring(7, path.length());
                Log.d("aaa", "--p-" + path);
            }

            if (!watermark) {
                posturl(path);
                return;
            }

            FileInputStream fis = new FileInputStream(path);
            Bitmap bitmap = BitmapFactory.decodeStream(fis);

            if (bitmap == null) {
                return;
            }
            //水印----日期生成
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy年MM月dd日   HH:mm:ss");
            Date curDate = new Date(System.currentTimeMillis());
            String str = formatter.format(curDate);
            //水印----日期生成 end

            //带水印Bitmap
            Bitmap newbitmap = createWatermark(bitmap, str);
            //质量处理
            ByteArrayInputStream isBm = compressImage(newbitmap);
            //取代原照片
            setnewpath(isBm, path);

            Log.d("aaa", path);
            posturl(path);

        } catch (Exception e) {
        }

    }


    public ByteArrayInputStream compressImage(Bitmap image) {
        Log.d("aaa", imgmaxSize + "----");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int options = compressQuality;
        image.compress(Bitmap.CompressFormat.JPEG, options, baos);// 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        while (baos.toByteArray().length / 1024 > imgmaxSize) { // 循环判断如果压缩后图片是否大于100kb,大于继续压缩
            baos.reset(); // 重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);// 这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;// 每次都减少10
        }
        Log.d("aaa", baos.toByteArray().length / 1024 + "----" + options);


        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());// 把压缩后的数据baos存放到ByteArrayInputStream中
//        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);// 把ByteArrayInputStream数据生成图片
        return isBm;
    }

    private void cropImageUri(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("scale", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        intent.putExtra("return-data", false);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true); // no face detection
        activity.startActivityForResult(intent, crop_REQUEST_CODE);
    }

    @Override
    public void getURL(String url) {
        mPickerPromise.resolve(url);

    }


    /**
     * init主题
     * end
     *
     * @return
     */
    private int uCropStatusColor;
    private int uCropToolbarColor;
    private int uCropActivityWidgetColor;
    private int uCropToolbarWidgetColor;
    private String uCropTitle;
    private String requestStorageAccessPermissionTips;

    public void setTheme() {
        uCropStatusColor = ThemeUtils.resolveColor(activity, cn.finalteam.rxgalleryfinal.R.attr.gallery_ucrop_status_bar_color, cn.finalteam.rxgalleryfinal.R.color.gallery_default_ucrop_color_widget_active);
        uCropToolbarColor = ThemeUtils.resolveColor(activity, cn.finalteam.rxgalleryfinal.R.attr.gallery_ucrop_toolbar_color, cn.finalteam.rxgalleryfinal.R.color.gallery_default_ucrop_color_widget_active);
        uCropActivityWidgetColor = ThemeUtils.resolveColor(activity, cn.finalteam.rxgalleryfinal.R.attr.gallery_ucrop_activity_widget_color, cn.finalteam.rxgalleryfinal.R.color.gallery_default_ucrop_color_widget);
        uCropToolbarWidgetColor = ThemeUtils.resolveColor(activity, cn.finalteam.rxgalleryfinal.R.attr.gallery_ucrop_toolbar_widget_color, cn.finalteam.rxgalleryfinal.R.color.gallery_default_toolbar_widget_color);
        uCropTitle = ThemeUtils.resolveString(activity, cn.finalteam.rxgalleryfinal.R.attr.gallery_ucrop_toolbar_title, cn.finalteam.rxgalleryfinal.R.string.gallery_title_cut);
        int pageColor = ThemeUtils.resolveColor(activity, cn.finalteam.rxgalleryfinal.R.attr.gallery_page_bg, cn.finalteam.rxgalleryfinal.R.color.gallery_default_page_bg);
//        mRlRootView.setBackgroundColor(pageColor);
        requestStorageAccessPermissionTips = ThemeUtils.resolveString(activity, cn.finalteam.rxgalleryfinal.R.attr.gallery_request_camera_permission_tips, cn.finalteam.rxgalleryfinal.R.string.gallery_default_camera_access_permission_tips);
    }

    private void setPostMediaBean(MediaBean mediaBean) {
        ImageCropBean bean = new ImageCropBean();
        bean.copyMediaBean(mediaBean);
        RxBus.getDefault().post(new ImageRadioResultEvent(bean));
    }

    private final String IMAGE_STORE_FILE_NAME = "IMG_%s.jpg";
    private static File mCropPath = null;
    private static File mImageStoreDir;
    private static File mImageStoreCropDir; //裁剪目录

    /**
     * init裁剪目录
     *
     * @return
     */
    public static File getImageStoreDirByFile() {
        return mImageStoreDir;
    }

    /**
     * getImageStoreDir
     *
     * @return 存储路径
     */
    public static String getImageStoreDirByStr() {
        if (mImageStoreDir != null)
            return mImageStoreDir.getPath();
        else
            return null;
    }

    public static void setImageStoreCropDir(File imgFile) {
        mImageStoreCropDir = imgFile;
        Logger.i("设置图片裁剪保存路径为：" + mImageStoreCropDir.getAbsolutePath());
    }

    public static File getImageStoreCropDirByFile() {
        return mImageStoreCropDir;
    }

    public static String getImageStoreCropDirByStr() {
        if (mImageStoreCropDir != null)
            return mImageStoreCropDir.getPath();
        else
            return null;
    }

    public void onLoadFile() {
        //没有的话就默认路径
        if (getImageStoreDirByFile() == null && getImageStoreDirByStr() == null) {
            mImageStoreDir = new File(Environment.getExternalStorageDirectory(), "/DCIM/IMMQY/");
            setImageStoreCropDir(mImageStoreDir);
        }
        if (!mImageStoreDir.exists()) {
            mImageStoreDir.mkdirs();
        }
        if (getImageStoreCropDirByFile() == null && getImageStoreCropDirByStr() == null) {
            mImageStoreCropDir = new File(mImageStoreDir, "crop");
            if (!mImageStoreCropDir.exists()) {
                mImageStoreCropDir.mkdirs();
            }
            setImageStoreCropDir(mImageStoreCropDir);
        }
    }

    private void radioNext(MediaBean mediaBean) {
//        mConfiguration=rxGalleryFinal.getConfiguration();
        setTheme();

//        Uri inputUri = Uri.fromFile(new File(mediaBean.getOriginalPath()));
//        Intent intent = new Intent(activity, UCropActivity.class);
//
//
//        // UCrop 参数 start
//        Bundle bundle = new Bundle();
//
//        bundle.putParcelable(UCrop.EXTRA_OUTPUT_URI, outUri);
//        bundle.putParcelable(UCrop.Options.EXTRA_ASPECT_RATIO_OPTIONS, mediaBean);
//        bundle.putInt(UCrop.Options.EXTRA_STATUS_BAR_COLOR, uCropStatusColor);
//        bundle.putInt(UCrop.Options.EXTRA_TOOL_BAR_COLOR, uCropToolbarColor);
//        bundle.putString(UCrop.Options.EXTRA_UCROP_TITLE_TEXT_TOOLBAR, uCropTitle);
//        bundle.putInt(UCrop.Options.EXTRA_UCROP_COLOR_WIDGET_ACTIVE, uCropActivityWidgetColor);
//        bundle.putInt(UCrop.Options.EXTRA_UCROP_WIDGET_COLOR_TOOLBAR, uCropToolbarWidgetColor);
////            bundle.putn(UCrop.Options.EXTRA_FREE_STYLE_CROP, mConfiguration.isFreestyleCropEnabled());
//        bundle.putParcelable(UCrop.EXTRA_INPUT_URI, inputUri);
//        // UCrop 参数 end
//
//        int bk = FileUtils.existImageDir(inputUri.getPath());
//        Logger.i("--->" + inputUri.getPath());
//        Logger.i("--->" + outUri.getPath());
//        ArrayList<AspectRatio> aspectRatioList = new ArrayList<>();
////            Aspo[]aspectRatios =  mConfiguration.getAspectRatio();
//        bundle.putParcelableArrayList(UCrop.Options.EXTRA_ASPECT_RATIO_OPTIONS, aspectRatioList);//EXTRA_CONFIGURATION
//        intent.putExtras(bundle);
//        if (bk != -1) {
//            //裁剪
//            activity.startActivity(intent);
////                startActivityForResult(intent, CROP_IMAGE_REQUEST_CODE);
//        } else {
//            Logger.w("点击图片无效");
//        }
//        }
    }

    Activity activity;

    @ReactMethod
    public void openPicker(final ReadableMap options, final Promise promise) {
        activity = getCurrentActivity();

        if (activity == null) {
            promise.reject(E_ACTIVITY_DOES_NOT_EXIST, "Activity doesn't exist");
            return;
        }

        setConfiguration(options);
        initImageLoader(activity);
        mPickerPromise = promise;

        RxGalleryFinal rxGalleryFinal = RxGalleryFinal.with(activity);
        if (openCameraOnStart) {
            rxGalleryFinal.openCameraOnStart();
        } else if (!isCamera) {
            rxGalleryFinal.hideCamera();
        }
        if (compressQuality > 0) {
            rxGalleryFinal.cropropCompressionQuality(compressQuality);
        }
        if (title != null) {
            rxGalleryFinal.setTitle(title);
        }
        if (returnAfterShot) {
            rxGalleryFinal.returnAfterShot();
        }
        if (isVideo) {
            rxGalleryFinal.video();
        } else {
            rxGalleryFinal.image();
        }
        if (isHidePreview) {
            rxGalleryFinal.hidePreview();
        }
        if (isHideVideoPreview) {
            rxGalleryFinal.videoPreview();
        }
        if (isPlayGif) {
            rxGalleryFinal.gif();
        }
        if (imageLoader != null) {
            switch (imageLoader) {
                case "PICASSO":
                    rxGalleryFinal.imageLoader(ImageLoaderType.PICASSO);
                    break;
                case "GLIDE":
                    rxGalleryFinal.imageLoader(ImageLoaderType.GLIDE);
                    break;
                case "FRESCO":
                    rxGalleryFinal.imageLoader(ImageLoaderType.FRESCO);
                    break;
                case "UNIVERSAL":
                    rxGalleryFinal.imageLoader(ImageLoaderType.UNIVERSAL);
                    break;
                default:
                    break;
            }
        } else {
            rxGalleryFinal.imageLoader(ImageLoaderType.GLIDE);
        }
        if (openCameraOnStart) {
            //拍照处理,直接跳去原生拍照
            openCamera();
            return;
        }
        if (!this.multiple) {
            if (cropping) {
                rxGalleryFinal.crop();
                rxGalleryFinal.cropMaxResultSize(this.width, this.height);
                //裁剪图片的回调
                RxGalleryListener
                        .getInstance()
                        .setRadioImageCheckedListener(
                                new IRadioImageCheckedListener() {
                                    @Override
                                    public void cropAfter(Object t) {
                                        WritableArray resultArr = new WritableNativeArray();
                                        try {
                                            resultArr.pushMap(getAsyncSelection(activity, t.toString()));
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        mPickerPromise.resolve(resultArr);
                                    }

                                    @Override
                                    public boolean isActivityFinish() {
                                        return true;
                                    }
                                });
            }
            rxGalleryFinal
                    .radio()
                    .subscribe(new RxBusResultDisposable<ImageRadioResultEvent>() {
                        @Override
                        protected void onEvent(ImageRadioResultEvent imageRadioResultEvent) throws Exception {
                            if (!cropping) {

                                ImageCropBean result = imageRadioResultEvent.getResult();
                                WritableArray resultArr = new WritableNativeArray();
                                resultArr.pushMap(getAsyncSelection(activity, result));
                                mPickerPromise.resolve(resultArr);
                            }
                        }
                    })
                    .openGallery();
        } else {
            rxGalleryFinal
                    .multiple()
                    .maxSize(maxSize)
                    .subscribe(new RxBusResultDisposable<ImageMultipleResultEvent>() {
                        @Override
                        protected void onEvent(ImageMultipleResultEvent imageMultipleResultEvent) throws Exception {
                            List<MediaBean> list = imageMultipleResultEvent.getResult();
                            WritableArray resultArr = new WritableNativeArray();
                            for (MediaBean bean : list) {
                                resultArr.pushMap(getAsyncSelection(activity, bean));
                            }
                            mPickerPromise.resolve(resultArr);
                        }

                        @Override
                        public void onComplete() {
                            super.onComplete();
                        }
                    })
                    .openGallery();
        }
    }

    private String getBase64StringFromFile(String absoluteFilePath) {
        InputStream inputStream;

        try {
            inputStream = new FileInputStream(new File(absoluteFilePath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        byte[] bytes;
        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        bytes = output.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }


    private BitmapFactory.Options validateImage(String path) throws Exception {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inDither = true;

        BitmapFactory.decodeFile(path, options);

        if (options.outMimeType == null || options.outWidth == 0 || options.outHeight == 0) {
            throw new Exception("Invalid image selected");
        }

        return options;
    }

    private Bitmap validateVideo(String path) throws Exception {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(path);
        Bitmap bmp = retriever.getFrameAtTime();

        if (bmp == null) {
            throw new Exception("Cannot retrieve video data");
        }

        return bmp;
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {


        if (requestCode == TAKE_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (!cropping) {
                //水印处理
                getBitmap(imageUri);
            } else {
                //裁剪
                crop2();
            }
            return;
        }

        if (requestCode == crop_REQUEST_CODE) {
            getBitmap(OUTUri.toString());
            return;
        }
    }

    Uri OUTUri;

    public void crop2() {

        setTheme();
//
        Uri inputUri = Uri.parse(imageUri);
        OUTUri = Uri.parse("file://" + img_savepath + System.currentTimeMillis()
                + ".jpg");

        Intent intent = new Intent(activity, UCropActivity.class);
        Bundle bundle = new Bundle();


        bundle.putParcelable(UCrop.EXTRA_OUTPUT_URI, OUTUri);
        bundle.putInt(UCrop.Options.EXTRA_STATUS_BAR_COLOR, uCropStatusColor);
        bundle.putInt(UCrop.Options.EXTRA_TOOL_BAR_COLOR, uCropToolbarColor);
        bundle.putString(UCrop.Options.EXTRA_UCROP_TITLE_TEXT_TOOLBAR, uCropTitle);
        bundle.putInt(UCrop.Options.EXTRA_UCROP_COLOR_WIDGET_ACTIVE, uCropActivityWidgetColor);
        bundle.putInt(UCrop.Options.EXTRA_UCROP_WIDGET_COLOR_TOOLBAR, uCropToolbarWidgetColor);
        bundle.putParcelable(UCrop.EXTRA_INPUT_URI, inputUri);
        // UCrop 参数 end

        int bk = FileUtils.existImageDir(inputUri.getPath());
//        ArrayList<AspectRatio> aspectRatioList = new ArrayList<>();
//        bundle.putParcelableArrayList(UCrop.Options.EXTRA_ASPECT_RATIO_OPTIONS, aspectRatioList);//EXTRA_CONFIGURATION
        intent.putExtras(bundle);
        if (bk != -1) {
            //裁剪
            activity.startActivityForResult(intent, crop_REQUEST_CODE);
        } else {
            Logger.w("点击图片无效");
        }
    }

    @Override
    public void onNewIntent(Intent intent) {

    }
}