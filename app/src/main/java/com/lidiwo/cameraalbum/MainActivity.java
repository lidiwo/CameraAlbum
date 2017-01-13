package com.lidiwo.cameraalbum;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity implements OnClickListener{

    private Button bt_take_phone;
    private Button bt_choose_from_album;
    private ImageView iv_picture;


    //回调标示
    private static  final int TAKE_PHONE=1;
    private static  final int CHOOSE_FORM_ALBUM=2;
    //权限标示
    private static  final int PERMISSION_WRITE_EXTERNAL_STORAGE=3;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initEvent();
    }

    private void initView() {
        bt_take_phone = (Button) findViewById(R.id.bt_take_phone);
        bt_choose_from_album = (Button) findViewById(R.id.bt_choose_from_album);
        iv_picture = (ImageView)findViewById(R.id.iv_picture);
    }

    private void initEvent() {
        bt_take_phone.setOnClickListener(this);
        bt_choose_from_album.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bt_take_phone:
                takePhone();
                break;
            case R.id.bt_choose_from_album:
                chooseFromAlbum();
                break;
        }
    }


    //调用相机拍照
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

    //调用相册图片
    private void chooseFromAlbum() {
        //权限检查
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            //没有权限，请求权限
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE},PERMISSION_WRITE_EXTERNAL_STORAGE);
        }else{
            //打开相册
            openAlbum();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PERMISSION_WRITE_EXTERNAL_STORAGE:
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    //用户给予授权，打开相册
                    openAlbum();
                }else{
                    Toast.makeText(MainActivity.this,"您没有给予授权",Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void openAlbum() {
        Intent intent=new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent,CHOOSE_FORM_ALBUM);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case TAKE_PHONE:
                //相机拍照回调
                if(resultCode==RESULT_OK){
                    try {
                        //将拍摄在照片显示出来
                        Bitmap bitmap=BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        iv_picture.setImageBitmap(bitmap);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                break;
            case CHOOSE_FORM_ALBUM:
                //从相册选取照片回调
                if(resultCode==RESULT_OK){
                    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT){
                        //Android4.4及以上版本
                        handleImageOnKitkat(data);
                    }else{
                        //Android4.4以下版本
                        handleImageBeforeKitkat(data);
                    }
                }
                break;
        }
    }

    @TargetApi(19)
    private void handleImageOnKitkat(Intent data) {
        String imagePath=null;
        Uri uri=data.getData();
        if(DocumentsContract.isDocumentUri(this,uri)){
            //如果是document类型的Uri，则通过document id处理
            String docId=DocumentsContract.getDocumentId(uri);

            if("com.android.providers.media.documents".equals(uri.getAuthority())){
                String id=docId.split(":")[1];//解析出数字格式的id
                String selection=MediaStore.Images.Media._ID+"="+id;

                imagePath=getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,selection);
            }else if("com.android.providers.downloads.documents".equals(uri.getAuthority())){
                Uri contentUri=ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),Long.valueOf(docId));
                imagePath=getImagePath(contentUri,null);
            }
        }else if("content".equalsIgnoreCase(uri.getScheme())){
            //如果是content类型的Uri，则使用普通方式处理
            imagePath=getImagePath(uri,null);
        }else if("file".equalsIgnoreCase(uri.getScheme())){
            //如果是file类型的Uri ，直接获取图片的路径即可
            imagePath=uri.getPath();
        }
        displayImage(imagePath);
    }


    private void handleImageBeforeKitkat(Intent data) {
        Uri uri=data.getData();
        String imagePath=getImagePath(uri,null);
        displayImage(imagePath);
    }


    //获取照片路径
    private String getImagePath(Uri uri, String selection) {
        String path=null;
        //通过Uri和 selection获取真实图片路径
        Cursor cursor=getContentResolver().query(uri,null,selection,null,null);
        if(cursor!=null){
            if(cursor.moveToFirst()){
                path=cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    //展示照片
    private void displayImage(String imagePath) {
        if(!TextUtils.isEmpty(imagePath)){
            Bitmap bitmap= BitmapFactory.decodeFile(imagePath);
            iv_picture.setImageBitmap(bitmap);
        }else{
           Toast.makeText(this,"错误的图片",Toast.LENGTH_SHORT).show();
        }
    }
}
