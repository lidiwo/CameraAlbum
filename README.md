# CameraAlbum

##说明
由于Android7.0私有目录被限制访问,"StrictMode API政策".之前Android版本中,是可以读取到手机存储中任何一个目录及文件,这带来很多安全问题.在Android7.0中为了提高私有文件的安全性.面向Android N或者更高版本将被限制访问.

##调用相机代码说明
1.调用相机拍照

    private void takePhone() {
        //创建一个File对象用于存储拍照后的照片
        File outputImage=new File(getExternalCacheDir(),"output_image.jpg");
        try{
            if(outputImage.exists()){
                outputImage.delete();
            }
            outputImage.createNewFile();
        }catch (Exception e){
            e.printStackTrace();
        }

        //判断Android版本是否是Android7.0以上
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
            imageUri= FileProvider.getUriForFile(MainActivity.this,"com.lidiwo.cameraalbum.fileprovider",outputImage);
        }else{
            imageUri=Uri.fromFile(outputImage);
        }

        //启动相机程序
        Intent intent=new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
        startActivityForResult(intent,TAKE_PHONE);
    }

2.注册存储照的容器


    <!--
    name:属性值，固定写法
    authorities:组件标识，按照江湖规矩,都以包名开头,避免和其它应用发生冲突。和FileProvider.getUriForFile()方法的第二个参数一致
    exported:要求必须为false，为true则会报安全异常。
    grantUriPermissions:true，表示授予 URI 临时访问权限。
    -->
    <provider
        android:name="android.support.v4.content.FileProvider"
        android:authorities="com.lidiwo.cameraalbum.fileprovider"
        android:exported="false"
        android:grantUriPermissions="true">

        <!--指定Uri的共享路径-->
        <meta-data
            android:name="android.support.FILE_PROVIDER_PATHS"
            android:resource="@xml/file_paths" />

    </provider>

3.创建file_paths文件
   在res目录创建一个xml文件夹，再在xml文件夹只创建一个file_paths.xml文件，文件内容如下：

    <?xml version="1.0" encoding="utf-8"?>
    <paths >
        <external-path name="my_image" path="" />

        <!--
        <files-path/>代表的根目录： Context.getFilesDir()
        <external-path/>代表的根目录: Environment.getExternalStorageDirectory()
        <cache-path/>代表的根目录: getCacheDir()

        name属性值可以随便填 path属性值表示共享的具体路径，设置为空，表示将整个SD卡目录进行共享，也可以设置共享的具体路径
        -->
    </paths>

